DynamoDB Tables:

1. UsersTable
- userId
- name
- email
- passwordHash
- role

2. FlightsTable
- flightId
- flightNumber
- from
- to
- departureTime
- arrivalTime
- price
- availableSeats
- status

3. BookingsTable
- bookingId: String partition key
- userId
- flightId
- passengerName
- seatCount
- totalAmount
- bookingStatus
- paymentStatus
- createdAt

4. BaggageTable
- baggageId
- bookingId
- userId
- status
- currentLocation
- lastUpdated

5. NotificationsTable
- notificationId
- userId
- type
- message
- createdAt
- readStatus