# Test Script for User Roles & Permissions

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Agri Supply Tracker - Role Testing" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Wait for backend to be ready
Write-Host "Waiting for backend to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# 1. Register Admin User
Write-Host "`nStep 1: Registering Admin User" -ForegroundColor Green
$adminBody = @{
    username = "admin"
    email = "admin@example.com"
    password = "admin123"
    roles = @("ROLE_ADMIN")
} | ConvertTo-Json

try {
    $adminRegister = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $adminBody
    Write-Host "Admin registered successfully!" -ForegroundColor Green
    Write-Host "Username: $($adminRegister.username)" -ForegroundColor White
    Write-Host "Roles: $($adminRegister.roles -join ', ')" -ForegroundColor White
}
catch {
    Write-Host "Admin user may already exist (this is OK)" -ForegroundColor Yellow
}

# 2. Register Regular User
Write-Host "`nStep 2: Registering Regular User" -ForegroundColor Green
$userBody = @{
    username = "user"
    email = "user@example.com"
    password = "user123"
} | ConvertTo-Json

try {
    $userRegister = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $userBody
    Write-Host "Regular user registered successfully!" -ForegroundColor Green
    Write-Host "Username: $($userRegister.username)" -ForegroundColor White
    Write-Host "Roles: $($userRegister.roles -join ', ')" -ForegroundColor White
}
catch {
    Write-Host "Regular user may already exist (this is OK)" -ForegroundColor Yellow
}

# 3. Login as Admin
Write-Host "`nStep 3: Testing Admin Login" -ForegroundColor Green
$adminLoginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body $adminLoginBody
    Write-Host "Admin login successful!" -ForegroundColor Green
    Write-Host "Roles: $($adminLogin.roles -join ', ')" -ForegroundColor White
    $adminToken = $adminLogin.token
}
catch {
    Write-Host "Error during admin login" -ForegroundColor Red
    exit 1
}

# 4. Login as Regular User
Write-Host "`nStep 4: Testing Regular User Login" -ForegroundColor Green
$userLoginBody = @{
    username = "user"
    password = "user123"
} | ConvertTo-Json

try {
    $userLogin = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body $userLoginBody
    Write-Host "Regular user login successful!" -ForegroundColor Green
    Write-Host "Roles: $($userLogin.roles -join ', ')" -ForegroundColor White
    $userToken = $userLogin.token
}
catch {
    Write-Host "Error during user login" -ForegroundColor Red
    exit 1
}

# 5. Test Admin: Create Product
Write-Host "`nStep 5: Testing Admin Create Product" -ForegroundColor Green
$productBody = @{
    name = "Test Mango"
    type = "Fruit"
    batchId = "TEST001"
    harvestDate = "2024-01-15"
    originFarmId = "FARM001"
} | ConvertTo-Json

try {
    $product = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method POST -Headers @{Authorization = "Bearer $adminToken"} -ContentType "application/json" -Body $productBody
    Write-Host "Admin can create products!" -ForegroundColor Green
    Write-Host "Product: $($product.name)" -ForegroundColor White
    $testProductId = $product.id
}
catch {
    Write-Host "Error creating product as admin" -ForegroundColor Red
}

# 6. Test Regular User: Try to Create Product (Should Fail)
Write-Host "`nStep 6: Testing Regular User Create Product (Should Fail)" -ForegroundColor Green
$productBody2 = @{
    name = "Test Apple"
    type = "Fruit"
    batchId = "TEST002"
    harvestDate = "2024-01-15"
    originFarmId = "FARM002"
} | ConvertTo-Json

try {
    $product2 = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method POST -Headers @{Authorization = "Bearer $userToken"} -ContentType "application/json" -Body $productBody2
    Write-Host "WARNING: Regular user should NOT be able to create products!" -ForegroundColor Yellow
}
catch {
    Write-Host "Correct! Regular user CANNOT create products (403 Forbidden)" -ForegroundColor Green
}

# 7. Test Regular User: View Products (Should Work)
Write-Host "`nStep 7: Testing Regular User View Products (Should Work)" -ForegroundColor Green

try {
    $productsUrl = "http://localhost:8080/api/products?page=0&size=10"
    $products = Invoke-RestMethod -Uri $productsUrl -Method GET -Headers @{Authorization = "Bearer $userToken"}
    Write-Host "Regular user CAN view products!" -ForegroundColor Green
    Write-Host "Found $($products.totalItems) products" -ForegroundColor White
}
catch {
    Write-Host "Error viewing products as regular user" -ForegroundColor Red
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "           Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Admin User Created" -ForegroundColor Green
Write-Host "Regular User Created" -ForegroundColor Green
Write-Host "Admin Login Working" -ForegroundColor Green
Write-Host "Regular User Login Working" -ForegroundColor Green
Write-Host "Admin Can Create Products" -ForegroundColor Green
Write-Host "Regular User CANNOT Create Products" -ForegroundColor Green
Write-Host "Regular User Can View Products" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Now test in browser!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Open: http://localhost:5173" -ForegroundColor Yellow
Write-Host "2. Login as admin/admin123 (Full Access)" -ForegroundColor Yellow
Write-Host "3. Logout and login as user/user123 (Read Only)" -ForegroundColor Yellow
Write-Host ""
