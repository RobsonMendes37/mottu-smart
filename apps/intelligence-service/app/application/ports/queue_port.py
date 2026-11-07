"""Queue port interface for background triage jobs."""
from abc import ABC, abstractmethod
from typing import Any, Dict


class QueuePort(ABC):
    @abstractmethod
    async def pop_triage_job(self, timeout_seconds: int = 2) -> Dict[str, Any] | None:
        """Pop a job from queue:ai_triage_jobs."""
        raise NotImplementedError

    @abstractmethod
    async def push_triage_result(self, result: Dict[str, Any]) -> None:
        """Push a result to queue:ai_triage_results."""
        raise NotImplementedError
