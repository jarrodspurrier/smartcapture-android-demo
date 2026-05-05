{\rtf1\ansi\ansicpg1252\cocoartf2868
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\margl1440\margr1440\vieww12920\viewh7080\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural\partightenfactor0

\f0\fs24 \cf0 # IVS Verification API Reference\
\
> Machine-readable reference for agents and external integrators.\
> **Last updated:** 2026-04-17\
> **Base URL:** `https://ditto.gbg.com` (legacy: `https://app.art-of-sales-engineering.com` continues to be accepted during the transition period)\
> **Authentication:** Bearer token (IVS API key) on all endpoints unless noted otherwise.\
\
---\
\
## Endpoints\
\
The northbound API surface consists of seven endpoints. All endpoints accept and return JSON unless noted.\
\
| # | Method | Path | Purpose |\
|---|--------|------|---------|\
| 1 | `POST` | `/api/verification/sessions` | Create a verification session |\
| 2 | `POST` | `/api/verification/sessions/:sessionId/images` | Submit captured images (mobile DocAuth) |\
| 3 | `GET`  | `/api/verification/sessions/:sessionId` | Get session status and result |\
| 4 | `GET`  | `/api/verification/sessions/:sessionId/qr` | Get the QR code image for a session |\
| 5 | `GET`  | `/api/verification/sessions/:sessionId/validate` | Validate a session token (used by the hosted browser flow) |\
| 6 | `GET`  | `/api/verification/sessions/:sessionId/audit` | Retrieve raw engine request/response telemetry |\
| 7 | `POST` | `/api/verification/validate-key` | Test whether an API key is valid |\
\
---\
\
### 1. Create Session\
\
`POST /api/verification/sessions`\
\
Creates a new verification session. The response includes a `verifyUrl` (and optionally a `qrCode`) for handing the user off to the hosted verification flow, or a `sessionId` that a native mobile client can use with endpoint #2.\
\
**Auth:** Bearer API key, required.\
\
**Request body:**\
\
| Field | Type | Required | Description |\
|-------|------|----------|-------------|\
| `returnUrl` | string | Yes | URL to redirect the user to after the hosted flow finishes. |\
| `verificationType` | string | No | One of `docBio` (default), `dataBio`, `dataOnly`, `docAuth`. See *Verification Types*. |\
| `customerName` | string | No | Customer/company name shown in the verification UI header. |\
| `logoUrl` | string | No | URL to a logo image shown in the verification UI header. |\
| `referenceId` | string | No | Your internal reference ID; echoed back on every status response. |\
| `resourceId` | string | No | Journey resource ID (applies to `docBio`/`dataBio`/`dataOnly` only; ignored for `docAuth`). |\
| `includeQr` | boolean | No | If `true`, response includes a `qrCode` block with image URL and short URL. |\
| `customerData` | object | Conditional | Required for `dataBio`/`dataOnly`; optional for `docBio`; ignored for `docAuth`. See below. |\
| `branding` | object | No | UI color overrides. See below. |\
| `faceMatchEnabled` | boolean | No | `docAuth` only. When `true`, the capture flow includes a selfie step. Default `false`. |\
| `sensorType` | string | No | `docAuth` only. One of `Unknown`, `Camera`, `Scanner`, `Mobile`. Default `Mobile`. |\
| `authenticationSensitivity` | string | No | `docAuth` only. One of `Normal`, `High`, `Low`. Default `Normal`. |\
\
**`customerData` object:**\
\
| Field | Type | Required | Description |\
|-------|------|----------|-------------|\
| `firstName` | string | `dataBio`/`dataOnly` only | Customer's first name. |\
| `lastName` | string | `dataBio`/`dataOnly` only | Customer's last name. |\
| `dateOfBirth` | string | `dataBio`/`dataOnly` only | YYYY-MM-DD. |\
| `address` | string | No | Full address. |\
| `phone` | string | No | Phone number. |\
| `email` | string | No | Email address. |\
| `dlNumber` | string | No | Driver's license number. |\
| `dlState` | string | No | Two-letter state code. |\
| `ssn4` | string | No | Last 4 digits of SSN. |\
\
**`branding` object:**\
\
| Field | Type | Description |\
|-------|------|-------------|\
| `headerBgColor` | string | Hex color, e.g. `#1f2937`. |\
| `headerTextColor` | string | Hex color. |\
| `buttonColor` | string | Hex color. |\
\
**Response (201 Created):**\
\
```json\
\{\
  "sessionId": "vs_abc123def456",\
  "instanceId": null,\
  "verifyUrl": "https://ditto.gbg.com/verify?token=eyJ...",\
  "expiresAt": "2026-04-17T12:30:00.000Z",\
  "referenceId": "customer-12345",\
  "qrCode": \{\
    "imageUrl": "https://ditto.gbg.com/api/verification/sessions/vs_abc123def456/qr",\
    "shortCode": "X7K9M2",\
    "shortUrl": "https://ditto.gbg.com/v/X7K9M2",\
    "expiresAt": "2026-04-17T12:30:00.000Z"\
  \}\
\}\
```\
\
| Field | Type | Description |\
|-------|------|-------------|\
| `sessionId` | string | The session identifier; use with endpoints 2\'966. |\
| `instanceId` | string \\| null | Engine transaction ID; always `null` at creation, populated after verification completes. |\
| `verifyUrl` | string | Full URL to redirect the user for the hosted browser flow. |\
| `expiresAt` | string (ISO 8601) | Session expires 30 minutes after creation. |\
| `referenceId` | string \\| null | Echo of your `referenceId`. |\
| `qrCode` | object \\| undefined | Present only when `includeQr: true`. |\
\
**Constraints:**\
- Sessions expire 30 minutes after creation.\
- Rate limit: 100 requests per 15 minutes per API key.\
\
**curl:**\
```bash\
curl -X POST https://ditto.gbg.com/api/verification/sessions \\\
  -H "Content-Type: application/json" \\\
  -H "Authorization: Bearer YOUR_API_KEY" \\\
  -d '\{\
    "returnUrl": "https://yourapp.com/done",\
    "verificationType": "docAuth",\
    "customerName": "Acme Corp",\
    "faceMatchEnabled": false\
  \}'\
```\
\
---\
\
### 2. Submit Images (Mobile DocAuth)\
\
`POST /api/verification/sessions/:sessionId/images`\
\
Submits captured document images for a pending `docAuth` session. Returns **202 Accepted** immediately \'97 processing happens asynchronously. Poll endpoint #3 to retrieve the final result. Designed for native mobile clients (Android/iOS) using a document capture SDK.\
\
**Auth:** Bearer API key, required. Must be the same API key (or owner) that created the session.\
\
**Path parameters:**\
\
| Parameter | Type | Description |\
|-----------|------|-------------|\
| `sessionId` | string | The session identifier returned by endpoint #1. |\
\
**Request body:**\
\
| Field | Type | Required | Description |\
|-------|------|----------|-------------|\
| `frontImage` | string | Yes | Base64-encoded front of document with data URI prefix (`data:image/jpeg;base64,...`). Use the SDK's native output \'97 do not recompress. |\
| `backImage` | string | Yes | Base64-encoded back of document with data URI prefix. |\
| `faceImage` | string | Conditional | Base64-encoded selfie. Required only when the session was created with `faceMatchEnabled: true`. |\
\
**Response (202 Accepted):**\
\
```json\
\{\
  "sessionId": "vs_abc123def456",\
  "status": "processing"\
\}\
```\
\
**Errors:**\
\
| Status | Meaning |\
|--------|---------|\
| 400 | Missing required image, session is not `docAuth` type, or session already processed. |\
| 401 | Missing or invalid API key. |\
| 403 | Session belongs to a different API key owner. |\
| 404 | Session not found. |\
| 429 | Rate limit exceeded. |\
| 503 | Backing store temporarily unavailable. |\
\
**Constraints:**\
- A session can only be submitted **once**. A second submission to the same `sessionId` returns 400.\
- Sessions expire 30 minutes after creation.\
- After 202, poll endpoint #3 every 4 seconds \'97 see *Polling Pattern*.\
\
**curl:**\
```bash\
curl -X POST https://ditto.gbg.com/api/verification/sessions/vs_abc123def456/images \\\
  -H "Content-Type: application/json" \\\
  -H "Authorization: Bearer YOUR_API_KEY" \\\
  -d '\{\
    "frontImage": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",\
    "backImage":  "data:image/jpeg;base64,/9j/4AAQSkZJRg...",\
    "faceImage":  "data:image/jpeg;base64,/9j/4AAQSkZJRg..."\
  \}'\
```\
\
---\
\
### 3. Get Session Status\
\
`GET /api/verification/sessions/:sessionId`\
\
Returns the current status and (when terminal) the verification result.\
\
**Auth:** Bearer API key, required.\
\
**Path parameters:**\
\
| Parameter | Type | Description |\
|-----------|------|-------------|\
| `sessionId` | string | The session identifier. |\
\
**Response shape varies by verification type.** All sessions share the top-level envelope:\
\
| Field | Type | Description |\
|-------|------|-------------|\
| `sessionId` | string | The session identifier. |\
| `status` | string | See *Status Values* below. |\
| `instanceId` | string \\| null | Engine transaction ID; `null` until verification has been triggered. |\
| `firstName` | string \\| null | From request or extracted from document. |\
| `lastName` | string \\| null | From request or extracted from document. |\
| `referenceId` | string \\| null | Echo of your `referenceId`. |\
| `result` | object \\| null | Present once `status` is terminal. Shape depends on verification type \'97 see below. |\
\
**Status values:**\
\
| Status | Description |\
|--------|-------------|\
| `pending` | Session created, waiting for the user (or for `POST /images`) to begin. Continue polling. |\
| `completed` | Verification finished successfully \'97 inspect `result` for the outcome. |\
| `failed` | Verification could not be completed due to error or rejection. |\
| `expired` | Session expired before completion (30-minute timeout). |\
\
#### Result shape \'97 `docBio`, `dataBio`, `dataOnly`\
\
```json\
\{\
  "sessionId": "vs_abc123def456",\
  "status": "completed",\
  "instanceId": "txn-abc123",\
  "firstName": "John",\
  "lastName": "Smith",\
  "referenceId": "customer-12345",\
  "result": \{\
    "ivsOverallResult": "pass",\
    "decision": "Decision: Accept",\
    "documentVerified": true,\
    "faceVerified": true,\
    "livenessVerified": true,\
    "faceMatchScore": 84,\
    "livenessScore": 99,\
    "verificationSteps": [\
      \{ "step": 1, "label": "Document Classification", "status": "pass", "details": \{ "...": "..." \} \},\
      \{ "step": 2, "label": "Document Extraction",    "status": "pass", "details": \{ "...": "..." \} \},\
      \{ "step": 3, "label": "Document Authentication","status": "pass", "details": \{ "...": "..." \} \},\
      \{ "step": 4, "label": "Liveness Check",         "status": "pass", "score": 99 \},\
      \{ "step": 5, "label": "Face Match",             "status": "pass", "score": 84 \}\
    ]\
  \},\
  "rawResponse": \{ "...": "complete engine response with image data stripped" \}\
\}\
```\
\
#### Result shape \'97 `docAuth`\
\
```json\
\{\
  "sessionId": "vs_abc123def456",\
  "status": "completed",\
  "instanceId": "PiXyz123-AbCdEfGh",\
  "firstName": null,\
  "lastName": null,\
  "referenceId": "customer-12345",\
  "faceMatchEnabled": false,\
  "processingTimeMs": 4821,\
  "result": \{\
    "ivsOverallResult": "pass",\
    "decision": "Pass",\
    "documentVerified": true,\
    "documentExpired": false,\
    "attentionNotices": null,\
    "faceVerified": null,\
    "faceMatchScore": null,\
    "failureReason": null\
  \}\
\}\
```\
\
| `result` field | Type | Description |\
|----------------|------|-------------|\
| `ivsOverallResult` | string | `pass`, `warn`, `fail`, or `unknown`. |\
| `decision` | string \\| null | Human-readable label: `Pass`, `Attention`, `Fail`. |\
| `documentVerified` | boolean | `true` if the document passed or received attention status. |\
| `documentExpired` | boolean | `true` if the document was identified as expired. **Can be `true` even when `ivsOverallResult` is `pass`** \'97 surface this to your users explicitly. |\
| `attentionNotices` | string[] \\| null | Array of human-readable notes (e.g. expired document). `null` if none. |\
| `faceVerified` | boolean \\| null | `true`/`false` if face match was performed; `null` if `faceMatchEnabled` was `false`. |\
| `faceMatchScore` | number \\| null | 0\'96100. `null` if no face match. |\
| `failureReason` | string \\| null | Short reason when `ivsOverallResult` is `fail`. |\
\
Additional `docAuth` timing fields on the top-level response: `processingTimeMs`, `instanceCreateMs`, `imageUploadMs`, `assureidProcessingMs`, `faceIdMs` (number \\| null), `faceMatchEnabled` (boolean).\
\
**curl:**\
```bash\
curl https://ditto.gbg.com/api/verification/sessions/vs_abc123def456 \\\
  -H "Authorization: Bearer YOUR_API_KEY"\
```\
\
---\
\
### 4. Get QR Code Image\
\
`GET /api/verification/sessions/:sessionId/qr`\
\
Returns the QR code as a PNG image. Only available for sessions created with `includeQr: true`.\
\
**Auth:** Bearer API key, required (must be the API key that created the session).\
\
**Path parameters:**\
\
| Parameter | Type | Description |\
|-----------|------|-------------|\
| `sessionId` | string | The session identifier. |\
\
**Response:** `200 OK`, `Content-Type: image/png`, raw PNG bytes.\
\
**Errors:** `401` invalid key, `403` wrong owner, `404` session not found.\
\
**curl:**\
```bash\
curl https://ditto.gbg.com/api/verification/sessions/vs_abc123def456/qr \\\
  -H "Authorization: Bearer YOUR_API_KEY" \\\
  --output qr.png\
```\
\
---\
\
### 5. Validate Session Token\
\
`GET /api/verification/sessions/:sessionId/validate?token=<sessionToken>`\
\
Validates a session token (the encrypted token embedded in `verifyUrl`) and returns the session's branding and capture configuration. **Used by the hosted browser flow** to bootstrap the verification UI; rarely called directly by integrators.\
\
**Auth:** Session token (passed as `?token=`), not an API key.\
\
**Query parameters:**\
\
| Parameter | Type | Required | Description |\
|-----------|------|----------|-------------|\
| `token` | string | Yes | The session token from the `verifyUrl`. |\
\
**Response (200 OK):**\
\
```json\
\{\
  "valid": true,\
  "session": \{\
    "id": "vs_abc123def456",\
    "verificationType": "docAuth",\
    "firstName": "",\
    "lastName": "",\
    "logoUrl": "https://yourapp.com/logo.png",\
    "returnUrl": "https://yourapp.com/done",\
    "headerBgColor": "#1a365d",\
    "headerTextColor": "#ffffff",\
    "buttonColor": "#3182ce",\
    "faceMatchEnabled": false,\
    "sdkVersion": "1.1.12"\
  \}\
\}\
```\
\
**Errors:**\
\
| Status | Meaning |\
|--------|---------|\
| 400 | Token missing, or session already completed or expired. |\
| 401 | Token invalid, expired, or doesn't match this `sessionId`. |\
| 404 | Session not found. |\
\
---\
\
### 6. Get Session Audit Telemetry\
\
`GET /api/verification/sessions/:sessionId/audit`\
\
Returns the raw request and response payloads exchanged with the underlying verification engine, with image data stripped. Useful for support and debugging.\
\
**Auth:** Either a Bearer API key (must own the session) **or** a `?token=` session token.\
\
**Path parameters:**\
\
| Parameter | Type | Description |\
|-----------|------|-------------|\
| `sessionId` | string | The session identifier. |\
\
**Response (200 OK):**\
\
```json\
\{\
  "sessionId": "vs_abc123def456",\
  "status": "completed",\
  "gbgEnvironment": "GBG_DEMO",\
  "gbgJourneyId": "journey-xyz",\
  "request":  \{ "timestamp": "...", "endpoint": "...", "userData": \{ "...": "..." \}, "imageSizes": \{ "...": "..." \} \},\
  "response": \{ "timestamp": "...", "status": 200, "ok": true, "body": \{ "...": "..." \} \},\
  "statusHistory": [],\
  "createdAt": "2026-04-17T12:00:00.000Z",\
  "completedAt": "2026-04-17T12:05:30.000Z"\
\}\
```\
\
**Note:** The `request`/`response` audit fields are populated for hosted browser verification types (`docBio`, `dataBio`, `dataOnly`). For native mobile `docAuth` submissions made via endpoint #2, these fields will be `null` \'97 use endpoint #3 for the verification result instead.\
\
**Errors:** `401` unauthorized, `404` session not found.\
\
---\
\
### 7. Validate API Key\
\
`POST /api/verification/validate-key`\
\
Tests whether an API key is valid without performing any other action. Useful as the first call from a new integration to confirm credentials.\
\
**Auth:** None \'97 the API key is passed in the body, not the header.\
\
**Request body:**\
\
```json\
\{ "apiKey": "pk_live_abc123..." \}\
```\
\
**Response (200 OK):**\
\
```json\
\{ "valid": true \}\
```\
\
**Errors:**\
\
| Status | Meaning |\
|--------|---------|\
| 400 | `apiKey` field missing. |\
| 403 | API key is invalid or revoked. Response body: `\{ "valid": false, "error": "..." \}`. |\
\
**curl:**\
```bash\
curl -X POST https://ditto.gbg.com/api/verification/validate-key \\\
  -H "Content-Type: application/json" \\\
  -d '\{ "apiKey": "pk_live_abc123..." \}'\
