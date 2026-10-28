# MediCare — Smart Medicine Reminder and Doctor Appointment System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%2B%20%2F%2026-orange.svg)](https://www.oracle.com/java/)
[![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20H2%20(MySQL%20Mode)-blue.svg)](https://www.mysql.com/)
[![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20CSS3%20%7C%20Vanilla%20JS-teal.svg)](#frontend)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**MediCare** is a full-stack, enterprise-grade healthcare web application designed for patients, clinics, and medical practitioners. It bridges the gap between patient treatment compliance and clinic scheduling through automated medicine reminders, pill inventory tracking, live camera & file prescription uploads, doctor appointment scheduling with double-booking prevention, and comprehensive adherence reporting.

---

## 1. Technology Stack

### Frontend
- **HTML5**: Semantic, accessible markup across 10 dedicated view templates.
- **CSS3**: Custom design system featuring CSS variables, responsive grids/flexbox, animations, and instant light/dark mode persistence.
- **Vanilla JavaScript**: Pure modern ES6+ utilizing the standard `Fetch` API, with no external frameworks (no React, Angular, or Vue).
- **Web APIs**: Integrated `navigator.mediaDevices.getUserMedia` for capturing live prescriptions directly from laptop or mobile webcams onto an HTML5 Canvas.

### Backend
- **Java 21 / 26**: Built using modern LTS Java.
- **Spring Boot 3.3.4**: Core application foundation.
- **Spring Web**: RESTful APIs with standardized JSON response envelopes and custom status codes.
- **Spring Data JPA & Hibernate**: Object-relational mapping, transaction management, and parameterized SQL queries.
- **Spring Security 6 & JJWT (0.12.6)**: Stateless authentication via JWT Bearer tokens and BCrypt password hashing.
- **Bean Validation (Jakarta Validation)**: Server-side validation for dates, passwords, doses, and appointment times.
- **Spring Task Scheduling**: Background engine that generates daily doses idempotently without duplicates.

### Database
- **MySQL 8.0+**: Production database with 9 normalized tables, foreign keys, indexes, and unique constraints.
- **H2 Persistent (MySQL Mode)**: Included as a zero-configuration developer profile (`application-dev.properties`) so students and evaluators can run the project immediately without needing a local MySQL server installed.

---

## 2. Project Directory Structure

```text
medicare/
├── frontend/                               # Standalone vanilla frontend
│   ├── index.html                          # Modern landing page
│   ├── login.html                          # Patient sign-in page
│   ├── register.html                       # Account registration page
│   ├── dashboard.html                      # Personalized patient control center
│   ├── medicines.html                      # Medicine CRUD and pill inventory
│   ├── schedule.html                       # Multi-time daily reminders & weekdays
│   ├── prescriptions.html                  # File upload & live camera photo capture
│   ├── appointments.html                   # Doctor directory & appointment booking
│   ├── reports.html                        # Compliance analytics, CSV & PDF export
│   ├── profile.html                        # User profile & security settings
│   ├── css/
│   │   ├── style.css                       # Color palette, themes, cards, modals, toast alerts
│   │   ├── auth.css                        # Dedicated authentication and profile styles
│   │   └── responsive.css                  # Mobile, tablet, desktop breakpoints & drawer
│   ├── js/
│   │   ├── api.js                          # Centralized Fetch wrapper, tokens & toast system
│   │   ├── auth.js                         # Authentication state, login & registration logic
│   │   ├── dashboard.js                    # Metrics, today's schedule & direct dose actions
│   │   ├── medicines.js                    # Medicine CRUD, search, filter & stock refill
│   │   ├── schedule.js                     # Reminder creation, timelines & status toggles
│   │   ├── prescriptions.js                # WebCam capture, canvas preview & secure downloads
│   │   ├── appointments.js                 # Doctor directory, dynamic slots & double-booking checks
│   │   ├── reports.js                      # Adherence calculation, bar charts & exports
│   │   └── profile.js                      # Profile editing & password change
│   └── assets/                             # Image and vector assets
│
├── medicare-backend/                       # Spring Boot backend
│   ├── pom.xml                             # Maven build configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/medicare/
│       │   │   ├── MedicareBackendApplication.java
│       │   │   ├── config/WebMvcConfig.java
│       │   │   ├── controller/             # REST API Controllers (Auth, Meds, Sched, etc.)
│       │   │   ├── dto/                    # Request & Response Data Transfer Objects
│       │   │   ├── entity/                 # JPA Entities (User, Medicine, Doctor, etc.)
│       │   │   ├── exception/              # Global Exception Handler & Custom Exceptions
│       │   │   ├── repository/             # Spring Data JPA Repositories
│       │   │   ├── scheduler/              # Daily Dose Generation Scheduler
│       │   │   ├── security/               # Spring Security 6, JWT Utils & Auth Filters
│       │   │   └── service/                # Business Logic Services
│       │   └── resources/
│       │       ├── application.properties  # Main configuration
│       │       ├── application-dev.properties # Embedded MySQL mode profile
│       │       ├── application-mysql.properties # Live MySQL server profile
│       │       └── static/                 # Synchronized frontend assets for unified serving
│       └── test/java/com/medicare/         # Automated unit and integration tests
│
├── database/
│   └── schema.sql                          # MySQL 8.x normalized table definition script
├── .env.example                            # Configuration environment template
├── .gitignore                              # Git exclusion rules
└── README.md                               # Comprehensive documentation
```

---

## 3. Key Features Walkthrough

### 1. Modern Landing Page (`index.html`)
- Medical cross logo and brand identity with white/blue theme and teal accents.
- Responsive navigation with Light/Dark mode toggle.
- Hero introduction, feature overview cards, 3-step workflow, clinical benefits, and footer.

### 2. Authentication & Profile Security (`login.html`, `register.html`, `profile.html`)
- User registration with name, email, phone, date of birth, emergency contact, and timezone.
- Password hashing using BCrypt with minimum length validation.
- Duplicate account prevention on email.
- Stateless authentication issuing signed JWT tokens.
- Protected profile management and password change forms.

### 3. Personalized Dashboard (`dashboard.html`)
- Personalized time-aware greeting (Good Morning, Good Afternoon, Good Evening).
- Key metrics: Taken Today, Pending Doses, Skipped Doses, Daily Adherence Rate.
- Next reminder card with immediate **Take Now** action.
- Today's medication cards with direct **✓ Taken**, **✕ Skip**, and **⏰ Snooze** actions.
- Upcoming doctor consultations and pill low-stock alert widgets.

### 4. Medicine Management (`medicines.html`)
- Full CRUD: Add, view, edit, and delete medications.
- Comprehensive fields: dosage quantity, unit (`mg`, `ml`, `tablets`, etc.), form (`Tablet`, `Capsule`, `Liquid`, `Inhaler`, etc.), meal timing (`Before Food`, `After Food`, `With Food`, `Anytime`), start/end dates, prescribing doctor, and color tags.
- Search and filter by medicine name, doctor, form, and active status.
- Pill stock tracker with custom low-stock threshold alert.
- Refill stock modal with automatic stock replenishment history.

### 5. Medicine Scheduling & Reminders (`schedule.html`)
- Multi-reminder configuration per day (e.g., 08:00 AM and 20:00 PM).
- Weekday selection (`Mon`, `Tue`, `Wed`, etc.) or everyday routines.
- Daily timeline view with enabled/paused status toggle.
- Scheduled dose generator ensuring idempotent daily dose creation without duplicates.

### 6. Doctor Prescriptions (`prescriptions.html`)
- **File Upload**: Supports PDF, JPG, JPEG, and PNG files up to 15MB with live preview.
- **Live Camera Photograph Capture**: Integrated browser media stream (`navigator.mediaDevices.getUserMedia`) allowing patients to photograph paper prescriptions using laptop or mobile webcams, preview snapshot, retake, or upload.
- **Secure File Retrieval**: Prescription files are stored in private server storage with randomized UUID filenames; files are only downloaded after server-side ownership verification.

### 7. Doctor Directory & Appointments (`appointments.html`)
- Comprehensive doctor directory with medical specialty, qualifications, clinic address, fee, and available days.
- **Dynamic Slot Availability**: Select any doctor and date to load open 30-minute time slots in real time.
- **Double-Booking Prevention**: Server-side transactional checks prevent two patients from reserving the same doctor slot.
- Reschedule and cancel appointment management.

### 8. Health Reports & Adherence Analytics (`reports.html`)
- Date range filtering (preset 7 days, 30 days, 90 days, or custom dates).
- Clinical adherence calculation using the documented formula:
  $$\text{Adherence Rate} = \left(\frac{\text{Taken Doses}}{\text{Taken Doses} + \text{Skipped Doses}}\right) \times 100$$
- Visual daily compliance bar chart with breakdown of taken vs. skipped doses.
- Dose audit trail table recording exact response timestamps and doctor/patient notes.
- **CSV Export**: Direct one-click download of all compliance records in `.csv` format.
- **Print / PDF Export**: Printer-friendly CSS layout for generating medical report PDFs.

### 9. Caregiver & Family Sharing Network (`profile.html`)
- Secure account linking by registered email address.
- Configurable sharing permissions: `VIEW_DOSES`, `MISSED_DOSE_ALERTS`.
- Distinction between unrecorded doses and confirmed missed doses (>1 hour past scheduled time).
- Authorized caregivers can view real-time compliance status without accessing private credentials.

### 10. Medication Safety & Drug Information Guide (`medicines.html`)
- Essential patient rules: strict prescribed dosing, never double-dose on missed intervals, meal timing (absorption vs. gastric shielding), and completion of antimicrobial courses.
- Critical drug-drug and drug-food interaction chart (NSAIDs + Anticoagulants, Statins + Grapefruit, ACE Inhibitors + Potassium, Antibiotics + Alcohol).
- Interactive quick drug lookup reference (Amoxicillin, Metformin, Atorvastatin, Lisinopril, Omeprazole, Paracetamol).
- Prominent educational medical disclaimer.

### 11. Browser Desktop Notifications & Reminders (`dashboard.html`)
- Native Web Notifications API integration with one-click permission request.
- Automated background check interval alerting patients of doses due within 10 minutes or overdue.

---

## 4. Setup and Installation

### Prerequisites
1. **Java JDK 21 or higher** (Java 21, 22, 23, 24, 25, or 26 supported).
2. **Apache Maven 3.9+** (or use the Maven Wrapper).
3. **MySQL Server 8.0+** (Optional — embedded MySQL mode is provided out of the box).

---

### Option A: Quick Start (Embedded MySQL-Compatible Database)
No external database installation is required! MediCare includes an embedded persistent database configured in MySQL mode.

1. Open a terminal in `medicare-backend/`:
   ```bash
   cd medicare-backend
   ```
2. Build and run the project:
   ```bash
   mvn spring-boot:run
   ```
3. Open your browser and navigate to:
   ```text
   http://localhost:8080/
   ```
   *The entire frontend and backend will be accessible simultaneously at port 8080.*

---

### Option B: Running with a Live MySQL Server
If you want to run against a dedicated MySQL database:

1. Open your MySQL client (MySQL Workbench, phpMyAdmin, or CLI) and run the creation script:
   ```sql
   source database/schema.sql;
   ```
   *(Or copy-paste the contents of `database/schema.sql` into your SQL console).*

2. Create a database user or use `root`:
   ```sql
   CREATE DATABASE IF NOT EXISTS medicare_db;
   ```

3. Launch Spring Boot with the `mysql` profile:
   ```bash
   cd medicare-backend
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

4. Or set environment variables:
   ```bash
   export DB_HOST=localhost
   export DB_PORT=3306
   export DB_NAME=medicare_db
   export DB_USERNAME=root
   export DB_PASSWORD=your_password
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

## 5. Running Automated Tests

MediCare includes comprehensive unit and integration tests covering user registration, login, JWT validation, medicine CRUD, inventory deduction, doctor slot availability, and double-booking prevention.

To run the test suite:
```bash
cd medicare-backend
mvn test
```

Expected output:
```text
[INFO] Results:
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 6. REST API Reference

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new patient account | No |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT token | No |
| `POST` | `/api/auth/logout` | Invalidate/clear active session | No |
| `GET` | `/api/profile` | Retrieve patient profile | Yes (Bearer) |
| `PUT` | `/api/profile` | Update profile information | Yes (Bearer) |
| `POST` | `/api/profile/change-password` | Update account password | Yes (Bearer) |
| `GET` | `/api/medicines` | List patient medications (with search/filters) | Yes (Bearer) |
| `POST` | `/api/medicines` | Create a new medication | Yes (Bearer) |
| `GET` | `/api/medicines/{id}` | Retrieve medication details | Yes (Bearer) |
| `PUT` | `/api/medicines/{id}` | Update medication details | Yes (Bearer) |
| `DELETE` | `/api/medicines/{id}` | Delete medication | Yes (Bearer) |
| `POST` | `/api/medicines/{id}/refill` | Replenish pill stock | Yes (Bearer) |
| `GET` | `/api/schedules` | List active reminder schedules | Yes (Bearer) |
| `POST` | `/api/schedules` | Create a reminder schedule | Yes (Bearer) |
| `PUT` | `/api/schedules/{id}/toggle` | Enable or pause schedule | Yes (Bearer) |
| `DELETE` | `/api/schedules/{id}` | Delete schedule | Yes (Bearer) |
| `GET` | `/api/doses/today` | Get today's scheduled doses | Yes (Bearer) |
| `POST` | `/api/doses/{id}/taken` | Mark dose Taken & deduct inventory | Yes (Bearer) |
| `POST` | `/api/doses/{id}/skipped` | Mark dose Skipped | Yes (Bearer) |
| `POST` | `/api/doses/{id}/snooze` | Snooze reminder by specified minutes | Yes (Bearer) |
| `GET` | `/api/prescriptions` | List patient prescriptions | Yes (Bearer) |
| `POST` | `/api/prescriptions` | Multipart upload (File or Camera Blob) | Yes (Bearer) |
| `GET` | `/api/prescriptions/{id}/file` | Secure download/view prescription file | Yes (Bearer) |
| `DELETE` | `/api/prescriptions/{id}` | Delete prescription record & file | Yes (Bearer) |
| `GET` | `/api/doctors` | List active doctors directory | No |
| `GET` | `/api/doctors/{id}/availability`| Get available slots for date | No |
| `GET` | `/api/appointments` | List patient appointments | Yes (Bearer) |
| `POST` | `/api/appointments` | Book appointment (double-booking safe) | Yes (Bearer) |
| `PUT` | `/api/appointments/{id}/reschedule` | Reschedule appointment to new slot | Yes (Bearer) |
| `POST` | `/api/appointments/{id}/cancel` | Cancel appointment & free doctor slot | Yes (Bearer) |
| `GET` | `/api/reports/dashboard` | Dashboard metrics & summary | Yes (Bearer) |
| `GET` | `/api/reports/adherence` | Adherence metrics & daily trend | Yes (Bearer) |
| `GET` | `/api/reports/export/csv` | Download CSV compliance report | Yes (Bearer) |
| `POST` | `/api/caregivers/invite` | Invite caregiver by registered email | Yes (Bearer) |
| `GET` | `/api/caregivers` | List my caregivers & caring-for patients | Yes (Bearer) |
| `POST` | `/api/caregivers/{id}/accept` | Accept caregiver request | Yes (Bearer) |
| `POST` | `/api/caregivers/{id}/revoke` | Revoke caregiver connection | Yes (Bearer) |
| `GET` | `/api/caregivers/patient/{id}/summary` | View authorized patient doses & missed flags | Yes (Bearer) |

---

## 7. Security and Architectural Decisions

1. **User Isolation**: Every database query for medicines, schedules, doses, appointments, and prescriptions filters by the authenticated `user_id` extracted from the server-validated JWT. Frontends cannot supply or spoof another user's ID.
2. **Password Security**: Passwords are never saved in plaintext; BCrypt with salt rounds is utilized.
3. **Private File Storage**: Uploaded prescription files are placed in an isolated server directory with randomized UUIDs (e.g., `8d2f-48c9-94ae.png`). URLs cannot be enumerated or scraped without an authenticated JWT token corresponding to the file owner.
4. **Idempotent Scheduling**: The background scheduler uses database uniqueness constraints (`uq_med_scheduled_time`) to prevent duplicate doses from ever being generated on the same day.
5. **Double-Booking Safe**: Doctors cannot be booked twice for the same 30-minute interval. Concurrent booking attempts are protected by database uniqueness checks and transaction isolation.

---

## 8. License

This project is open-source and available under the [MIT License](LICENSE). Built for college project demonstration and healthcare management education.
