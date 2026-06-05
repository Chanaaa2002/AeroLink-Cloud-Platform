import { useEffect, useState } from 'react';
import { getMyNotifications } from '../services/apiClient';

function formatNotificationDate(value) {
  if (!value) {
    return 'Time unavailable';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

function getNotificationIcon(type) {
  if (type === 'PAYMENT_SUCCESS') {
    return '✓';
  }

  if (type === 'BAGGAGE_STATUS_UPDATE') {
    return '⌁';
  }

  return '◔';
}

function PassengerAlerts({ onBack }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  async function loadNotifications() {
    setLoading(true);
    setErrorMessage('');

    try {
      const result = await getMyNotifications();
      setNotifications(Array.isArray(result) ? result : []);
    } catch (error) {
      setErrorMessage(
        error.message || 'Unable to load your travel alerts at this time.'
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadNotifications();
  }, []);

  return (
    <section className="alerts-view">
      <div className="alerts-heading">
        <div>
          <p className="dash-eyebrow">Passenger alerts</p>
          <h1>Travel Notifications</h1>
          <p>
            Your latest payment confirmations and baggage journey updates.
          </p>
        </div>

        <div className="alerts-actions">
          <button className="alerts-back-button" type="button" onClick={onBack}>
            ← Overview
          </button>

          <button
            className="alerts-refresh-button"
            type="button"
            onClick={loadNotifications}
            disabled={loading}
          >
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {loading && (
        <div className="alerts-state-card">
          <span className="alerts-loader" />
          <h2>Loading your alerts...</h2>
          <p>Retrieving your notifications securely through AeroLink.</p>
        </div>
      )}

      {!loading && errorMessage && (
        <div className="alerts-state-card alerts-error">
          <span className="alerts-state-icon">!</span>
          <h2>Unable to retrieve alerts</h2>
          <p>{errorMessage}</p>
          <button type="button" onClick={loadNotifications}>
            Try again
          </button>
        </div>
      )}

      {!loading && !errorMessage && notifications.length === 0 && (
        <div className="alerts-state-card">
          <span className="alerts-state-icon">◔</span>
          <h2>No travel alerts yet</h2>
          <p>
            Payment confirmation and baggage status updates will appear here.
          </p>
        </div>
      )}

      {!loading && !errorMessage && notifications.length > 0 && (
        <div className="alerts-list">
          {notifications.map((notification, index) => (
            <article
              className="alert-card"
              key={notification.notificationId || `${notification.type}-${index}`}
            >
              <div className={`alert-icon alert-icon-${notification.type || 'DEFAULT'}`}>
                {getNotificationIcon(notification.type)}
              </div>

              <div className="alert-content">
                <div className="alert-title-row">
                  <h2>{notification.title || 'Travel update'}</h2>

                  <span className="alert-status">
                    {notification.status || 'UNREAD'}
                  </span>
                </div>

                <p>{notification.message || 'Your journey has been updated.'}</p>

                <div className="alert-meta">
                  <span>{formatNotificationDate(notification.createdAt)}</span>

                  {notification.baggageStatus && (
                    <span>Baggage: {notification.baggageStatus}</span>
                  )}

                  {notification.currentLocation && (
                    <span>Location: {notification.currentLocation}</span>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default PassengerAlerts;