# Generic Java Setup Guide

## Overview

This guide covers setting up the GameOn-CodeSphere backend using **plain Java** with Servlets and JDBC — no Spring framework. This approach gives full manual control over the application lifecycle.

---

## Prerequisites

- Java 17+ (JDK)
- Apache Tomcat 10+ (or embedded Jetty)
- Maven or manual classpath management
- SQL Server instance with `GameOnDb` database
- IDE: IntelliJ IDEA or VS Code with Java extensions

---

## Project Structure

```
backend/
├── pom.xml (or manual lib/ folder)
└── src/
    └── main/
        ├── java/
        │   └── com/gameon/codesphere/
        │       ├── App.java (main entry or servlet init)
        │       ├── servlet/
        │       │   ├── AuthServlet.java
        │       │   ├── ListingServlet.java
        │       │   ├── PostServlet.java
        │       │   ├── UserServlet.java
        │       │   └── ...
        │       ├── service/
        │       │   ├── AuthService.java
        │       │   ├── ListingService.java
        │       │   └── ...
        │       ├── dao/
        │       │   ├── UserDao.java
        │       │   ├── ListingDao.java
        │       │   └── ...
        │       ├── model/
        │       │   ├── User.java
        │       │   ├── GameListing.java
        │       │   └── ...
        │       └── util/
        │           ├── DatabaseConnection.java
        │           ├── JsonUtil.java
        │           └── PasswordUtil.java
        └── webapp/
            └── WEB-INF/
                └── web.xml
```

---

## Maven pom.xml (Minimal)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.gameon</groupId>
    <artifactId>codesphere</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Servlet API -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- SQL Server JDBC Driver -->
        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <version>12.4.2.jre11</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>

        <!-- Password Hashing -->
        <dependency>
            <groupId>org.mindrot</groupId>
            <artifactId>jbcrypt</artifactId>
            <version>0.4</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Database Connection Utility

```java
package com.gameon.codesphere.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
        "jdbc:sqlserver://localhost:1433;databaseName=GameOnDb;encrypt=true;trustServerCertificate=true";
    private static final String USER = "your_username";
    private static final String PASS = "your_password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
```

> **Note:** In production, use a connection pool (e.g., HikariCP) and externalise credentials.

---

## Servlet Example

```java
package com.gameon.codesphere.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/listings")
public class ListingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        // TODO: Call ListingService and return JSON
        out.print("{\"success\": true, \"data\": [], \"message\": \"Listings retrieved\"}");
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // TODO: Parse JSON body, validate, call service
        resp.setStatus(HttpServletResponse.SC_CREATED);
    }
}
```

---

## web.xml Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         version="6.0">

    <display-name>GameOn-CodeSphere</display-name>

    <!-- CORS Filter (custom implementation needed) -->
    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>com.gameon.codesphere.util.CorsFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
    </filter-mapping>
</web-app>
```

---

## Deployment

### External Tomcat

1. Build the WAR: `mvn clean package`
2. Copy `target/codesphere-1.0.0-SNAPSHOT.war` to Tomcat's `webapps/` directory.
3. Start Tomcat: `catalina.sh run` (Linux/Mac) or `catalina.bat run` (Windows).
4. API available at `http://localhost:8080/codesphere/api/`.

### Embedded Jetty (Alternative)

Add Jetty dependency and a main class to run without external container.

---

## Comparison: Generic Java vs Spring Boot

| Aspect | Generic Java | Spring Boot |
|--------|-------------|-------------|
| Setup complexity | Higher (manual wiring) | Lower (auto-config) |
| Dependency injection | Manual or service locator | Built-in (@Autowired) |
| Data access | Raw JDBC | Spring Data JPA |
| Configuration | web.xml + properties files | application.properties |
| Deployment | WAR to Tomcat | Embedded server (JAR) |
| Learning curve | Understand fundamentals | Framework conventions |
| Control | Full manual control | Convention over configuration |

---

## Useful Commands

| Command | Description |
|---------|-------------|
| `mvn clean package` | Build WAR file |
| `mvn compile` | Compile sources |
| `mvn test` | Run tests |
