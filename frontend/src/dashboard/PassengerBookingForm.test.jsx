import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  createBooking: vi.fn(),
}));

import { createBooking } from '../services/apiClient';
import PassengerBookingForm from './PassengerBookingForm';

describe('PassengerBookingForm', () => {
  beforeEach(() => {
    createBooking.mockReset();
  });

  it('sends flightId when booking is submitted', async () => {
    const onBookingCreated = vi.fn();
    const onCancel = vi.fn();
    createBooking.mockResolvedValue({ bookingId: 'BK-1' });

    render(
      <PassengerBookingForm
        flight={{
          flightId: 'FL-1',
          flightNumber: 'AL101',
          fromLocation: 'Colombo',
          toLocation: 'Dubai',
          price: 25000,
          availableSeats: 4,
        }}
        onCancel={onCancel}
        onBookingCreated={onBookingCreated}
      />
    );

    fireEvent.change(screen.getByLabelText(/Passenger name/i), {
      target: { value: 'Ada Lovelace' },
    });
    fireEvent.change(screen.getByLabelText(/Number of seats/i), {
      target: { value: '2' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Confirm booking/i }));

    await waitFor(() => expect(createBooking).toHaveBeenCalledTimes(1));
    expect(createBooking).toHaveBeenCalledWith({
      flightId: 'FL-1',
      passengerName: 'Ada Lovelace',
      seatCount: 2,
    });
    expect(onBookingCreated).toHaveBeenCalledWith({ bookingId: 'BK-1' });
  });
});