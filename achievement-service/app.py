import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List
import redis
import json
from datetime import datetime, timedelta

from shared.database import get_db, create_tables
from shared.models import Achievement, PlayerAchievement, Player, Score

app = FastAPI(title="Achievement Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Redis connection with error handling
try:
    redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
    redis_client.ping()  # Test connection
except:
    redis_client = None
    print("⚠️  Redis not available - running without cache")

@app.on_event("startup")
def startup():
    create_tables()
    initialize_default_achievements()

def initialize_default_achievements():
    """Create default achievements if they don't exist"""
    db = next(get_db())
    
    default_achievements = [
        {
            "name": "First Steps",
            "description": "Submit your first score",
            "icon": "🎯",
            "points": 50,
            "category": "BEGINNER",
            "criteria_type": "GAMES",
            "criteria_value": 1
        },
        {
            "name": "Century Club",
            "description": "Score 100 points in a single game",
            "icon": "💯",
            "points": 100,
            "category": "SCORE",
            "criteria_type": "SCORE",
            "criteria_value": 100
        },
        {
            "name": "High Roller",
            "description": "Score 1000 points in a single game",
            "icon": "🔥",
            "points": 200,
            "category": "SCORE",
            "criteria_type": "SCORE",
            "criteria_value": 1000
        }
    ]
    
    for ach_data in default_achievements:
        existing = db.query(Achievement).filter(Achievement.name == ach_data["name"]).first()
        if not existing:
            achievement = Achievement(**ach_data)
            db.add(achievement)
    
    db.commit()

@app.get("/api/achievements")
def get_all_achievements(db: Session = Depends(get_db)):
    """Get all available achievements"""
    achievements = db.query(Achievement).all()
    return [
        {
            "id": ach.id,
            "name": ach.name,
            "description": ach.description,
            "icon": ach.icon,
            "points": ach.points,
            "category": ach.category,
            "criteria_type": ach.criteria_type,
            "criteria_value": ach.criteria_value
        }
        for ach in achievements
    ]

@app.get("/api/achievements/player/{player_id}")
def get_player_achievements(player_id: int, db: Session = Depends(get_db)):
    """Get all achievements for a player"""
    player_achievements = db.query(PlayerAchievement).filter(
        PlayerAchievement.player_id == player_id
    ).join(Achievement).all()
    
    return [
        {
            "id": pa.id,
            "player_id": pa.player_id,
            "achievement_id": pa.achievement_id,
            "achievement_name": pa.achievement.name,
            "achievement_description": pa.achievement.description,
            "achievement_icon": pa.achievement.icon,
            "achievement_points": pa.achievement.points,
            "unlocked_at": pa.unlocked_at,
            "progress": pa.progress
        }
        for pa in player_achievements
    ]

@app.post("/api/achievements/check/{player_id}")
def check_achievements(player_id: int, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    """Check and unlock achievements for a player"""
    background_tasks.add_task(process_achievements, player_id)
    return {"message": "Achievement check queued"}

def process_achievements(player_id: int):
    """Background task to process achievements"""
    db = next(get_db())
    
    try:
        player = db.query(Player).filter(Player.id == player_id).first()
        if not player:
            return
        
        # Get all achievements
        all_achievements = db.query(Achievement).all()
        
        # Get player's current achievements
        current_achievements = set([
            pa.achievement_id for pa in db.query(PlayerAchievement).filter(
                PlayerAchievement.player_id == player_id
            ).all()
        ])
        
        # Calculate player stats
        total_games = db.query(Score).filter(Score.player_id == player_id).count()
        best_score = db.query(func.max(Score.score)).filter(Score.player_id == player_id).scalar() or 0
        total_score = db.query(func.sum(Score.score)).filter(Score.player_id == player_id).scalar() or 0
        
        # Check each achievement
        new_achievements = []
        for achievement in all_achievements:
            if achievement.id in current_achievements:
                continue
                
            unlocked = False
            
            if achievement.criteria_type == "GAMES":
                unlocked = total_games >= achievement.criteria_value
            elif achievement.criteria_type == "SCORE":
                unlocked = best_score >= achievement.criteria_value
            elif achievement.criteria_type == "TOTAL_SCORE":
                unlocked = total_score >= achievement.criteria_value
            
            if unlocked:
                player_achievement = PlayerAchievement(
                    player_id=player_id,
                    achievement_id=achievement.id,
                    unlocked_at=datetime.utcnow()
                )
                db.add(player_achievement)
                new_achievements.append(achievement.name)
        
        db.commit()
        
        if new_achievements:
            print(f"Player {player_id} unlocked achievements: {new_achievements}")
            
    except Exception as e:
        print(f"Error processing achievements: {e}")
        db.rollback()

@app.get("/api/achievements/leaderboard")
def get_achievement_leaderboard(limit: int = 10, db: Session = Depends(get_db)):
    """Get top players by achievement points"""
    leaderboard = db.query(
        Player.id,
        Player.display_name,
        Player.username,
        func.count(PlayerAchievement.id).label('achievement_count'),
        func.coalesce(func.sum(Achievement.points), 0).label('total_points')
    ).outerjoin(
        PlayerAchievement, Player.id == PlayerAchievement.player_id
    ).outerjoin(
        Achievement, PlayerAchievement.achievement_id == Achievement.id
    ).group_by(Player.id).order_by(
        func.coalesce(func.sum(Achievement.points), 0).desc()
    ).limit(limit).all()
    
    return [
        {
            "rank": idx + 1,
            "player_id": row.id,
            "display_name": row.display_name,
            "username": row.username,
            "achievement_count": row.achievement_count,
            "total_points": int(row.total_points) if row.total_points else 0
        }
        for idx, row in enumerate(leaderboard)
    ]

@app.get("/health")
def health():
    redis_status = "connected" if redis_client else "disconnected"
    return {
        "status": "healthy",
        "service": "achievement-service",
        "redis": redis_status
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8004"))
    uvicorn.run(app, host="0.0.0.0", port=port)