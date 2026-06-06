import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../services/apiClient', () => ({
  getMyNotifications: vi.fn().mockResolvedValue([]),
}));

import DashboardHome from './DashboardHome';

describe('DashboardHome', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/');
  });

  it('renders passenger features and sign-out action', () => {
    const onSignOut = vi.fn();

    render(
      <DashboardHome
        role="PASSENGER"
        accountLabel="Verified passenger account"
        onSignOut={onSignOut}
      />
    );

    expect(screen.getByText('Passenger Portal')).toBeInTheDocument();
    expect(screen.getByText('Available Flights')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Sign out/i }));
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });

  it('renders staff role specific dashboard actions', () => {
    render(
      <DashboardHome
        role="STAFF"
        accountLabel="Verified staff account"
        onSignOut={vi.fn()}
      />
    );

    expect(screen.getByText('Staff Portal')).toBeInTheDocument();
    expect(screen.getByText('Flight Management')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Baggage Operations/i })).toBeInTheDocument();
  });
});