import { getAccessToken } from '../auth/authHelpers';

async function authenticatedRequest(url, options = {}) {
  const accessToken = await getAccessToken();

  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
      ...(options.headers || {}),
    },
  });

  if (response.status === 401) {
    throw new Error('Your session has expired. Please sign in again.');
  }

  if (response.status === 403) {
    throw new Error('You do not have permission to complete this action.');
  }

  if (!response.ok) {
    throw new Error(`API request failed with status ${response.status}.`);
  }

  return response.json();
}

export async function getMyNotifications() {
  const notificationApiUrl = import.meta.env.VITE_NOTIFICATION_API_URL;

  if (!notificationApiUrl) {
    throw new Error('Notification API URL is not configured.');
  }

  return authenticatedRequest(`${notificationApiUrl}/notifications/me`);
}

export async function getFlights() {
  const flightApiUrl = import.meta.env.VITE_FLIGHT_API_URL;

  if (!flightApiUrl) {
    throw new Error('Flight API URL is not configured.');
  }

  return authenticatedRequest(`${flightApiUrl}/flights`);
}

export async function createBooking(bookingRequest) {
  const bookingApiUrl = import.meta.env.VITE_BOOKING_API_URL;

  if (!bookingApiUrl) {
    throw new Error('Booking API URL is not configured.');
  }

  return authenticatedRequest(`${bookingApiUrl}/bookings`, {
    method: 'POST',
    body: JSON.stringify({
      flightId: bookingRequest.flightId,
      passengerName: bookingRequest.passengerName,
      seatCount: bookingRequest.seatCount,
    }),
  });
}

export async function getMyBookings() {
  const bookingApiUrl = import.meta.env.VITE_BOOKING_API_URL;

  if (!bookingApiUrl) {
    throw new Error('Booking API URL is not configured.');
  }

  return authenticatedRequest(`${bookingApiUrl}/bookings/me`);
}

export async function createPayment(bookingId) {
  const paymentApiUrl = import.meta.env.VITE_PAYMENT_API_URL;

  if (!paymentApiUrl) {
    throw new Error('Payment API URL is not configured.');
  }

  return authenticatedRequest(`${paymentApiUrl}/payments`, {
    method: 'POST',
    body: JSON.stringify({
      bookingId,
    }),
  });
}

export async function createCheckoutSession(paymentId) {
  const paymentApiUrl = import.meta.env.VITE_PAYMENT_API_URL;

  if (!paymentApiUrl) {
    throw new Error('Payment API URL is not configured.');
  }

  return authenticatedRequest(
    `${paymentApiUrl}/payments/${paymentId}/checkout-session`,
    {
      method: 'POST',
    }
  );
}

export async function getBaggageByBookingId(bookingId) {
  const baggageApiUrl = import.meta.env.VITE_BAGGAGE_API_URL;

  if (!baggageApiUrl) {
    throw new Error('Baggage API URL is not configured.');
  }

  return authenticatedRequest(`${baggageApiUrl}/baggage/booking/${bookingId}`);
}

export async function getAllBookings() {
  const bookingApiUrl = import.meta.env.VITE_BOOKING_API_URL;

  if (!bookingApiUrl) {
    throw new Error('Booking API URL is not configured.');
  }

  return authenticatedRequest(`${bookingApiUrl}/bookings`);
}

export async function createBaggageForBooking(bookingId, currentLocation) {
  const baggageApiUrl = import.meta.env.VITE_BAGGAGE_API_URL;

  if (!baggageApiUrl) {
    throw new Error('Baggage API URL is not configured.');
  }

  return authenticatedRequest(`${baggageApiUrl}/baggage`, {
    method: 'POST',
    body: JSON.stringify({
      bookingId,
      currentLocation,
    }),
  });
}

export async function updateBaggageStatus(baggageId, status, currentLocation) {
  const baggageApiUrl = import.meta.env.VITE_BAGGAGE_API_URL;

  if (!baggageApiUrl) {
    throw new Error('Baggage API URL is not configured.');
  }

  return authenticatedRequest(`${baggageApiUrl}/baggage/${baggageId}/status`, {
    method: 'PUT',
    body: JSON.stringify({
      status,
      currentLocation,
    }),
  });
}

export async function createFlight(flight) {
  const flightApiUrl = import.meta.env.VITE_FLIGHT_API_URL;

  if (!flightApiUrl) {
    throw new Error('Flight API URL is not configured.');
  }

  return authenticatedRequest(`${flightApiUrl}/flights`, {
    method: 'POST',
    body: JSON.stringify(flight),
  });
}

export async function updateFlight(flightId, flight) {
  const flightApiUrl = import.meta.env.VITE_FLIGHT_API_URL;

  if (!flightApiUrl) {
    throw new Error('Flight API URL is not configured.');
  }

  return authenticatedRequest(`${flightApiUrl}/flights/${flightId}`, {
    method: 'PUT',
    body: JSON.stringify(flight),
  });
}