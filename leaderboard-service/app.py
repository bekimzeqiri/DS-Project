import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Depends, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import desc, func
from typing import List, Optional
import redis
import json

from shared.database import get_db, create_tables
from shared.models import Score, Player
from models import LeaderboardEntry

app = FastAPI(title="Leaderboard Service")

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

@app.get("/api/leaderboard/global", response_model=List[LeaderboardEntry])
def get_global_leaderboard(
    limit: int = Query(10, le=100),
    game_mode: Optional[str] = None,
    db: Session = Depends(get_db)
):
    cache_key = f"leaderboard:global:{limit}:{game_mode or 'all'}"
    cached = get_cached_data(cache_key)
    
    if cached:
        return [LeaderboardEntry(**item) for item in cached]
    
    query = db.query(
        Player.id,
        Player.display_name,
        Player.username,
        func.max(Score.score).label('best_score'),
        func.count(Score.id).label('total_games'),
        func.avg(Score.score).label('avg_score')
    ).join(Score)
    
    if game_mode:
        query = query.filter(Score.game_mode == game_mode)
    
    leaderboard_data = query.group_by(Player.id).order_by(
        desc(func.max(Score.score))
    ).limit(limit).all()
    
    result = []
    for idx, row in enumerate(leaderboard_data):
        result.append(LeaderboardEntry(
            rank=idx + 1,
            player_id=row.id,
            display_name=row.display_name,
            username=row.username,
            best_score=row.best_score,
            total_games=row.total_games,
            avg_score=round(float(row.avg_score), 2) if row.avg_score else 0
        ))
    
    # Cache for 2 minutes (leaderboard changes frequently)
    set_cached_data(cache_key, [entry.dict() for entry in result], 120)
    
    return result

@app.get("/api/leaderboard/gamemode/{game_mode}", response_model=List[LeaderboardEntry])
def get_gamemode_leaderboard(
    game_mode: str,
    limit: int = Query(10, le=100),
    db: Session = Depends(get_db)
):
    return get_global_leaderboard(limit=limit, game_mode=game_mode, db=db)

@app.get("/api/leaderboard/player/{player_id}/rank")
def get_player_rank(player_id: int, game_mode: Optional[str] = None, db: Session = Depends(get_db)):
    cache_key = f"leaderboard:rank:{player_id}:{game_mode or 'all'}"
    cached = get_cached_data(cache_key)
    
    if cached:
        return cached
    
    # Get player's best score
    query = db.query(func.max(Score.score)).filter(Score.player_id == player_id)
    if game_mode:
        query = query.filter(Score.game_mode == game_mode)
    
    player_best = query.scalar()
    if not player_best:
        result = {"rank": None, "total_players": 0, "best_score": 0}
        set_cached_data(cache_key, result, 300)
        return result
    
    # Count players with better scores
    subquery = db.query(
        Player.id,
        func.max(Score.score).label('best_score')
    ).join(Score)
    
    if game_mode:
        subquery = subquery.filter(Score.game_mode == game_mode)
    
    better_players = subquery.group_by(Player.id).having(
        func.max(Score.score) > player_best
    ).count()
    
    # Get total players
    total_query = db.query(Player.id).join(Score)
    if game_mode:
        total_query = total_query.filter(Score.game_mode == game_mode)
    total_players = total_query.distinct().count()
    
    result = {
        "rank": better_players + 1,
        "total_players": total_players,
        "best_score": player_best
    }
    
    # Cache for 5 minutes
    set_cached_data(cache_key, result, 300)
    
    return result

@app.get("/api/leaderboard/recent", response_model=List[dict])
def get_recent_scores(limit: int = Query(20, le=50), db: Session = Depends(get_db)):
    cache_key = f"leaderboard:recent:{limit}"
    cached = get_cached_data(cache_key)
    
    if cached:
        return cached
    
    recent_scores = db.query(
        Score.score,
        Score.game_mode,
        Score.created_at,
        Player.display_name,
        Player.username
    ).join(Player).order_by(desc(Score.created_at)).limit(limit).all()
    
    result = [
        {
            "score": score.score,
            "game_mode": score.game_mode,
            "created_at": score.created_at.isoformat(),
            "display_name": score.display_name,
            "username": score.username
        }
        for score in recent_scores
    ]
    
    # Cache for 1 minute (recent activity changes frequently)
    set_cached_data(cache_key, result, 60)
    
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
        "service": "leaderboard-service",
        "redis": redis_status
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8003"))
    uvicorn.run(app, host="0.0.0.0", port=port)