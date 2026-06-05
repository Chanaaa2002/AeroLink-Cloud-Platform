import { useState } from 'react';
import { createBooking } from '../services/apiClient';
import './PassengerBookingForm.css';

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

function PassengerBookingForm({ flight, onCancel, onBookingCreated }) {
  const [passengerName, setPassengerName] = useState('');
  const [seatCount, setSeatCount] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const totalAmount = Number(flight.price || 0) * Number(seatCount || 0);

  async function handleSubmit(event) {
    event.preventDefault();
    setErrorMessage('');

    if (!passengerName.trim()) {
      setErrorMessage('Please enter the passenger name.');
      return;
    }

    if (seatCount < 1 || seatCount > flight.availableSeats) {
      setErrorMessage('Please select a valid number of seats.');
      return;
    }

    setSubmitting(true);

    try {
      const createdBooking = await createBooking({
        flightId: flight.flightId,
        passengerName: passengerName.trim(),
        seatCount: Number(seatCount),
      });

      onBookingCreated(createdBooking);
    } catch (error) {
      setErrorMessage(
        error.message || 'Unable to create your booking at this time.'
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="booking-overlay">
      <section className="booking-modal" aria-label="Create booking">
        <div className="booking-modal-header">
          <div>
            <p className="dash-eyebrow">Selected flight</p>
            <h2>{flight.flightNumber}</h2>
          </div>

          <button
            className="booking-close-button"
            type="button"
            onClick={onCancel}
            aria-label="Close booking form"
          >
            ×
          </button>
        </div>

        <div className="booking-route-summary">
          <div>
            <small>From</small>
            <strong>{flight.fromLocation}</strong>
          </div>

          <span>✈</span>

          <div>
            <small>To</small>
            <strong>{flight.toLocation}</strong>
          </div>
        </div>

        <form className="booking-form" onSubmit={handleSubmit}>
          <label>
            Passenger name
            <input
              type="text"
              value={passengerName}
              onChange={(event) => setPassengerName(event.target.value)}
              placeholder="Enter passenger full name"
              required
            />
          </label>

          <label>
            Number of seats
            <input
              type="number"
              min="1"
              max={flight.availableSeats}
              value={seatCount}
              onChange={(event) => setSeatCount(Number(event.target.value))}
              required
            />
          </label>

          <div className="booking-summary">
            <div>
              <span>Available seats</span>
              <strong>{flight.availableSeats}</strong>
            </div>

            <div>
              <span>Total amount</span>
              <strong>{formatPrice(totalAmount)}</strong>
            </div>
          </div>

          {errorMessage && (
            <p className="booking-error-message">{errorMessage}</p>
          )}

          <div className="booking-buttons">
            <button
              className="booking-cancel-button"
              type="button"
              onClick={onCancel}
              disabled={submitting}
            >
              Cancel
            </button>

            <button
              className="booking-submit-button"
              type="submit"
              disabled={submitting}
            >
              {submitting ? 'Creating booking...' : 'Confirm booking'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default PassengerBookingForm;