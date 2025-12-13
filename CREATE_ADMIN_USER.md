# How to Create an Admin User

## Using cURL (Windows PowerShell)

```powershell
# Create an admin user
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"admin","email":"admin@example.com","password":"admin123","roles":["ROLE_ADMIN"]}'

# Create a regular user
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"user","email":"user@example.com","password":"user123"}'
```

## Using the Frontend

1. Open http://localhost:5173 in your browser
2. Click "Register" 
3. Enter username, email, and password
4. Click "Register" button
5. **Note**: Regular users will be created by default through the UI. To create an admin, use the PowerShell command above.

## Testing Role-Based Access

### As Admin User
- Login with `admin / admin123`
- You should see " Administrator" in the header
- You can:
  - View all products
  - Create new products (+ Add product button visible)
  - Edit products (Edit button visible in table)
  - Delete products (Delete button visible in table)
  - Export to CSV

### As Regular User
- Login with `user / user123`
- You should see " User" in the header
- You can:
  - View all products
  - Search and filter products
  - Export to CSV
  - **Cannot** create, edit, or delete products (buttons hidden/disabled)

## Direct API Testing

### Login as Admin
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'

$token = $response.token
Write-Host "Admin Token: $token"
Write-Host "Roles: $($response.roles -join ', ')"
```

### Try to Create Product as Admin
```powershell
# Use the $token from above
Invoke-RestMethod -Uri "http://localhost:8080/api/products" `
  -Method POST `
  -Headers @{Authorization="Bearer $token"} `
  -ContentType "application/json" `
  -Body '{"name":"Mango","type":"Fruit","batchId":"MNG001","harvestDate":"2024-01-15","originFarmId":"FARM001"}'
```

### Try to Create Product as Regular User (Should Fail)
```powershell
# Login as regular user
$userResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"user","password":"user123"}'

$userToken = $userResponse.token

# This should return 403 Forbidden
Invoke-RestMethod -Uri "http://localhost:8080/api/products" `
  -Method POST `
  -Headers @{Authorization="Bearer $userToken"} `
  -ContentType "application/json" `
  -Body '{"name":"Apple","type":"Fruit","batchId":"APP001","harvestDate":"2024-01-15","originFarmId":"FARM002"}'
```

## Role-Based Endpoints Summary

| Endpoint | Method | ROLE_USER | ROLE_ADMIN |
|----------|--------|-----------|------------|
| `/api/auth/login` | POST | ✅ Public | ✅ Public |
| `/api/auth/register` | POST | ✅ Public | ✅ Public |
| `/api/products` | GET | ✅ Yes | ✅ Yes |
| `/api/products/{id}` | GET | ✅ Yes | ✅ Yes |
| `/api/products/search` | GET | ✅ Yes | ✅ Yes |
| `/api/products` | POST | ❌ No | ✅ Yes |
| `/api/products/{id}` | PUT | ❌ No | ✅ Yes |
| `/api/products/{id}` | PATCH | ❌ No | ✅ Yes |
| `/api/products/{id}` | DELETE | ❌ No | ✅ Yes |

## What Changed?

### Backend Changes:
1. ✅ Added `@EnableMethodSecurity` to `SecurityConfig.java`
2. ✅ Added `@PreAuthorize("hasRole('ADMIN')")` to create/update/delete endpoints in `ProductController.java`
3. ✅ Modified `AuthResponse.java` to include `Set<String> roles`
4. ✅ Modified `RegisterRequest.java` to accept roles during registration
5. ✅ Updated `AuthController` login/register to return user roles

### Frontend Changes:
1. ✅ Added `userRoles` state to track current user's roles
2. ✅ Store/retrieve roles from localStorage
3. ✅ Conditionally show/hide "Add Product" button based on ROLE_ADMIN
4. ✅ Conditionally show/hide Edit/Delete buttons in table based on ROLE_ADMIN
5. ✅ Added role badge in header (👑 Administrator or 👤 User)

## Troubleshooting

### Error: 403 Forbidden
- This means the user doesn't have the required role (ROLE_ADMIN) for the operation
- Regular users can only view products, not modify them

### Error: 401 Unauthorized
- This means the JWT token is missing or invalid
- Re-login to get a fresh token

### Can't see Admin features
- Make sure you created the user with ROLE_ADMIN using the PowerShell command
- Check localStorage in browser DevTools - roles should be stored as JSON array
- Logout and login again to refresh the session
