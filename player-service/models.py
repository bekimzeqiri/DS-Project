from pydantic import BaseModel, EmailStr
from datetime import datetime
from typing import Optional

class PlayerCreate(BaseModel):
    username: str
    email: str
    display_name: Optional[str] = None

class PlayerUpdate(BaseModel):
    username: Optional[str] = None
    email: Optional[str] = None
    display_name: Optional[str] = None

class PlayerResponse(BaseModel):
    id: int
    username: str
    email: str
    display_name: Optional[str] = None
    created_at: datetime
    last_active: datetime
    
    class Config:
        from_attributes = True