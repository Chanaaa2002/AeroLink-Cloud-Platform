import { useEffect, useState } from 'react';
import { Authenticator } from '@aws-amplify/ui-react';
import '@aws-amplify/ui-react/styles.css';
import { signOut as amplifySignOut } from 'aws-amplify/auth';
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useNavigate,
} from 'react-router-dom';

import { getAuthenticatedUserContext } from './auth/authHelpers';
import DashboardHome from './dashboard/DashboardHome';
import './App.css';

function AuthenticationLoadingCard() {
  return (
    <div className="welcome-shell">
      <div className="welcome-card">
        <div className="welcome-icon">✈</div>
        <p className="eyebrow">Verifying access</p>
        <h1>Preparing your journey...</h1>
        <p className="welcome-copy">
          Checking your secure AeroLink account permissions.
        </p>
      </div>
    </div>
  );
}

function AuthenticatedRedirect({ signOut }) {
  const navigate = useNavigate();
  const [accessError, setAccessError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function redirectByRole() {
      try {
        const context = await getAuthenticatedUserContext();

        if (!isMounted) {
          return;
        }

        if (context.isStaff) {
          navigate('/staff/dashboard', { replace: true });
          return;
        }

        if (context.isPassenger) {
          navigate('/passenger/dashboard', { replace: true });
          return;
        }

        setAccessError(
          'Your account is authenticated, but no AeroLink application role is assigned.'
        );
      } catch (error) {
        console.error('Unable to read authenticated Cognito role:', error);

        if (isMounted) {
          setAccessError('Unable to verify your AeroLink account role.');
        }
      }
    }

    redirectByRole();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  if (!accessError) {
    return <AuthenticationLoadingCard />;
  }

  return (
    <div className="welcome-shell">
      <div className="welcome-card">
        <div className="welcome-icon">✈</div>
        <p className="eyebrow">Access unavailable</p>
        <h1>Unable to continue</h1>
        <p className="welcome-copy">{accessError}</p>

        <button className="signout-button" type="button" onClick={signOut}>
          Sign out
        </button>
      </div>
    </div>
  );
}

function ProtectedDashboard({ requiredRole }) {
  const navigate = useNavigate();
  const [checkingAccess, setCheckingAccess] = useState(true);
  const [accessAllowed, setAccessAllowed] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function validateDashboardAccess() {
      try {
        const context = await getAuthenticatedUserContext();

        if (!isMounted) {
          return;
        }

        const hasRequiredRole =
          requiredRole === 'STAFF'
            ? context.isStaff
            : context.isPassenger;

        if (hasRequiredRole) {
          setAccessAllowed(true);
          setCheckingAccess(false);
          return;
        }

        if (context.isStaff) {
          navigate('/staff/dashboard', { replace: true });
          return;
        }

        if (context.isPassenger) {
          navigate('/passenger/dashboard', { replace: true });
          return;
        }

        await amplifySignOut();
        navigate('/', { replace: true });
      } catch (error) {
        console.error('Dashboard access validation failed:', error);

        if (isMounted) {
          await amplifySignOut();
          navigate('/', { replace: true });
        }
      }
    }

    validateDashboardAccess();

    return () => {
      isMounted = false;
    };
  }, [navigate, requiredRole]);

  async function handleSignOut() {
    await amplifySignOut();
    navigate('/', { replace: true });
  }

  if (checkingAccess || !accessAllowed) {
    return (
      <main className="dash-page">
        <div className="dash-glow dash-glow-left" />
        <div className="dash-glow dash-glow-right" />

        <section className="dash-hero">
          <div>
            <p className="dash-eyebrow">Secure access</p>
            <h1>Loading your AeroLink dashboard...</h1>
            <p>Validating your Cognito role and preparing your portal.</p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <DashboardHome
      role={requiredRole}
      accountLabel={
        requiredRole === 'STAFF'
          ? 'Verified staff account'
          : 'Verified passenger account'
      }
      onSignOut={handleSignOut}
    />
  );
}

function AuthenticationPage() {
  return (
    <main className="aerolink-auth-page">
      <div className="sky-glow sky-glow-one" />
      <div className="sky-glow sky-glow-two" />
      <div className="stars" aria-hidden="true" />

      <header className="brand-header">
        <div className="brand-logo">
          <span className="brand-plane">✈</span>

          <div>
            <span className="brand-name">AeroLink</span>
            <span className="brand-tagline">Cloud Aviation Platform</span>
          </div>
        </div>
      </header>

      <section className="auth-layout">
        <div className="hero-panel">
          <p className="eyebrow">Seamless journeys begin here</p>

          <h1 className="hero-title">
            Fly smarter.
            <span>Stay connected.</span>
          </h1>

          <p className="hero-description">
            Discover flights, complete secure bookings, track baggage journeys
            and receive real-time travel notifications through AeroLink.
          </p>

          <div className="route-card">
            <div className="route-row">
              <div>
                <span className="route-label">Departure</span>
                <strong>CMB</strong>
                <small>Colombo</small>
              </div>

              <div className="route-line">
                <span className="route-plane">✈</span>
              </div>

              <div className="destination">
                <span className="route-label">Arrival</span>
                <strong>DXB</strong>
                <small>Dubai</small>
              </div>
            </div>

            <div className="flight-meta">
              <div>
                <span>Status</span>
                <strong className="status-ready">Ready for boarding</strong>
              </div>

              <div>
                <span>Platform</span>
                <strong>Cloud Native</strong>
              </div>
            </div>
          </div>
        </div>

        <div className="auth-panel">
          <div className="auth-panel-heading">
            <p className="eyebrow">AeroLink access</p>
            <h2>Welcome aboard</h2>
            <p>Sign in securely or create a new passenger account.</p>
          </div>

          <Authenticator
            loginMechanisms={['email']}
            signUpAttributes={['email']}
            formFields={{
              signIn: {
                username: {
                  label: 'Email address',
                  placeholder: 'Enter your email address',
                },
                password: {
                  label: 'Password',
                  placeholder: 'Enter your password',
                },
              },
              signUp: {
                email: {
                  label: 'Email address',
                  placeholder: 'Enter your email address',
                  order: 1,
                },
                password: {
                  label: 'Create password',
                  placeholder: 'Create a secure password',
                  order: 2,
                },
                confirm_password: {
                  label: 'Confirm password',
                  placeholder: 'Confirm your password',
                  order: 3,
                },
              },
              confirmSignUp: {
                confirmation_code: {
                  label: 'Confirmation code',
                  placeholder: 'Enter the code sent to your email',
                },
              },
            }}
          >
            {({ signOut }) => <AuthenticatedRedirect signOut={signOut} />}
          </Authenticator>
        </div>
      </section>

      <footer className="auth-footer">
        <span>© 2026 AeroLink</span>
        <span>Passenger portal · Secure cloud aviation experience</span>
      </footer>
    </main>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AuthenticationPage />} />

        <Route
          path="/passenger/dashboard"
          element={<ProtectedDashboard requiredRole="PASSENGER" />}
        />

        <Route
          path="/staff/dashboard"
          element={<ProtectedDashboard requiredRole="STAFF" />}
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;