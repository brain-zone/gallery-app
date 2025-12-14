
# Gallery Application — Legacy Version

This module contains the **legacy implementation** of the Gallery Application — originally built using:

- **Spring MVC (XML-configured)**
- **Hibernate ORM (SessionFactory, DAOs)**
- **JSP views**
- **WAR packaging on Tomcat 8**
- **Java 8**
- **Maven build**

I have intentionally preserved as a **reference system** to support a complete modernisation into **Spring Boot 3.x (Java 21)** in `modernised/gallery`.

---

## Purpose of the Legacy Module

The legacy app represents:

- The original **domain model** (Category, Art, Exhibition, etc.)
- The existing **user flows**:
  - browse categories
  - view artworks
  - basic “interested” interaction
  - admin upload / exhibition creation
- The old **controller routes** and JSP UI
- Historical architectural issues that the new version explicitly fixes

This module is **frozen**: no new features are planned here.

---

##  Tech Stack (Legacy)

| Layer| Technology|
|----|----|
| Language | Java 8|
| Framework| Spring MVC (XML configuration)|
| Persistence| Hibernate ORM + DAOs|
| Views | JSP + JSTL|
| Packaging| WAR|
| Server| Apache Tomcat 8|
| Build Tool| Maven 3|
| Database    | H2 / Derby (no formal migrations)|

---

## 📦 Build & Run (Docker)

A Dockerfile is provided to sandbox the legacy app:

```dockerfile
FROM maven:3.6.3-jdk-8 AS build
COPY . /app
WORKDIR /app
RUN mvn -B -q clean package -DskipTests

FROM tomcat:8.5-jre8-alpine
RUN rm -rf /usr/local/tomcat/webapps.dist/* /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
````

### Build

```bash
cd legacy/gallery
docker build -t legacy-gallery .
```

### Run

```bash
docker run -p 8080:8080 legacy-gallery
```

Then open:
this link [http://localhost:8080](http://localhost:8080)


## Features Overview

### Public Flows

* List categories
* Select a category -> view artworks
* Select artwork -> view detail page
* Basic interest action

### Admin Flows

* Login (custom controller-based)
* Admin menu (JSP)
* Upload artwork
* Create exhibitions

---

## Known Issues (Intentionally Kept)

* Writes inside `GET` handlers
* Open Session in View / manual Session management
* XML-heavy Spring configuration
* Tight coupling between controllers, DAOs, and views
* No schema migrations (manual DB evolution)
* Weak validation and error handling

These are **not** to be fixed here — they are the **input problems** for the modernised version in `modernised/gallery`.

---

## Layout

```text
legacy/gallery
├── pom.xml
└── src
    ├── main
    │   ├── java        # Controllers, DAOs, entities
    │   ├── resources   # Spring XML config
    │   └── webapp      # JSP views, WEB-INF, web.xml
    └── test
```

For the modern implementation, see [`../../modernised/gallery`](../../modernised/gallery).

---