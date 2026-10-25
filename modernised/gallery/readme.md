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

At application startup, `ArtworkCatalogStartup` invokes the transactional `ArtworkCatalogImporter` against the bundled metadata file `static/artworks/artworks.json`. Artwork bytes are read through `ArtworkStorage` from external filesystem storage; they are not application resources.

The importer:

- validates all catalog records and referenced images before writing;
- imports 14 artworks across Abstract, Landscape, Portrait, Sculpture, Urban, and Wireframes;
- reuses category names with case-insensitive exact matching and creates only missing categories;
- uses the source `logicalFileName`, normalized to lower case, as `ArtEntity.catalogKey`;
- relies on the nullable unique `catalog_key` index introduced by Flyway V4;
- updates an existing catalog record on repeat startup rather than creating a duplicate;
- synchronizes one category and the `ORIGINAL`, `GALLERY`, and `THUMBNAIL` renditions per curated record;
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
| `GET /admin` | Administration landing page | `ROLE_ADMIN` only |
| `GET /api/categories` | Category summaries as JSON | Anonymous |
| `GET /api/categories/{id}` | Category and artwork summaries as JSON | Anonymous |
| `GET /api/artworks/{id}` | Artwork detail as JSON | Anonymous |
| `GET /css/**` | Gallery stylesheet | Anonymous |
| `GET /images/**` | Preserved shell and Bio image assets | Anonymous |
| `GET`/`HEAD /media/**` | Externally stored artwork originals and renditions | Anonymous |

### Security behavior

- Public access is limited to the documented GET routes and static asset families.
- Error dispatches are permitted so missing public resources remain 404 instead of becoming authentication failures.
- The custom login form posts to `/login`, uses Spring Security's `username` and `password` fields, and includes a CSRF token.
- Successful authentication always redirects to `/admin`; failed authentication returns to `/login?error`.
- `/admin` and `/admin/**` require the persisted administrator's `ROLE_ADMIN` authority.
- Anonymous Actuator access remains protected; for example, `/actuator/health` returns 401.
- HTTP Basic remains enabled alongside form login.
- Spring Security loads the administrator from the database; Spring Boot's generated development user is not used.

### Administrator bootstrap

On the first startup against a fresh database, supply the initial administrator password through
the environment and then start the application:

```bash
export GALLERY_ADMIN_BOOTSTRAP_PASSWORD='<choose-a-strong-password>'
./gradlew bootRun
```

The application creates the `admin` account with `ROLE_ADMIN` and stores only a BCrypt hash. Later
startups preserve the existing account and password hash, so the environment variable is no longer
required after the account exists. If the account is absent and the variable is missing or blank,
startup fails without exposing a password value.

## Persistence model and migrations

- `ArtEntity`, `Category`, `Comment`, and rendition value objects form the current model.
- `ArtworkRepository` and `CategoryRepository` provide aggregate loading and catalog/category identity lookups.
- Flyway creates the schema and indexes, seeds five categories, records the superseded V3 demo, and prepares catalog identity in V4.
- Hibernate runs with `ddl-auto=validate`; Flyway owns schema changes.
- H2 is the verified database for local and automated execution.
- PostgreSQL dependencies are present, but PostgreSQL migration/integration and production-readiness proof remain future work.

## Build, test, and run

### Artwork filesystem storage

The repository's 42 curated media source files are held outside `src/main/resources`. Each artwork
has a directory named with its stable source identity and three purpose-named rendition objects:

```text
artwork-media/artworks/images/{artwork-identity}/original.{ext}
artwork-media/artworks/images/{artwork-identity}/gallery.{ext}
artwork-media/artworks/images/{artwork-identity}/thumbnail.{ext}
```

The default local configuration reads the tracked external-media directory directly:

```properties
gallery.artwork.storage.root=${GALLERY_ARTWORK_STORAGE_ROOT:./artwork-media}
```

Run Gradle from `modernised/gallery`, so the default relative path resolves correctly. To provision
an independent writable storage root, copy the complete curated set with the repeatable Gradle task
and point the application at it:

```bash
./gradlew provisionArtworkMedia -PartworkStorageRoot=/absolute/path/to/gallery-artwork-media
export GALLERY_ARTWORK_STORAGE_ROOT=/absolute/path/to/gallery-artwork-media
```

The task deliberately requires an explicit destination and refuses to overwrite the tracked source
directory. Preserve the `artworks/images/{artwork-identity}/{rendition}.{ext}` layout because those
paths are persistent logical object keys in the database. The application validates all 42 objects
at startup and fails before catalogue writes when any required original or derivative is unavailable
or invalid.

Browser-facing URLs are resolved separately from storage keys and currently use `/media/{objectKey}`.
That resolver can later be replaced by a cloud/CDN URL strategy without changing gallery templates.

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
