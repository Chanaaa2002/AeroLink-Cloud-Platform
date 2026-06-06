import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getFlights: vi.fn(),
  createFlight: vi.fn(),
  updateFlight: vi.fn(),
}));

import { createFlight, getFlights, updateFlight } from '../services/apiClient';
import StaffFlightManagement from './StaffFlightManagement';

describe('StaffFlightManagement', () => {
  beforeEach(() => {
    getFlights.mockReset();
    createFlight.mockReset();
    updateFlight.mockReset();
  });

  it('submits a new flight', async () => {
    getFlights.mockResolvedValue([]);
    createFlight.mockResolvedValue({ flightId: 'FL-1' });

    render(<StaffFlightManagement onBack={vi.fn()} />);

    await screen.findByText('No flights found yet.');

    fireEvent.change(screen.getByLabelText('Flight ID'), { target: { value: 'FL-1' } });
    fireEvent.change(screen.getByLabelText('Flight number'), { target: { value: 'AL101' } });
    fireEvent.click(screen.getByRole('button', { name: /Create flight/i }));

    await waitFor(() => expect(createFlight).toHaveBeenCalledTimes(1));
    expect(createFlight).toHaveBeenCalledWith(expect.objectContaining({
      flightId: 'FL-1',
      flightNumber: 'AL101',
    }));
  });
});