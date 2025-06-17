import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import desc, func
from typing import List
import redis
import json
import httpx
from datetime import datetime

from shared.database import get_db, create_tables
from shared.models import Score, Player
from models import ScoreCreate, ScoreResponse

app = FastAPI(title="Score Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Redis connection
redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

@app.on_event("startup")
def startup():
    create_tables()

def get_cached_data(key: str, expiry: int = 300):
    """Get data from Redis cache"""
    try:
        cached = redis_client.get(key)
        if cached:
            return json.loads(cached)
    except Exception as e:
        print(f"Redis get error: {e}")
    return None

def set_cached_data(key: str, data: any, expiry: int = 300):
    """Set data in Redis cache"""
    try:
        redis_client.setex(key, expiry, json.dumps(data, default=str))
    except Exception as e:
        print(f"Redis set error: {e}")

def invalidate_cache_pattern(pattern: str):
    """Invalidate cache keys matching pattern"""
    try:
        keys = redis_client.keys(pattern)
        if keys:
            redis_client.delete(*keys)
    except Exception as e:
        print(f"Redis invalidate error: {e}")

async def trigger_achievement_check(player_id: int):
    """Trigger achievement check in background"""
    try:
        async with httpx.AsyncClient() as client:
            await client.post(f"http://localhost:8004/api/achievements/check/{player_id}")
    except Exception as e:
        print(f"Failed to trigger achievement check: {e}")

@app.post("/api/scores", response_model=ScoreResponse)
def submit_score(score_data: ScoreCreate, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    # Check if player exists
    player = db.query(Player).filter(Player.id == score_data.player_id).first()
    if not player:
        raise HTTPException(status_code=404, detail="Player not found")
    
    # Create new score
    new_score = Score(
        player_id=score_data.player_id,
        game_mode=score_data.game_mode,
        score=score_data.score,
        created_at=datetime.utcnow()
    )
    
    db.add(new_score)
    db.commit()
    db.refresh(new_score)
    
    # Invalidate related caches
    invalidate_cache_pattern(f"scores:player:{score_data.player_id}*")
    invalidate_cache_pattern(f"leaderboard:*")
    invalidate_cache_pattern(f"player:stats:{score_data.player_id}")
    
    # Trigger achievement check in background
    background_tasks.add_task(trigger_achievement_check, score_data.player_id)
    
    return new_score

@app.get("/api/scores", response_model=List[ScoreResponse])
def get_all_scores(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    cache_key = f"scores:all:{skip}:{limit}"
    cached = get_cached_data(cache_key)
    
    if cached:
        return [ScoreResponse(**item) for item in cached]
    
    scores = db.query(Score).order_by(desc(Score.score)).offset(skip).limit(limit).all()
    result = [ScoreResponse.from_orm(score) for score in scores]
    
    # Cache for 2 minutes
    set_cached_data(cache_key, [score.dict() for score in result], 120)
    
    return result

@app.get("/api/scores/player/{player_id}", response_model=List[ScoreResponse])
def get_player_scores(player_id: int, skip: int = 0, limit: int = 50, db: Session = Depends(get_db)):
    cache_key = f"scores:player:{player_id}:{skip}:{limit}"
    cached = get_cached_data(cache_key)
    
    if cached:
        return [ScoreResponse(**item) for item in cached]
    
    scores = db.query(Score).filter(
        Score.player_id == player_id
    ).order_by(desc(Score.score)).offset(skip).limit(limit).all()
    
    result = [ScoreResponse.from_orm(score) for score in scores]
    
    # Cache for 5 minutes
    set_cached_data(cache_key, [score.dict() for score in result], 300)
    
    return result

@app.get("/api/scores/player/{player_id}/stats")
def get_player_stats(player_id: int, db: Session = Depends(get_db)):
    cache_key = f"scores:player:{player_id}:stats"
    cached = get_cached_data(cache_key)
    
    if cached:
        return cached
    
    stats = db.query(
        Score.game_mode,
        func.max(Score.score).label('best_score'),
        func.avg(Score.score).label('avg_score'),
        func.count(Score.id).label('total_games')
    ).filter(Score.player_id == player_id).group_by(Score.game_mode).all()
    
    result = [
        {
            "game_mode": stat.game_mode,
            "best_score": stat.best_score,
            "avg_score": round(float(stat.avg_score), 2),
            "total_games": stat.total_games
        }
        for stat in stats
    ]
    
    # Cache for 10 minutes
    set_cached_data(cache_key, result, 600)
    
    return result

@app.get("/health")
def health():
    redis_status = "connected"
    try:
        redis_client.ping()
    except:
        redis_status = "disconnected"
    
    return {
        "status": "healthy",
        "service": "score-service",
        "redis": redis_status
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8002"))
    uvicorn.run(app, host="0.0.0.0", port=port)