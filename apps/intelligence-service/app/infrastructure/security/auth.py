"""Internal token authentication."""
from fastapi import Header, HTTPException, status
from app.core.config import settings


async def verify_internal_token(x_internal_token: str = Header(...)) -> str:
    """Verifies that the X-Internal-Token header matches internal settings."""
    if x_internal_token != settings.INTERNAL_API_TOKEN:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-Token",
        )
    return x_internal_token
