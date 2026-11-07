"""Domain Purity Enforcement Test (AGENTS.md Law 2).

Verifies that all files in app/domain/ contain ONLY pure standard Python
and ZERO external framework imports (fastapi, sqlalchemy, pydantic, starlette, redis, asyncpg, google, etc.).
"""
import ast
from pathlib import Path

FORBIDDEN_FRAMEWORKS = [
    "fastapi",
    "sqlalchemy",
    "pydantic",
    "starlette",
    "redis",
    "asyncpg",
    "google",
    "boto3",
    "requests",
    "httpx",
]

DOMAIN_DIR = Path(__file__).resolve().parent.parent / "app" / "domain"


def test_domain_hexagonal_purity():
    assert DOMAIN_DIR.exists(), f"Domain directory {DOMAIN_DIR} does not exist"

    violations = []
    py_files = list(DOMAIN_DIR.rglob("*.py"))
    assert len(py_files) > 0, "No python files found in app/domain"

    for file_path in py_files:
        with open(file_path, "r", encoding="utf-8") as f:
            tree = ast.parse(f.read(), filename=str(file_path))

        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    for forbidden in FORBIDDEN_FRAMEWORKS:
                        if alias.name == forbidden or alias.name.startswith(f"{forbidden}."):
                            violations.append(
                                f"{file_path.name}:{node.lineno} -> import {alias.name}"
                            )
            elif isinstance(node, ast.ImportFrom):
                if node.module:
                    for forbidden in FORBIDDEN_FRAMEWORKS:
                        if node.module == forbidden or node.module.startswith(f"{forbidden}."):
                            violations.append(
                                f"{file_path.name}:{node.lineno} -> from {node.module} import ..."
                            )

    assert not violations, (
        f"Domain Hexagonal Purity VIOLATION! Prohibited imports found in app/domain:\n"
        + "\n".join(violations)
    )
