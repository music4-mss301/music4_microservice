from sentence_transformers import SentenceTransformer
from core.config import settings

class EmbedService:
    def __init__(self):
        print(f"Loading AI Model: {settings.AI_MODEL_NAME}...")
        self.model = SentenceTransformer(settings.AI_MODEL_NAME)
        print("AI Model loaded successfully!")

    def generate_vector(self, text: str) -> list[float]:
        vector = self.model.encode(text)
        return vector.tolist()

embed_service_instance = EmbedService()