"""Health check router."""
from fastapi import APIRouter
from app.application.dtos.health_dto import HealthResponseDTO
from app.core.config import settings

router = APIRouter(tags=["Health"])


@router.get("/health", response_model=HealthResponseDTO)
def health_check() -> HealthResponseDTO:
    """Service health check endpoint."""
    return HealthResponseDTO(
        status="UP",
        service=settings.SERVICE_NAME,
        version=settings.SERVICE_VERSION,
    )
