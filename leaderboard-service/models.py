from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class LeaderboardEntry(BaseModel):
    rank: int
    player_id: int
    display_name: str
    username: str
    best_score: int
    total_games: int
    avg_score: float

class PlayerRank(BaseModel):
    rank: int
    total_players: int
    best_score: int