"""Application settings and environment loading with offline defaults."""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Service Identification
    SERVICE_NAME: str = "intelligence-service"
    SERVICE_VERSION: str = "1.0.0"
    PORT: int = 8000
    INTERNAL_API_TOKEN: str = "internal-mottu-service-token-2026"

    # PostgreSQL with pgvector
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 15432
    POSTGRES_DB: str = "mottu_ops"
    POSTGRES_USER: str = "mottu"
    POSTGRES_PASSWORD: str = "mottu_dev_pass"

    # Redis
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 16379

    # MinIO / S3 Storage
    S3_BUCKET_NAME: str = "mottu-inspections"
    S3_ACCESS_KEY: str = "minioadmin"
    S3_SECRET_KEY: str = "minioadmin123"
    S3_INTERNAL_ENDPOINT: str = "http://localhost:19000"

    # AI Gemini (Multi-Model Smart Cascading)
    GEMINI_API_KEY: str = ""
    GEMINI_VISION_MODEL: str = "gemini-3.1-flash-lite"
    GEMINI_VISION_MODELS_CASCADE: str = "gemini-3.1-flash-lite,gemini-3.5-flash-lite,gemini-3.8-flash,gemini-2.5-flash"
    GEMINI_EMBEDDING_MODEL: str = "gemini-embedding-2"
    GEMINI_EMBEDDING_MODELS_CASCADE: str = "gemini-embedding-2,text-embedding-004"
    GEMINI_MOCK_MODE: bool = True

    @property
    def vision_models_list(self) -> list[str]:
        if self.GEMINI_VISION_MODELS_CASCADE:
            return [m.strip() for m in self.GEMINI_VISION_MODELS_CASCADE.split(",") if m.strip()]
        return [self.GEMINI_VISION_MODEL]

    @property
    def embedding_models_list(self) -> list[str]:
        if self.GEMINI_EMBEDDING_MODELS_CASCADE:
            return [m.strip() for m in self.GEMINI_EMBEDDING_MODELS_CASCADE.split(",") if m.strip()]
        return [self.GEMINI_EMBEDDING_MODEL]

    # External VTEX Store
    VTEX_STORE_BASE_URL: str = "https://www.loja.mottu.com.br"

    # Analytics / BigQuery (Offline Default - Law 8)
    BIGQUERY_ENABLED: bool = False


settings = Settings()
