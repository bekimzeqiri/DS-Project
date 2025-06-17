import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import httpx
import json

app = FastAPI(title="API Gateway")

# Enhanced CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

# Service registry
SERVICES = {
    "players": "http://localhost:8001",
    "scores": "http://localhost:8002", 
    "leaderboard": "http://localhost:8003",
    "achievements": "http://localhost:8004",
}

async def proxy_request(service_url: str, path: str, method: str, headers: dict, body: bytes = None, params: dict = None):
    """Proxy request to microservice with better error handling"""
    url = f"{service_url}{path}"
    
    # Filter out problematic headers
    clean_headers = {}
    for key, value in headers.items():
        if key.lower() not in ['host', 'content-length', 'connection']:
            clean_headers[key] = value
    
    async with httpx.AsyncClient(timeout=30.0, follow_redirects=True) as client:
        try:
            if method == "GET":
                response = await client.get(url, headers=clean_headers, params=params)
            elif method == "POST":
                response = await client.post(url, headers=clean_headers, content=body)
            elif method == "PUT":
                response = await client.put(url, headers=clean_headers, content=body)
            elif method == "DELETE":
                response = await client.delete(url, headers=clean_headers)
            else:
                raise HTTPException(status_code=405, detail="Method not allowed")
            
            return response
        except httpx.RequestError as e:
            raise HTTPException(status_code=503, detail=f"Service unavailable: {str(e)}")

def safe_json_response(response):
    """Safely parse JSON response with fallback handling"""
    try:
        # Check if response has content
        if not response.content or len(response.content) == 0:
            return JSONResponse(
                content={"message": "Success", "status": "completed"}, 
                status_code=response.status_code
            )
        
        # Try to parse as JSON
        content = response.json()
        return JSONResponse(content=content, status_code=response.status_code)
        
    except (json.JSONDecodeError, ValueError):
        # If JSON parsing fails, return the text content
        try:
            text_content = response.text
            return JSONResponse(
                content={"message": text_content, "raw_response": True}, 
                status_code=response.status_code
            )
        except:
            # Ultimate fallback
            return JSONResponse(
                content={"message": "Response received", "status_code": response.status_code}, 
                status_code=response.status_code
            )

# Player routes - enhanced with safe JSON handling
@app.get("/api/players")
async def get_players(request: Request):
    response = await proxy_request(
        SERVICES["players"], 
        "/api/players", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.post("/api/players")
async def create_player(request: Request):
    body = await request.body()
    print(f"🔄 Gateway: Creating player with body: {body.decode()}")
    
    try:
        response = await proxy_request(
            SERVICES["players"], 
            "/api/players", 
            "POST", 
            dict(request.headers),
            body
        )
        print(f"📡 Gateway: Received response - Status: {response.status_code}, Content: {response.content}")
        return safe_json_response(response)
        
    except Exception as e:
        print(f"❌ Gateway error: {e}")
        raise HTTPException(status_code=500, detail=f"Gateway error: {str(e)}")

@app.get("/api/players/{player_id}")
async def get_player(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["players"], 
        f"/api/players/{player_id}", 
        "GET", 
        dict(request.headers)
    )
    return safe_json_response(response)

@app.put("/api/players/{player_id}")
async def update_player(player_id: int, request: Request):
    body = await request.body()
    response = await proxy_request(
        SERVICES["players"], 
        f"/api/players/{player_id}", 
        "PUT", 
        dict(request.headers),
        body
    )
    return safe_json_response(response)

@app.delete("/api/players/{player_id}")
async def delete_player(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["players"], 
        f"/api/players/{player_id}", 
        "DELETE", 
        dict(request.headers)
    )
    return safe_json_response(response)

# Score routes
@app.get("/api/scores")
async def get_scores(request: Request):
    response = await proxy_request(
        SERVICES["scores"], 
        "/api/scores", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.post("/api/scores")
async def create_score(request: Request):
    body = await request.body()
    response = await proxy_request(
        SERVICES["scores"], 
        "/api/scores", 
        "POST", 
        dict(request.headers),
        body
    )
    return safe_json_response(response)

@app.get("/api/scores/player/{player_id}")
async def get_player_scores(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["scores"], 
        f"/api/scores/player/{player_id}", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.get("/api/scores/player/{player_id}/stats")
async def get_player_stats(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["scores"], 
        f"/api/scores/player/{player_id}/stats", 
        "GET", 
        dict(request.headers)
    )
    return safe_json_response(response)

# Leaderboard routes
@app.get("/api/leaderboard/global")
async def get_global_leaderboard(request: Request):
    response = await proxy_request(
        SERVICES["leaderboard"], 
        "/api/leaderboard/global", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.get("/api/leaderboard/gamemode/{game_mode}")
async def get_gamemode_leaderboard(game_mode: str, request: Request):
    response = await proxy_request(
        SERVICES["leaderboard"], 
        f"/api/leaderboard/gamemode/{game_mode}", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.get("/api/leaderboard/recent")
async def get_recent_scores(request: Request):
    response = await proxy_request(
        SERVICES["leaderboard"], 
        "/api/leaderboard/recent", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.get("/api/leaderboard/player/{player_id}/rank")
async def get_player_rank(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["leaderboard"], 
        f"/api/leaderboard/player/{player_id}/rank", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

# Achievement routes
@app.get("/api/achievements")
async def get_achievements(request: Request):
    response = await proxy_request(
        SERVICES["achievements"], 
        "/api/achievements", 
        "GET", 
        dict(request.headers)
    )
    return safe_json_response(response)

@app.get("/api/achievements/player/{player_id}")
async def get_player_achievements(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["achievements"], 
        f"/api/achievements/player/{player_id}", 
        "GET", 
        dict(request.headers)
    )
    return safe_json_response(response)

@app.post("/api/achievements/check/{player_id}")
async def check_achievements(player_id: int, request: Request):
    response = await proxy_request(
        SERVICES["achievements"], 
        f"/api/achievements/check/{player_id}", 
        "POST", 
        dict(request.headers)
    )
    return safe_json_response(response)

@app.get("/api/achievements/leaderboard")
async def get_achievement_leaderboard(request: Request):
    response = await proxy_request(
        SERVICES["achievements"], 
        "/api/achievements/leaderboard", 
        "GET", 
        dict(request.headers),
        params=dict(request.query_params)
    )
    return safe_json_response(response)

@app.get("/health")
async def health_check():
    """Check health of all services"""
    service_health = {}
    
    for service_name, service_url in SERVICES.items():
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{service_url}/health")
                service_health[service_name] = {
                    "status": "healthy" if response.status_code == 200 else "unhealthy",
                    "response_time": response.elapsed.total_seconds()
                }
        except Exception as e:
            service_health[service_name] = {
                "status": "unhealthy",
                "error": str(e)
            }
    
    # Determine overall health
    overall_status = "healthy" if all(
        service["status"] == "healthy" for service in service_health.values()
    ) else "degraded"
    
    return {
        "status": overall_status,
        "services": service_health,
        "gateway": "healthy"
    }

@app.get("/services")
def get_services():
    """Get available services"""
    return {
        "services": SERVICES,
        "version": "1.0.0",
        "features": ["achievements", "redis_caching", "real_time_stats"]
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("SERVICE_PORT", "8000"))
    uvicorn.run(app, host="0.0.0.0", port=port)