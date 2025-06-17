from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Boolean, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from datetime import datetime

Base = declarative_base()

class Player(Base):
    __tablename__ = "players"
    
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    display_name = Column(String(100))
    created_at = Column(DateTime, default=datetime.utcnow)
    last_active = Column(DateTime, default=datetime.utcnow)
    
    # Stats for achievements
    total_games = Column(Integer, default=0)
    total_score = Column(Integer, default=0)
    best_score = Column(Integer, default=0)
    current_streak = Column(Integer, default=0)
    longest_streak = Column(Integer, default=0)
    
    scores = relationship("Score", back_populates="player")
    achievements = relationship("PlayerAchievement", back_populates="player")

class Score(Base):
    __tablename__ = "scores"
    
    id = Column(Integer, primary_key=True, index=True)
    player_id = Column(Integer, ForeignKey("players.id"), nullable=False)
    game_mode = Column(String(50), default="CLASSIC")
    score = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    
    player = relationship("Player", back_populates="scores")

class Achievement(Base):
    __tablename__ = "achievements"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=False)
    icon = Column(String(50), default="🏆")
    points = Column(Integer, default=100)
    category = Column(String(50), default="GENERAL")
    
    # Achievement criteria
    criteria_type = Column(String(50))  # SCORE, GAMES, STREAK, etc.
    criteria_value = Column(Integer)
    
    players = relationship("PlayerAchievement", back_populates="achievement")

class PlayerAchievement(Base):
    __tablename__ = "player_achievements"
    
    id = Column(Integer, primary_key=True, index=True)
    player_id = Column(Integer, ForeignKey("players.id"), nullable=False)
    achievement_id = Column(Integer, ForeignKey("achievements.id"), nullable=False)
    unlocked_at = Column(DateTime, default=datetime.utcnow)
    progress = Column(Integer, default=0)  # For progressive achievements
    
    player = relationship("Player", back_populates="achievements")
    achievement = relationship("Achievement", back_populates="players")