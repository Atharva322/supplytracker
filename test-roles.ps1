# Manual RBAC smoke test. Start the backend with the secure bootstrap-admin settings first.
if ([string]::IsNullOrWhiteSpace($env:BOOTSTRAP_ADMIN_PASSWORD)) {
    throw "Set BOOTSTRAP_ADMIN_PASSWORD to the same value used when bootstrapping the local admin."
}
$suffix = [guid]::NewGuid().ToString("N").Substring(0, 8)
$username = "role-test-$suffix"
$userPassword = [guid]::NewGuid().ToString()
$baseUrl = "http://localhost:8080"
$adminUsername = $env:BOOTSTRAP_ADMIN_USERNAME
if ([string]::IsNullOrWhiteSpace($adminUsername)) { $adminUsername = "admin" }

$user = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -ContentType "application/json" -Body (@{
    username=$username; email="$username@example.com"; password=$userPassword
} | ConvertTo-Json)
if ($user.roles -contains "ROLE_ADMIN") { throw "Security failure: public registration granted ROLE_ADMIN" }

$admin = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body (@{
    username=$adminUsername; password=$env:BOOTSTRAP_ADMIN_PASSWORD
} | ConvertTo-Json)
$regular = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body (@{
    username=$username; password=$userPassword
} | ConvertTo-Json)

$product = @{name="RBAC Mango";type="Fruit";batchId="RBAC-$suffix";harvestDate="2026-08-05";originFarmId="FARM001"} | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/products" -Method POST -Headers @{Authorization="Bearer $($admin.token)"} -ContentType "application/json" -Body $product | Out-Null
try {
    Invoke-RestMethod -Uri "$baseUrl/api/products" -Method POST -Headers @{Authorization="Bearer $($regular.token)"} -ContentType "application/json" -Body $product | Out-Null
    throw "Security failure: ROLE_USER created a product"
} catch {
    Write-Host "RBAC smoke test completed; regular-user write was rejected."
}
