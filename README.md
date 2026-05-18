# order-service

Order Service is a Spring Boot based microservice responsible for handling 
order-related operations such as order creation, order management, 
order cancellation, and fetching user orders for the ecommerce application.

____________________________________________________________________________________________________

## Features

- Create Orders
- Fetch Orders
- Fetch Logged-in User Orders
- Cancel Orders
- Order Status Management
- Product Quantity Validation
- Product Stock Reduction
- Order Filtering Support
- Role-Based Authorization
- Request Validation using DTOs
- Global Exception Handling
- Redis Caching Support
- Audit Information Tracking

____________________________________________________________________________________________________

## Technologies Used

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Redis
- Maven
- Lombok
- JWT Authentication
- Swagger / OpenAPI
- RestTemplate (Inter-Service Communication)

____________________________________________________________________________________________________

## Architecture

The application follows layered architecture:

```text
Controller → Service → Repository → Database
```

____________________________________________________________________________________________________
## Design Patterns Used

- Dependency Injection
- Repository Pattern
- DTO Pattern
- Layered Architecture

____________________________________________________________________________________________________

## Project Structure

```text
src/main/java
│
├── controller
├── service
├── repository
├── dto
├── entity
├── client
├── config
├── security
├── exception
├── util
└── audit
```

____________________________________________________________________________________________________
## Configuration

Update the following properties inside:

```properties
src/main/resources/application.properties
```

Example:

```properties
# Application
spring.application.name=order-service
server.port=8098

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ecom_db
spring.datasource.username=root
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true

# JWT
jwt.secret=your_secret_key
jwt.expiration=3600000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Service URLs
# Local development URLs.

user.service.url=${USER_SERVICE_URL:http://localhost:8095/api/users}
product.service.url=${PRODUCT_SERVICE_URL:http://localhost:8097/api/products}
```
-----------------------------------------------------------------------------

## Required Common Library Setup

This service depends on the `ecommerce-common` library.
Because `ecommerce-common` is in a separate repository, it must be installed into the local Maven repository before building this service.

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>ecommerce-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
----------------------------------------------------------------------------------
## Build Project

```bash
mvn clean install
```

____________________________________________________________________________________________________
## Run Project

```bash
mvn spring-boot:run
```

____________________________________________________________________________________________________
## Swagger Documentation

After starting the application:

Swagger UI:
```text
http://localhost:8098/swagger-ui/index.html
```

OpenAPI Docs:
```text
http://localhost:8098/v3/api-docs
```

____________________________________________________________________________________________________
## Main APIs

### Order APIs

| Method | API | Description |
|---|---|---|
| POST | `/api/orders` | Create order |
| GET | `/api/orders` | Fetch orders |
| GET | `/api/orders/my` | Fetch logged-in user orders |
| PUT | `/api/orders/cancel/{orderId}` | Cancel order |

____________________________________________________________________________________________________

## Inter-Service Communication

Order Service communicates with:

- user-service
- product-service

using:

- RestTemplate

Purpose:
- Fetch logged-in user details
- Fetch product details
- Validate product quantity
- Reduce product stock during order creation
------------------------------------------------------------------------------------------
### Service URL Contract

For local development, order-service can use localhost URLs:

```properties
USER_SERVICE_URL=http://localhost:8095/api/users
PRODUCT_SERVICE_URL=http://localhost:8097/api/products
```
---------------------------------------------------------------------------------------
## Security

- JWT-based authentication
- Spring Security integration
- Protected APIs using authorization filters
- Role-based access control

____________________________________________________________________________________________________
## Caching

Redis is used for:

- Order caching
- Frequently accessed order data
- Performance optimization

____________________________________________________________________________________________________
## Profiles

```text
application-dev.properties   → Development Environment
application-prod.properties  → Production Environment
