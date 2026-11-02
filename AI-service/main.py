## py -3.14 -m venv .venv
## .\.venv\Scripts\python -m pip install -r requirements.txt
## .\.venv\Scripts\python -m uvicorn main:app --port 8000

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import py_eureka_client.eureka_client as eureka_client
from api.routes import router
from core.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    registered = False
    try:
        # Doc tu bien moi truong de chay duoc trong Docker/Render (khong hardcode localhost)
        if os.getenv("EUREKA_ENABLED", "true").lower() == "true":
            await eureka_client.init_async(
                eureka_server=os.getenv("EUREKA_SERVER_URL", "http://localhost:8761/eureka/"),
                app_name="ai-service",
                instance_port=int(os.getenv("PORT", "8000")),
                instance_host=os.getenv("INSTANCE_HOST", "localhost"),
            )
            registered = True
            print("[Eureka] AI-Service da dang ky thanh cong voi Eureka Server!")
    except Exception as e:
        print(f"[Eureka] Khong the ket noi toi Eureka Server: {e}")

    yield

    # --- TAT: dung ket noi Eureka ---
    if registered:
        try:
            await eureka_client.stop_async()
            print("[Eureka] Da dung ket noi Eureka.")
        except Exception:
            pass


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.PROJECT_VERSION,
    description="Service AI chuyen biet dich Gu am nhac sang Vector cho Music4.",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/", tags=["Health"])
def health_check():
    return {
        "status": "UP",
        "message": "Music4 AI Service is running smoothly!",
        "version": settings.PROJECT_VERSION
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=int(os.getenv("PORT", "8000")))