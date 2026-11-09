import json
import time
import urllib.error
import urllib.request

BASE_URL = "http://161.153.2.148:18088"

def test_live():
    results = {}
    
    # 1. Public config
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/auth/public-config")
        with urllib.request.urlopen(req, timeout=10) as r:
            results["public_config"] = {"status": r.status, "data": json.loads(r.read().decode())}
    except Exception as e:
        results["public_config"] = {"error": str(e)}

    # 2. Login as gestor
    token = None
    try:
        login_data = json.dumps({"email": "gestor@mottu-ops.local", "password": "Mottu@2026!"}).encode()
        req = urllib.request.Request(
            f"{BASE_URL}/api/v1/auth/login",
            data=login_data,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=10) as r:
            body = json.loads(r.read().decode())
            token = body.get("accessToken") or body.get("token")
            results["login"] = {"status": r.status, "role": body.get("role"), "name": body.get("fullName")}
    except Exception as e:
        results["login"] = {"error": str(e)}

    if not token:
        print("Login failed, aborting authenticated calls:")
        print(json.dumps(results, indent=2))
        return

    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

    # 3. Profile Me
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/profile/me", headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            results["profile_me"] = {"status": r.status, "data": json.loads(r.read().decode())}
    except Exception as e:
        results["profile_me"] = {"error": str(e)}

    # 4. Analytics Summary
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/manager/analytics/summary", headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            results["analytics_summary"] = {"status": r.status, "data": json.loads(r.read().decode())}
    except Exception as e:
        results["analytics_summary"] = {"error": str(e)}

    # 5. Analytics Detailed (was failing with 500)
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/manager/analytics/detailed", headers=headers)
        with urllib.request.urlopen(req, timeout=15) as r:
            data = json.loads(r.read().decode())
            results["analytics_detailed"] = {
                "status": r.status,
                "yardSnapshots_count": len(data.get("yardSnapshots", [])),
                "dailyVolumeBars_count": len(data.get("dailyVolumeBars", [])),
                "verdictBreakdown": data.get("verdictBreakdown"),
                "topAuditedParts_count": len(data.get("topAuditedParts", []))
            }
    except urllib.error.HTTPError as e:
        results["analytics_detailed"] = {"status": e.code, "error": e.read().decode()}
    except Exception as e:
        results["analytics_detailed"] = {"error": str(e)}

    # 6. AI Advisor (was failing with 500)
    try:
        req = urllib.request.Request(
            f"{BASE_URL}/api/v1/manager/analytics/ai-advisor?yardKey=ALL",
            data=b"{}",
            headers=headers
        )
        with urllib.request.urlopen(req, timeout=20) as r:
            data = json.loads(r.read().decode())
            results["analytics_ai_advisor"] = {
                "status": r.status,
                "headline": data.get("executiveHeadline"),
                "projectedNext7DaysInspections": data.get("projectedNext7DaysInspections"),
                "actions_count": len(data.get("recommendedActions", []))
            }
    except urllib.error.HTTPError as e:
        results["analytics_ai_advisor"] = {"status": e.code, "error": e.read().decode()}
    except Exception as e:
        results["analytics_ai_advisor"] = {"error": str(e)}

    # 7. Store Sync
    try:
        req = urllib.request.Request(
            f"{BASE_URL}/api/v1/manager/catalog/sync-mottu-store",
            data=b"{}",
            headers=headers
        )
        with urllib.request.urlopen(req, timeout=25) as r:
            results["catalog_sync"] = {"status": r.status, "data": json.loads(r.read().decode())}
    except urllib.error.HTTPError as e:
        results["catalog_sync"] = {"status": e.code, "error": e.read().decode()}
    except Exception as e:
        results["catalog_sync"] = {"error": str(e)}

    # 8. Billing Contracts
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/manager/billing/contracts", headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode())
            results["billing_contracts"] = {"status": r.status, "count": len(data)}
    except urllib.error.HTTPError as e:
        results["billing_contracts"] = {"status": e.code, "error": e.read().decode()}
    except Exception as e:
        results["billing_contracts"] = {"error": str(e)}

    # 9. Catalog Parts
    try:
        req = urllib.request.Request(f"{BASE_URL}/api/v1/manager/catalog/parts", headers=headers)
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode())
            results["catalog_parts"] = {"status": r.status, "count": len(data)}
    except urllib.error.HTTPError as e:
        results["catalog_parts"] = {"status": e.code, "error": e.read().decode()}
    except Exception as e:
        results["catalog_parts"] = {"error": str(e)}

    print(json.dumps(results, indent=2))

if __name__ == "__main__":
    test_live()

