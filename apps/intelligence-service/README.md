# Mottu Smart Ops — Intelligence Service

Python 3.12+ microservice providing multimodal AI triage (Gemini Vision), semantic Corrective Retrieval-Augmented Generation (CRAG), and VTEX catalog mining for Mottu Smart Ops motorcycle fleet operations.

## Architecture

This service strictly adheres to **Hexagonal Architecture (Ports and Adapters)**:
- `app/domain/`: Pure standard Python domain models, normalizers, and evaluators. Zero external framework dependencies.
- `app/application/`: Inbound/outbound ports and DTOs.
- `app/modules/`: Specialized business logic (`triage_ai`, `catalog_miner`).
- `app/infrastructure/`: Web routers, Redis consumers, PostgreSQL/pgvector adapters, security.
- `app/core/`: Settings with offline defaults (`GEMINI_MOCK_MODE=True`, `BIGQUERY_ENABLED=False`).

## Development Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
```

## Running Tests & Coverage Gate

```bash
pytest --cov=app/domain --cov-fail-under=100 -v
```

## Running the Service

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
