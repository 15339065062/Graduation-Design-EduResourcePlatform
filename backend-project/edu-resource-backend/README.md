# Edu Resource Platform - Backend

## Project Information
- **Project Name**: edu-resource-backend
- **Technology Stack**: Servlet + JSP + MySQL + Tomcat
- **Build Tool**: Maven
- **Java Version**: 1.8

## Prerequisites
- JDK 8 or higher
- Apache Tomcat 9.0 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## Database Setup

### Step 1: Create Database
```bash
# Login to MySQL
mysql -u root -p
# Enter password: root

# Execute the init.sql file
source C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend\src\main\resources\init.sql
```

Or run directly from command line:
```bash
mysql -u root -p < C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend\src\main\resources\init.sql
```

### Step 2: Verify Database
```bash
mysql -u root -p
# Enter password: root

USE edu_resource;
SHOW TABLES;
```

## Build the Project

### Step 1: Navigate to Project Directory
```bash
cd C:\APP\Graduation-Design-EduResourcePlatform\backend-project\edu-resource-backend
```

### Step 2: Clean and Build
```bash
mvn clean package
```

This will create a WAR file in the `target` directory: `target/edu-resource-backend.war`

## Deploy to Tomcat

### Option 1: Manual Deployment
1. Copy the WAR file to Tomcat webapps directory:
```bash
copy target\edu-resource-backend.war C:\apache-tomcat-9.0.x\webapps\
```

2. Start Tomcat:
```bash
cd C:\apache-tomcat-9.0.x\bin
startup.bat
```

### Option 2: Using Maven Tomcat Plugin
Add the following to your pom.xml (optional):
```xml
<plugin>
    <groupId>org.apache.tomcat.maven</groupId>
    <artifactId>tomcat7-maven-plugin</artifactId>
    <version>2.2</version>
    <configuration>
        <url>http://localhost:8080/manager/text</url>
        <server>tomcat</server>
        <path>/edu-resource-backend</path>
    </configuration>
</plugin>
```

Then run:
```bash
mvn tomcat7:deploy
```

## Start the Application

### Start Tomcat (if not already running)
```bash
cd C:\apache-tomcat-9.0.x\bin
startup.bat
```

### Access the Application
- **Home Page**: http://localhost:8080/edu-resource-backend/
- **Hello Servlet**: http://localhost:8080/edu-resource-backend/hello
- **Resources API**: http://localhost:8080/edu-resource-backend/api/resources

## Verification

### Step 1: Check Tomcat Status
Open browser and visit: http://localhost:8080/
You should see the Tomcat welcome page.

### Step 2: Test the Application
1. Visit http://localhost:8080/edu-resource-backend/
   - Expected: Welcome page with project information

2. Visit http://localhost:8080/edu-resource-backend/hello
   - Expected: "Welcome to Edu Resource Platform Backend!" message

3. Visit http://localhost:8080/edu-resource-backend/api/resources
   - Expected: JSON response with sample resources

## Troubleshooting

### Issue: Port 8080 already in use
**Solution**: Change Tomcat port in `conf/server.xml`:
```xml
<Connector port="8081" protocol="HTTP/1.1" ... />
```

### Issue: Database connection failed
**Solution**: 
1. Check MySQL service is running
2. Verify database credentials in `web.xml`
3. Ensure database `edu_resource` exists

### Issue: Class not found errors
**Solution**: 
1. Run `mvn clean install` to rebuild
2. Check all dependencies are downloaded
3. Verify Tomcat lib folder contains required JARs

## Project Structure
```
edu-resource-backend/
├── src/
│   ├── main/
│   │   ├── java/com/edu/
│   │   │   ├── servlet/
│   │   │   │   ├── HelloServlet.java
│   │   │   │   └── ResourceServlet.java
│   │   │   └── filter/
│   │   │       └── CharacterEncodingFilter.java
│   │   ├── resources/
│   │   │   ├── database.properties
│   │   │   └── init.sql
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── web.xml
│   │       └── index.jsp
│   └── test/
│       └── java/
└── pom.xml
```

## API Endpoints

### GET /hello
Returns a simple HTML welcome message.

### GET /api/resources
Returns a JSON array of educational resources.

### POST /api/resources
Creates a new resource (placeholder implementation).

## Configuration

### Database Configuration
Edit `src/main/webapp/WEB-INF/web.xml` to change database settings:
```xml
<context-param>
    <param-name>dbUrl</param-name>
    <param-value>jdbc:mysql://localhost:3306/edu_resource?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8</param-value>
</context-param>
<context-param>
    <param-name>dbUsername</param-name>
    <param-value>root</param-value>
</context-param>
<context-param>
    <param-name>dbPassword</param-name>
    <param-value>root</param-value>
</context-param>
```

## Development Notes
- All class names, method names, and file names are in English
- Character encoding is set to UTF-8 for all requests/responses
- The project uses standard Maven directory structure
- Servlet API version: 4.0.1
- MySQL Connector version: 8.0.33
