"""Feedback polish request and response DTOs."""
from typing import List
from pydantic import BaseModel


class FeedbackPolishRequestDTO(BaseModel):
    finalVerdict: str
    partNames: List[str]
    totalEstimatedBrl: float
    mechanicNotes: str


class FeedbackPolishResponseDTO(BaseModel):
    customerFriendlyMessage: str
