# API Examples

All examples assume the API is running locally at `http://localhost:8080`
and use `jq` purely for readability in the sample output — it isn't
required to call the API itself.

## Register

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
        "username": "jane.doe",
        "email": "jane.doe@example.com",
        "password": "SecurePass123!",
        "firstName": "Jane",
        "lastName": "Doe",
        "phoneNumber": "+14155552671"
      }'
```

```json
{
  "message": "Registration successful. Please check your email to verify your account before logging in."
}
```

## Verify email

The verification email contains a link of the form
`http://localhost:5173/verify-email?token=<token>`. The frontend calls:

```bash
curl -s -X GET "http://localhost:8080/api/v1/auth/verify-email?token=<token>"
```

A `204 No Content` response indicates success.

## Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
        "usernameOrEmail": "jane.doe",
        "password": "SecurePass123!"
      }'
```

```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "username": "jane.doe",
  "email": "jane.doe@example.com",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "6RZ2f1n8QW3v...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "roles": ["EMPLOYEE"]
}
```

## Call a protected endpoint

```bash
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "username": "jane.doe",
  "email": "jane.doe@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "status": "ACTIVE",
  "emailVerified": true,
  "roles": ["EMPLOYEE"],
  "permissions": ["TASK_CREATE", "TASK_READ", "TASK_UPDATE", "PROJECT_READ", "DEPARTMENT_READ", "EMPLOYEE_READ"]
}
```

## Refresh an access token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "6RZ2f1n8QW3v..."}'
```

The response reuses the same shape as login, minus `userId`/`username`/`roles`.
The refresh token in the response is a **new** token — the one just
presented has already been revoked.

## Change password

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/change-password \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "currentPassword": "SecurePass123!",
        "newPassword": "EvenMoreSecure456!"
      }'
```

Returns `204 No Content` and revokes every refresh token on the account.

## Forgot / reset password

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.doe@example.com"}'

curl -s -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token": "<token-from-email>", "newPassword": "BrandNewPass789!"}'
```

## Search users (administrator)

```bash
curl -s "http://localhost:8080/api/v1/users?searchTerm=jane&status=ACTIVE&page=0&size=20&sort=lastName,asc" \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" | jq
```

```json
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "username": "jane.doe",
      "email": "jane.doe@example.com",
      "firstName": "Jane",
      "lastName": "Doe",
      "status": "ACTIVE",
      "emailVerified": true,
      "roles": ["EMPLOYEE"]
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## Assign a role to a user (administrator)

```bash
curl -s -X POST http://localhost:8080/api/v1/users/3fa85f64-5717-4562-b3fc-2c963f66afa6/roles \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleIds": ["b2f1e2a0-...-role-uuid"]}'
```

## Create a role with initial permissions (SUPER_ADMIN)

```bash
curl -s -X POST http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $SUPER_ADMIN_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "PROJECT_COORDINATOR",
        "description": "Coordinates cross-team project schedules",
        "hierarchyLevel": 50,
        "permissionIds": ["<project-read-permission-id>", "<task-read-permission-id>"]
      }'
```

## Error response shape

Every error, whether raised by a security filter or a controller, has the
same shape:

```json
{
  "timestamp": "2026-07-25T09:15:32.104Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "The username/email or password provided is incorrect",
  "path": "/api/v1/auth/login"
}
```

Bean Validation failures additionally populate `validationErrors`:

```json
{
  "timestamp": "2026-07-25T09:16:01.552Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/auth/register",
  "validationErrors": {
    "password": "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit and a special character"
  }
}
```
