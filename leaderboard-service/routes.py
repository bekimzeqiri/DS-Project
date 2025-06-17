import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List

from shared.database import get_db
from models import LeaderboardEntry, PlayerRank  # Change from .models to models

router = APIRouter(prefix="/api/leaderboard", tags=["leaderboard"])

@router.get("/global", response_model=List[LeaderboardEntry])
def get_global_leaderboard(limit: int = 10, db: Session = Depends(get_db)):
    query = text("""
        SELECT 
            p.id as player_id,
            p.display_name as player_name,
            MAX(s.score) as best_score,
            ROW_NUMBER() OVER (ORDER BY MAX(s.score) DESC) as rank,
            'GLOBAL' as game_mode
        FROM players p 
        JOIN scores s ON p.id = s.player_id 
        GROUP BY p.id, p.display_name 
        ORDER BY best_score DESC 
        LIMIT :limit
    """)
    
    result = db.execute(query, {"limit": limit})
    leaderboard = []
    
    for row in result:
        leaderboard.append(LeaderboardEntry(
            player_id=row.player_id,
            player_name=row.player_name,
            best_score=row.best_score,
            rank=row.rank,
            game_mode=row.game_mode
        ))
    
    return leaderboard

@router.get("/gamemode/{game_mode}", response_model=List[LeaderboardEntry])
def get_leaderboard_by_game_mode(game_mode: str, limit: int = 10, db: Session = Depends(get_db)):
    query = text("""
        SELECT 
            p.id as player_id,
            p.display_name as player_name,
            MAX(s.score) as best_score,
            ROW_NUMBER() OVER (ORDER BY MAX(s.score) DESC) as rank,
            :game_mode as game_mode
        FROM players p 
        JOIN scores s ON p.id = s.player_id 
        WHERE s.game_mode = :game_mode
        GROUP BY p.id, p.display_name 
        ORDER BY best_score DESC 
        LIMIT :limit
    """)
    
    result = db.execute(query, {"game_mode": game_mode, "limit": limit})
    leaderboard = []
    
    for row in result:
        leaderboard.append(LeaderboardEntry(
            player_id=row.player_id,
            player_name=row.player_name,
            best_score=row.best_score,
            rank=row.rank,
            game_mode=row.game_mode
        ))
    
    return leaderboard

@router.get("/player/{player_id}/rank", response_model=PlayerRank)
def get_player_rank(player_id: int, game_mode: str = "GLOBAL", db: Session = Depends(get_db)):
    if game_mode.upper() == "GLOBAL":
        query = text("""
            SELECT rank_table.rank FROM (
                SELECT 
                    p.id,
                    ROW_NUMBER() OVER (ORDER BY MAX(s.score) DESC) as rank
                FROM players p 
                JOIN scores s ON p.id = s.player_id 
                GROUP BY p.id
            ) rank_table 
            WHERE rank_table.id = :player_id
        """)
        params = {"player_id": player_id}
    else:
        query = text("""
            SELECT rank_table.rank FROM (
                SELECT 
                    p.id,
                    ROW_NUMBER() OVER (ORDER BY MAX(s.score) DESC) as rank
                FROM players p 
                JOIN scores s ON p.id = s.player_id 
                WHERE s.game_mode = :game_mode
                GROUP BY p.id
            ) rank_table 
            WHERE rank_table.id = :player_id
        """)
        params = {"player_id": player_id, "game_mode": game_mode}
    
    result = db.execute(query, params).first()
    rank = result.rank if result else None
    
    return PlayerRank(
        player_id=player_id,
        rank=rank,
        game_mode=game_mode
    )