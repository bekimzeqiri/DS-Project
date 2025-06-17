import asyncio
import aiohttp
from typing import Dict, List
import logging
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)

class ServiceRegistry:
    def __init__(self):
        self.services: Dict[str, Dict] = {}
        self.health_check_interval = 30
        
    def register_service(self, name: str, host: str, port: int, health_endpoint: str = "/health"):
        service_info = {
            "host": host,
            "port": port,
            "health_endpoint": health_endpoint,
            "url": f"http://{host}:{port}",
            "last_health_check": datetime.utcnow(),
            "healthy": True
        }
        self.services[name] = service_info
        logger.info(f"Registered service: {name} at {service_info['url']}")
        
    def get_service(self, name: str) -> Dict:
        return self.services.get(name)
        
    def get_all_services(self) -> Dict[str, Dict]:
        return {name: info for name, info in self.services.items() if info["healthy"]}
        
    async def health_check(self, service_name: str, service_info: Dict) -> bool:
        try:
            url = f"{service_info['url']}{service_info['health_endpoint']}"
            async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=5)) as session:
                async with session.get(url) as response:
                    return response.status == 200
        except Exception as e:
            logger.warning(f"Health check failed for {service_name}: {e}")
            return False
            
    async def periodic_health_check(self):
        while True:
            for service_name, service_info in self.services.items():
                healthy = await self.health_check(service_name, service_info)
                service_info["healthy"] = healthy
                service_info["last_health_check"] = datetime.utcnow()
                
                if not healthy:
                    logger.warning(f"Service {service_name} is unhealthy")
                    
            await asyncio.sleep(self.health_check_interval)

# Global service registry instance
registry = ServiceRegistry()