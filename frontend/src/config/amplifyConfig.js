import { Amplify } from 'aws-amplify';

const userPoolId = import.meta.env.VITE_COGNITO_USER_POOL_ID;
const userPoolClientId = import.meta.env.VITE_COGNITO_USER_POOL_CLIENT_ID;

if (!userPoolId || !userPoolClientId) {
  throw new Error(
    'Missing Cognito configuration. Check the frontend .env.local file.'
  );
}

const amplifyConfig = {
  Auth: {
    Cognito: {
      userPoolId,
      userPoolClientId,
      loginWith: {
        email: true,
      },
      signUpVerificationMethod: 'code',
      userAttributes: {
        email: {
          required: true,
        },
      },
    },
  },
};

Amplify.configure(amplifyConfig);

export default amplifyConfig;