import aiohttp
import asyncio
from typing import Dict, Optional
import random
import logging

logger = logging.getLogger(__name__)

class ServiceDiscovery:
    def __init__(self):
        self.services = {
            "player-service": [
                {"host": "localhost", "port": 8001, "healthy": True},
            ],
            "score-service": [
                {"host": "localhost", "port": 8002, "healthy": True},
            ],
            "leaderboard-service": [
                {"host": "localhost", "port": 8003, "healthy": True},
            ]
        }
    
    def get_service_url(self, service_name: str) -> Optional[str]:
        if service_name not in self.services:
            return None
        
        # Get healthy instances
        healthy_instances = [
            instance for instance in self.services[service_name] 
            if instance["healthy"]
        ]
        
        if not healthy_instances:
            logger.warning(f"No healthy instances for {service_name}")
            return None
        
        # Simple round-robin (or random selection)
        instance = random.choice(healthy_instances)
        return f"http://{instance['host']}:{instance['port']}"
    
    async def health_check_services(self):
        while True:
            for service_name, instances in self.services.items():
                for instance in instances:
                    try:
                        url = f"http://{instance['host']}:{instance['port']}/health"
                        async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=5)) as session:
                            async with session.get(url) as response:
                                instance["healthy"] = response.status == 200
                    except Exception as e:
                        logger.warning(f"Health check failed for {service_name}: {e}")
                        instance["healthy"] = False
            
            await asyncio.sleep(30)  # Check every 30 seconds

# Global service discovery instance
discovery = ServiceDiscovery()