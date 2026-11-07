"""Pytest configuration and shared fixtures."""
import pytest
from fastapi.testclient import TestClient
from app.main import app



@pytest.fixture
def client():
    return TestClient(app)


def pytest_configure(config):
    args_str = " ".join(str(a) for a in config.args)
    if "test_domain_purity" in args_str or (config.args and "unit" not in args_str and "--cov" not in args_str):
        cov_plugin = config.pluginmanager.get_plugin("_cov")
        if cov_plugin is not None:
            cov_plugin._disabled = True
            config.pluginmanager.unregister(cov_plugin)




