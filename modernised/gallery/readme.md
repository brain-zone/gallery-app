# Gallery Application — Modernised

This module is the actively developed Spring Boot implementation of the Gallery Application. It modernises verified legacy behaviour in small, tested vertical slices rather than claiming a complete feature-for-feature port.


## Current stack

| Area | Implementation |
| --- | --- |
| Language | Java 21 toolchain |
| Framework | Spring Boot 3.5.8, Spring Web MVC |
| Views | Thymeleaf templates with shared navigation and preserved legacy visual identity |
| Security | Spring Security form login and HTTP Basic |
| Persistence | Spring Data JPA and transactional services |
| Schema | Forward-only Flyway migrations V0–V4 |
| Database | H2 for verified local/runtime tests; PostgreSQL driver and Flyway support included but not production-proven |
| Build and quality | Gradle, JUnit 5, Spring tests, JaCoCo, Spotless, and SpotBugs |

## Implemented and verified

### Architecture and browsing

- Repositories load category and artwork aggregates.
- Transactional services map entities to immutable read models.
- Separate MVC and REST controllers expose HTML and JSON routes without serializing persistence entities.
- Public category browsing renders category counts, artwork summaries, curated images, and artwork details.
- Missing public category, artwork, and image resources return 404.

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
- persists source artist and genre in dedicated fields; and
- validates price entries but does not persist them because the current domain has no price model.

V4 also removes only the known synthetic V3 `Evening Sky` record by matching its seed metadata and rendition checksum. The original migrations remain forward-only.

### Public shell and legacy identity

The legacy visual-parity milestone is implemented. The modern application restores the recognisable Barbara Israel Fine Arts splash page, background, header, navigation, divider, category composition, artwork layout, thumbnail rail, login shell, and administration shell using modern Thymeleaf and CSS.

This is verified preservation of visual identity and layout intent, not a claim of pixel-perfect rendering across every browser. Prototype, Scriptaculous, the old Lightbox implementation, IE hacks, and obsolete layout techniques were not restored.

The Bio page is implemented as a public portfolio profile while retaining the established visual shell.

### Routes

| Method and route | Result | Access |
| --- | --- | --- |
| `GET /` | Restored splash home | Anonymous |
| `GET /categories` | Category list | Anonymous |
| `GET /categories/{id}` | Category with curated artwork images | Anonymous |
| `GET /artworks/{id}` | Artwork image, artist, genre, metadata, categories, and renditions | Anonymous |
| `GET /bio` | Portfolio Bio page | Anonymous |
| `GET /login` | Custom Spring Security login page | Anonymous |
| `POST /login` | Spring Security form authentication | Anonymous with CSRF token |
| `GET /admin` | Administration landing page | Authenticated |
| `GET /api/categories` | Category summaries as JSON | Anonymous |
| `GET /api/categories/{id}` | Category and artwork summaries as JSON | Anonymous |
| `GET /api/artworks/{id}` | Artwork detail as JSON | Anonymous |
| `GET /css/**` | Gallery stylesheet | Anonymous |
| `GET /images/**` | Preserved shell and Bio image assets | Anonymous |
| `GET /artworks/images/**` | Bundled catalog images | Anonymous |

### Security behavior

- Public access is limited to the documented GET routes and static asset families.
- Error dispatches are permitted so missing public resources remain 404 instead of becoming authentication failures.
- The custom login form posts to `/login`, uses Spring Security's `username` and `password` fields, and includes a CSRF token.
- Successful authentication always redirects to `/admin`; failed authentication returns to `/login?error`.
- `/admin` is protected but currently requires authentication only, not an administrator role.
- Anonymous Actuator access remains protected; for example, `/actuator/health` returns 401.
- HTTP Basic remains enabled alongside form login.
- Spring Boot's generated development user/password is the current runtime credential source. This is not production authentication.

## Persistence model and migrations

- `ArtEntity`, `Category`, `Comment`, and rendition value objects form the current model.
- `ArtworkRepository` and `CategoryRepository` provide aggregate loading and catalog/category identity lookups.
- Flyway creates the schema and indexes, seeds five categories, records the superseded V3 demo, and prepares catalog identity in V4.
- Hibernate runs with `ddl-auto=validate`; Flyway owns schema changes.
- H2 is the verified database for local and automated execution.
- PostgreSQL dependencies are present, but PostgreSQL migration/integration and production-readiness proof remain future work.

## Build, test, and run

From this module:

```bash
cd modernised/gallery

./gradlew spotlessApply
./gradlew clean test
./gradlew build
./gradlew bootRun
```

Open [http://localhost:8080](http://localhost:8080). The root renders the restored splash home; Galleries links to `/categories` and Bio links to `/bio`.

The latest verification on `develop` passed:

- 71 automated tests with no failures, errors, or skips;
- the complete Gradle build, including Spotless, SpotBugs, JaCoCo, and packaging;
- real HTTP checks for the splash home, Bio, public browsing pages/APIs, CSS, images, missing resources, login success/failure, authenticated `/admin`, CSRF enforcement, and protected Actuator access; and
- startup import of 10 curated records plus repeat-safe importer coverage.

The GitHub Actions build runs `./gradlew build --no-daemon` for pushes to `develop`, pull requests to `master`, and manual dispatch. A separate dependency-submission workflow is configured for `master` pushes and manual dispatch.

## Intentional placeholders

- Virtual Exhibitions is displayed in the preserved navigation but is intentionally non-functional.
- Recent Works is displayed on the category page but is intentionally non-functional.
- `/admin` is currently an authenticated landing page only.

## Current boundaries

- Artwork upload, curation, exhibition management, and related administration workflows are future slices.
- The bundled catalog uses the five seeded categories and may create a missing category during import. Production taxonomy ownership will be addressed when content-management workflows are introduced.
- `generalViewable` and `privilegeViewable` remain in the inherited domain model but are not yet enforced as public-access rules.
- Authentication currently uses Spring Boot's generated development user; production identities, roles, and authorization remain future work.
- Catalog price data is validated but intentionally remains outside the current domain model.
- PostgreSQL support is present as a dependency, but production/integration proof remains future work.

## Future roadmap

- artwork upload, curation, update/delete, and related administrator workflows;
- exhibition creation and management;
- visitor interest capture;
- approved previous/next or modern lightbox behavior;
- production authentication, with OAuth/OIDC still future work;
- PostgreSQL integration and migration proof;
- container, deployment, health-policy, observability, and operational documentation; and
- final traceable legacy-to-modern acceptance.

For the historical behaviour and assets that guide later slices, see the [legacy module README](../../legacy/gallery/readme.md).
