# 🏥 MediQueue — Hospital Queue Management System

> **Real-time patient queue management built with Spring Boot 3, Thymeleaf, WebSockets, JWT security, and comprehensive reporting.**

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)

---

## ✨ Features

| Category | Details |
|---|---|
| **Authentication** | JWT in HttpOnly cookie, role-based access control (ADMIN / DOCTOR / RECEPTIONIST) |
| **Queue Management** | Real-time queue with priority levels (Critical / High / Normal / Low), auto ticket generation, capacity enforcement |
| **Live Updates** | WebSocket (STOMP over SockJS) — queue changes broadcast instantly to all connected clients |
| **Staff Chat** | Real-time group chat rooms per department with typing indicators |
| **Reports** | Excel (.xlsx via Apache POI) and PDF (.pdf via OpenPDF) — downloadable or emailed async |
| **Email** | Thymeleaf HTML emails: queue confirmation, appointment reminder, report delivery |
| **Audit Logs** | Every state-changing HTTP request is logged with user, IP, before/after values |
| **Seeded Database** | 8 departments, 2 admins, 6 doctors, 3 receptionists, 12 patients, and 13 queue entries — ready to explore |
| **Docker** | Multi-stage Dockerfile + docker-compose with PostgreSQL |

---

## 🗂️ Project Structure

```
hospital-queue/
├── src/main/java/com/hospital/queue/
│   ├── config/              # Security, WebSocket, Cache, MVC, Async
│   ├── controller/          # Auth, Admin, Doctor, Receptionist, Report, Chat
│   ├── domain/
│   │   ├── entity/          # User, Patient, Doctor, Department, QueueEntry, AuditLog, ChatMessage
│   │   └── enums/           # Role, QueueStatus, Priority, DoctorStatus
│   ├── dto/
│   │   ├── request/         # LoginRequest, RegisterRequest, PatientRequest, QueueEntryRequest
│   │   └── response/        # ApiResponse, AuthResponse, QueueEntryResponse, DashboardStats
│   ├── exception/           # GlobalExceptionHandler, custom exceptions
│   ├── interceptor/         # AuditInterceptor
│   ├── repository/          # Spring Data JPA repositories
│   ├── security/            # JWT provider, CustomUserDetails, JwtAuthFilter
│   ├── service/             # Business logic (Auth, Queue, Doctor, Patient, Report, Email, Chat)
│   └── util/                # QueueNumberGenerator, SecurityUtils
│
├── src/main/resources/
│   ├── db/migration/        # Flyway V1 (schema), V2 (seed users/doctors), V3 (seed patients)
│   ├── templates/           # Thymeleaf: auth, admin, doctor, receptionist, chat, reports, email, error
│   ├── static/css/          # app.css — complete design system
│   ├── static/js/           # app.js, queue.js (WebSocket), chat.js (WebSocket)
│   └── application.yml      # dev (H2) + prod (PostgreSQL) profiles
│
├── Dockerfile               # Multi-stage build
├── docker-compose.yml       # App + PostgreSQL
├── .env.example             # Environment variables template
└── pom.xml                  # All Maven dependencies
```

---

## 🚀 Quick Start

### Option A — Run with H2 (zero setup, 30 seconds)

**Requirements:** Java 21, Maven 3.9+

```bash
# Clone / unzip the project
cd hospital-queue

# Run with the dev profile (H2 in-memory, no config needed)
mvn spring-boot:run

# App starts at http://localhost:8080
```

### Option B — Run with Docker Compose (PostgreSQL)

**Requirements:** Docker 24+, Docker Compose

```bash
# 1. Copy and fill environment variables
cp .env.example .env
nano .env            # set DB password, JWT secret, mail credentials

# 2. Build and start
docker compose up --build

# App starts at http://localhost:8080
# PostgreSQL at localhost:5432
```

### Option C — Run with a local PostgreSQL

