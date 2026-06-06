# AeroLink Hosted API Postman Testing

This folder contains a Postman v2.1 collection for the hosted AeroLink API Gateway endpoint, not localhost.

Files:

- `AeroLink_Hosted_API.postman_collection.json`
- `AeroLink_Hosted_API.postman_environment.json`

Import into Postman
-------------------

1. Open Postman.
2. Click **Import**.
3. Import `postman/AeroLink_Hosted_API.postman_collection.json`.
4. Import `postman/AeroLink_Hosted_API.postman_environment.json`.
5. Select the `AeroLink Hosted API` environment from the environment dropdown.

Set tokens manually
-------------------

Use fresh Cognito access tokens and paste them into the environment values before running the collection.

- `passengerToken` = passenger Cognito JWT access token
- `staffToken` = staff Cognito JWT access token

Do not commit real tokens
-------------------------

- Keep tokens only in your local Postman environment.
- Do not export or commit screenshots containing real JWTs.
- Do not place secrets, AWS IDs, Stripe keys, or internal service keys into the collection.

Recommended run order
---------------------

Run the collection in this order:

1. Health / Public
2. Passenger Booking Flow
3. Payment Flow
4. Staff Baggage Flow
5. Passenger Baggage / Notifications
6. Security / RBAC Negative Tests

What the collection covers
--------------------------

- Public flights and optional health checks
- Passenger booking creation and booking lookup
- Payment creation and Stripe checkout session creation
- Staff baggage creation, baggage lookup, valid status transitions, and an invalid transition check
- Passenger baggage lookup and notification lookup
- Safe RBAC negative tests for missing/incorrect roles

Hosted API Gateway only
-----------------------

This collection targets the deployed AWS API Gateway base URL:

`https://sm9b7gip7i.execute-api.us-east-1.amazonaws.com`

It does not use localhost.

Stripe payment note
-------------------

The collection intentionally avoids real card payment completion. The Stripe hosted checkout confirmation is tested manually in the browser and in Stripe sandbox because final confirmation depends on Stripe redirect and webhook flow.

Evidence to capture
-------------------

Capture screenshots showing:

- Postman request success states
- Collection Runner summary
- Passed tests count
- No tokens visible in screenshots

Optional Newman command
-----------------------

```powershell
newman run postman/AeroLink_Hosted_API.postman_collection.json -e postman/AeroLink_Hosted_API.postman_environment.json
```
