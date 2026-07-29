# Spring Boot Setup Guide

## Overview

This guide covers setting up the GameOn-CodeSphere backend using **Spring Boot** with **Maven** as the build tool.

---

## Prerequisites

- Java 17+ (JDK)
- Maven 3.8+
- SQL Server instance with `GameOnDb` database
- IDE: IntelliJ IDEA (recommended) or VS Code with Java extensions

---

## Project Initialisation

### Option A: Spring Initializr

1. Go to [start.spring.io](https://start.spring.io)
2. Configure:
   - **Project:** Maven
   - **Language:** Java
   - **Spring Boot:** 3.x (latest stable)
   - **Group:** `com.gameon`
   - **Artifact:** `codesphere`
   - **Packaging:** Jar
   - **Java:** 17
3. Add dependencies:
   - Spring Web
   - Spring Data JPA
   - MS SQL Server Driver
   - Spring Session
   - Validation
4. Generate and extract into the `backend/` folder.

### Option B: Manual pom.xml

Create `pom.xml` in `backend/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.gameon</groupId>
    <artifactId>codesphere</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>GameOn-CodeSphere</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- SQL Server Driver -->
        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Project Structure

```
backend/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/gameon/codesphere/
        │       ├── CodeSphereApplication.java
        │       ├── config/
        │       │   ├── CorsConfig.java
        │       │   └── SecurityConfig.java
        │       ├── controller/
        │       │   ├── AuthController.java
        │       │   ├── ListingController.java
        │       │   ├── PostController.java
        │       │   ├── UserController.java
        │       │   └── ...
        │       ├── service/
        │       │   ├── AuthService.java
        │       │   ├── ListingService.java
        │       │   └── ...
        │       ├── repository/
        │       │   ├── UserRepository.java
        │       │   ├── ListingRepository.java
        │       │   └── ...
        │       ├── model/
        │       │   ├── User.java
        │       │   ├── GameListing.java
        │       │   └── ...
        │       └── dto/
        │           ├── ApiResponse.java
        │           └── ...
        └── resources/
            ├── application.properties
            └── static/
```

---

## Configuration (application.properties)

```properties
# Server
server.port=8080

# SQL Server
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=GameOnDb;encrypt=true;trustServerCertificate=true
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

---

## Running the Application

```bash
# From the backend/ directory
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package
java -jar target/codesphere-1.0.0-SNAPSHOT.jar
```

---

## CORS Configuration Example

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5500", "http://127.0.0.1:5500")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```

---

## Useful Commands

| Command | Description |
|---------|-------------|
| `mvn spring-boot:run` | Start dev server |
| `mvn clean package` | Build JAR |
| `mvn test` | Run tests |
| `mvn clean install -DskipTests` | Build without tests |
