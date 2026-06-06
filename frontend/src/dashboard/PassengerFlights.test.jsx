import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getFlights: vi.fn(),
}));

import { getFlights } from '../services/apiClient';
import PassengerFlights from './PassengerFlights';

describe('PassengerFlights', () => {
  beforeEach(() => {
    getFlights.mockReset();
  });

  it('renders flight cards and opens the booking form', async () => {
    getFlights.mockResolvedValue([
      {
        flightId: 'FL-1',
        flightNumber: 'AL101',
        fromLocation: 'Colombo',
        toLocation: 'Dubai',
        departureTime: '2026-06-05T08:00:00',
        arrivalTime: '2026-06-05T12:00:00',
        price: 25000,
        availableSeats: 4,
        status: 'SCHEDULED',
      },
    ]);

    render(<PassengerFlights onBack={vi.fn()} />);

    expect(await screen.findByText('AL101')).toBeInTheDocument();
    expect(screen.getByText('Colombo')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Select flight/i }));

    expect(await screen.findByLabelText('Create booking')).toBeInTheDocument();
  });
});