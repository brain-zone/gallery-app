# Gallery Application — Modernised

This module is the actively developed Spring Boot implementation of the Gallery Application. It
modernises verified legacy behaviour in small, tested vertical slices rather than claiming a
complete feature-for-feature port.

## Modernisation approach

The repository retains both implementations:

- `legacy/gallery` is the historical application and the evidence source for existing behaviour;
- `modernised/gallery` is the replacement Spring Boot application, built and accepted one vertical
  slice at a time.

Modernisation is delivered through incremental, tested vertical slices. Each slice is verified
against the relevant legacy behaviour before it is accepted as the modern replacement. The legacy
module remains available as the behavioural reference until every required capability has a verified
modern implementation and the final cutover is complete.


## Current stack

| Area | Implementation |
| --- | --- |
| Language | Java 21 toolchain |
| Framework | Spring Boot 3.5.8, Spring Web MVC |
| Views | Thymeleaf templates with shared Home, Galleries, disabled Virtual Exhibitions and Bio navigation, current-page and keyboard-focus states, and preserved legacy visual identity |
| Security | Spring Security form login and HTTP Basic |
| Persistence | Spring Data JPA and transactional services |
| Schema | Forward-only Flyway migrations V0–V6 |
| Database | File-backed H2 for normal local runs; isolated in-memory H2 for automated tests |
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
| `GET /categories/{categoryId}/artworks/{artworkId}` | Selected artwork within its category | Anonymous |
| `GET /artworks/{id}` | Artwork image, artist, genre, metadata, categories, and renditions | Anonymous |
| `GET /bio` | Portfolio Bio page | Anonymous |
| `GET /login` | Custom Spring Security login page | Anonymous |
| `POST /login` | Spring Security form authentication | Anonymous with CSRF token |
| `POST /logout` | Invalidate the authenticated session | CSRF token required |
| `GET /admin` | Administration landing page | `ROLE_ADMIN` only |
| `GET /admin/artworks/upload` | Artwork-upload form | `ROLE_ADMIN` only |
| `POST /admin/artworks/upload` | Generate renditions and publish artwork | `ROLE_ADMIN` and CSRF token |
| `GET /api/categories` | Category summaries as JSON | Anonymous |
| `GET /api/categories/{id}` | Category and artwork summaries as JSON | Anonymous |
| `GET /api/artworks/{id}` | Artwork detail as JSON | Anonymous |
| `GET /css/**` | Gallery stylesheet | Anonymous |
| `GET /images/**` | Preserved shell and Bio image assets | Anonymous |
| `GET /js/**` | Artwork dialog and rendition-selection scripts | Anonymous |
| `GET`/`HEAD /media/**` | Externally stored artwork originals and renditions | Public artwork only; hidden/unregistered objects return 404 |

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
- Flyway migrations V0–V6 create and evolve the schema, seed the initial categories, import the
  curated catalog model, persist administrator identity, and activate general artwork visibility.
- Hibernate runs with `ddl-auto=validate`; Flyway owns schema changes.
- Normal local execution uses file-backed H2 at `./gallery-data/gallery` (relative to
  `modernised/gallery`). Override the location without changing source using
  `GALLERY_DATABASE_PATH=/absolute/path/to/gallery-db/gallery`. H2 creates files alongside that
  basename; `gallery-data/` is excluded from Git.
- Automated tests override the datasource with isolated in-memory H2 databases.
- PostgreSQL dependencies are present, but PostgreSQL migration/integration and production-readiness proof remain future work.

To reset only the local database, stop the application and remove the generated files whose names
begin with `gallery` from `modernised/gallery/gallery-data/` (or from the configured
`GALLERY_DATABASE_PATH` directory). On the next startup, supply a new
`GALLERY_ADMIN_BOOTSTRAP_PASSWORD`; Flyway recreates the schema, the 14 curated artworks are
re-imported once, and the administrator is bootstrapped again. Resetting the database does not
delete uploaded rendition directories from a separately configured artwork storage root.

### Administrator artwork upload

After signing in as `admin`, follow **Upload an artwork** from `/admin`. The form requires one JPEG
or PNG image (maximum 10 MB), a title, and exactly one existing category selected from the database.
Artist, genre, subtitle, media, display date, caption, and description are optional. Category
creation and publication controls are intentionally absent.

Each successful upload receives a UUID and is stored as:

```text
artwork-media/artworks/images/{uuid}/original.{jpeg|jpg|png}
artwork-media/artworks/images/{uuid}/gallery.{jpeg|jpg|png}
artwork-media/artworks/images/{uuid}/thumbnail.{jpeg|jpg|png}
```

The original is preserved byte-for-byte. Gallery (maximum 800 × 600) and thumbnail (maximum 96 ×
96) renditions preserve aspect ratio and are not upscaled. JPA stores their logical object keys,
content types, byte sizes, dimensions, and SHA-256 checksums—not image binaries. The files are
published as one directory before metadata is committed; a validation or persistence failure
removes the new directory. New artwork has `generalViewable=true` immediately, retains the existing
default `privilegeViewable=false`, and flows through the existing category, artwork detail,
Lightbox, `ArtworkUrlResolver`, and `/media` behavior.

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

The baseline verification on `feature/admin-artwork-upload` passed:

- 113 automated tests with no failures, errors, or skips;
- the complete Gradle build, including Spotless, SpotBugs, JaCoCo, and packaging;
- HTTP integration checks for the splash home, Bio, public browsing pages/APIs, externally stored
  artwork media, missing and unsafe media paths, login success/failure, authenticated `/admin`, CSRF
  enforcement, and protected Actuator access; and
- startup import of 14 curated records with 42 renditions plus repeat-safe importer coverage.

The GitHub Actions build runs `./gradlew build --no-daemon` for pushes to `develop`, pull requests to `master`, and manual dispatch. A separate dependency-submission workflow is configured for `master` pushes and manual dispatch.

## Intentional placeholders

- Virtual Exhibitions is displayed in the preserved navigation but is intentionally non-functional.
- Recent Works is displayed on the category page but is intentionally non-functional.
- Administration beyond artwork upload is still under construction.

## Current boundaries

- Artwork edit/delete, category management, exhibition management, and other administration workflows are future slices.
- The bundled catalog spans six categories (Abstract, Landscape, Portrait, Sculpture, Urban, and Wireframes) and may create a missing category during import. Production taxonomy ownership remains outside this slice.
- `generalViewable` controls anonymous listings, counts, artwork details, navigation, APIs, and rendition delivery. `privilegeViewable` remains outside the current public-access model.
- Authentication uses a persisted administrator identity with `ROLE_ADMIN`; broader identity and authorization requirements remain future work.
- Catalog price data is validated but intentionally remains outside the current domain model.
- PostgreSQL support is present as a dependency, but production/integration proof remains future work.

## Future roadmap

- curation, update/delete, category management, and related administrator workflows beyond the implemented upload;
- exhibition creation and management;
- visitor interest capture;
- production authentication, with OAuth/OIDC still future work;
- PostgreSQL integration and migration proof;
- container, deployment, health-policy, observability, and operational documentation; and
- final traceable legacy-to-modern acceptance.

For the historical behaviour and assets that guide later slices, see the [legacy module README](../../legacy/gallery/readme.md).
