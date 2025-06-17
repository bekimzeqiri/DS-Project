import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from typing import List

from shared.database import get_db
from shared.models import Score, Player
from models import ScoreCreate, ScoreResponse, PlayerBestScore

router = APIRouter(prefix="/api/scores", tags=["scores"])

@router.post("/", response_model=ScoreResponse, status_code=status.HTTP_201_CREATED)
def submit_score(score: ScoreCreate, db: Session = Depends(get_db)):
    # Check if player exists
    player = db.query(Player).filter(Player.id == score.player_id).first()
    if not player:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Player not found"
        )
    
    # Create new score
    db_score = Score(
        player_id=score.player_id,
        game_mode=score.game_mode,
        score=score.score
    )
    db.add(db_score)
    db.commit()
    db.refresh(db_score)
    return db_score

@router.get("/", response_model=List[ScoreResponse])
def get_all_scores(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    scores = db.query(Score).order_by(desc(Score.score)).offset(skip).limit(limit).all()
    return scores

@router.get("/player/{player_id}", response_model=List[ScoreResponse])
def get_player_scores(player_id: int, db: Session = Depends(get_db)):
    scores = db.query(Score).filter(Score.player_id == player_id).order_by(desc(Score.score)).all()
    return scores

@router.get("/gamemode/{game_mode}", response_model=List[ScoreResponse])
def get_scores_by_game_mode(game_mode: str, db: Session = Depends(get_db)):
    scores = db.query(Score).filter(Score.game_mode == game_mode).order_by(desc(Score.score)).all()
    return scores

@router.get("/player/{player_id}/best")
def get_player_best_score(player_id: int, game_mode: str = "CLASSIC", db: Session = Depends(get_db)):
    best_score = db.query(func.max(Score.score)).filter(
        Score.player_id == player_id,
        Score.game_mode == game_mode
    ).scalar()
    
    if best_score is None:
        best_score = 0
    
    return PlayerBestScore(
        player_id=player_id,
        game_mode=game_mode,
        best_score=best_score
    )

@router.get("/player/{player_id}/stats")
def get_player_stats(player_id: int, db: Session = Depends(get_db)):
    stats = db.query(
        Score.game_mode,
        func.max(Score.score).label('best_score'),
        func.avg(Score.score).label('avg_score'),
        func.count(Score.id).label('total_games')
    ).filter(Score.player_id == player_id).group_by(Score.game_mode).all()
    
    return [
        {
            "game_mode": stat.game_mode,
            "best_score": stat.best_score,
            "avg_score": round(float(stat.avg_score), 2),
            "total_games": stat.total_games
        }
        for stat in stats
    ]