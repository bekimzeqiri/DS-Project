import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import aiohttp
import asyncio
import json
from contextlib import asynccontextmanager

from service_discovery import discovery  # Change from .service_discovery to service_discovery

@asynccontextmanager
async def lifespan(app: FastAPI):
    health_task = asyncio.create_task(discovery.health_check_services())
    yield
    health_task.cancel()

app = FastAPI(
    title="API Gateway",
    description="Gateway for distributed leaderboard system",
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

async def proxy_request(service_name: str, path: str, method: str, request: Request):
    service_url = discovery.get_service_url(service_name)
    if not service_url:
        raise HTTPException(status_code=503, detail=f"Service {service_name} unavailable")
    
    target_url = f"{service_url}{path}"
    
    body = None
    if method in ["POST", "PUT", "PATCH"]:
        body = await request.body()
    
    headers = {k: v for k, v in request.headers.items() if k.lower() != "host"}
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.request(
                method=method,
                url=target_url,
                headers=headers,
                data=body,
                params=dict(request.query_params)
            ) as response:
                content = await response.read()
                
                return JSONResponse(
                    content=json.loads(content) if content else None,
                    status_code=response.status,
                    headers=dict(response.headers)
                )
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gateway error: {str(e)}")

@app.api_route("/api/players/{path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def player_service_proxy(path: str, request: Request):
    full_path = f"/api/players/{path}" if path else "/api/players"
    return await proxy_request("player-service", full_path, request.method, request)

@app.api_route("/api/players", methods=["GET", "POST"])
async def player_service_root(request: Request):
    return await proxy_request("player-service", "/api/players", request.method, request)

@app.api_route("/api/scores/{path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def score_service_proxy(path: str, request: Request):
    full_path = f"/api/scores/{path}" if path else "/api/scores"
    return await proxy_request("score-service", full_path, request.method, request)

@app.api_route("/api/scores", methods=["GET", "POST"])
async def score_service_root(request: Request):
    return await proxy_request("score-service", "/api/scores", request.method, request)

@app.api_route("/api/leaderboard/{path:path}", methods=["GET"])
async def leaderboard_service_proxy(path: str, request: Request):
    full_path = f"/api/leaderboard/{path}" if path else "/api/leaderboard"
    return await proxy_request("leaderboard-service", full_path, request.method, request)

@app.get("/health")
def gateway_health():
    return {"status": "healthy", "service": "api-gateway"}

@app.get("/services")
def get_services_status():
    return {
        "services": {
            name: [
                {
                    "url": f"http://{instance['host']}:{instance['port']}",
                    "healthy": instance["healthy"]
                }
                for instance in instances
            ]
            for name, instances in discovery.services.items()
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)