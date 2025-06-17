# Create this file: score-service/models.py

from pydantic import BaseModel
from datetime import datetime
from typing import Optional

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