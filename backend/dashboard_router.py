
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
import asyncio
import random
import logging
from typing import List

router = APIRouter(prefix="/api/dashboard", tags=["Dashboard Metrics"])
logger = logging.getLogger("omnis.dashboard")

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in self.active_connections:
            await connection.send_json(message)

manager = ConnectionManager()

async def metric_generator():
    """Simuluje živý stream metrik."""
    while True:
        metrics = {
            "token_usage": random.randint(100, 5000),
            "sync_status": random.choice(["SYNCED", "SYNCING", "ERROR"]),
            "threats_detected": random.randint(0, 5)
        }
        await manager.broadcast(metrics)
        await asyncio.sleep(2) # 2s interval pro live update

@router.on_event("startup")
async def startup_event():
    asyncio.create_task(metric_generator())

@router.websocket("/ws/metrics")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)
