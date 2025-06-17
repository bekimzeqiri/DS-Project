import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import desc
from typing import List
from datetime import datetime

from shared.database import get_db, create_tables
from shared.models import Score, Player

app = FastAPI(title="Score Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Simple Pydantic models inline
from pydantic import BaseModel

class ScoreCreate(BaseModel):
    player_id: int
    game_mode: str = "CLASSIC"
    score: int

class ScoreResponse(BaseModel):
    id: int
    player_id: int
    game_mode: str
    score: int
    created_at: datetime
    
    class Config:
        from_attributes = True

@app.on_event("startup")
def startup():
    create_tables()

@app.post("/api/scores", response_model=ScoreResponse)
def submit_score(score: ScoreCreate, db: Session = Depends(get_db)):
    # Check if player exists
    player = db.query(Player).filter(Player.id == score.player_id).first()
    if not player:
        raise HTTPException(status_code=404, detail="Player not found")
    
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

@app.get("/api/scores", response_model=List[ScoreResponse])
def get_all_scores(db: Session = Depends(get_db)):
    scores = db.query(Score).order_by(desc(Score.score)).limit(100).all()
    return scores

@app.get("/health")
def health():
    return {"status": "healthy", "service": "score-service"}

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8002"))
    uvicorn.run(app, host="0.0.0.0", port=port)