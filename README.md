<<<<<<< HEAD
# Simple Spring Boot CRUD Application

A comprehensive Spring Boot application demonstrating CRUD operations with REST API, JPA, H2 database, and extensive testing.

## 🚀 Features

- **Complete CRUD Operations** - Create, Read, Update, Delete users
- **REST API** - RESTful endpoints with JSON responses
- **Data Validation** - Input validation with proper error messages
- **Exception Handling** - Global exception handler with consistent error responses
- **Database Integration** - H2 in-memory database with JPA/Hibernate
- **Sample Data** - Pre-loaded sample users for testing
- **Comprehensive Testing** - Unit tests for all layers (Controller, Service, Repository)
- **Development Tools** - H2 Console for database inspection

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring Web**
- **H2 Database**
- **Maven**
- **JUnit 5**
- **Mockito**

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+ or use the included Maven wrapper

## 🔧 Getting Started

### Clone the repository
```bash
git clone https://github.com/jeonck/simple-springboot-crud.git
cd simple-springboot-crud
```

### Build the application
```bash
mvn clean compile
```

### Run tests
```bash
mvn test
```

### Start the application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access H2 Database Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/users
```

### Endpoints

#### 1. Get All Users
```http
GET /api/users
```

**Response:**
```json
{
  "users": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john.doe@example.com",
      "phone": "010-1234-5678",
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    }
  ],
  "total": 1
}
```

#### 2. Get User by ID
```http
GET /api/users/{id}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "010-1234-5678",
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:00"
}
```

#### 3. Get User by Email
```http
GET /api/users/email/{email}
```

#### 4. Search Users by Name
```http
GET /api/users/search?name={searchTerm}
```

**Response:**
```json
{
  "users": [...],
  "total": 2,
  "searchTerm": "John"
}
```

#### 5. Create User
```http
POST /api/users
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "010-9876-5432"
}
```

**Response:**
```json
{
  "message": "User created successfully",
  "user": {
    "id": 6,
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "phone": "010-9876-5432",
    "createdAt": "2025-01-01T11:00:00",
    "updatedAt": "2025-01-01T11:00:00"
  }
}
```

#### 6. Update User
```http
PUT /api/users/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "John Updated",
  "email": "john.updated@example.com",
  "phone": "010-1111-2222"
}
```

**Response:**
```json
{
  "message": "User updated successfully",
  "user": {
    "id": 1,
    "name": "John Updated",
    "email": "john.updated@example.com",
    "phone": "010-1111-2222",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T11:30:00"
  }
}
```

#### 7. Delete User
```http
DELETE /api/users/{id}
```

**Response:**
```json
{
  "message": "User deleted successfully",
  "userId": 1
}
```

#### 8. Health Check
```http
GET /api/users/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "User Management API"
}
```

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=UserServiceTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=UserServiceTest#createUser_WhenEmailNotExists_ShouldCreateUser
```

### Test Coverage
The application includes comprehensive tests for:
- **Controller Layer** - REST endpoint testing with MockMvc
- **Service Layer** - Business logic testing with Mockito
- **Repository Layer** - Database integration testing with @DataJpaTest
- **Application Context** - Spring Boot context loading test

## 📊 Sample Data

The application comes with pre-loaded sample data:

| ID | Name | Email | Phone |
|-----|------|-------|-------|
| 1 | John Doe | john.doe@example.com | 010-1234-5678 |
| 2 | Jane Smith | jane.smith@example.com | 010-2345-6789 |
| 3 | Bob Johnson | bob.johnson@example.com | 010-3456-7890 |
| 4 | Alice Brown | alice.brown@example.com | 010-4567-8901 |
| 5 | Charlie Wilson | charlie.wilson@example.com | 010-5678-9012 |

## 🔍 Error Handling

The application provides consistent error responses:

### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2025-01-01T12:00:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "name": "Name is required",
    "email": "Email should be valid"
  }
}
```

### User Not Found (404 Not Found)
```json
{
  "timestamp": "2025-01-01T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 999"
}
```

### Duplicate Email (400 Bad Request)
```json
{
  "timestamp": "2025-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "User with email john@example.com already exists"
}
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/example/crud/
│   │   ├── CrudApplication.java          # Main application class
│   │   ├── controller/
│   │   │   └── UserController.java       # REST controller
│   │   ├── dto/
│   │   │   └── UserDTO.java             # Data transfer object
│   │   ├── entity/
│   │   │   └── User.java                # JPA entity
│   │   ├── exception/
│   │   │   ├── UserNotFoundException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   └── UserRepository.java      # JPA repository
│   │   └── service/
│   │       └── UserService.java         # Business logic
│   └── resources/
│       ├── application.properties       # App configuration
│       └── data.sql                     # Sample data
└── test/
    ├── java/com/example/crud/
    │   ├── CrudApplicationTests.java    # Context test
    │   ├── controller/
    │   │   └── UserControllerTest.java  # Controller tests
    │   ├── repository/
    │   │   └── UserRepositoryTest.java  # Repository tests
    │   └── service/
    │       └── UserServiceTest.java     # Service tests
    └── resources/
        └── application-test.properties  # Test configuration
```

## 🌟 Key Design Patterns

- **Repository Pattern** - Data access abstraction
- **Service Layer Pattern** - Business logic separation
- **DTO Pattern** - Data transfer between layers
- **Exception Handling Pattern** - Centralized error management

## 🚦 Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Request successful |
| 201 | Created - User created successfully |
| 400 | Bad Request - Validation error or duplicate email |
| 404 | Not Found - User not found |
| 500 | Internal Server Error - Unexpected error |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run all tests to ensure they pass
6. Submit a pull request

## 📄 License

This project is open source and available under the [MIT License](LICENSE).