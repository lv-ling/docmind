import uvicorn

from docmind_ai.config import load_settings


def run() -> None:
    settings = load_settings()
    uvicorn.run(
        "docmind_ai.app:create_app",
        factory=True,
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level.lower(),
    )


if __name__ == "__main__":
    run()
