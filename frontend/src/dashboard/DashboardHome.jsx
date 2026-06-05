import { useEffect, useRef, useState } from 'react';
import PassengerAlerts from './PassengerAlerts';
import PassengerFlights from './PassengerFlights';
import PassengerBookings from './PassengerBookings';
import PassengerBaggage from './PassengerBaggage';
import StaffBaggageOperations from './StaffBaggageOperations';
import StaffFlightManagement from './StaffFlightManagement';
import { getMyNotifications } from '../services/apiClient';
import './DashboardHome.css';
import './PassengerFlights.css';
import './PassengerBaggage.css';
import './StaffBaggageOperations.css';
import './StaffFlightManagement.css';
import './PaymentNotice.css';

const passengerFeatures = [
  {
    title: 'Available Flights',
    description: 'Browse active flight routes and updated ticket prices.',
    action: 'Search flights',
    icon: '✈',
    view: 'flights',
  },
  {
    title: 'My Bookings',
    description: 'View reservations and payment confirmation status.',
    action: 'View bookings',
    icon: '▣',
    view: 'bookings',
  },
  {
    title: 'Baggage Tracking',
    description: 'Follow your baggage journey and latest location.',
    action: 'Track baggage',
    icon: '⌁',
    view: 'baggage',
  },
  {
    title: 'Travel Alerts',
    description: 'Receive payment and baggage notifications instantly.',
    action: 'Open alerts',
    icon: '◔',
    view: 'alerts',
  },
];

const staffFeatures = [
  {
    title: 'Flight Management',
    description: 'Maintain flight availability, price and route information.',
    action: 'Manage flights',
    icon: '✈',
    view: 'flights',
  },
  {
    title: 'Baggage Operations',
    description: 'Update passenger baggage journey and transfer status.',
    action: 'Update baggage',
    icon: '⌁',
    view: 'baggage',
  },
 
];

