"""FastAPI Main Application Entry Point."""
import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.infrastructure.adapters.accident_worker import AccidentJobWorker
from app.infrastructure.adapters.redis_worker import TriageJobWorker
from app.infrastructure.web.health_router import router as health_router
from app.infrastructure.web.internal_router import router as internal_router

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: launch background queue workers
    triage_worker = TriageJobWorker()
    accident_worker = AccidentJobWorker()
    triage_task = asyncio.create_task(triage_worker.run_loop())
    accident_task = asyncio.create_task(accident_worker.run_loop())
    app.state.triage_worker = triage_worker
    app.state.accident_worker = accident_worker
    logger.info("FastAPI Intelligence Service started with background triage and accident workers")
    yield
    # Shutdown: stop workers gracefully
    triage_worker.stop()
    accident_worker.stop()
    for task in (triage_task, accident_task):
        try:
            await asyncio.wait_for(task, timeout=3.0)
        except (asyncio.TimeoutError, asyncio.CancelledError):
            task.cancel()
    logger.info("FastAPI Intelligence Service shutdown completed")


app = FastAPI(
    title="Mottu Intelligence Service",
    description="AI Triage, CRAG and VTEX Catalog Mining Service",
    version=settings.SERVICE_VERSION,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(internal_router)
