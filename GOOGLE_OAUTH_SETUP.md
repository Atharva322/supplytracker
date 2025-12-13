# Google OAuth2 Setup Guide

## Step 1: Create Google OAuth2 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Google+ API**:
   - Go to "APIs & Services" > "Library"
   - Search for "Google+ API"
   - Click "Enable"

4. Create OAuth2 credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Select "Web application"
   - Name: "Agri Supply Tracker"
   - Add Authorized JavaScript origins:
     - `http://localhost:5173`
     - `http://localhost:8080`
   - Add Authorized redirect URIs:
     - `http://localhost:8080/login/oauth2/code/google`
   - Click "Create"

5. Copy the **Client ID** and **Client Secret**

## Step 2: Configure Backend

1. Open `supplytracker1/src/main/resources/application.properties`
2. Replace the placeholders with your actual values:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_ACTUAL_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_ACTUAL_CLIENT_SECRET
```

## Step 3: Rebuild and Restart

1. Stop the backend server (if running)
2. Rebuild:
   ```powershell
   cd c:\Users\athar\Desktop\supplytracker\supplytracker1
   mvn clean package -DskipTests
   ```
3. Start the backend:
   ```powershell
   java -jar target\supplytracker-1.0-SNAPSHOT.jar
   ```

## Step 4: Test Google Sign-In

1. Open http://localhost:5173
2. Click **"Sign in with Google"** button
3. Choose your Google account
4. Grant permissions
5. You'll be redirected back and automatically logged in!

## How It Works

1. User clicks "Sign in with Google"
2. Redirects to Google OAuth consent screen
3. User authorizes the app
4. Google redirects back to backend: `/login/oauth2/code/google`
5. Backend creates/finds user in MongoDB
6. Backend generates JWT token
7. Backend redirects to frontend with token in URL
8. Frontend extracts token and stores it
9. User is logged in!

## Default Permissions

Users who sign in with Google will be assigned **ROLE_USER** by default (read-only access).

To make a Google user an admin:
1. Login with Google first
2. Find the user in MongoDB
3. Update their roles to include "ROLE_ADMIN"

Or you can modify the `OAuth2LoginSuccessHandler.java` to assign admin role based on email domain or specific emails.

## Troubleshooting

### Error: "redirect_uri_mismatch"
- Make sure you added `http://localhost:8080/login/oauth2/code/google` to Authorized redirect URIs

### Error: "Invalid client"
- Check that Client ID and Client Secret are correct in application.properties

### Backend not starting
- Make sure Maven dependency was added correctly
- Run `mvn clean package -DskipTests` again

## Security Notes

- Never commit `application.properties` with real credentials to Git
- Consider using environment variables for production:
  ```properties
  spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
  spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
  ```