```\
\
---\
\
## Verification Types\
\
| Type | PII Required | Document Capture | Biometrics | Native Mobile (endpoint #2) |\
|------|--------------|------------------|------------|------------------------------|\
| `docBio` (default) | Yes | Front + back | Selfie + liveness | No |\
| `dataBio` | Yes | Optional (up to 2) | Selfie + liveness | No |\
| `dataOnly` | Yes | None | None | No |\
| `docAuth` | No | Front + back | Optional face match | **Yes** |\
\
Only `docAuth` sessions accept image submissions through the native mobile endpoint (`POST /sessions/:sessionId/images`). For other verification types, send the user to the hosted flow via `verifyUrl`.\
\
---\
\
## Polling Pattern\
\
After endpoint #1 (or endpoint #2) returns, poll endpoint #3 until the session reaches a terminal status.\
\
- **Interval:** 4 seconds.\
- **Max attempts:** 10 (\uc0\u8776  40 seconds total wall-clock).\
- **Stop polling when:** `status` is `completed`, `failed`, or `expired`.\
- **Sessions expire:** 30 minutes after creation. If you receive `expired`, the session cannot be reused.\
- **Single submission:** A `docAuth` session can only be submitted once via endpoint #2 \'97 a second POST returns 400.\
\
```javascript\
async function pollForCompletion(sessionId, apiKey, \{ intervalMs = 4000, maxAttempts = 10 \} = \{\}) \{\
  for (let i = 0; i < maxAttempts; i++) \{\
    const res = await fetch(`https://ditto.gbg.com/api/verification/sessions/$\{sessionId\}`, \{\
      headers: \{ Authorization: `Bearer $\{apiKey\}` \}\
    \});\
    const data = await res.json();\
    if (['completed', 'failed', 'expired'].includes(data.status)) return data;\
    await new Promise(r => setTimeout(r, intervalMs));\
  \}\
  throw new Error('Polling timed out');\
\}\
```\
\
---\
\
## Error Codes\
\
| HTTP | Meaning |\
|------|---------|\
| 200 | OK. |\
| 201 | Created (session creation). |\
| 202 | Accepted \'97 request received, processing asynchronously. |\
| 400 | Bad request: missing required field, invalid data, session in wrong state, or duplicate submission. |\
| 401 | Unauthorized: missing or invalid API key (or session token where applicable). |\
| 403 | Forbidden: API key does not own the resource, or key revoked. |\
| 404 | Not found: session does not exist. |\
| 429 | Rate limit exceeded. |\
| 500 | Internal error. |\
| 503 | Backing store temporarily unavailable. |\
\
All error responses are JSON with at least an `error` field describing the problem:\
\
```json\
\{ "error": "Session not found" \}\
```\
\
---\
\
## Data Types\
\
- **`sessionId`** \'97 opaque string, prefix `vs_`. Treat as case-sensitive.\
- **`instanceId`** \'97 opaque engine transaction ID. Useful for support queries; do not parse.\
- **Timestamps** \'97 all timestamps are ISO 8601 strings in UTC (e.g. `2026-04-17T12:30:00.000Z`).\
- **Images** \'97 base64-encoded with a `data:image/jpeg;base64,` (or `image/png`) prefix. Use SDK-native output; do not recompress.\
- **Hex colors** \'97 6-digit `#RRGGBB`.\
\
---\
\
## Rate Limits\
\
| Endpoint | Default limit |\
|----------|---------------|\
| `POST /sessions` | 100 requests / 15 minutes / API key |\
| `POST /sessions/:id/images` | 100 requests / 15 minutes / API key |\
| `GET /sessions/:id` | 200 requests / 15 minutes / API key |\
| `GET /sessions/:id/qr` | 100 requests / 15 minutes / API key |\
\
Per-key limits may be raised on request \'97 contact the IVS team.\
\
---\
\
## Security Notes\
\
- API keys are secret. Never expose them in client-side code, mobile binaries, or browser network calls.\
- Sessions expire 30 minutes after creation; QR codes and short URLs expire with their session.\
- Verification results are retained for 30 days.\
- ID document and selfie images are not stored in long-term storage \'97 only metadata and verification results are persisted.\
- All endpoints are served over HTTPS only.\
}