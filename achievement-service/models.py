from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class AchievementCreate(BaseModel):
    name: str
    description: str
    icon: str = "🏆"
    points: int = 100
    category: str = "GENERAL"
    criteria_type: str
    criteria_value: int

class AchievementResponse(BaseModel):
    id: int
    name: str
    description: str
    icon: str
    points: int
    category: str
    criteria_type: str
    criteria_value: int
    
    class Config:
        from_attributes = True

class PlayerAchievementResponse(BaseModel):
    id: int
    player_id: int
    achievement_id: int
    achievement_name: str
    achievement_description: str
    achievement_icon: str
    achievement_points: int
    unlocked_at: datetime
    progress: int = 0