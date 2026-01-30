<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Edu Resource Platform - Home</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
        }
        .api-links {
            margin-top: 30px;
        }
        .api-link {
            display: inline-block;
            margin: 10px;
            padding: 10px 20px;
            background-color: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
        .api-link:hover {
            background-color: #45a049;
        }
        .info {
            margin-top: 20px;
            padding: 15px;
            background-color: #e8f5e9;
            border-left: 4px solid #4CAF50;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Welcome to Edu Resource Platform</h1>
        
        <div class="info">
            <p><strong>Project Name:</strong> edu-resource-backend</p>
            <p><strong>Technology Stack:</strong> Servlet + JSP + MySQL + Tomcat</p>
            <p><strong>Database:</strong> MySQL (username: root, password: root)</p>
        </div>
        
        <div class="api-links">
            <h2>Available APIs:</h2>
            <a href="hello" class="api-link">Hello Servlet</a>
            <a href="api/resources" class="api-link">Get Resources (JSON)</a>
        </div>
        
        <div style="margin-top: 30px;">
            <h2>Project Structure:</h2>
            <pre style="background-color: #f0f0f0; padding: 15px; border-radius: 4px;">
edu-resource-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/edu/
│   │   │       ├── servlet/
│   │   │       │   ├── HelloServlet.java
│   │   │       │   └── ResourceServlet.java
│   │   │       └── filter/
│   │   │           └── CharacterEncodingFilter.java
│   │   ├── resources/
│   │   │   └── database.properties
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── web.xml
│   │       └── index.jsp (this file)
│   └── test/
│       └── java/
└── pom.xml
            </pre>
        </div>
    </div>
</body>
</html>
