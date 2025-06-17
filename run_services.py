#!/usr/bin/env python3
import subprocess
import sys
import os
import time

def run_service(service_dir, port):
    env = os.environ.copy()
    env['SERVICE_PORT'] = str(port)
    env['PYTHONPATH'] = os.path.dirname(os.path.abspath(__file__))
    
    return subprocess.Popen(
        [sys.executable, 'app.py'],
        cwd=service_dir,
        env=env
    )

def check_redis():
    """Check if Redis is running"""
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        print("✅ Redis is running")
        return True
    except Exception as e:
        print(f"❌ Redis is not running: {e}")
        print("Please start Redis with: redis-server")
        return False

def main():
    # Ensure we're in the main project directory
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    # Check Redis first
    if not check_redis():
        print("\n🚨 Please start Redis before running the services:")
        print("   redis-server")
        return
    
    services = [
        {'name': 'player-service', 'port': 8001},
        {'name': 'score-service', 'port': 8002},
        {'name': 'leaderboard-service', 'port': 8003},
        {'name': 'achievement-service', 'port': 8004},
        {'name': 'api-gateway', 'port': 8000},
    ]
    
    processes = []
    
    try:
        for service in services:
            print(f"🚀 Starting {service['name']} on port {service['port']}...")
            process = run_service(service['name'], service['port'])
            processes.append(process)
            time.sleep(3)
        
        print("\n🎯 All services started successfully!")
        print("📡 API Gateway: http://localhost:8000")
        print("👥 Player Service: http://localhost:8001")
        print("🎮 Score Service: http://localhost:8002")
        print("🏆 Leaderboard Service: http://localhost:8003")
        print("🏅 Achievement Service: http://localhost:8004")
        print("\n🎨 Open leaderboard.html in your browser to start!")
        print("Press Ctrl+C to stop all services")
        
        # Wait for all processes
        for process in processes:
            process.wait()
            
    except KeyboardInterrupt:
        print("\n🛑 Shutting down services...")
        for process in processes:
            process.terminate()
        print("✅ All services stopped")

if __name__ == "__main__":
    main()