function DashboardHome({ role, accountLabel, onSignOut }) {
  const isStaff = role === 'STAFF';
  const features = isStaff ? staffFeatures : passengerFeatures;

  const [activeView, setActiveView] = useState('overview');
  const [paymentNotice, setPaymentNotice] = useState(null);
  const [liveNotification, setLiveNotification] = useState(null);
  const latestNotificationKeyRef = useRef(null);

  useEffect(() => {
    const queryParams = new URLSearchParams(window.location.search);
    const paymentStatus = queryParams.get('payment');

    if (paymentStatus === 'success') {
      setPaymentNotice({
        type: 'success',
        title: 'Payment completed successfully',
        message:
          'Your Stripe Sandbox payment was completed. Refresh My Bookings and Alerts to see the confirmed booking and payment notification.',
      });

      window.history.replaceState({}, document.title, window.location.pathname);
    }

    if (paymentStatus === 'cancel') {
      setPaymentNotice({
        type: 'cancel',
        title: 'Payment was cancelled',
        message:
          'Your booking is still waiting for payment. You can continue payment anytime from My Bookings.',
      });

      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  useEffect(() => {
    if (isStaff) {
      return undefined;
    }

    function getNotificationKey(notification) {
      return (
        notification.notificationId ||
        `${notification.title || 'notification'}-${notification.createdAt || ''}`
      );
    }

    function getLatestNotification(notifications) {
      if (!Array.isArray(notifications) || notifications.length === 0) {
        return null;
      }

      return [...notifications].sort((first, second) => {
        const firstTime = new Date(first.createdAt || 0).getTime();
        const secondTime = new Date(second.createdAt || 0).getTime();

        return secondTime - firstTime;
      })[0];
    }

    async function checkForNewNotification(showPopup) {
      try {
        const notifications = await getMyNotifications();
        const latestNotification = getLatestNotification(notifications);

        if (!latestNotification) {
          return;
        }

        const latestKey = getNotificationKey(latestNotification);

        if (!latestNotificationKeyRef.current) {
          latestNotificationKeyRef.current = latestKey;
          return;
        }

        if (showPopup && latestKey !== latestNotificationKeyRef.current) {
          latestNotificationKeyRef.current = latestKey;

          setLiveNotification({
            type:
              latestNotification.type === 'PAYMENT_SUCCESS'
                ? 'success'
                : 'baggage',
            title: latestNotification.title || 'New travel notification',
            message:
              latestNotification.message ||
              'Your AeroLink journey has been updated.',
          });
        }
      } catch (error) {
        console.warn('Notification polling failed:', error);
      }
    }

    checkForNewNotification(false);

    const intervalId = window.setInterval(() => {
      checkForNewNotification(true);
    }, 10000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [isStaff]);

  function openFeature(feature) {
    if (isStaff) {
      if (feature.view === 'flights') {
        setActiveView('staff-flights');
      }

      if (feature.view === 'baggage') {
        setActiveView('staff-baggage');
      }

      return;
    }

    if (
      feature.view === 'flights' ||
      feature.view === 'bookings' ||
      feature.view === 'baggage' ||
      feature.view === 'alerts'
    ) {
      setActiveView(feature.view);
    }
  }

  const showingAlerts = !isStaff && activeView === 'alerts';
  const showingFlights = !isStaff && activeView === 'flights';
  const showingBookings = !isStaff && activeView === 'bookings';
  const showingBaggage = !isStaff && activeView === 'baggage';
  const showingStaffBaggage = isStaff && activeView === 'staff-baggage';
  const showingStaffFlights = isStaff && activeView === 'staff-flights';

  return (
    <main className="dash-page">
      <div className="dash-glow dash-glow-left" />
      <div className="dash-glow dash-glow-right" />

      <header className="dash-header">
        <div className="dash-brand">
          <span className="dash-brand-plane">✈</span>

          <div>
            <strong>AeroLink</strong>
            <small>Cloud Aviation Platform</small>
          </div>
        </div>

        <div className="dash-header-actions">
          <span className="dash-role-badge">
            {isStaff ? 'Staff Portal' : 'Passenger Portal'}
          </span>

          <div className="dash-account">
            <span>{accountLabel}</span>

            <button type="button" onClick={onSignOut}>
              Sign out
            </button>
          </div>
        </div>
      </header>

      {paymentNotice && (
        <div className="payment-popup-overlay">
          <div className={`payment-popup-card ${paymentNotice.type}`}>
            <div className="payment-popup-icon">
              {paymentNotice.type === 'success' ? '✓' : '!'}
            </div>

            <div className="payment-popup-content">
              <strong>{paymentNotice.title}</strong>
              <p>{paymentNotice.message}</p>
            </div>

            <button
              className="payment-popup-close"
              type="button"
              onClick={() => setPaymentNotice(null)}
              aria-label="Close payment message"
            >
              ×
            </button>
          </div>
        </div>
      )}

      {liveNotification && (
        <div className="payment-popup-overlay live-alert-popup">
          <div className="payment-popup-card success">
            <div className="payment-popup-icon">
              {liveNotification.type === 'success' ? '✓' : '⌁'}
            </div>

            <div className="payment-popup-content">
              <strong>{liveNotification.title}</strong>
              <p>{liveNotification.message}</p>
            </div>

            <button
              className="payment-popup-close"
              type="button"
              onClick={() => setLiveNotification(null)}
              aria-label="Close live notification"
            >
              ×
            </button>
          </div>
        </div>
      )}

      <div className="dash-layout">
        <aside className="dash-sidebar">
          <p className="dash-menu-title">Navigation</p>

          <nav className="dash-nav">
            <button
              className={activeView === 'overview' ? 'active' : ''}
              type="button"
              onClick={() => setActiveView('overview')}
            >
              <span>◈</span> Overview
            </button>

            <button
              className={
                (!isStaff && activeView === 'flights') ||
                (isStaff && activeView === 'staff-flights')
                  ? 'active'
                  : ''
              }
              type="button"
              onClick={() => {
                if (isStaff) {
                  setActiveView('staff-flights');
                } else {
                  setActiveView('flights');
                }
              }}
            >
              <span>✈</span> Flights
            </button>

            {!isStaff && (
              <>
                <button
                  className={activeView === 'bookings' ? 'active' : ''}
                  type="button"
                  onClick={() => setActiveView('bookings')}
                >
                  <span>▣</span> My Bookings
                </button>

                <button
                  className={activeView === 'baggage' ? 'active' : ''}
                  type="button"
                  onClick={() => setActiveView('baggage')}
                >
                  <span>⌁</span> Baggage
                </button>

                <button
                  className={activeView === 'alerts' ? 'active' : ''}
                  type="button"
                  onClick={() => setActiveView('alerts')}
                >
                  <span>◔</span> Alerts
                </button>
              </>
            )}

            {isStaff && (
              <button
                className={activeView === 'staff-baggage' ? 'active' : ''}
                type="button"
                onClick={() => setActiveView('staff-baggage')}
              >
                <span>⌁</span> Baggage Operations
              </button>
            )}
          </nav>

          <div className="dash-security-card">
            <span>Authenticated role</span>
            <strong>{isStaff ? 'STAFF' : 'PASSENGER'}</strong>
            <p>Verified through Amazon Cognito JWT access control.</p>
          </div>
        </aside>

        <section className="dash-content">
          {showingAlerts ? (
            <PassengerAlerts onBack={() => setActiveView('overview')} />
          ) : showingFlights ? (
            <PassengerFlights onBack={() => setActiveView('overview')} />
          ) : showingBookings ? (
            <PassengerBookings onBack={() => setActiveView('overview')} />
          ) : showingBaggage ? (
            <PassengerBaggage onBack={() => setActiveView('overview')} />
          ) : showingStaffBaggage ? (
            <StaffBaggageOperations onBack={() => setActiveView('overview')} />
          ) : showingStaffFlights ? (
            <StaffFlightManagement onBack={() => setActiveView('overview')} />
          ) : (
            <>
              <section className="dash-hero">
                <div>
                  <p className="dash-eyebrow">
                    {isStaff ? 'Operations Console' : 'Passenger Journey'}
                  </p>

                  <h1>
                    {isStaff
                      ? 'Welcome to the AeroLink Control Tower'
                      : 'Ready for your next journey?'}
                  </h1>

                  <p>
                    {isStaff
                      ? 'Manage permitted airline operations through secured cloud services.'
                      : 'Search flights, manage bookings and stay updated throughout your journey.'}
                  </p>
                </div>

                <div className="dash-hero-route">
                  <span>CMB</span>

                  <div>
                    <small>Live cloud route</small>
                    <strong>✈</strong>
                  </div>

                  <span>DXB</span>
                </div>
              </section>

              <div className="dash-status-row">
                <div>
                  <span>Authentication</span>
                  <strong>Verified</strong>
                </div>

                <div>
                  <span>Role Access</span>
                  <strong>{isStaff ? 'Staff' : 'Passenger'}</strong>
                </div>

                <div>
                  <span>API Connection</span>
                  <strong>{isStaff ? 'Next Step' : 'Connected'}</strong>
                </div>
              </div>

              <section className="dash-feature-grid">
                {features.map((feature) => (
                  <article className="dash-feature-card" key={feature.title}>
                    <span className="dash-feature-icon">{feature.icon}</span>
                    <h2>{feature.title}</h2>
                    <p>{feature.description}</p>

                    <button type="button" onClick={() => openFeature(feature)}>
                      {feature.action} →
                    </button>
                  </article>
                ))}
              </section>
            </>
          )}
        </section>
      </div>
    </main>
  );
}

export default DashboardHome;