import requests
import json
import time

BASE_URL = "http://localhost:8000"

def test_complete_flow():
    print(" Testing Distributed Leaderboard System")
    
    # 1. Create players
    print("\n1️ Creating players...")
    players = [
        {"username": "alice", "email": "alice@example.com", "display_name": "Alice Wonder"},
        {"username": "bob", "email": "bob@example.com", "display_name": "Bob Builder"},
        {"username": "charlie", "email": "charlie@example.com", "display_name": "Charlie Brown"},
        {"username": "diana", "email": "diana@example.com", "display_name": "Diana Prince"}
    ]
    
    created_players = []
    for player in players:
        response = requests.post(f"{BASE_URL}/api/players", json=player)
        if response.status_code == 201:
            created_player = response.json()
            created_players.append(created_player)
            print(f" Created player: {created_player['display_name']} (ID: {created_player['id']})")
        else:
            print(f" Failed to create player {player['username']}: {response.text}")
    
    time.sleep(1)
    
    # 2. Submit scores
    print("\n2️ Submitting scores...")
    scores = [
        {"player_id": 1, "game_mode": "CLASSIC", "score": 1500},
        {"player_id": 1, "game_mode": "CLASSIC", "score": 1800},
        {"player_id": 1, "game_mode": "ARCADE", "score": 2200},
        {"player_id": 2, "game_mode": "CLASSIC", "score": 1200},
        {"player_id": 2, "game_mode": "ARCADE", "score": 2500},
        {"player_id": 3, "game_mode": "CLASSIC", "score": 1750},
        {"player_id": 3, "game_mode": "ARCADE", "score": 1900},
        {"player_id": 4, "game_mode": "CLASSIC", "score": 2100},
        {"player_id": 4, "game_mode": "ARCADE", "score": 2800},
    ]
    
    for score in scores:
        response = requests.post(f"{BASE_URL}/api/scores", json=score)
        if response.status_code == 201:
            submitted_score = response.json()
            print(f" Score submitted: Player {score['player_id']} - {score['score']} points in {score['game_mode']}")
        else:
            print(f" Failed to submit score: {response.text}")
    
    time.sleep(1)
    
    # 3. Get global leaderboard
    print("\n3️ Global Leaderboard:")
    response = requests.get(f"{BASE_URL}/api/leaderboard/global?limit=5")
    if response.status_code == 200:
        leaderboard = response.json()
        for entry in leaderboard:
            print(f"#{entry['rank']} {entry['player_name']} - {entry['best_score']} points")
    
    # 4. Get game mode specific leaderboard
    print("\n4️ ARCADE Mode Leaderboard:")
    response = requests.get(f"{BASE_URL}/api/leaderboard/gamemode/ARCADE?limit=5")
    if response.status_code == 200:
        leaderboard = response.json()
        for entry in leaderboard:
            print(f"#{entry['rank']} {entry['player_name']} - {entry['best_score']} points")
    
    # 5. Get player rank
    print("\n5️ Player Rankings:")
    for player in created_players:
        response = requests.get(f"{BASE_URL}/api/leaderboard/player/{player['id']}/rank?game_mode=GLOBAL")
        if response.status_code == 200:
            rank_data = response.json()
            rank = rank_data['rank'] if rank_data['rank'] else "Unranked"
            print(f"{player['display_name']}: Global Rank #{rank}")
    
    # 6. Get player stats
    print("\n6️ Player Statistics:")
    for player in created_players[:2]:  # Just first two players
        response = requests.get(f"{BASE_URL}/api/scores/player/{player['id']}/stats")
        if response.status_code == 200:
            stats = response.json()
            print(f"\n{player['display_name']} Stats:")
            for stat in stats:
                print(f"  {stat['game_mode']}: Best {stat['best_score']}, Avg {stat['avg_score']}, Games {stat['total_games']}")
    
    print("\n Testing completed!")

if __name__ == "__main__":
    # Wait a bit for services to start
    print("Waiting for services to start...")
    time.sleep(5)
    test_complete_flow()