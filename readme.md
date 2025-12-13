# Gallery Application – Legacy & Modernised

This repository contains two related implementations of the same Gallery Application:

- `legacy/gallery` – the original **Spring MVC + Hibernate + WAR on Tomcat** app  
- `modernised/gallery` – the **Spring Boot 3 + Java 21** rewrite

The goal of this repo is to **showcase a full-stack modernisation journey**:
from a 2000s-style Java web app to a 2025-ready, container-friendly Spring Boot service.

---

## Repository Structure

```text
.
├── legacy
│   └── gallery
│       ├── pom.xml
│       ├── src
│       │   ├── main
│       │   └── test
│       └── target
│           ├── classes
│           └── test-classes
└── modernised
    └── gallery
        ├── build.gradle
        ├── gradle
        │   └── wrapper
        ├── gradlew
        ├── gradlew.bat
        ├── HELP.md
        ├── settings.gradle
        └── src
            ├── main
            └── test
````

* See [`legacy/gallery/readme.md`](legacy/gallery/readme.md) for the legacy app details.
* See [`modernised/gallery/readme.md`](modernised/gallery/readme.md) for the modern Spring Boot app.

---

## Modernisation Story (High Level)

**Legacy app**

* Java 8
* Spring MVC (XML configuration)
* Hibernate SessionFactory / DAOs
* JSP views
* WAR deployment on Tomcat

**Modernised app**

* Java 21
* Spring Boot 3.x (Gradle build)
* Spring Web MVC, Spring Security
* Spring Data JPA + Flyway
* Thymeleaf views
* Fat JAR + containerised deployment

The two codebases implement **the same functional flows** (gallery browsing, artwork upload, exhibitions, interest capture), making it easy to compare architectures, patterns, and operational characteristics.

---

## How to Use This Repo

* If you want to **understand the old design**, start in `legacy/gallery`.
* If you want to see the **modern implementation**, go to `modernised/gallery`.
* If you care about the **before/after diff**, look at:

  * controllers -> REST + MVC controllers in Boot
  * DAOs -> Spring Data JPA repositories
  * JSPs -> Thymeleaf templates
  * XML Spring config → Boot auto-configuration + `@Configuration` classes

>  **Historical Note:**  
> This legacy app was originally built after studying *Spring Persistence with Hibernate*, 
> a legendary (and now very ancient) book from the XML era of enterprise Java.  
> It taught me DAOs, Hibernate plumbing, `<tx:advice>`, and enough XML to carpet a 
> 3-bedroom house.  
> 
> This repo keeps that version intact so the modern rewrite can show just how far the 
> Spring ecosystem has evolved—and why modern developers should never have to touch a 
> SessionFactory again.
