# Gallery Application — Modernised

This module is the actively developed Spring Boot implementation of the Gallery Application. It modernises verified legacy behaviour in small, tested slices rather than claiming a complete feature-for-feature port.

## Current stack

| Area | Implementation |
| --- | --- |
| Language | Java 21 toolchain |
| Framework | Spring Boot 3.5.8, Spring Web MVC |
| Views | Thymeleaf templates with shared navigation and legacy-inspired CSS |
| Security | Spring Security form login and HTTP Basic |
| Persistence | Spring Data JPA and transactional services |
| Schema | Forward-only Flyway migrations V0–V4 |
| Database | H2 for local/runtime tests; PostgreSQL driver and Flyway support are included |
| Build and quality | Gradle, JUnit 5, Spring tests, JaCoCo, Spotless, and SpotBugs |

## Implemented behaviour

### Curated catalog bootstrap

At application startup, `ArtworkCatalogStartup` invokes the transactional `ArtworkCatalogImporter` against `static/artworks/artworks.json`.

The importer:

- validates all catalog records and referenced images before writing;
- imports 10 artworks across Abstract, Landscape, Portrait, Sculpture, and Urban;
- reuses category names with case-insensitive exact matching and creates only missing categories;
- uses the source `logicalFileName`, normalized to lower case, as `ArtEntity.catalogKey`;
- relies on the nullable unique `catalog_key` index introduced by Flyway V4;
- updates an existing catalog record on repeat startup rather than creating a duplicate;
- synchronizes one category and one `GALLERY` rendition per curated record;
- derives image content type, byte size, dimensions, and SHA-256 checksum;
- persists source artist and genre in dedicated fields;
- validates price entries but does not persist them because the current domain has no price model.

V4 also removes only the known synthetic V3 `Evening Sky` record by matching its seed metadata and rendition checksum. The original migrations remain forward-only.

### Browse and shell routes

| Method and route | Result | Anonymous access |
| --- | --- | --- |
| `GET /` | 302 redirect to `/categories` | Yes |
| `GET /categories` | Thymeleaf category list | Yes |
| `GET /categories/{id}` | Category with curated artwork images | Yes |
| `GET /artworks/{id}` | Artwork image, artist, genre, metadata, categories and renditions | Yes |
| `GET /login` | Custom Spring Security login page | Yes |
| `GET /api/categories` | Category summaries as JSON | Yes |
| `GET /api/categories/{id}` | Category and artwork summaries as JSON | Yes |
| `GET /api/artworks/{id}` | Artwork detail as JSON | Yes |
| `GET /css/**` | Gallery stylesheet | Yes |
| `GET /artworks/images/**` | Bundled catalog image resources | Yes |

Missing category, artwork, and image resources return 404. Other routes require authentication; for example, an anonymous `GET /actuator/health` returns 401. Error dispatches are permitted so public missing-resource responses remain 404 instead of becoming authentication failures.

The login form posts to `/login`, uses Spring Security's `username` and `password` fields, and includes a CSRF token. No administrator application flow exists yet. With the current development configuration, Spring Boot supplies its generated development user/password at startup.

The shared navigation currently renders Galleries, Virtual Exhibitions, and Bio. Only Galleries is implemented: Virtual Exhibitions is a placeholder and `/bio` has no controller/content in the modern module.

## Persistence model and migrations

- `ArtEntity`, `Category`, `Comment`, and rendition value objects form the current model.
- `ArtworkRepository` and `CategoryRepository` provide aggregate loading and catalog/category identity lookups.
- DTO-style immutable records isolate HTML/JSON browsing from entity serialization.
- Flyway creates the schema and indexes, seeds the five categories, records the superseded V3 demo, and prepares the curated catalog in V4.
- Hibernate runs with `ddl-auto=validate`; Flyway owns schema changes.

## Build, test, and run

From this module:

```bash
cd modernised/gallery

./gradlew spotlessApply
./gradlew test
./gradlew build
./gradlew bootRun
```

Open [http://localhost:8080](http://localhost:8080). The root redirects to the category browser.

The latest verification of the current uncommitted feature branch passed:

- 64 automated tests;
- the complete Gradle build, including Spotless, SpotBugs, JaCoCo, and packaging;
- real HTTP checks for public pages/APIs, CSS, images, missing resources, login markup, and protected Actuator access;
- startup import of 10 curated records and a repeat-safe importer test.

The repository GitHub Actions build runs `./gradlew build --no-daemon` for pushes to `develop`, pull requests to `master`, and manual dispatch. A separate dependency-submission workflow is configured for `master` pushes and manual dispatch.

## Current limitations and future work

- Exact legacy visual parity is incomplete; the current CSS is legacy-inspired only.
- Virtual Exhibitions and Bio are not implemented in the modern module.
- Artwork administration, browser upload, curation, update/delete, and interest capture are not implemented.
- OAuth/OIDC is not configured.
- Advanced gallery navigation and lightbox behaviour remain future work.
- Catalog price metadata is validated but not persisted.
- The JSON catalog references 10 JPEGs. Four WebP source assets and their manually created derivative files remain outside the catalog because no JSON record references them.
- H2 is the verified local/test database. Production PostgreSQL configuration, containers, deployment, and observability remain future scope.

For the historical behaviour and assets that guide later slices, see the [legacy module README](../../legacy/gallery/readme.md).
