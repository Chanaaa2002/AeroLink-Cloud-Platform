import { useEffect, useState } from 'react';
import {
  createCheckoutSession,
  createPayment,
  getMyBookings,
} from '../services/apiClient';
import './PassengerBookings.css';

function formatPrice(value) {
  const amount = Number(value);

  if (Number.isNaN(amount)) {
    return value;
  }

  return new Intl.NumberFormat('en-LK', {
    style: 'currency',
    currency: 'LKR',
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
  }).format(amount);
}

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

function getBookingStatusClass(status) {
  if (status === 'CONFIRMED') {
    return 'confirmed';
  }

  if (status === 'PENDING_PAYMENT') {
    return 'pending';
  }

  return 'default';
}

function PassengerBookings({ onBack }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [paymentError, setPaymentError] = useState('');
  const [processingBookingId, setProcessingBookingId] = useState('');

  async function loadBookings() {
    setLoading(true);
    setErrorMessage('');
    setPaymentError('');

    try {
      const result = await getMyBookings();
      setBookings(Array.isArray(result) ? result : []);
    } catch (error) {
      setErrorMessage(
        error.message || 'Unable to retrieve your bookings.'
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleProceedToPayment(booking) {
    setPaymentError('');
    setProcessingBookingId(booking.bookingId);

    try {
      const payment = await createPayment(booking.bookingId);
      const checkoutSession = await createCheckoutSession(payment.paymentId);

      if (!checkoutSession.checkoutUrl) {
        throw new Error('Stripe checkout URL was not returned.');
      }

      window.location.href = checkoutSession.checkoutUrl;
    } catch (error) {
      setPaymentError(
        error.message || 'Unable to start Stripe checkout for this booking.'
      );
      setProcessingBookingId('');
    }
  }

  useEffect(() => {
    loadBookings();
  }, []);

  return (
    <section className="bookings-view">
      <div className="bookings-heading">
        <div>
          <p className="dash-eyebrow">My bookings</p>
          <h1>Your Reservations</h1>
          <p>
            Review your pending payments and confirmed AeroLink journeys.
          </p>
        </div>

        <div className="bookings-actions">
          <button className="bookings-back-button" type="button" onClick={onBack}>
            ← Overview
          </button>

          <button
            className="bookings-refresh-button"
            type="button"
            onClick={loadBookings}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {paymentError && (
        <div className="payment-error-panel">
          <strong>Payment could not be started</strong>
          <span>{paymentError}</span>
        </div>
      )}

      {loading && (
        <div className="bookings-state-card">
          <span className="bookings-loader" />
          <h2>Loading your bookings...</h2>
          <p>Retrieving your secure passenger reservations.</p>
        </div>
      )}

      {!loading && errorMessage && (
        <div className="bookings-state-card">
          <span className="bookings-state-icon">!</span>
          <h2>Unable to retrieve bookings</h2>
          <p>{errorMessage}</p>
          <button type="button" onClick={loadBookings}>
            Try again
          </button>
        </div>
      )}

      {!loading && !errorMessage && bookings.length === 0 && (
        <div className="bookings-state-card">
          <span className="bookings-state-icon">✈</span>
          <h2>No bookings yet</h2>
          <p>Select an available flight to begin your journey.</p>
        </div>
      )}

      {!loading && !errorMessage && bookings.length > 0 && (
        <div className="bookings-list">
          {bookings.map((booking) => (
            <article className="booking-card" key={booking.bookingId}>
              <div className="booking-card-top">
                <div>
                  <p className="booking-flight-label">Flight reference</p>
                  <h2>{booking.flightId}</h2>
                </div>

                <span
                  className={`booking-status ${getBookingStatusClass(
                    booking.bookingStatus
                  )}`}
                >
                  {booking.bookingStatus}
                </span>
              </div>

              <div className="booking-passenger-row">
                <div>
                  <span>Passenger</span>
                  <strong>{booking.passengerName}</strong>
                </div>

                <div>
                  <span>Seats</span>
                  <strong>{booking.seatCount}</strong>
                </div>

                <div>
                  <span>Total amount</span>
                  <strong className="booking-total">
                    {formatPrice(booking.totalAmount)}
                  </strong>
                </div>
              </div>

              <div className="booking-card-footer">
                <div>
                  <span className="booking-created">
                    Created: {formatDateTime(booking.createdAt)}
                  </span>

                  <span className="payment-status">
                    Payment: {booking.paymentStatus}
                  </span>
                </div>

                {booking.paymentStatus === 'PENDING' && (
                  <button
                    className="proceed-payment-button"
                    type="button"
                    onClick={() => handleProceedToPayment(booking)}
                    disabled={processingBookingId === booking.bookingId}
                  >
                    {processingBookingId === booking.bookingId
                      ? 'Preparing checkout...'
                      : 'Proceed to Payment'}
                  </button>
                )}

                {booking.paymentStatus === 'PAID' && (
                  <span className="paid-badge">Paid securely</span>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default PassengerBookings;