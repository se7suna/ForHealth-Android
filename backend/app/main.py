import asyncio
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pathlib import Path
from contextlib import asynccontextmanager
from app.config import settings
from app.database import (connect_to_mongo, close_mongo_connection)
from app.db_init.init_dataset import (initialize_foods_table,
                          initialize_sports_table,initialize_default_user)
from app.routers import auth, user, sports, food, recipe, visualization


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理""" 
    # 启动时执行
    print("🚀 启动 FastAPI 应用...")
    await connect_to_mongo()

    asyncio.create_task(run_initialization())# 异步初始化数据

    yield

    # 关闭时执行
    print("👋 关闭 FastAPI 应用...")
    await close_mongo_connection()

async def run_initialization():
    """异步后台初始化：不会阻塞应用启动。"""
    print("⚙️ 开始初始化后台数据...")
    
    print("⚙️ 开始初始化默认用户...")
    await initialize_default_user()
    print("⚙️ 开始初始化运动表...")
    await initialize_sports_table()
    print("⚙️ 开始初始化食物表...")
    await initialize_foods_table()

    print("✅ 数据库初始化完成！")


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
app.include_router(food.router, prefix="/api")
app.include_router(recipe.router, prefix="/api")
app.include_router(visualization.router)

# 配置静态文件服务（用于访问上传的图片）
# 基于 backend 目录构建上传路径，确保路径一致性
backend_dir = Path(__file__).parent.parent  # backend/app -> backend
base_path = Path(settings.IMAGE_STORAGE_PATH)
if base_path.is_absolute():
    uploads_path = base_path
else:
    uploads_path = backend_dir / base_path
uploads_path.mkdir(parents=True, exist_ok=True)
app.mount(settings.IMAGE_BASE_URL, StaticFiles(directory=str(uploads_path)), name="static")


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
