from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.config import settings
from app.database import connect_to_mongo, close_mongo_connection
from app.routers import auth, user,sports


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理""" 
    # 启动时执行
    print("🚀 启动 FastAPI 应用...")
    await connect_to_mongo()
    from app.services.user_service import create_user
    from app.utils.security import get_password_hash
    await create_user("user@example.com","testuser",get_password_hash("string"))
    from app.services.sports_service import initialize_sports_table
    await initialize_sports_table()
    print(11111111111)
    yield
    # 关闭时执行
    print("👋 关闭 FastAPI 应用...")
    await close_mongo_connection()


# 创建 FastAPI 应用
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="For Health - 卡路里消耗记录系统后端 API",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(auth.router, prefix="/api")
app.include_router(user.router, prefix="/api")
app.include_router(sports.router, prefix="/api")


@app.get("/")
async def root():
    """根路径"""
    return {
        "message": "Welcome to For Health API",
        "version": settings.APP_VERSION,
        "docs": "/docs",
    }


@app.get("/health")
async def health_check():
    """健康检查端点"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
    )
