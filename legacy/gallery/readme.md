# Gallery Application — Legacy Reference

This module is the preserved historical implementation of the Gallery Application. It is reference material for the Spring Boot modernisation in [`../../modernised/gallery`](../../modernised/gallery/readme.md); it is not being modernised in place.

## Historical stack

The checked-in build and configuration use:

| Area | Repository evidence |
| --- | --- |
| Language level | Maven compiler source/target 1.6 |
| Framework | Spring Framework and Spring MVC 3.0.2, primarily XML-configured |
| Persistence | Hibernate 3.5, JPA/Hibernate DAO implementations, Open EntityManager in View |
| Views | JSP and JSTL under `src/main/webapp` |
| Web platform | Servlet 2.5, `*.art` and `/art/*` dispatcher mappings |
| Database | H2 configuration without Flyway migrations |
| Build/deployment | Maven WAR with historical Tomcat and Jetty Maven plugins |

The repository does not contain the Dockerfile previously described by this README, and the POM targets Java 6 rather than Java 8.

## Historical application structure

```text
legacy/gallery/
├── pom.xml
└── src/
    ├── main/java/                    # Domain, DAO, service and controller code
    ├── main/resources/META-INF/      # Spring, datasource and persistence XML
    └── main/webapp/
        ├── WEB-INF/JSP/              # Gallery and administration JSPs
        ├── WEB-INF/spring/           # MVC handler and view configuration
        ├── css/, js/, images/        # Original visual theme and lightbox assets
        └── home.jsp                  # Welcome page
```

## Behaviour retained as reference

The legacy code and configuration contain:

- a home page, category list, category selection, artwork selection, and image-display flow;
- JSPs and assets for artwork detail, interest capture, Bio, exhibitions, and lightbox-style navigation;
- controller-backed administrator login, artwork upload, and exhibition creation;
- category/artwork REST endpoints returning DTO representations;
- the original category, artwork, comment, person, and exhibition domain concepts.

Some exhibition mappings are commented out and the presence of a JSP or domain class does not by itself establish a complete runnable flow. These artifacts are treated as behavioural evidence, not as acceptance proof for the modernised application.

## Build and reference use

Historical Maven commands are:

```bash
cd legacy/gallery
mvn test
mvn package
```

The module uses old plugins, repositories, and Java-era dependencies, so running it may require a compatible legacy JDK and repository access. Its current build/runtime was not revalidated as part of the curated-catalog milestone.

For modernisation work, inspect the following before reproducing a flow:

- `WEB-INF/spring/spring-web-gallery.xml` for handler and view mappings;
- `WEB-INF/JSP/` and `home.jsp` for page behaviour;
- `bifa.css`, `admin.css`, `css/`, `js/`, and `images/` for the legacy presentation;
- controller, facade, DAO, and domain packages for persistence semantics.

## Known technical debt preserved intentionally

- XML-heavy wiring and legacy framework/plugin versions;
- direct Spring MVC controller interfaces and deprecated base controller classes;
- parallel Hibernate and JPA DAO implementations;
- Open EntityManager in View and view-layer persistence coupling;
- manual schema evolution with no migration history;
- tightly coupled upload, image processing, authentication, controller, and JSP behaviour;
- limited validation and error handling compared with the modern module.

## Difference from the modern module

The modern application currently reproduces only the verified public category/artwork browsing slice, curated catalog bootstrap, static image delivery, root redirect, custom login shell, and scoped security. Legacy exhibitions, Bio, interest capture, administration/upload, and exact theme behaviour remain future modernisation work.
