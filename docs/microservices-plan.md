The AeroLink platform uses five microservices:

1. Auth Service
- Handles user registration, login, JWT token generation, and role-based access.

2. Flight Service
- Handles flight creation, flight search, schedule updates, price updates, and seat availability.

3. Booking Service
- Handles flight booking, booking cancellation, and booking history.

4. Baggage Service
- Handles baggage registration, baggage tracking, and baggage status updates.

5. Notification Service
- Handles user notifications for booking confirmation, baggage status changes, and flight updates.   




Implemented Spring Boot Microservices:

1. Flight Service
- Manages flights, seat availability, schedules, and prices.
- Uses DynamoDB FlightsTable.

2. Booking Service
- Manages passenger bookings and booking status.
- Uses DynamoDB BookingsTable.

3. Payment Service
- Manages test payments using Stripe test mode.
- Uses DynamoDB PaymentsTable.

4. Baggage Service
- Manages baggage registration and tracking.
- Uses DynamoDB BaggageTable.

AWS Managed Components:

1. Amazon Cognito
- Handles user authentication and roles.

2. AWS Lambda
- Handles notification processing when booking, payment, baggage, or flight events occur.

3. Amazon EventBridge / DynamoDB Streams
- Handles event-driven communication.

4. Amazon CloudWatch
- Handles logs and monitoring.

5. Amazon ECS Fargate and ECR
- Deploy Dockerized microservices.

6. Amazon API Gateway
- Provides one public API entry point.