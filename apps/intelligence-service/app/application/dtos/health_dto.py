"""Health check response DTO."""
from pydantic import BaseModel


class HealthResponseDTO(BaseModel):
    status: str
    service: str
    version: str
