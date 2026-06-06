import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getMyBookings: vi.fn(),
  createPayment: vi.fn(),
  createCheckoutSession: vi.fn(),
}));

import {
  createCheckoutSession,
  createPayment,
  getMyBookings,
} from '../services/apiClient';
import PassengerBookings from './PassengerBookings';

describe('PassengerBookings', () => {
  beforeEach(() => {
    getMyBookings.mockReset();
    createPayment.mockReset();
    createCheckoutSession.mockReset();
  });

  it('displays bookings and payment state', async () => {
    getMyBookings.mockResolvedValue([
      {
        bookingId: 'BK-1',
        flightId: 'FL-1',
        passengerName: 'Ada Lovelace',
        seatCount: 2,
        totalAmount: 50000,
        bookingStatus: 'PENDING_PAYMENT',
        paymentStatus: 'PENDING',
        createdAt: '2026-06-05T08:00:00',
      },
      {
        bookingId: 'BK-2',
        flightId: 'FL-2',
        passengerName: 'Grace Hopper',
        seatCount: 1,
        totalAmount: 25000,
        bookingStatus: 'CONFIRMED',
        paymentStatus: 'PAID',
        createdAt: '2026-06-05T09:00:00',
      },
    ]);

    render(<PassengerBookings onBack={vi.fn()} />);

    expect(await screen.findByText('FL-1')).toBeInTheDocument();
    expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText('PENDING_PAYMENT')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Proceed to Payment/i })).toBeInTheDocument();
  });
});