
import subprocess
import sys
import os
import time

def run_command(command, cwd=None):
    """Run a command and return success status"""
    try:
        subprocess.run(command, shell=True, check=True, cwd=cwd)
        return True
    except subprocess.CalledProcessError:
        return False

def install_dependencies():
    """Install dependencies for all services"""
    services = ['player-service', 'score-service', 'leaderboard-service', 'api-gateway']
    
    for service in services:
        print(f" Installing dependencies for {service}...")
        if run_command(f"pip install -r requirements.txt", cwd=service):
            print(f" Dependencies installed for {service}")
        else:
            print(f" Failed to install dependencies for {service}")

def start_services():
    """Start all services in development mode"""
    services = [
        {'name': 'player-service', 'port': 8001, 'dir': 'player-service'},
        {'name': 'score-service', 'port': 8002, 'dir': 'score-service'},
        {'name': 'leaderboard-service', 'port': 8003, 'dir': 'leaderboard-service'},
        {'name': 'api-gateway', 'port': 8000, 'dir': 'api-gateway'},
    ]
    
    processes = []
    
    for service in services:
        print(f" Starting {service['name']} on port {service['port']}...")
        env = os.environ.copy()
        env['SERVICE_PORT'] = str(service['port'])
        
        process = subprocess.Popen(
            [sys.executable, 'app.py'],
            cwd=service['dir'],
            env=env
        )
        processes.append(process)
        time.sleep(2)  # Give each service time to start
    
    print("\n All services started!")
    print(" API Gateway: http://localhost:8000")
    print(" Player Service: http://localhost:8001")
    print(" Score Service: http://localhost:8002")
    print(" Leaderboard Service: http://localhost:8003")
    
    try:
        # Wait for all processes
        for process in processes:
            process.wait()
    except KeyboardInterrupt:
        print("\n Shutting down services...")
        for process in processes:
            process.terminate()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] == "install":
            install_dependencies()
        elif sys.argv[1] == "start":
            start_services()
        else:
            print("Usage: python setup.py [install|start]")
    else:
        print("Setting up development environment...")
        install_dependencies()
        print("\nTo start services, run: python setup.py start")