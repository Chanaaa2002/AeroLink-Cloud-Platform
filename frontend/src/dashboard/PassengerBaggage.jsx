import { useEffect, useState } from 'react';
import { getBaggageByBookingId, getMyBookings } from '../services/apiClient';
import './PassengerBaggage.css';

function formatDateTime(value) {
  if (!value) {
    return 'Not available';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString('en-LK', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function PassengerBaggage({ onBack }) {
  const [baggageItems, setBaggageItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  async function loadBaggage() {
    setLoading(true);
    setErrorMessage('');

    try {
      const bookings = await getMyBookings();

      const paidBookings = Array.isArray(bookings)
        ? bookings.filter(
            (booking) =>
              booking.bookingStatus === 'CONFIRMED' ||
              booking.paymentStatus === 'PAID'
          )
        : [];

      const baggageResponses = await Promise.all(
        paidBookings.map(async (booking) => {
          try {
            const baggage = await getBaggageByBookingId(booking.bookingId);

            return Array.isArray(baggage)
              ? baggage.map((item) => ({
                  ...item,
                  passengerName: booking.passengerName,
                  flightId: booking.flightId,
                }))
              : [];
          } catch {
            return [];
          }
        })
      );

      setBaggageItems(baggageResponses.flat());
    } catch (error) {
      setErrorMessage(error.message || 'Unable to retrieve baggage records.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadBaggage();
  }, []);

  return (
    <section className="baggage-view">
      <div className="baggage-heading">
        <div>
          <p className="dash-eyebrow">Baggage tracking</p>
          <h1>Your Baggage Journey</h1>
          <p>Track checked-in baggage, current airport location and latest journey status.</p>
        </div>

        <div className="baggage-actions">
          <button className="baggage-back-button" type="button" onClick={onBack}>
            ← Overview
          </button>

          <button
            className="baggage-refresh-button"
            type="button"
            onClick={loadBaggage}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {loading && (
        <div className="baggage-state-card">
          <span className="baggage-loader" />
          <h2>Loading baggage details...</h2>
          <p>Checking your confirmed booking baggage records.</p>
        </div>
      )}

      {!loading && errorMessage && (
        <div className="baggage-state-card">
          <span className="baggage-state-icon">!</span>
          <h2>Unable to retrieve baggage</h2>
          <p>{errorMessage}</p>
          <button type="button" onClick={loadBaggage}>
            Try again
          </button>
        </div>
      )}

      {!loading && !errorMessage && baggageItems.length === 0 && (
        <div className="baggage-state-card">
          <span className="baggage-state-icon">⌁</span>
          <h2>No baggage records yet</h2>
          <p>
            Baggage records will appear here after staff creates baggage for a confirmed booking.
          </p>
        </div>
      )}

      {!loading && !errorMessage && baggageItems.length > 0 && (
        <div className="baggage-list">
          {baggageItems.map((item) => (
            <article className="baggage-card" key={item.baggageId}>
              <div className="baggage-card-top">
                <div>
                  <p>Baggage tag</p>
                  <h2>{item.tagNumber}</h2>
                </div>

                <span>{item.status}</span>
              </div>

              <div className="baggage-route-line">
                <div>
                  <small>Flight</small>
                  <strong>{item.flightId}</strong>
                </div>

                <div className="baggage-plane">✈</div>

                <div>
                  <small>Passenger</small>
                  <strong>{item.passengerName || 'Passenger'}</strong>
                </div>
              </div>

              <div className="baggage-info-grid">
                <div>
                  <span>Current location</span>
                  <strong>{item.currentLocation || 'Not updated'}</strong>
                </div>

                <div>
                  <span>Last updated</span>
                  <strong>{formatDateTime(item.lastUpdated)}</strong>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default PassengerBaggage;