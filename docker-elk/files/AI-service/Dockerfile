# AI-service: FastAPI + sentence-transformers (tạo vector 384 chiều cho bài hát)
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
# torch bản CPU nhẹ hơn nhiều so với bản mặc định (CUDA, vài GB)
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
