import { useEffect, useState } from 'react';
import {
  createBaggageForBooking,
  getAllBookings,
  getBaggageByBookingId,
  updateBaggageStatus,
} from '../services/apiClient';
import './StaffBaggageOperations.css';

const baggageStatusTransitions = {
  CHECKED_IN: ['LOADED', 'DELAYED'],
  LOADED: ['IN_TRANSIT', 'DELAYED'],
  IN_TRANSIT: ['ARRIVED', 'DELAYED'],
  ARRIVED: ['COLLECTED'],
  DELAYED: ['LOADED', 'IN_TRANSIT', 'ARRIVED'],
  COLLECTED: [],
};

const locationOptions = [
  'Check-in Counter',
  'Baggage Drop',
  'Security Screening',
  'Sorting Area',
  'Loading Bay',
  'Aircraft Hold',
  'Departure Gate',
  'Transfer Handling Area',
  'In Transit',
  'Destination Airport',
  'Arrival Hall',
  'Baggage Claim Belt',
  'Lost and Found Desk',
  'Delivered to Passenger',
];

function formatStatus(status) {
  return status ? status.replaceAll('_', ' ') : 'UNKNOWN';
}

function StaffBaggageOperations({ onBack }) {
  const [bookings, setBookings] = useState([]);
  const [selectedBookingId, setSelectedBookingId] = useState('');
  const [baggageItems, setBaggageItems] = useState([]);
  const [currentLocation, setCurrentLocation] = useState('Check-in Counter');
  const [updateLocation, setUpdateLocation] = useState('Transfer Handling Area');
  const [updateStatus, setUpdateStatus] = useState('');
  const [selectedBaggageId, setSelectedBaggageId] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const confirmedBookings = bookings.filter(
    (booking) =>
      booking.bookingStatus === 'CONFIRMED' ||
      booking.paymentStatus === 'PAID'
  );

  const selectedBaggage = baggageItems.find(
    (item) => item.baggageId === selectedBaggageId
  );

  const currentBaggageStatus = (
    selectedBaggage?.status ||
    selectedBaggage?.baggageStatus ||
    ''
  ).toUpperCase();

  const allowedNextStatuses =
    baggageStatusTransitions[currentBaggageStatus] || [];

  async function loadBookings() {
    setLoading(true);
    setErrorMessage('');
    setMessage('');

    try {
      const result = await getAllBookings();
      setBookings(Array.isArray(result) ? result : []);
    } catch (error) {
      setErrorMessage(error.message || 'Unable to load staff booking records.');
    } finally {
      setLoading(false);
    }
  }

  async function loadBaggageForBooking(bookingId) {
    if (!bookingId) {
      setBaggageItems([]);
      setSelectedBaggageId('');
      return;
    }

    setErrorMessage('');
    setMessage('');

    try {
      const result = await getBaggageByBookingId(bookingId);
      const baggageList = Array.isArray(result) ? result : [];

      setBaggageItems(baggageList);

      if (baggageList.length > 0) {
        setSelectedBaggageId(baggageList[0].baggageId);
      } else {
        setSelectedBaggageId('');
      }
    } catch (error) {
      setErrorMessage(error.message || 'Unable to load baggage for booking.');
      setBaggageItems([]);
      setSelectedBaggageId('');
    }
  }

  async function handleSelectBooking(event) {
    const bookingId = event.target.value;
    setSelectedBookingId(bookingId);
    await loadBaggageForBooking(bookingId);
  }

  async function handleCreateBaggage(event) {
    event.preventDefault();

    if (!selectedBookingId) {
      setErrorMessage('Please select a confirmed booking first.');
      return;
    }

    setWorking(true);
    setErrorMessage('');
    setMessage('');

    try {
      const created = await createBaggageForBooking(
        selectedBookingId,
        currentLocation
      );

      setMessage(
        `Baggage created successfully: ${created.tagNumber || created.baggageId}`
      );

      await loadBaggageForBooking(selectedBookingId);
    } catch (error) {
      setErrorMessage(error.message || 'Unable to create baggage.');
    } finally {
      setWorking(false);
    }
  }

  async function handleUpdateBaggage(event) {
    event.preventDefault();

    if (!selectedBaggageId) {
      setErrorMessage('Please select a baggage record to update.');
      return;
    }

    if (!updateStatus) {
      setErrorMessage('No valid next status is available for this baggage record.');
      return;
    }

    setWorking(true);
    setErrorMessage('');
    setMessage('');

    try {
      const updated = await updateBaggageStatus(
        selectedBaggageId,
        updateStatus,
        updateLocation
      );

      setMessage(
        `Baggage updated to ${updated.status || updated.baggageStatus || updateStatus}. Passenger notification will be created through EventBridge and Lambda.`
      );

      await loadBaggageForBooking(selectedBookingId);
    } catch (error) {
      setErrorMessage(error.message || 'Unable to update baggage status.');
    } finally {
      setWorking(false);
    }
  }

  useEffect(() => {
    loadBookings();
  }, []);

  useEffect(() => {
    if (allowedNextStatuses.length > 0) {
      setUpdateStatus((previousStatus) =>
        allowedNextStatuses.includes(previousStatus)
          ? previousStatus
          : allowedNextStatuses[0]
      );
    } else {
      setUpdateStatus('');
    }
  }, [selectedBaggageId, currentBaggageStatus]);

  return (
    <section className="staff-baggage-view">
      <div className="staff-baggage-heading">
        <div>
          <p className="dash-eyebrow">Staff operations</p>
          <h1>Baggage Control</h1>
          <p>
            Create baggage records for confirmed bookings and update baggage journey status.
          </p>
        </div>

        <div className="staff-baggage-actions">
          <button type="button" onClick={onBack}>
            ← Overview
          </button>

          <button type="button" onClick={loadBookings} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {message && (
        <div className="staff-baggage-message success">
          <strong>Operation completed</strong>
          <span>{message}</span>
        </div>
      )}

      {errorMessage && (
        <div className="staff-baggage-message error">
          <strong>Action failed</strong>
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="staff-baggage-grid">
        <article className="staff-baggage-panel">
          <h2>Select confirmed booking</h2>
          <p>Only confirmed or paid bookings are shown here.</p>

          <label>
            Booking
            <select value={selectedBookingId} onChange={handleSelectBooking}>
              <option value="">Select booking</option>

              {confirmedBookings.map((booking) => (
                <option value={booking.bookingId} key={booking.bookingId}>
                  {booking.flightId} · {booking.passengerName} · {booking.seatCount} seats
                </option>
              ))}
            </select>
          </label>

          <form onSubmit={handleCreateBaggage}>
            <label>
              Initial baggage location
              <select
                value={currentLocation}
                onChange={(event) => setCurrentLocation(event.target.value)}
              >
                {locationOptions.map((location) => (
                  <option value={location} key={location}>
                    {location}
                  </option>
                ))}
              </select>
            </label>

            <button type="submit" disabled={working || !selectedBookingId}>
              {working ? 'Processing...' : 'Create baggage'}
            </button>
          </form>
        </article>

        <article className="staff-baggage-panel">
          <h2>Update baggage journey</h2>
          <p>Status updates automatically create passenger notifications.</p>

          <form onSubmit={handleUpdateBaggage}>
            <label>
              Baggage record
              <select
                value={selectedBaggageId}
                onChange={(event) => setSelectedBaggageId(event.target.value)}
              >
                <option value="">Select baggage</option>

                {baggageItems.map((item) => (
                  <option value={item.baggageId} key={item.baggageId}>
                    {item.tagNumber || item.baggageId} · {formatStatus(item.status || item.baggageStatus)}
                  </option>
                ))}
              </select>
            </label>

            {selectedBaggage && (
              <p className="staff-baggage-helper">
                Current status: <strong>{formatStatus(currentBaggageStatus)}</strong>
              </p>
            )}

            <label>
              New status
              <select
                value={updateStatus}
                onChange={(event) => setUpdateStatus(event.target.value)}
                disabled={!selectedBaggageId || allowedNextStatuses.length === 0}
              >
                {allowedNextStatuses.length === 0 ? (
                  <option value="">No further movement available</option>
                ) : (
                  allowedNextStatuses.map((status) => (
                    <option value={status} key={status}>
                      {formatStatus(status)}
                    </option>
                  ))
                )}
              </select>
            </label>

            <label>
              Current location
              <select
                value={updateLocation}
                onChange={(event) => setUpdateLocation(event.target.value)}
              >
                {locationOptions.map((location) => (
                  <option value={location} key={location}>
                    {location}
                  </option>
                ))}
              </select>
            </label>

            <button
              type="submit"
              disabled={working || !selectedBaggageId || allowedNextStatuses.length === 0}
            >
              {working ? 'Updating...' : 'Update baggage status'}
            </button>
          </form>
        </article>
      </div>

      <section className="staff-baggage-list">
        <div className="staff-baggage-list-heading">
          <h2>Baggage records for selected booking</h2>
          <span>{baggageItems.length} record(s)</span>
        </div>

        {baggageItems.length === 0 ? (
          <div className="staff-baggage-empty">
            <span>⌁</span>
            <p>No baggage records found for this booking yet.</p>
          </div>
        ) : (
          baggageItems.map((item) => (
            <article className="staff-baggage-card" key={item.baggageId}>
              <div>
                <small>Tag number</small>
                <strong>{item.tagNumber || item.baggageId}</strong>
              </div>

              <div>
                <small>Status</small>
                <strong>{formatStatus(item.status || item.baggageStatus)}</strong>
              </div>

              <div>
                <small>Location</small>
                <strong>{item.currentLocation || 'Not updated'}</strong>
              </div>
            </article>
          ))
        )}
      </section>
    </section>
  );
}

export default StaffBaggageOperations;