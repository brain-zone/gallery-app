
# Gallery Application — Modernised (Spring Boot 3 + Java 21)

This module contains the **modernised implementation** of the Gallery Application,
rewritten from the legacy `legacy/gallery` app into a clean, modern container ready stack.

---

## Goals

- Preserve **core user flows** from the legacy app:
  - Browse categories
  - View artworks
  - View exhibitions
  - Express interest in an artwork
  - Admin upload & curation

- Replace the legacy stack with:
  - **Spring Boot 3.x**
  - **Java 21**
  - **Spring Web MVC + Thymeleaf**
  - **Spring Security (form login, OAuth2-ready)**
  - **Spring Data JPA**
  - **Flyway DB migrations**
  - **Container-friendly Boot JAR**

---

## Tech Stack

| Layer| Technology |
|---|-----|
| Language    | Java 21 |
| Framework   | Spring Boot 3.5.x |
| Web         | Spring Web MVC|
| Security    | Spring Security (+ optional OAuth2 client)|
| Persistence | Spring Data JPA|
| DB Migration| Flyway|
| Views       | Thymeleaf templates (planned)|
| DB          | H2 (dev), PostgreSQL (prod-ready)|
| Build Tool  | Gradle (Groovy DSL)|

---

## Project Layout

```text
modernised/gallery
├── build.gradle
├── settings.gradle
└── src
    ├── main
    │   ├── java/net/matrix/gallery/...
    │   └── resources
    │       ├── application.properties
    │       ├── templates/...
    │       └── db/migration/...
    └── test
````

---

## Running the App (Dev)

```bash
cd modernised/gallery
./gradlew clean bootRun
```

Then open:
 - [http://localhost:8080](http://localhost:8080)

---

## Migration Mapping

This module is the “after” in a **before/after** pair:

* `legacy/gallery` -> original Spring MVC + Hibernate app
* `modernised/gallery` -> refactored Spring Boot 3 implementation

Domain concepts like `Category`, `ArtEntity`, `Exhibition`, and `Interest` are preserved, but:

* DAOs -> **Spring Data JPA repositories**
* JSPs -> **Thymeleaf templates (planned; none yet)**
* XML config -> **Boot auto-config + Java `@Configuration`**
* Manual transactions -> **`@Transactional` service layer**
---
