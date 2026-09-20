# Gallery Application: Legacy and Modernised

This repository preserves an early Java gallery application beside an incremental Spring Boot modernisation. It is intended as a before-and-after engineering study: the legacy module supplies behavioural and design evidence, while the modernised module rebuilds selected flows with current Java, explicit migrations, automated tests, and narrower security boundaries.

The modernisation is not a feature-for-feature port. Completed behaviour and future work are kept separate below.

## Repository structure

```text
gallery-app/
├── legacy/gallery/       # Preserved Spring MVC, JSP, Hibernate and Maven WAR
└── modernised/gallery/   # Spring Boot, Thymeleaf, Spring Data JPA and Gradle
```

- [Legacy module reference](legacy/gallery/readme.md)
- [Modernised implementation and runbook](modernised/gallery/readme.md)

## What is implemented now

The modernised application currently provides:

- public category and artwork browsing through Thymeleaf pages and JSON APIs;
- a deterministic startup import of 10 curated artworks and their static images;
- category reuse, stable catalog identity, rendition metadata, and repeat-safe imports;
- a root redirect to the category browser;
- a custom Spring Security login page while unrelated routes, including Actuator, remain protected;
- legacy-inspired page-shell styling and shared navigation, without claiming exact visual parity;
- Flyway-managed schema evolution and repository, service, controller, persistence, and HTTP integration tests.

The latest local verification on the current feature branch passed 64 tests, the full Gradle build, static analysis, and packaged-application HTTP acceptance. GitHub Actions also defines a Gradle build workflow for pushes to `develop` and pull requests to `master`; this README does not imply that the current uncommitted branch has run remotely.

## Technology direction

| Legacy reference | Modernised implementation |
| --- | --- |
| Java 6 source target | Java 21 toolchain |
| Spring Framework 3.0 XML configuration | Spring Boot 3.5 and Java configuration |
| Hibernate 3 DAOs / JPA alternatives | Spring Data JPA repositories and transactional services |
| JSP/JSTL and servlet mappings | Thymeleaf MVC pages and JSON controllers |
| Manually evolved schema | Forward-only Flyway migrations |
| Maven WAR | Gradle executable Boot JAR |

The strategy is to modernise in verified slices: understand a legacy flow, implement the smallest coherent replacement, add automated coverage, prove it over real HTTP, and only then update documentation and the project tracker.

## Remaining roadmap

The modernised module does not yet implement Virtual Exhibitions, Bio content, artwork administration/upload, interest capture, OAuth/OIDC, advanced gallery navigation or lightbox behaviour, exact legacy visual parity, or production container/observability infrastructure. See the [modernised module README](modernised/gallery/readme.md) for precise routes, commands, and limitations.

## Historical context

The legacy application grew from hands-on work with the XML-era Spring and Hibernate stack. It remains intact because its controllers, mappings, JSPs, CSS, assets, and domain model are more useful as traceable source material than as code to retrofit in place.
