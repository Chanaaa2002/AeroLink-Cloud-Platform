import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getAllBookings: vi.fn(),
  getBaggageByBookingId: vi.fn(),
  createBaggageForBooking: vi.fn(),
  updateBaggageStatus: vi.fn(),
}));

import {
  getAllBookings,
  getBaggageByBookingId,
  updateBaggageStatus,
} from '../services/apiClient';
import StaffBaggageOperations from './StaffBaggageOperations';

describe('StaffBaggageOperations', () => {
  beforeEach(() => {
    getAllBookings.mockReset();
    getBaggageByBookingId.mockReset();
    updateBaggageStatus.mockReset();
  });

  it('shows only valid next statuses for a checked-in baggage item', async () => {
    getAllBookings.mockResolvedValue([
      {
        bookingId: 'BK-1',
        flightId: 'FL-1',
        passengerName: 'Ada Lovelace',
        seatCount: 2,
        bookingStatus: 'CONFIRMED',
        paymentStatus: 'PAID',
      },
    ]);
    getBaggageByBookingId.mockResolvedValue([
      {
        baggageId: 'BAG-1',
        tagNumber: 'TAG-AERO-1',
        status: 'CHECKED_IN',
        currentLocation: 'Check-in Counter',
      },
    ]);

    render(<StaffBaggageOperations onBack={vi.fn()} />);

    fireEvent.change(await screen.findByLabelText('Booking'), {
      target: { value: 'BK-1' },
    });

    const statusSelect = await screen.findByLabelText('New status');
    expect(statusSelect).toHaveValue('LOADED');
    expect(screen.getByRole('option', { name: 'LOADED' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'DELAYED' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'COLLECTED' })).not.toBeInTheDocument();
  });
});