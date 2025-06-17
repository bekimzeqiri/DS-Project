import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from shared.database import create_tables
from shared.service_registry import registry
from routes import router  # Change from .routes to routes

@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    host = os.getenv("SERVICE_HOST", "localhost")
    port = int(os.getenv("SERVICE_PORT", "8003"))
    registry.register_service("leaderboard-service", host, port)
    yield

app = FastAPI(
    title="Leaderboard Service",
    description="Service for generating leaderboards and rankings",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "leaderboard-service"}

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8003"))
    uvicorn.run(app, host="0.0.0.0", port=port)