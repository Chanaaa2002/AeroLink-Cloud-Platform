# AeroLink Load Testing (Locust)

This folder contains a safe, read-heavy Locust load testing setup for the AeroLink public API. It intentionally avoids destructive workflows (no Stripe/payment calls, no creation of many bookings by default, no secrets embedded in source).

Files:

- `locustfile.py` — Locust test definition with `AeroLinkPassengerUser` tasks.
- `.gitignore` — ignore caches and CSV outputs.

Requirements
------------

- Python 3.8+ and `pip`.
- Locust: install with:

```powershell
python -m pip install locust
```

Environment variables
---------------------

Set the following environment variables in PowerShell before running Locust (or use `--host` CLI option). Example PowerShell commands:

```powershell
$env:AEROLINK_API_BASE_URL="https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com"
$env:AEROLINK_ACCESS_TOKEN="PASTE_COGNITO_ACCESS_TOKEN_HERE"
$env:AEROLINK_TEST_BOOKING_ID="PASTE_BOOKING_ID_HERE"
```

- `AEROLINK_API_BASE_URL` — optional; defaults to `https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com`.
- `AEROLINK_ACCESS_TOKEN` — optional; if not provided, protected endpoints are skipped.
- `AEROLINK_TEST_BOOKING_ID` — optional; if not provided, baggage booking endpoint is skipped.

How to run Locust (interactive)
-------------------------------

1. Open PowerShell in this folder:

```powershell
cd <path-to-repo>\load-testing
```

2. (Optional) Set environment variables as shown above.

3. Start Locust:

```powershell
locust -f locustfile.py
```

4. Open the web UI at:

```
http://localhost:8089
```

Recommended configurations (examples)
------------------------------------

- Light load: 20 users, spawn rate 2, run 5 minutes
- Medium load: 50 users, spawn rate 5, run 5 minutes
- Stress test: 100 users, spawn rate 10, run 3 minutes

Headless examples
-----------------

```powershell
locust -f locustfile.py --headless -u 20 -r 2 -t 5m --host https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com --csv aerolink-load-20-users

locust -f locustfile.py --headless -u 50 -r 5 -t 5m --host https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com --csv aerolink-load-50-users

locust -f locustfile.py --headless -u 100 -r 10 -t 3m --host https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com --csv aerolink-stress-100-users
```

Notes and safety
----------------

- The Locust tasks are intentionally read-heavy and avoid payment endpoints to prevent creating real Stripe sessions.
- Protected endpoints (`/bookings/me`, `/notifications/me`, `/baggage/booking/{bookingId}`) only run when `AEROLINK_ACCESS_TOKEN` (and `AEROLINK_TEST_BOOKING_ID` for baggage) are set.
- Do not commit real tokens or secrets into repository.

Evidence to capture (recommended screenshots)
-------------------------------------------

- Locust summary page (overall statistics)
- Endpoint statistics table
- Failures tab (if any)
- Response time graph
- Requests-per-second graph
- AWS ECS services running during test
- Relevant CloudWatch metrics and logs (if available)

Excluding payment endpoints
---------------------------

This load test explicitly excludes payment creation or checkout endpoints to avoid accidental charges or Stripe webhook overhead. If you need to load test payment paths, perform that in an isolated, test-only environment with test Stripe keys and small controlled traffic.