```bash
# 1. Create database
createdb hospitaldb

# 2. Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/hospitaldb
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=yourpassword
export JWT_SECRET=your-64-char-secret-here

# 3. Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 🔑 Default Credentials

All passwords are **`Password@123`**

| Username | Role | Portal |
|---|---|---|
| `admin` | Administrator | `/admin/dashboard` |
| `superadmin` | Administrator | `/admin/dashboard` |
| `dr.adewale` | Doctor (Emergency) | `/doctor/dashboard` |
| `dr.okonkwo` | Doctor (General Practice) | `/doctor/dashboard` |
| `dr.hassan` | Doctor (Cardiology) | `/doctor/dashboard` |
| `dr.eze` | Doctor (Pediatrics) | `/doctor/dashboard` |
| `dr.ibrahim` | Doctor (Orthopedics) | `/doctor/dashboard` |
| `dr.osei` | Doctor (Dermatology) | `/doctor/dashboard` |
| `rec.aisha` | Receptionist | `/receptionist/dashboard` |
| `rec.funke` | Receptionist | `/receptionist/dashboard` |
| `rec.musa` | Receptionist | `/receptionist/dashboard` |

**Dev extras:**
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:hospitaldb`, user: `sa`, password: `password`)

---

## 🛠️ Configuration

### `application.yml` Profiles

| Profile | Database | When to use |
|---|---|---|
| `dev` (default) | H2 in-memory | Local development, demos |
| `prod` | PostgreSQL via env vars | Staging, production |

### Key Environment Variables (production)

| Variable | Description | Example |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/hospitaldb` |
| `DATABASE_USERNAME` | DB user | `hospitaluser` |
| `DATABASE_PASSWORD` | DB password | `s3cr3t` |
| `JWT_SECRET` | HMAC secret, min 64 chars | (random string) |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `MAIL_HOST` | SMTP server | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | `noreply@hospital.com` |
| `MAIL_PASSWORD` | SMTP app password | (app-specific password) |

---

## 📡 WebSocket Topics

| Topic | Description |
|---|---|
| `/topic/queue/{deptId}` | Queue updates for a specific department |
| `/topic/queue/global` | Global queue events (all departments) |
| `/topic/doctor-status` | Doctor availability changes |
| `/topic/chat/{roomId}` | Chat messages for a specific room |
| `/topic/chat/{roomId}/typing` | Typing indicator events |

**Connect via:**
```javascript
const socket = new SockJS('/ws');
const client = Stomp.over(socket);
client.connect({}, () => {
  client.subscribe('/topic/queue/global', (frame) => {
    const payload = JSON.parse(frame.body);
    console.log('Queue event:', payload.event);
  });
});
```

---

## 🌐 REST API

All API endpoints return `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Operation result",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00"
}
```

### Auth
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Login, returns JWT + sets cookie |
| `POST` | `/api/auth/register` | Register new user |

### Receptionist
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/receptionist/queue` | Add patient to queue |
| `POST` | `/receptionist/queue/{deptId}/call-next` | Call next waiting patient |
| `GET` | `/receptionist/patients/search?q=` | Search patients |

### Doctor
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/doctor/status?status=AVAILABLE` | Update doctor status |
| `POST` | `/doctor/queue/{entryId}/status?status=COMPLETED` | Update queue entry status |

### Reports
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/reports/excel?from=2024-01-01&to=2024-01-31` | Download Excel report |
| `GET` | `/reports/pdf?from=2024-01-01&to=2024-01-31` | Download PDF report |
| `POST` | `/reports/email` | Email report to current user |

