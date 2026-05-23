Auth Service APIs:
POST /auth/register
POST /auth/login
GET /auth/profile

Flight Service APIs:
GET /flights
GET /flights/{flightId}
POST /flights
PUT /flights/{flightId}
PUT /flights/{flightId}/seats
PUT /flights/{flightId}/price

Booking Service APIs:
POST /bookings
GET /bookings/{bookingId}
GET /bookings/user/{userId}
PUT /bookings/{bookingId}/cancel

Baggage Service APIs:
POST /baggage
GET /baggage/{baggageId}
GET /baggage/booking/{bookingId}
PUT /baggage/{baggageId}/status

Notification Service APIs:
GET /notifications/user/{userId}
PUT /notifications/{notificationId}/read