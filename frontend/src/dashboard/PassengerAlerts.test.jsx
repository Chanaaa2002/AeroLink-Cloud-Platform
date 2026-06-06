import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getMyNotifications: vi.fn(),
}));

import { getMyNotifications } from '../services/apiClient';
import PassengerAlerts from './PassengerAlerts';

describe('PassengerAlerts', () => {
  beforeEach(() => {
    getMyNotifications.mockReset();
  });

  it('shows empty state when there are no notifications', async () => {
    getMyNotifications.mockResolvedValue([]);

    render(<PassengerAlerts onBack={vi.fn()} />);

    expect(await screen.findByText('No travel alerts yet')).toBeInTheDocument();
  });

  it('renders notification cards', async () => {
    getMyNotifications.mockResolvedValue([
      {
        notificationId: 'NT-1',
        type: 'PAYMENT_SUCCESS',
        title: 'Payment completed successfully',
        message: 'Your payment is confirmed.',
        status: 'UNREAD',
        createdAt: '2026-06-05T08:00:00',
      },
    ]);

    render(<PassengerAlerts onBack={vi.fn()} />);

    expect(await screen.findByText('Payment completed successfully')).toBeInTheDocument();
    expect(screen.getByText('Your payment is confirmed.')).toBeInTheDocument();
  });
});