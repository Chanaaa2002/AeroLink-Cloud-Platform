Passenger / Staff / Admin
          |
          v
Frontend Web App
          |
          v
AWS API Gateway
          |
          v
------------------------------------------------
| Auth Service                                  |
| Flight Service                                |
| Booking Service                               |
| Baggage Service                               |
| Notification Service                          |
------------------------------------------------
          |
          v
Amazon DynamoDB
- UsersTable
- FlightsTable
- BookingsTable
- BaggageTable
- NotificationsTable
          |
          v
DynamoDB Streams
          |
          v
------------------------------------------------
| BookingEventLambda                            |
| BaggageStatusLambda                           |
| FlightUpdateLambda                            |
------------------------------------------------
          |
          v
AWS CloudWatch