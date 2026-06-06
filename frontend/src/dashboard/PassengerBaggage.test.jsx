import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getMyBookings: vi.fn(),
  getBaggageByBookingId: vi.fn(),
}));

import { getBaggageByBookingId, getMyBookings } from '../services/apiClient';
import PassengerBaggage from './PassengerBaggage';

describe('PassengerBaggage', () => {
  beforeEach(() => {
    getMyBookings.mockReset();
    getBaggageByBookingId.mockReset();
  });

  it('shows empty state when there is no baggage to display', async () => {
    getMyBookings.mockResolvedValue([
      {
        bookingId: 'BK-1',
        flightId: 'FL-1',
        passengerName: 'Ada Lovelace',
        bookingStatus: 'PENDING_PAYMENT',
        paymentStatus: 'PENDING',
      },
    ]);

    render(<PassengerBaggage onBack={vi.fn()} />);

    expect(await screen.findByText('No baggage records yet')).toBeInTheDocument();
    expect(getBaggageByBookingId).not.toHaveBeenCalled();
  });

  it('renders baggage cards for confirmed bookings', async () => {
    getMyBookings.mockResolvedValue([
      {
        bookingId: 'BK-1',
        flightId: 'FL-1',
        passengerName: 'Ada Lovelace',
        bookingStatus: 'CONFIRMED',
        paymentStatus: 'PAID',
      },
    ]);
    getBaggageByBookingId.mockResolvedValue([
      {
        baggageId: 'BAG-1',
        tagNumber: 'TAG-AERO-1',
        status: 'LOADED',
        currentLocation: 'Loading Bay',
        lastUpdated: '2026-06-05T08:00:00',
      },
    ]);

    render(<PassengerBaggage onBack={vi.fn()} />);

    expect(await screen.findByText('TAG-AERO-1')).toBeInTheDocument();
    expect(screen.getByText('Loading Bay')).toBeInTheDocument();
  });
});