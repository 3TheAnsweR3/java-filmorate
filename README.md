# Filmorate

Filmorate is an educational Spring Boot REST API for managing films and users. The project was completed as part of the Yandex Practicum Java Developer programme and focuses on HTTP endpoints, validation, in-memory storage, logging, and automated testing.

> Educational project. It represents coursework rather than commercial development experience.

## Features

- Create, list, and update films through `/films`.
- Create, list, and update users through `/users`.
- Assign sequential identifiers and keep data in memory.
- Validate request bodies with Jakarta Validation.
- Apply project-specific rules for release dates, logins, and missing user names.
- Log successful operations and validation failures.

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/films` | Create a film |
| `GET` | `/films` | List all films |
| `PUT` | `/films` | Update an existing film |
| `POST` | `/users` | Create a user |
| `GET` | `/users` | List all users |
| `PUT` | `/users` | Update an existing user |

## Validation rules

Films require a non-blank name, a description of no more than 200 characters, a release date on or after 28 December 1895, and a positive duration.

Users require a valid email, a non-blank login without spaces, and a birthday that is not in the future. When the name is missing, the login is used as the display name.

## Technology

- Java 21
- Spring Boot 3
- Spring Web
- Jakarta Validation
- Lombok
- JUnit 5
- Maven
- GitHub Actions

## Run locally

Requirements: JDK 21 and Maven.

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

Example request:

```bash
curl -X POST http://localhost:8080/films \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Arrival",
    "description": "Science-fiction drama",
    "releaseDate": "2016-11-11",
    "duration": 116
  }'
```

## Tests

```bash
mvn test
```

The test suite covers controller behaviour, request validation, update scenarios, and HTTP responses.

## Current limitations

Data is stored in memory and is lost when the application restarts. Persistent storage and a service/repository layer are planned for later stages of the course.
