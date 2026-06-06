import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../auth/authHelpers', () => ({
  getAccessToken: vi.fn(),
}));

import { getAccessToken } from '../auth/authHelpers';
import {
  createBooking,
  createBaggageForBooking,
  createCheckoutSession,
  createFlight,
  createPayment,
  getBaggageByBookingId,
  getFlights,
  getMyBookings,
  getMyNotifications,
  updateBaggageStatus,
  updateFlight,
} from './apiClient';

describe('apiClient', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_NOTIFICATION_API_URL', 'https://notifications.example');
    vi.stubEnv('VITE_FLIGHT_API_URL', 'https://flights.example');
    vi.stubEnv('VITE_BOOKING_API_URL', 'https://bookings.example');
    vi.stubEnv('VITE_PAYMENT_API_URL', 'https://payments.example');
    vi.stubEnv('VITE_BAGGAGE_API_URL', 'https://baggage.example');
    vi.stubGlobal('fetch', vi.fn());
    getAccessToken.mockResolvedValue('access-token-123');
  });

  it('builds authenticated flight requests', async () => {
    fetch.mockResolvedValue(mockJsonResponse([{ flightId: 'FL-1' }]));

    await expect(getFlights()).resolves.toEqual([{ flightId: 'FL-1' }]);

    expect(fetch).toHaveBeenCalledWith(
      'https://flights.example/flights',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer access-token-123',
        }),
      })
    );
  });

  it('sends booking payloads using flightId', async () => {
    fetch.mockResolvedValue(mockJsonResponse({ bookingId: 'BK-1' }));

    await createBooking({
      flightId: 'FL-1',
      flightNumber: 'AL101',
      passengerName: 'Ada Lovelace',
      seatCount: 2,
    });

    expect(fetch).toHaveBeenCalledWith(
      'https://bookings.example/bookings',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          flightId: 'FL-1',
          passengerName: 'Ada Lovelace',
          seatCount: 2,
        }),
      })
    );
  });

  it('builds the baggage status update request', async () => {
    fetch.mockResolvedValue(mockJsonResponse({ baggageId: 'BAG-1' }));

    await updateBaggageStatus('BAG-1', 'LOADED', 'Loading Bay');

    expect(fetch).toHaveBeenCalledWith(
      'https://baggage.example/baggage/BAG-1/status',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({
          status: 'LOADED',
          currentLocation: 'Loading Bay',
        }),
      })
    );
  });

  it('builds payment and notification endpoints', async () => {
    fetch.mockResolvedValue(mockJsonResponse([]));

    await getMyNotifications();
    await getMyBookings();
    await createPayment('BK-1');
    await createCheckoutSession('PAY-1');
    await getBaggageByBookingId('BK-1');
    await createBaggageForBooking('BK-1', 'Sorting Area');
    await createFlight({ flightId: 'FL-1' });
    await updateFlight('FL-1', { status: 'SCHEDULED' });

    expect(fetch).toHaveBeenCalledWith('https://notifications.example/notifications/me', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://bookings.example/bookings/me', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://payments.example/payments', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://payments.example/payments/PAY-1/checkout-session', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://baggage.example/baggage/booking/BK-1', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://baggage.example/baggage', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://flights.example/flights', expect.any(Object));
    expect(fetch).toHaveBeenCalledWith('https://flights.example/flights/FL-1', expect.any(Object));
  });

  function mockJsonResponse(payload, status = 200) {
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => payload,
    };
  }
});