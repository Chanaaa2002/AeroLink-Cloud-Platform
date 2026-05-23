AWS Services Used:

1. AWS API Gateway
- Used as the main entry point for all frontend API requests.
- Routes requests to the correct microservice.

2. Amazon DynamoDB
- Used as the main cloud database.
- Stores users, flights, bookings, baggage, and notifications.

3. DynamoDB Streams
- Used to detect real-time changes in DynamoDB tables.

4. AWS Lambda
- Used for serverless event processing.
- Handles booking, baggage, and flight update events.

5. AWS CloudWatch
- Used for logs, metrics, monitoring, and fault diagnosis.

6. Docker
- Used to containerise microservices.