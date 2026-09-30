# Gallery Application: Legacy and Modernised

This repository preserves an early Java gallery application beside an incremental Spring Boot modernisation. It is a before-and-after engineering study: the legacy module remains the behavioural and visual reference, while selected capabilities are rebuilt as independently verified vertical slices.

The modernisation path represented here is:

> preserved legacy reference → modern domain and repositories → service and web slices → curated catalog import → public category and artwork browsing → restored legacy visual identity → splash home and portfolio Bio → authenticated administration shell

This is not a feature-for-feature port, and the overall modernisation is not complete.

## Repository structure

```text
gallery-app/
├── legacy/gallery/       # Preserved Spring MVC, JSP, Hibernate and Maven WAR
└── modernised/gallery/   # Spring Boot, Thymeleaf, Spring Data JPA and Gradle
```

- [Legacy module reference](legacy/gallery/readme.md)
- [Modernised implementation and runbook](modernised/gallery/readme.md)

## Implemented and verified

The modernised application currently provides:

- Java 21 domain and repository foundations backed by Flyway-managed schema evolution;
- transactional services and immutable read models separating persistence entities from web/API contracts;
- a deterministic startup import of 10 curated artworks and their static images;
- category reuse, stable catalog identity, rendition metadata, and repeat-safe imports;
- public category and artwork browsing through Thymeleaf pages and JSON APIs;
- restored Barbara Israel Fine Arts visual identity using the preserved shell, navigation, and image assets, without claiming pixel-perfect parity;
- a restored splash home at `/` and a public portfolio Bio at `/bio`;
- a custom Spring Security login page whose successful login redirects to authenticated `/admin`;
- a protected, legacy-styled administration landing page that intentionally contains no management functionality yet; and
- repository, service, controller, persistence, security, rendering, and real-HTTP integration coverage.

The latest local verification passed 71 automated tests, the full Gradle build, static analysis, packaging, and light runtime acceptance.

## Technology direction

| Legacy reference | Modernised implementation |
| --- | --- |
| Java 6 source target | Java 21 toolchain |
| Spring Framework 3.0 XML configuration | Spring Boot 3.5 and Java configuration |
| Hibernate 3 DAOs / JPA alternatives | Spring Data JPA repositories and transactional services |
| JSP/JSTL and servlet mappings | Thymeleaf MVC pages and JSON controllers |
| Manually evolved schema | Forward-only Flyway migrations |
| Maven WAR | Gradle executable Boot JAR |

The strategy is to understand a legacy flow, implement the smallest coherent replacement, add automated coverage, prove it over real HTTP, and only then extend the modern application.

## Current scope and boundaries

The modernised application intentionally focuses on the browsing, catalog, visual-parity, and authentication foundations completed so far.

- Virtual Exhibitions and Recent Works remain intentionally non-functional rather than inventing behavior for incomplete legacy features.
- The administration shell is implemented, while artwork upload, curation, update/delete, exhibition management, and interest capture remain later modernization slices.
- The current curated catalog uses the established five-category model. Production taxonomy ownership will be addressed when administrator-driven content management is introduced.
- `generalViewable` and `privilegeViewable` are retained from the domain model but are not yet used as authorization rules.
- Current authentication is development-oriented; production identity and role-based authorization remain future work.

## Remaining roadmap

- define production authentication and authorization, with OAuth/OIDC still future work;
- prove Flyway and persistence behavior against PostgreSQL rather than treating the included driver as production readiness;
- implement only approved upload, exhibition, interest, and advanced navigation behavior;
- add production container, deployment, health-policy, observability, and operational documentation; and
- complete final traceable legacy-to-modern acceptance after the remaining decisions and implementation slices.

## Historical context

The legacy application grew from hands-on work with the XML-era Spring and Hibernate stack. It remains intact because its controllers, mappings, JSPs, CSS, assets, and domain model are more useful as traceable source material than as code to retrofit in place.
