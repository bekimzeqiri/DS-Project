from pydantic import BaseModel, EmailStr
from datetime import datetime
from typing import Optional

class PlayerCreate(BaseModel):
    username: str
    email: EmailStr
    display_name: Optional[str] = None

class PlayerUpdate(BaseModel):
    email: Optional[EmailStr] = None
    display_name: Optional[str] = None

class PlayerResponse(BaseModel):
    id: int
    username: str
    email: str
    display_name: Optional[str]
    created_at: datetime
    last_active: datetime
    
    class Config:
        from_attributes = True