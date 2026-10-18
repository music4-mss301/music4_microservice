from fastapi import APIRouter, Security, HTTPException, status
from fastapi.security import APIKeyHeader
from pydantic import BaseModel
from services.embed_service import embed_service_instance
from core.config import settings

router = APIRouter()

api_key_header = APIKeyHeader(name="X-API-Key", auto_error=True)

# Hàm kiểm tra key
def verify_api_key(api_key: str = Security(api_key_header)):
    if api_key != settings.API_KEY:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Sai API Key! Server từ chối tính toán.",
        )
    return api_key
class TextRequest(BaseModel):
    text: str

@router.post("/api/ai/embed", summary="Dịch Gu âm nhạc sang Vector")
async def get_embedding(request: TextRequest, api_key: str = Security(verify_api_key)):

    vector_data = embed_service_instance.generate_vector(request.text)    
   
    return {"vector": vector_data}
    

@router.get("/health", summary="Kiểm tra trạng thái Service AI")
async def health_check():
    return {"status": "UP", "service": "Music4 AI Vector Service"}