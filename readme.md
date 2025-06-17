#  Distributed Gaming Leaderboard System

A microservices-based leaderboard system with a beautiful web interface for tracking gaming scores and player rankings.

##  Architecture

This system uses a distributed microservices architecture:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │  Player Service │
│  (HTML/JS)      │◄──►│    :8000        │◄──►│     :8001       │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                │                        │
                       ┌─────────────────┐    ┌─────────────────┐
                       │ Score Service   │    │Leaderboard Svc  │
                       │     :8002       │    │     :8003       │
                       │                 │    │                 │
                       └─────────────────┘    └─────────────────┘
                                │                        │
                                └────────┬───────────────┘
                                         │
                                ┌─────────────────┐
                                │   SQLite DB     │
                                │ leaderboard.db  │
                                └─────────────────┘
```

##  Features

###  Core Functionality
- **Player Management**: Create, update, and manage player profiles
- **Score Tracking**: Submit and track scores across different game modes
- **Real-time Leaderboards**: View rankings globally or by game mode
- **Multiple Game Modes**: Support for Classic, Arcade, and custom modes

###  Microservices
- **API Gateway**: Routes requests and handles CORS
- **Player Service**: Manages player profiles and authentication
- **Score Service**: Handles score submission and retrieval
- **Leaderboard Service**: Generates rankings and statistics

##  Prerequisites

- Python 3.8+
- Virtual environment (recommended)

##  Installation

### 1. Clone the Repository

### 2. Create Virtual Environment (recommended)

### 3. Install Dependencies
```bash
pip install fastapi uvicorn sqlalchemy pydantic[email] aiohttp
```

### 4. Project Structure
```
DistributedProject/
├── shared/
│   ├── __init__.py
│   ├── database.py          # Database configuration
│   ├── models.py            # SQLAlchemy models
│   └── service_registry.py  # Service discovery
├── player-service/
│   ├── __init__.py
│   ├── app.py             # Player service main app
│   ├── routes.py          # Player API endpoints
│   └── models.py          # Pydantic models
├── score-service/
│   ├── __init__.py
│   ├── app.py             # Score service main app
│   ├── routes.py          # Score API endpoints
│   └── models.py          # Pydantic models
├── leaderboard-service/
│   ├── __init__.py
│   ├── app.py             # Leaderboard service main app
│   ├── routes.py          # Leaderboard API endpoints
│   └── models.py          # Pydantic models
├── api-gateway/
│   ├── __init__.py
│   ├── app.py             # API Gateway main app
│   └── service_discovery.py # Service discovery logic
├── run_services.py        # Service startup script
├── leaderboard.html       # Frontend interface
└── README.md
```

##  Running the Application

### 1. Start All Services
```bash
python run_services.py
```

This will start:
- **API Gateway**: http://localhost:8000
- **Player Service**: http://localhost:8001
- **Score Service**: http://localhost:8002
- **Leaderboard Service**: http://localhost:8003


### 2. Test features
1. **Add Players**: Go to "Players" tab → "Add Player"
2. **Submit Scores**: Go to "Leaderboard" tab → "Submit Score"
3. **View Rankings**: Switch between Global, Classic, and Arcade modes

##  API Documentation

### Player Service (Port 8001)
- `POST /api/players` - Create a new player
- `GET /api/players` - Get all players
- `GET /api/players/{id}` - Get player by ID
- `PUT /api/players/{id}` - Update player
- `DELETE /api/players/{id}` - Delete player

### Score Service (Port 8002)
- `POST /api/scores` - Submit a new score
- `GET /api/scores` - Get all scores
- `GET /api/scores/player/{id}` - Get scores for a player
- `GET /api/scores/gamemode/{mode}` - Get scores by game mode

### Leaderboard Service (Port 8003)
- `GET /api/leaderboard/global` - Global leaderboard
- `GET /api/leaderboard/gamemode/{mode}` - Game mode leaderboard
- `GET /api/leaderboard/player/{id}/rank` - Get player rank

### API Gateway (Port 8000)
All above endpoints are accessible through the gateway at port 8000.

## Testing

### Manual API Testing
```bash
# Health check
curl http://localhost:8000/health

# Create a player
curl -X POST http://localhost:8000/api/players \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "email": "test@example.com", "display_name": "Test Player"}'

# Submit a score
curl -X POST http://localhost:8000/api/scores \
  -H "Content-Type: application/json" \
  -d '{"player_id": 1, "game_mode": "CLASSIC", "score": 1500}'

# Get leaderboard
curl "http://localhost:8000/api/leaderboard/global?limit=10"
```

### Frontend Testing
1. Open `leaderboard.html` in browser
2. Use the interface to add players and submit scores
3. Verify leaderboard updates


### Docker (Future Enhancement)
Each service includes a Dockerfile for containerization:
```bash
docker-compose up
```

### Game Modes
Currently supported:
- `CLASSIC`: Traditional gameplay
- `ARCADE`: Arcade-style gameplay
- Custom modes can be added easily

##  Future Enhancements

- [ ] User authentication and sessions
- [ ] Real-time multiplayer scores
- [ ] Achievement system
- [ ] Score history and analytics
- [ ] Admin dashboard
- [ ] Mobile app
- [ ] WebSocket support for live updates
- [ ] Caching layer with Redis
- [ ] Database migrations
- [ ] Automated testing suite