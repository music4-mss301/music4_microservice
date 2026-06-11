from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI(title="Music4 Embedding Service")

# Model AI HuggingFace
model = SentenceTransformer('all-MiniLM-L6-v2')

class EmbedRequest(BaseModel):
    text: str

@app.post("/api/ai/embed")
def get_embedding(request: EmbedRequest):
    # Dịch chữ thành mảng 384 con số thực (Float)
    vector = model.encode(request.text).tolist()
    return {"vector": vector}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)