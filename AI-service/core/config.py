import os
from dotenv import load_dotenv

load_dotenv()
class Settings:
    PROJECT_NAME: str = "Music4 AI Vector Service"
    PROJECT_VERSION: str = "1.0.0"
    AI_MODEL_NAME: str = "sentence-transformers/all-MiniLM-L6-v2"
    # Không hardcode key trong code: đặt AI_API_KEY trong .env (search-admin gửi kèm header X-API-Key)
    API_KEY: str = os.getenv("AI_API_KEY", "")

settings = Settings()