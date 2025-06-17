#!/usr/bin/env python3
import subprocess
import sys
import os
import time

def run_service(service_dir, port):
    env = os.environ.copy()
    env['SERVICE_PORT'] = str(port)
    # Set PYTHONPATH to the main project directory
    env['PYTHONPATH'] = os.path.dirname(os.path.abspath(__file__))
    
    return subprocess.Popen(
        [sys.executable, 'app.py'],
        cwd=service_dir,
        env=env,
        # Important: Set working directory to main project folder
        # so all services create database in same location
    )

def main():
    # Ensure we're in the main project directory
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    services = [
        {'name': 'player-service', 'port': 8001},
        {'name': 'score-service', 'port': 8002},
        {'name': 'leaderboard-service', 'port': 8003},
        {'name': 'api-gateway', 'port': 8000},
    ]
    
    processes = []
    
    try:
        for service in services:
            print(f"🚀 Starting {service['name']} on port {service['port']}...")
            process = run_service(service['name'], service['port'])
            processes.append(process)
            time.sleep(3)
        
        print("\n🎯 All services started!")
        print("📡 API Gateway: http://localhost:8000")
        print("Press Ctrl+C to stop all services")
        
        for process in processes:
            process.wait()
            
    except KeyboardInterrupt:
        print("\n🛑 Shutting down services...")
        for process in processes:
            process.terminate()

if __name__ == "__main__":
    main()