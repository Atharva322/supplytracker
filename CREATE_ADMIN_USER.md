# Bootstrap an Admin Safely

Public registration always creates `ROLE_USER`. It intentionally cannot create administrators.

For local/demo setup only, inject a strong bootstrap password before starting the backend:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="true"
$env:BOOTSTRAP_ADMIN_USERNAME="admin"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@supplytracker.local"
$env:BOOTSTRAP_ADMIN_PASSWORD="<your-unique-12+-character-password>"
$env:JWT_SECRET="<your-random-32+-character-jwt-secret>"
```

The bootstrap initializer is disabled by default and refuses passwords shorter than 12 characters. Do not commit either value. After creating the required admin, disable `BOOTSTRAP_ADMIN_ENABLED` for subsequent deployments.

Regular users register through `/api/auth/register` with `username`, `email`, and `password`; a caller-provided `roles` field is not part of the request contract.
