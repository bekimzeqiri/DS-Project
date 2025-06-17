from pydantic import BaseModel
from typing import Optional

class LeaderboardEntry(BaseModel):
    player_id: int
    player_name: str
    best_score: int
    rank: int
    game_mode: str

class PlayerRank(BaseModel):
    player_id: int
    rank: Optional[int]
    game_mode: str