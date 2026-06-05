import { useEffect, useState } from 'react';
import { createFlight, getFlights, updateFlight } from '../services/apiClient';
import './StaffFlightManagement.css';

const statusOptions = ['SCHEDULED', 'DELAYED', 'BOARDING', 'CANCELLED', 'COMPLETED'];

const initialFlightForm = {
  flightId: '',
  flightNumber: '',
  fromLocation: 'Colombo',
  toLocation: 'Dubai',
  departureTime: '',
  arrivalTime: '',
  price: 0,
  availableSeats: 0,
  status: 'SCHEDULED',
};

function StaffFlightManagement({ onBack }) {
  const [flights, setFlights] = useState([]);
  const [selectedFlightId, setSelectedFlightId] = useState('');
  const [form, setForm] = useState(initialFlightForm);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  async function loadFlights() {
    setLoading(true);
    setMessage('');
    setErrorMessage('');

    try {
      const result = await getFlights();
      setFlights(Array.isArray(result) ? result : []);
    } catch (error) {
      setErrorMessage(error.message || 'Unable to load flights.');
    } finally {
      setLoading(false);
    }
  }

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]:
        name === 'price' || name === 'availableSeats'
          ? Number(value)
          : value,
    }));
  }

  function handleSelectFlight(event) {
    const flightId = event.target.value;
    setSelectedFlightId(flightId);
    setMessage('');
    setErrorMessage('');

    const selected = flights.find((flight) => flight.flightId === flightId);

    if (selected) {
      setForm({
        flightId: selected.flightId || '',
        flightNumber: selected.flightNumber || '',
        fromLocation: selected.fromLocation || '',
        toLocation: selected.toLocation || '',
        departureTime: selected.departureTime || '',
        arrivalTime: selected.arrivalTime || '',
        price: Number(selected.price || 0),
        availableSeats: Number(selected.availableSeats || 0),
        status: selected.status || 'SCHEDULED',
      });
    }
  }

  function resetForm() {
    setSelectedFlightId('');
    setForm(initialFlightForm);
    setMessage('');
    setErrorMessage('');
  }

  async function handleCreateFlight(event) {
    event.preventDefault();

    if (!form.flightId.trim() || !form.flightNumber.trim()) {
      setErrorMessage('Flight ID and flight number are required.');
      return;
    }

    setWorking(true);
    setMessage('');
    setErrorMessage('');

    try {
      await createFlight({
        ...form,
        flightId: form.flightId.trim(),
        flightNumber: form.flightNumber.trim(),
      });

      setMessage(`Flight ${form.flightNumber} created successfully.`);
      await loadFlights();
      resetForm();
    } catch (error) {
      setErrorMessage(error.message || 'Unable to create flight.');
    } finally {
      setWorking(false);
    }
  }

  async function handleUpdateFlight(event) {
    event.preventDefault();

    if (!selectedFlightId) {
      setErrorMessage('Please select a flight before updating.');
      return;
    }

    setWorking(true);
    setMessage('');
    setErrorMessage('');

    try {
      await updateFlight(selectedFlightId, form);
      setMessage(`Flight ${form.flightNumber} updated successfully.`);
      await loadFlights();
    } catch (error) {
      setErrorMessage(error.message || 'Unable to update flight.');
    } finally {
      setWorking(false);
    }
  }

  useEffect(() => {
    loadFlights();
  }, []);

  return (
    <section className="staff-flight-view">
      <div className="staff-flight-heading">
        <div>
          <p className="dash-eyebrow">Staff operations</p>
          <h1>Flight Management</h1>
          <p>
            Create flights and maintain route, schedule, fare, seat and status details.
          </p>
        </div>

        <div className="staff-flight-actions">
          <button type="button" onClick={onBack}>
            ← Overview
          </button>

          <button type="button" onClick={loadFlights} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {message && (
        <div className="staff-flight-message success">
          <strong>Operation completed</strong>
          <span>{message}</span>
        </div>
      )}

      {errorMessage && (
        <div className="staff-flight-message error">
          <strong>Action failed</strong>
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="staff-flight-grid">
        <article className="staff-flight-panel">
          <h2>Create / Update Flight</h2>
          <p>Select an existing flight to update it, or clear the form to add a new flight.</p>

          <label>
            Select existing flight
            <select value={selectedFlightId} onChange={handleSelectFlight}>
              <option value="">Create new flight</option>

              {flights.map((flight) => (
                <option value={flight.flightId} key={flight.flightId}>
                  {flight.flightNumber} · {flight.fromLocation} to {flight.toLocation}
                </option>
              ))}
            </select>
          </label>

          <form className="staff-flight-form">
            <div className="staff-flight-two">
              <label>
                Flight ID
                <input
                  name="flightId"
                  value={form.flightId}
                  onChange={handleChange}
                  placeholder="FL003"
                  disabled={Boolean(selectedFlightId)}
                />
              </label>

              <label>
                Flight number
                <input
                  name="flightNumber"
                  value={form.flightNumber}
                  onChange={handleChange}
                  placeholder="AL303"
                />
              </label>
            </div>

            <div className="staff-flight-two">
              <label>
                From
                <input
                  name="fromLocation"
                  value={form.fromLocation}
                  onChange={handleChange}
                  placeholder="Colombo"
                />
              </label>

              <label>
                To
                <input
                  name="toLocation"
                  value={form.toLocation}
                  onChange={handleChange}
                  placeholder="Dubai"
                />
              </label>
            </div>

            <div className="staff-flight-two">
              <label>
                Departure time
                <input
                  name="departureTime"
                  type="datetime-local"
                  value={form.departureTime}
                  onChange={handleChange}
                />
              </label>

              <label>
                Arrival time
                <input
                  name="arrivalTime"
                  type="datetime-local"
                  value={form.arrivalTime}
                  onChange={handleChange}
                />
              </label>
            </div>

            <div className="staff-flight-three">
              <label>
                Price LKR
                <input
                  name="price"
                  type="number"
                  min="0"
                  value={form.price}
                  onChange={handleChange}
                />
              </label>

              <label>
                Seats
                <input
                  name="availableSeats"
                  type="number"
                  min="0"
                  value={form.availableSeats}
                  onChange={handleChange}
                />
              </label>

              <label>
                Status
                <select name="status" value={form.status} onChange={handleChange}>
                  {statusOptions.map((status) => (
                    <option value={status} key={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="staff-flight-form-actions">
              <button
                type="button"
                onClick={handleCreateFlight}
                disabled={working || Boolean(selectedFlightId)}
              >
                {working ? 'Saving...' : 'Create flight'}
              </button>

              <button
                type="button"
                onClick={handleUpdateFlight}
                disabled={working || !selectedFlightId}
              >
                {working ? 'Updating...' : 'Update flight'}
              </button>

              <button type="button" onClick={resetForm}>
                Clear
              </button>
            </div>
          </form>
        </article>

        <article className="staff-flight-list-panel">
          <div className="staff-flight-list-heading">
            <h2>Current Flights</h2>
            <span>{flights.length} flight(s)</span>
          </div>

          {loading ? (
            <div className="staff-flight-empty">
              <span>✈</span>
              <p>Loading flight records...</p>
            </div>
          ) : flights.length === 0 ? (
            <div className="staff-flight-empty">
              <span>✈</span>
              <p>No flights found yet.</p>
            </div>
          ) : (
            <div className="staff-flight-list">
              {flights.map((flight) => (
                <article className="staff-flight-card" key={flight.flightId}>
                  <div className="staff-flight-card-top">
                    <div>
                      <small>{flight.flightId}</small>
                      <h3>{flight.flightNumber}</h3>
                    </div>

                    <span>{flight.status}</span>
                  </div>

                  <div className="staff-flight-route">
                    <strong>{flight.fromLocation}</strong>
                    <span>✈</span>
                    <strong>{flight.toLocation}</strong>
                  </div>

                  <div className="staff-flight-meta">
                    <span>LKR {Number(flight.price || 0).toLocaleString('en-LK')}</span>
                    <span>{flight.availableSeats} seats</span>
                  </div>

                  <button type="button" onClick={() => handleSelectFlight({ target: { value: flight.flightId } })}>
                    Edit flight
                  </button>
                </article>
              ))}
            </div>
          )}
        </article>
      </div>
    </section>
  );
}

export default StaffFlightManagement;