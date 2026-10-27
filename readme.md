# Gallery Application: Legacy and Modernised

This repository preserves an early Java gallery application beside an incremental Spring Boot modernisation. It is a before-and-after engineering study: the legacy module remains the behavioural and visual reference, while selected capabilities are rebuilt as independently verified vertical slices.

The modernisation path represented here is:

> preserved legacy reference → modern domain and repositories → service and web slices → curated catalog import → public category and artwork browsing → restored legacy visual identity → splash home and portfolio Bio → persisted administrator authorization → artwork upload and public visibility enforcement

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
- a repeat-safe startup import of 14 demonstration artworks across six categories, with ORIGINAL, GALLERY, and THUMBNAIL renditions in external filesystem storage;
- category reuse, stable catalog identity, rendition metadata, and repeat-safe imports;
- public category and artwork browsing through Thymeleaf pages and JSON APIs;
- restored Barbara Israel Fine Arts visual identity using the preserved shell, navigation, and image assets, without claiming pixel-perfect parity;
- a restored splash home at `/` and a public portfolio Bio at `/bio`;
- a custom Spring Security login page whose successful login redirects to authenticated `/admin`;
- a persisted, BCrypt-protected administrator with `ROLE_ADMIN`, an administration menu, and CSRF-protected logout;
- administrator upload at `GET`/`POST /admin/artworks/upload`, accepting JPEG/PNG, metadata, and one existing category, generating three renditions and publishing the artwork immediately;
- category-scoped thumbnails, bounded previous/next navigation, category and standalone artwork Lightboxes, and detail-page rendition selection;
- `generalViewable` enforcement across public counts, listings, navigation, detail pages, APIs, and direct media requests; and
- repository, service, controller, persistence, security, rendering, and real-HTTP integration coverage.

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
- Artwork upload and administrator authorization are implemented. Curation controls, artwork update/delete, category management, exhibition management, and interest capture remain later slices.
- The demonstration catalog spans Abstract, Landscape, Portrait, Sculpture, Urban, and Wireframes. Production taxonomy ownership and migration remain outside this slice.
- `generalViewable` controls public access; `privilegeViewable` does not grant a separate viewing entitlement. There is no administrator publication editor yet.
- Authentication uses a persisted administrator and enforced `ROLE_ADMIN` access. Broader account lifecycle, production identity integration, and OAuth/OIDC remain future work.
- Shared navigation still lacks a Home link and route-aware current-page state; fixed-width layout and image-based labels remain presentation gaps.

## Remaining roadmap

- define production authentication and authorization, with OAuth/OIDC still future work;
- prove Flyway and persistence behavior against PostgreSQL rather than treating the included driver as production readiness;
- define and implement the remaining curation, exhibition, recent-works, interest, and broader administration flows;
- add production container, deployment, health-policy, observability, and operational documentation; and
- complete final traceable legacy-to-modern acceptance after the remaining decisions and implementation slices.

## Historical context

The legacy application grew from hands-on work with the XML-era Spring and Hibernate stack. It remains intact because its controllers, mappings, JSPs, CSS, assets, and domain model are more useful as traceable source material than as code to retrofit in place.
