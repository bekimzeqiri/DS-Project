from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class ScoreCreate(BaseModel):
    player_id: int = Field(..., description="ID of the player")
    game_mode: str = Field(default="CLASSIC", description="Game mode (CLASSIC, ARCADE, etc.)")
    score: int = Field(..., ge=0, description="Score value (must be positive)")

class ScoreResponse(BaseModel):
    id: int
    player_id: int
    game_mode: str
    score: int
    created_at: datetime
    
    class Config:
        from_attributes = True

class PlayerBestScore(BaseModel):
    player_id: int
    game_mode: str
    best_score: int