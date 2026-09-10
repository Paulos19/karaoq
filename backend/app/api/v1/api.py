from fastapi import APIRouter
from app.api.v1.endpoints import separate

api_router = APIRouter()
api_router.include_router(separate.router, prefix="/separate", tags=["Stem Separation"])
