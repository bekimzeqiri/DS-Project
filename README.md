# Distributed Leaderboard System

A comprehensive distributed microservices architecture for managing player leaderboards with real-time score tracking, anti-cheat detection, and global rankings.

## 🏗️ Architecture Overview

This system implements a microservices architecture with the following components:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Gateway   │────│  Discovery Service │────│  Load Balancer  │
│   (Port 8080)   │    │   (Port 8761)     │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │
    ┌────┴────┬─────────────┬─────────────┬──────────────┐
    │         │             │             │              │
┌───▼───┐ ┌──▼────┐ ┌──────▼──────┐ ┌───▼────┐ ┌──────▼──────┐
│Player │ │ Score │ │ Leaderboard │ │Notification│ │   Auth   │
│Service│ │Service│ │   Service   │ │  Service   │ │ Service  │
│:8081  │ │:8082  │ │    :8083    │ │   :8084    │ │ :8080   │
└───────┘ └───────┘ └─────────────┘ └────────────┘ └─────────┘
    │         │
┌───▼───┐ ┌──▼────┐
│MySQL  │ │MySQL  │
│Player │ │Score  │
│  DB   │ │  DB   │
└───────┘ └───────┘
```

## 🚀 Features

### Core Functionality
- **Player Management**: Registration, profiles, statistics
- **Score Submission**: Real-time score tracking with validation
- **Leaderboards**: Global, daily, weekly, monthly rankings
- **Anti-Cheat System**: Advanced fraud detection algorithms
- **Rate Limiting**: Prevents spam and abuse
- **Caching**: Redis-based performance optimization

### Technical Features
- **Microservices Architecture**: Independently deployable services
- **Service Discovery**: Eureka-based service registration
- **API Gateway**: Centralized routing and authentication
- **Database Per Service**: MySQL for persistence
- **Circuit Breakers**: Resilience4j for fault tolerance
- **Distributed Caching**: Redis for performance
- **JWT Authentication**: Secure API access

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Spring Boot 3.1.5+ |
| **Language** | Java 17 |
| **Database** | MySQL 8.0+ |
| **Cache** | Redis 6.0+ |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway |
| **Authentication** | JWT with Spring Security |
| **Build Tool** | Maven |
| **Containerization** | Docker (optional) |

## 📋 Prerequisites

Before running the system, ensure you have:

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Git**

## ⚡ Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/distributed-leaderboard-system.git
cd distributed-leaderboard-system
```

### 2. Database Setup

#### MySQL Setup
```sql
-- Create databases
CREATE DATABASE PlayerDb;
CREATE DATABASE ScoreDb;

-- Create user (update password as needed)
CREATE USER 'admin'@'localhost' IDENTIFIED BY 'Desingerica*1';
GRANT ALL PRIVILEGES ON PlayerDb.* TO 'admin'@'localhost';
GRANT ALL PRIVILEGES ON ScoreDb.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
```

#### Redis Setup
```bash
# Start Redis server
redis-server

# Or using Docker
docker run -d -p 6379:6379 redis:6-alpine
```

### 3. Service Startup Order

**Important**: Start services in this exact order for proper service discovery:

#### Step 1: Discovery Service
```bash
cd discovery_service
./mvnw spring-boot:run
```
Wait for "Eureka Server started" message (usually ~30 seconds)

#### Step 2: Core Services (in parallel)
```bash
# Terminal 2 - Player Service
cd player-service
./mvnw spring-boot:run

# Terminal 3 - Score Service  
cd score-service
./mvnw spring-boot:run

# Terminal 4 - Leaderboard Service
cd leaderboard-service
./mvnw spring-boot:run

# Terminal 5 - Notification Service
cd notification-service
./mvnw spring-boot:run
```

#### Step 3: API Gateway (last)
```bash
# Terminal 6 - API Gateway
cd api-gateway
./mvnw spring-boot:run
```

### 4. Verify System Health

Check that all services are running:

```bash
# Eureka Dashboard
http://localhost:8761

# API Gateway Health
curl http://localhost:8080/actuator/health

# Service Health Checks
curl http://localhost:8080/api/players/health
curl http://localhost:8080/api/scores/health
```

## 📖 API Documentation

### Authentication

First, obtain a JWT token:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player1",
    "password": "password1"
  }'
```

Use the returned token in subsequent requests:
```bash
Authorization: Bearer <your-jwt-token>
```

### Player Management

#### Register a New Player
```bash
curl -X POST http://localhost:8080/api/players/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newplayer",
    "email": "player@example.com",
    "password": "securepassword",
    "displayName": "New Player"
  }'
```

#### Get Player Profile
```bash
curl -X GET http://localhost:8080/api/players/{playerId} \
  -H "Authorization: Bearer <token>"
```

### Score Management

#### Submit a Score
```bash
curl -X POST http://localhost:8080/api/scores/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "playerId": 1,
    "leaderboardId": "global",
    "value": 1500,
    "type": "POINTS",
    "gameplayDurationMs": 120000,
    "platform": "web"
  }'