### Chat
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/chat/{roomId}/history?limit=50` | Get message history |

---

## 🏛️ Architecture

```
Browser ──────────────────────────────────────── Spring Boot App
  │                                                     │
  │── HTTP Requests ──────► SecurityFilterChain        │
  │                            │                        │
  │                    JwtAuthenticationFilter          │
  │                            │                        │
  │                    AuthorizationFilter               │
  │                            │                        │
  │                       Controllers ──────► Services ─┤
  │                                              │       │
  │                                        Repository    │
  │                                              │       │
  │                                      H2 / PostgreSQL │
  │                                                      │
  │── WebSocket (STOMP/SockJS) ───► WebSocketConfig      │
  │                                   │                  │
  │                            /topic/queue/* ────► Queue Updates
  │                            /topic/chat/*  ────► Chat Messages
  │                            /topic/doctor-status ──► Status
```

---

## 🎨 UI Design System

- **Fonts:** Syne (display headers) + DM Sans (body)
- **Palette:** Navy `#1a3a52` · Teal `#0d9488` · Sky `#0ea5e9`
- **Components:** Stat cards, doctor status board, queue cards with priority bars, chat bubbles, ticket output, report cards
- **Responsive:** Mobile sidebar collapses to slide-in overlay at 900px breakpoint

---

## 🗄️ Database Schema

```sql
departments    -- id, name, code, max_queue, active
users          -- id, username, email, password_hash, full_name, role, enabled
doctors        -- id, user_id, department_id, specialization, license_number, status
patients       -- id, patient_number, full_name, phone, dob, blood_type, allergies
queue_entries  -- id, ticket_number, patient_id, doctor_id, department_id, priority, status, timestamps
chat_messages  -- id, room_id, sender_id, content, sent_at
audit_logs     -- id, username, action, entity_type, old_value, new_value, ip_address, performed_at
```

Migrations run automatically via **Flyway** on startup:
- `V1__init_schema.sql` — creates all tables + indexes
- `V2__seed_data.sql` — seeds departments, users, doctors
- `V3__seed_patients_and_queue.sql` — seeds patients + today's queue entries

---

## 🧪 Testing the System

After login as `admin / Password@123`:

1. **Admin Dashboard** → See live doctor status board and queue stats
2. **Users** → Create a new doctor or receptionist
3. **Reports** → Download Excel or PDF for today's queue

After login as `rec.aisha / Password@123`:

4. **Register Patient** → Search for `Emeka` or register new patient
5. **Add to Queue** → Select department, choose priority, submit — see ticket generated
6. **Dashboard** → Click "Call Next" on any department

After login as `dr.adewale / Password@123`:

7. **Doctor Dashboard** → See your queue; click **Call → Start → Done** buttons
8. **Status Switcher** → Toggle between Available / On Break / Busy

From any account:

9. **Staff Chat** → Open `/chat` — messages appear in real time across tabs
10. **H2 Console** → `http://localhost:8080/h2-console` — inspect live data

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3.4, Java 21 |
| Security | Spring Security 6, JJWT 0.12.6 (JWT in HttpOnly cookie) |
| Persistence | Spring Data JPA, Hibernate, Flyway |
| Database | H2 (dev), PostgreSQL 16 (prod) |
| Real-time | Spring WebSocket, STOMP, SockJS |
| Frontend | Thymeleaf 3.1, SockJS 1.6, STOMP.js 2.3, Lucide Icons |
| Email | Spring Mail, Thymeleaf HTML templates |
| Reports | Apache POI 5.3 (Excel), OpenPDF 1.3 (PDF) |
| Utilities | Lombok, MapStruct, Spring Cache |
| Infrastructure | Docker, Docker Compose, Maven |

---

## 📋 Production Checklist

- [ ] Set a strong `JWT_SECRET` (≥ 64 random characters)
- [ ] Set `POSTGRES_PASSWORD` to something strong
- [ ] Configure real SMTP credentials (`MAIL_*` variables)
- [ ] Enable HTTPS and uncomment `cookie.setSecure(true)` in `AuthService`
- [ ] Change default admin passwords immediately after first login
- [ ] Consider enabling Spring Security's `cookie.setSameSite("Strict")`
- [ ] Set up backups for PostgreSQL (`pg_dump`)
- [ ] Optionally add Nginx reverse proxy (template commented in docker-compose.yml)
- [ ] Monitor via `/actuator/health` and `/actuator/metrics`

---

## 📄 License

MIT — free for personal and commercial use.

---

*Built with ❤️ using Spring Boot, Thymeleaf, and WebSockets*
