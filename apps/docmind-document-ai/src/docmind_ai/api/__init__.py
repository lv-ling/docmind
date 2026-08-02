from docmind_ai.api.capabilities import router as capabilities_router
from docmind_ai.api.documents import router as documents_router
from docmind_ai.api.extractions import router as extractions_router
from docmind_ai.api.health import router as health_router
from docmind_ai.api.sensitive import router as sensitive_router

__all__ = [
    "capabilities_router",
    "documents_router",
    "extractions_router",
    "health_router",
    "sensitive_router",
]