```

#### Get Top Scores
```bash
curl -X GET "http://localhost:8080/api/scores/leaderboard/global/top?limit=10"
```

### Leaderboard Access

#### Get Global Leaderboard
```bash
curl -X GET "http://localhost:8080/leaderboards/global?limit=10"
```

#### Get Player Rank
```bash
curl -X GET http://localhost:8080/leaderboards/global/rank/{playerId}
```

## 🔧 Configuration

### Application Ports

| Service | Port | Description |
|---------|------|-------------|
| Discovery Service | 8761 | Eureka Server |
| API Gateway | 8080 | Main entry point |
| Player Service | 8081 | Player management |
| Score Service | 8082 | Score tracking |
| Leaderboard Service | 8083 | Rankings |
| Notification Service | 8084 | Notifications |

### Database Configuration

Update database credentials in `application.properties`:

```properties
# Player Service
spring.datasource.url=jdbc:mysql://localhost:3306/PlayerDb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=YOUR_PASSWORD

# Score Service  
spring.datasource.url=jdbc:mysql://localhost:3306/ScoreDb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=YOUR_PASSWORD
```

### JWT Configuration

Update JWT secret in API Gateway `application.properties`:

```properties
jwt.secret=YOUR_SECURE_SECRET_KEY_HERE_MINIMUM_256_BITS
jwt.expiration=86400000
```

## 🧪 Testing

### Manual Testing

Test the complete flow:

```bash
# 1. Register a player
curl -X POST http://localhost:8080/api/players/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testplayer", "email": "test@example.com", "password": "password123"}'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testplayer", "password": "password123"}'

# 3. Submit a score
curl -X POST http://localhost:8080/api/scores/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"playerId": 1, "leaderboardId": "global", "value": 1000, "type": "POINTS"}'

# 4. Check leaderboard
curl -X GET "http://localhost:8080/leaderboards/global?limit=5"
```

### Running Unit Tests

```bash
# Test all services
./mvnw test

# Test specific service
cd player-service
./mvnw test
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Service Discovery Problems
**Symptom**: Services can't find each other
```bash
# Check Eureka dashboard
http://localhost:8761

# Restart services in order:
# 1. Discovery Service
# 2. All other services
# 3. API Gateway last
```

#### 2. Database Connection Errors
**Symptom**: "Unable to connect to database"
```bash
# Check MySQL is running
mysql -u admin -p

# Verify database exists
SHOW DATABASES;

# Check credentials in application.properties
```

#### 3. Port Conflicts
**Symptom**: "Port already in use"
```bash
# Check what's using the port
netstat -tulpn | grep :8080

# Kill the process
kill -9 <process-id>
```

#### 4. Redis Connection Issues
**Symptom**: Caching not working
```bash
# Check Redis is running
redis-cli ping

# Should return "PONG"
```

### Service Health Checks

```bash
# Check all service health
curl http://localhost:8080/actuator/health

# Individual service health
curl http://localhost:8081/actuator/health  # Player Service
curl http://localhost:8082/actuator/health  # Score Service
```

### Log Analysis

Service logs are available in:
```bash
# Player Service logs
tail -f player-service/logs/player-service.log

# Score Service logs  
tail -f score-service/logs/score-service.log

# API Gateway logs
tail -f api-gateway/logs/api-gateway.log
```

## 🔒 Security Features

### JWT Authentication
- **Token Expiration**: 24 hours (configurable)
- **Refresh Mechanism**: Automatic token renewal
- **Role-Based Access**: Admin vs Player permissions

### Anti-Cheat System
- **Statistical Analysis**: Outlier detection
- **Rate Limiting**: Prevents score spam
- **Gameplay Validation**: Duration and consistency checks
- **Suspicious Score Flagging**: Automatic review system

### Rate Limiting
- **Score Submissions**: 10 per minute per player
- **API Calls**: Configurable per endpoint
- **Circuit Breakers**: Automatic service protection

## 📊 Monitoring & Performance

### Metrics Available
- **Service Health**: `/actuator/health`
- **Application Metrics**: `/actuator/metrics`
- **Service Discovery**: Eureka Dashboard at `:8761`

### Performance Optimization
- **Redis Caching**: Leaderboards cached for 10 minutes
- **Database Indexing**: Optimized queries for leaderboards
- **Async Processing**: Non-blocking operations

## 🏆 Leaderboard Types

The system supports multiple leaderboard types:

| Type | Description | Reset Period |
|------|-------------|--------------|
| **global** | All-time best scores | Never |
| **daily** | Best scores in last 24 hours | Daily |
| **weekly** | Best scores in last 7 days | Weekly |  
| **monthly** | Best scores in last 30 days | Monthly |
| **seasonal** | Best scores in last 90 days | Quarterly |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Your Name**
- GitHub: [@bekimzeqiri](https://github.com/bekimzeqiri)
- Email: 12bekim21@gmail.com

## 🙏 Acknowledgments

- Spring Boot team for excellent documentation
- Netflix OSS for Eureka service discovery
- Redis team for robust caching solution
- MySQL team for reliable database engine

---

**Need Help?** Open an issue or check the troubleshooting section above.