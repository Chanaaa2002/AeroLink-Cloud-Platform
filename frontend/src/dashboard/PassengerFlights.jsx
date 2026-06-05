import { useEffect, useState } from 'react';
import { getFlights } from '../services/apiClient';
import PassengerBookingForm from './PassengerBookingForm';
import './BookingSuccess.css';

function formatFlightDateTime(value) {
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

function PassengerFlights({ onBack }) {
  const [flights, setFlights] = useState([]);
  const [selectedFlight, setSelectedFlight] = useState(null);
  const [createdBooking, setCreatedBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  async function loadFlights() {
    setLoading(true);
    setErrorMessage('');

    try {
      const result = await getFlights();
      setFlights(Array.isArray(result) ? result : []);
    } catch (error) {
      setErrorMessage(
        error.message || 'Unable to retrieve available flights.'
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadFlights();
  }, []);

  function handleSelectFlight(flight) {
    setCreatedBooking(null);
    setSelectedFlight(flight);
  }

async function handleBookingCreated(booking) {
  setSelectedFlight(null);
  setCreatedBooking(booking);
  await loadFlights();
}

  return (
    <section className="flights-view">
      <div className="flights-heading">
        <div>
          <p className="dash-eyebrow">Available flights</p>
          <h1>Choose Your Journey</h1>
          <p>
            Browse current routes, fares and available seats before booking.
          </p>
        </div>

        <div className="flights-actions">
          <button className="flights-back-button" type="button" onClick={onBack}>
            ← Overview
          </button>

          <button
            className="flights-refresh-button"
            type="button"
            onClick={loadFlights}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {createdBooking && (
        <div className="flight-booking-success">
          <div className="flight-booking-success-icon">✓</div>

          <div>
            <p className="dash-eyebrow">Booking created</p>
            <h2>Your reservation is awaiting payment</h2>
            <p>
              Booking status: <strong>{createdBooking.bookingStatus}</strong>
              {' · '}
              Payment status: <strong>{createdBooking.paymentStatus}</strong>
            </p>
          </div>
        </div>
      )}

      {loading && (
        <div className="flights-state-card">
          <span className="flights-loader" />
          <h2>Loading available flights...</h2>
          <p>Retrieving live AeroLink flight information securely.</p>
        </div>
      )}

      {!loading && errorMessage && (
        <div className="flights-state-card">
          <span className="flights-state-icon">!</span>
          <h2>Unable to retrieve flights</h2>
          <p>{errorMessage}</p>
          <button type="button" onClick={loadFlights}>
            Try again
          </button>
        </div>
      )}

      {!loading && !errorMessage && flights.length === 0 && (
        <div className="flights-state-card">
          <span className="flights-state-icon">✈</span>
          <h2>No flights available</h2>
          <p>New AeroLink journeys will appear here when scheduled.</p>
        </div>
      )}

      {!loading && !errorMessage && flights.length > 0 && (
        <div className="flights-grid">
          {flights.map((flight) => (
            <article className="flight-result-card" key={flight.flightId}>
              <div className="flight-result-top">
                <div>
                  <span className="flight-number">{flight.flightNumber}</span>
                  <span className="flight-status">{flight.status}</span>
                </div>

                <strong className="flight-price">
                  {formatPrice(flight.price)}
                </strong>
              </div>

              <div className="flight-route">
                <div>
                  <small>From</small>
                  <strong>{flight.fromLocation}</strong>
                </div>

                <div className="flight-route-line">
                  <span>✈</span>
                </div>

                <div className="flight-route-destination">
                  <small>To</small>
                  <strong>{flight.toLocation}</strong>
                </div>
              </div>

              <div className="flight-times">
                <div>
                  <span>Departure</span>
                  <strong>{formatFlightDateTime(flight.departureTime)}</strong>
                </div>

                <div>
                  <span>Arrival</span>
                  <strong>{formatFlightDateTime(flight.arrivalTime)}</strong>
                </div>
              </div>

              <div className="flight-result-footer">
                <span>
                  <strong>{flight.availableSeats}</strong> seats available
                </span>

                <button
                  type="button"
                  onClick={() => handleSelectFlight(flight)}
                >
                  Select flight
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {selectedFlight && (
        <PassengerBookingForm
          flight={selectedFlight}
          onCancel={() => setSelectedFlight(null)}
          onBookingCreated={handleBookingCreated}
        />
      )}
    </section>
  );
}

export default PassengerFlights;