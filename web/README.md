# Historia Web Module

This module provides a web application for the Historia project, allowing users to submit and track analysis requests for git repositories.

## Features

- Web UI for submitting analysis requests
- Asynchronous processing of analysis requests
- Visualization of analysis results
- RESTful API for programmatic access
- User authentication and authorization

## Getting Started

### Prerequisites

- Java 11 or later
- Maven 3.6.0 or later

### Running the Application

1. Build the project:

```bash
mvn clean install
```

2. Run the web application:

```bash
cd web
mvn quarkus:dev
```

3. Access the application at http://localhost:8080

### Default Users

The application comes with two default users:

- Regular user:
  - Username: `user`
  - Password: `userpassword`
  - Role: `user`

- Administrator:
  - Username: `admin`
  - Password: `adminpassword`
  - Role: `admin`

## API Documentation

The RESTful API is available at the following endpoints:

- `GET /api/requests` - Get all analysis requests
- `GET /api/requests/{id}` - Get a specific analysis request
- `POST /api/requests` - Create a new analysis request
- `PUT /api/requests/{id}/cancel` - Cancel an analysis request
- `DELETE /api/requests/{id}` - Delete an analysis request
- `GET /api/requests/{id}/result` - Get the result of an analysis request

## Configuration

The application can be configured using the `application.properties` file in the `src/main/resources` directory. The following properties are available:

- `historia.results.directory` - Directory where analysis results are stored
- `historia.max.concurrent.analyses` - Maximum number of concurrent analyses

## Architecture

The web module is built using the following technologies:

- [Quarkus](https://quarkus.io/) - A Kubernetes Native Java stack
- [RESTEasy](https://resteasy.dev/) - JAX-RS implementation
- [Hibernate ORM](https://hibernate.org/orm/) - Object-relational mapping
- [Qute](https://quarkus.io/guides/qute) - Template engine
- [Bootstrap](https://getbootstrap.com/) - CSS framework
- [Vega-Lite](https://vega.github.io/vega-lite/) - Visualization grammar

The application follows a layered architecture:

- **Model** - Domain objects and DTOs
- **Repository** - Data access layer
- **Service** - Business logic
- **Controller** - Web UI controllers
- **Resource** - RESTful API endpoints
- **Templates** - HTML templates

## Development

### Adding a New Feature

1. Create or modify the necessary model classes
2. Update the repository and service layers
3. Add or modify the REST endpoints
4. Create or update the HTML templates
5. Add any required JavaScript for client-side functionality

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package -Pnative
```

This will create a native executable in the `target` directory.
