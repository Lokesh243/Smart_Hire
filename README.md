# SmartHire — Job Board & Applicant Tracking System

A production-ready full-stack ATS built with Spring Boot, Spring Security, JWT, JPA/Hibernate, MySQL.

## Tech Stack
- Java 21 + Spring Boot 3.5.14
- Spring Security + JWT Authentication
- JPA/Hibernate + MySQL
- AOP Audit Logging
- Async Email Notifications (@Async)
- Scheduled Job Expiry (@Scheduled)
- Spring Data Specifications (dynamic search)
- Swagger/OpenAPI documentation
- Docker + Docker Compose

## Three Role-Based Portals
| Role | Access |
|------|--------|
| ADMIN | User management, system metrics, audit logs |
| HR | Post jobs, manage pipeline, review applicants |
| CANDIDATE | Browse jobs, apply, track applications |

## API Endpoints
Full documentation at `/swagger-ui/index.html`

## Running Locally

### With Docker
```bash
docker compose up --build
```

### Without Docker
1. Create MySQL database: `CREATE DATABASE smarthire_db;`
2. Update `application.yml` with your credentials
3. Run `SmarthireApplication.java`

## Key Features
- JWT Auth with refresh token support
- Role-based access control (@PreAuthorize)
- Application pipeline: APPLIED → SCREENED → INTERVIEW → OFFER/REJECTED
- Async email notifications on every stage change
- AOP audit logging on all write operations
- Auto job expiry via @Scheduled cron
- Dynamic job search with Spring Specifications
- Paginated API responses
- Global exception handling
