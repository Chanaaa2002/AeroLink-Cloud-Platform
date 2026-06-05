import { fetchAuthSession } from 'aws-amplify/auth';

export async function getAuthenticatedUserContext() {
  const session = await fetchAuthSession();
  const accessToken = session.tokens?.accessToken;

  if (!accessToken) {
    throw new Error('No authenticated Cognito access token is available.');
  }

  const payload = accessToken.payload;
  const groups = Array.isArray(payload['cognito:groups'])
    ? payload['cognito:groups']
    : [];

  return {
    accessToken: accessToken.toString(),
    groups,
    isPassenger: groups.includes('PASSENGER'),
    isStaff: groups.includes('STAFF'),
  };
}

export async function getAccessToken() {
  const context = await getAuthenticatedUserContext();
  return context.accessToken;
}