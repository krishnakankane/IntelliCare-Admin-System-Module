# IntelliCare Backend — API Reference & Sample Requests

Base URL: `http://localhost:8080/api`  
Swagger UI: `http://localhost:8080/api/swagger-ui.html`

---

## 1. Authentication

### POST /auth/register
```json
// Request
{
  "email": "jane.doe@example.com",
  "password": "Secure@Pass1",
  "firstName": "Jane",
  "lastName": "Doe",
  "phoneNumber": "+919876543210"
}

// Response 201
{
  "success": true,
  "message": "User registered successfully",
  "timestamp": "2024-11-01T10:00:00Z",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 5,
      "email": "jane.doe@example.com",
      "firstName": "Jane",
      "lastName": "Doe",
      "roles": ["ROLE_PATIENT"]
    }
  }
}
```

### POST /auth/login
```json
// Request
{ "email": "admin@intellicare.com", "password": "Admin@12345" }

// Response 200
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "a1b2c3d4-...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

### POST /auth/refresh
```json
// Request
{ "refreshToken": "a1b2c3d4-..." }

// Response 200 — new token pair issued, old refresh token revoked
```

### POST /auth/logout
```json
// Header: Authorization: Bearer <access_token>
// Request
{ "refreshToken": "a1b2c3d4-..." }

// Response 200
{ "success": true, "message": "Logged out successfully" }
```

---

## 2. User Management

All endpoints require `Authorization: Bearer <token>`.

### GET /users/me
```json
// Response 200
{
  "success": true,
  "data": {
    "id": 5,
    "email": "jane.doe@example.com",
    "firstName": "Jane",
    "lastName": "Doe",
    "phoneNumber": "+919876543210",
    "isActive": true,
    "isVerified": false,
    "roles": ["ROLE_PATIENT"],
    "createdAt": "2024-11-01T10:00:00Z",
    "lastLoginAt": "2024-11-01T10:00:00Z"
  }
}
```

### GET /users?page=0&size=20  [ADMIN]
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  }
}
```

### GET /users/by-role/ROLE_DOCTOR  [ADMIN]

### PUT /users/{id}/roles  [ADMIN]
```json
// Request
{ "roles": ["ROLE_DOCTOR", "ROLE_ADMIN"] }
```

### PATCH /users/{id}/deactivate  [ADMIN]
```json
// Response 200
{ "success": true, "message": "User deactivated", "data": { "isActive": false, ... } }
```

---

## 3. Notifications

### POST /notifications/send  [ADMIN]
```json
// Request — Email
{
  "userId": 5,
  "title": "Appointment Reminder",
  "message": "You have an appointment tomorrow at 10am.",
  "channel": "EMAIL"
}

// Request — Template-based
{
  "userId": 5,
  "title": "Appointment Reminder",
  "message": "",
  "channel": "EMAIL",
  "templateName": "APPOINTMENT_REMINDER",
  "templateVariables": {
    "patientName": "Jane Doe",
    "doctorName": "Dr. Smith",
    "appointmentDate": "2024-11-02",
    "appointmentTime": "10:00 AM",
    "location": "Room 203, IntelliCare Clinic"
  }
}

// Response 201
{
  "success": true,
  "message": "Notification queued",
  "data": {
    "id": 12,
    "userId": 5,
    "channel": "EMAIL",
    "status": "PENDING",
    "isRead": false,
    "createdAt": "2024-11-01T10:05:00Z"
  }
}
```

### GET /notifications
```json
// Response — paginated list of user's own notifications
```

### PATCH /notifications/{id}/read
### PATCH /notifications/read-all
### GET /notifications/unread/count
```json
{ "success": true, "data": { "unreadCount": 3 } }
```

---

## 4. Audit Logs  [ADMIN only]

### GET /audit-logs/search?action=USER_LOGIN&from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "userId": 5,
        "username": "jane.doe@example.com",
        "action": "USER_LOGIN",
        "ipAddress": "103.21.58.10",
        "status": "SUCCESS",
        "createdAt": "2024-11-01T10:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### GET /audit-logs/user/{userId}

---

## 5. AI Orchestration

### POST /ai/chat
```json
// Request
{
  "prompt": "What are the side effects of Metformin?",
  "sessionId": "optional-session-uuid",
  "systemPrompt": "You are an AI health assistant. Provide accurate medical information."
}

// Response 200
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "content": "Metformin is generally well-tolerated. Common side effects include...",
    "model": "gpt-4o",
    "promptTokens": 25,
    "completionTokens": 118,
    "totalTokens": 143,
    "latencyMs": 1240,
    "mockResponse": true
  }
}
```

### POST /ai/transcribe
```json
// Request
{
  "audioData": "<base64-encoded-audio>",
  "format": "mp3",
  "language": "en"
}

// Response 200
{
  "data": {
    "transcript": "Schedule my appointment for next Monday.",
    "language": "en",
    "confidence": 0.95,
    "mockResponse": true
  }
}
```

### POST /ai/synthesize
```json
// Request
{ "text": "Your appointment is confirmed for November 5th at 10am." }

// Response 200
{
  "data": {
    "audioData": "<base64-encoded-mp3>",
    "format": "mp3",
    "latencyMs": 540,
    "mockResponse": true
  }
}
```

### GET /ai/conversations
### GET /ai/conversations/{sessionId}
### DELETE /ai/conversations/{sessionId}

---

## Error Response Format

All errors follow this envelope:

```json
{
  "success": false,
  "message": "Email already registered: jane.doe@example.com",
  "errorCode": "CONFLICT",
  "timestamp": "2024-11-01T10:00:00Z"
}
```

| HTTP | errorCode            | Trigger                        |
|------|----------------------|-------------------------------|
| 400  | BAD_REQUEST          | Invalid input data             |
| 400  | VALIDATION_ERROR     | Bean validation failure        |
| 401  | INVALID_CREDENTIALS  | Wrong email/password           |
| 401  | TOKEN_ERROR          | Expired/revoked token          |
| 401  | ACCOUNT_DISABLED     | Inactive account               |
| 403  | ACCESS_DENIED        | Insufficient role              |
| 404  | RESOURCE_NOT_FOUND   | Entity not found               |
| 409  | CONFLICT             | Duplicate data                 |
| 500  | INTERNAL_ERROR       | Unexpected server error        |
