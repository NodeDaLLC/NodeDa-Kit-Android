# Nrova Android

**Current version: `1.0`** &nbsp;·&nbsp; available at runtime as `Nrova.VERSION`.

The official Android/Kotlin SDK for the **Nrova** HTTP APIs. One typed
client, one auth scheme, every public service Nrova exposes — built on
Kotlin coroutines, `kotlinx.serialization`, and OkHttp. Mirrors the
[Nrova Swift package](./Nrova%20Swift/) API for API parity across iOS
and Android apps.

```kotlin
import com.nrova.sdk.NrovaClient
import com.nrova.sdk.distribution.DistributionChannel
import com.nrova.sdk.distribution.DistributionPlatform

// Reads `com.nrova.sdk.ApiKey` and (optional) `com.nrova.sdk.OrganizationId`
// from your AndroidManifest.xml.
val client = NrovaClient.fromManifest(context)

val latest = client.distribution.latest(
    appId = "acme-notes",
    platform = DistributionPlatform.MACOS,
    channel = DistributionChannel.STABLE,
)
println("Latest version: ${latest.artifact.version ?: latest.release.version}")
println("SDK version: ${Nrova.VERSION}") // "1.0"
```

## Table of contents

- [Version](#version)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration via AndroidManifest.xml](#configuration-via-androidmanifestxml)
- [Authentication](#authentication)
- [Top-level client](#top-level-client)
- [Services](#services)
  - [Distribution API](#distribution-api)
  - [Support API](#support-api)
  - [Sales API](#sales-api)
  - [Careers API](#careers-api)
  - [Newsroom API](#newsroom-api)
  - [Feature Flags API](#feature-flags-api)
  - [System Status API](#system-status-api)
  - [Legal Policies API](#legal-policies-api)
- [Error handling](#error-handling)
- [Custom transports & testing](#custom-transports--testing)
- [Configuration reference](#configuration-reference)
- [Project layout](#project-layout)
- [Building and contributing](#building-and-contributing)
- [Publishing releases](#publishing-releases)
- [SPM ↔ Gradle: what to know](#spm--gradle-what-to-know)
- [License](#license)

---

## Version

|                     |                                                |
| ------------------- | ---------------------------------------------- |
| **SDK version**     | `1.0`                                          |
| **Runtime constant**| `com.nrova.sdk.Nrova.VERSION`                  |
| **Schema**          | `nrova.distribution.v1` (Distribution API)     |

`Nrova.VERSION` is updated in lockstep with the released git tag — log it
at startup to make support tickets easier to triage:

```kotlin
Log.i("NrovaApp", "Nrova SDK ${Nrova.VERSION} booted")
```

## Requirements

| Tooling          | Minimum                                      |
| ---------------- | -------------------------------------------- |
| Android          | 5.0 (API 21) — `minSdk = 21`                 |
| Compile SDK      | 35                                           |
| Java             | 17                                           |
| Kotlin           | 2.0+                                         |
| Android Gradle Plugin | 8.3+                                    |

No third-party Nrova dependencies — only OkHttp, kotlinx-serialization,
and kotlinx-coroutines, all of which are standard on modern Android
projects.

## Installation

The library is published as a single Maven artifact:

```text
com.nrova:nrova-android:1.0
```

### Gradle (Kotlin DSL — `build.gradle.kts`)

Add Maven Central to your repositories (it's there by default in
`settings.gradle.kts` for new Android projects), then declare the
dependency on your app module:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.nrova:nrova-android:1.0")
}
```

### Gradle (Groovy DSL — `build.gradle`)

```groovy
dependencies {
    implementation 'com.nrova:nrova-android:1.0'
}
```

### Version catalog (`gradle/libs.versions.toml`)

```toml
[versions]
nrova = "1.0"

[libraries]
nrova-android = { group = "com.nrova", name = "nrova-android", version.ref = "nrova" }
```

```kotlin
dependencies {
    implementation(libs.nrova.android)
}
```

### From GitHub Packages

If your team prefers GitHub Packages over Maven Central, add the GitHub
repo (replace `OWNER`) to your settings file's `dependencyResolutionManagement`:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/OWNER/Nrova-Android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

GitHub Packages always requires a credential, even for public packages.

## Configuration via AndroidManifest.xml

The recommended way to wire the client up — keep credentials out of source.

In your app's `AndroidManifest.xml`:

```xml
<application
    android:name=".NrovaApp"
    …>

    <meta-data
        android:name="com.nrova.sdk.ApiKey"
        android:value="@string/nrova_api_key" />
    <meta-data
        android:name="com.nrova.sdk.OrganizationId"
        android:value="@string/nrova_org_id" />

</application>
```

The string resources should be fed from `local.properties` /
`BuildConfig` / a CI secret store rather than committed verbatim. A
common pattern in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        val props = Properties().apply {
            rootProject.file("local.properties").takeIf(File::exists)?.let { load(it.inputStream()) }
        }
        resValue("string", "nrova_api_key", props.getProperty("nrova.apiKey", ""))
        resValue("string", "nrova_org_id",  props.getProperty("nrova.orgId",  ""))
    }
}
```

Then in your `Application` subclass:

```kotlin
class NrovaApp : Application() {
    lateinit var nrova: NrovaClient
        private set

    override fun onCreate() {
        super.onCreate()
        nrova = NrovaClient.fromManifest(this)
    }
}
```

If you'd rather hand-construct the configuration you can do that too:

```kotlin
val client = NrovaClient(
    apiKey = BuildConfig.NROVA_API_KEY,
    organizationId = BuildConfig.NROVA_ORG_ID,
)
```

## Authentication

Every authenticated request sends both:

- `Authorization: Bearer <apiKey>`
- `X-API-Key: <apiKey>`

…matching the Swift SDK exactly. The `health()` endpoint on every
service is unauthenticated and never carries the headers.

## Top-level client

```kotlin
val client = NrovaClient(apiKey = "sk_live_…")

client.distribution      // DistributionService
client.support           // SupportService
client.sales             // SalesService
client.careers           // CareersService
client.newsroom          // NewsroomService
client.featureFlags      // FeatureFlagsService
client.systemStatus      // SystemStatusService
client.legal             // LegalService
```

All service methods are `suspend` — call them from a coroutine scope
(`viewModelScope`, `lifecycleScope`, your own structured scope, etc.).

```kotlin
viewModelScope.launch {
    val health = client.healthAll()
    health.forEach { (service, status) ->
        Log.i("Health", "$service -> ${status.ok}")
    }
}
```

`healthAll()` issues the eight `GET /health` requests in parallel using
`coroutineScope { async { … } }` (Kotlin equivalent of Swift's
`withThrowingTaskGroup`).

## Services

The full list of operations on each service is documented in the source
KDoc — the snippets below are starting points.

### Distribution API

```kotlin
val apps = client.distribution.listApplications().applications
val latest = client.distribution.latest(
    appId = "acme-notes",
    platform = DistributionPlatform.MACOS,
    channel = DistributionChannel.STABLE,
)
val downloadUrl = client.distribution.resolveDownloadUrl(
    appId = "acme-notes",
    platform = DistributionPlatform.MACOS,
)
```

### Support API

```kotlin
val ticket = client.support.createTicket(
    CreateSupportTicketRequest(
        contactEmail = "alice@example.com",
        applicationName = "Acme Notes Android",
        subject = "Crash on launch",
        body = "App crashes immediately after splash screen.",
        priority = SupportPriority.HIGH,
        category = SupportCategory.TECHNICAL,
    )
)
val comments = client.support.listComments(ticket.id)
```

### Sales API

```kotlin
val submission = client.sales.createSubmission(
    CreateSalesSubmissionRequest(
        contactEmail = "buyer@example.com",
        formName = "enterprise_inquiry",
        firstName = "Buyer",
        lastName = "Person",
        company = "Big Corp",
    )
)
```

### Careers API

```kotlin
val openings = client.careers.listPostings()
val template = client.careers.applicationTemplate()
val application = client.careers.submitApplication(
    SubmitCareerApplicationRequest(
        requisitionNodeId = openings.first().requisitionNodeId,
        templateVersion = template.templateVersion,
        applicantEmail = "applicant@example.com",
        answers = mapOf(
            "fullName" to JsonValue.Text("Jane Doe"),
            "yearsExperience" to JsonValue.IntValue(7),
        ),
    )
)
```

### Newsroom API

```kotlin
val published = client.newsroom.listPosts(status = NewsroomStatus.PUBLISHED)
val post = client.newsroom.getPost(idOrSlug = "launch-day", includeDocument = true)
```

### Feature Flags API

```kotlin
val enabled = client.featureFlags.isEnabled(
    flagKey = "dark_mode",
    subjectId = currentUserId,
    countryCode = "US",
)
```

### System Status API

```kotlin
val rollup = client.systemStatus.rollup()
Log.i("Status", "Overall: ${rollup.status?.name}")
rollup.components.forEach { Log.i("Status", "${it.name}: ${it.status?.name}") }
```

### Legal Policies API

```kotlin
val privacy = client.legal.getPolicyByKey(LegalPolicyKey.PRIVACY)
privacy.sections.forEach { println(it.title) }
```

## Error handling

`NrovaError` is a sealed exception hierarchy — catch the specific
subtype you care about, or the parent `NrovaError` to handle them all:

| Type | Thrown when |
| --- | --- |
| `NrovaError.Api` | Server returned a non-2xx status with a JSON `{error,message,details}` envelope. The `error.code` and `error.status` match the documented slugs (`invalid_api_key`, `not_found`, …). |
| `NrovaError.Transport` | The underlying OkHttp / network layer threw. `cause` is the original `IOException`. |
| `NrovaError.Decoding` | A 2xx response had a body the SDK could not decode against the declared model. `data` holds the raw bytes for debugging. |
| `NrovaError.UnexpectedStatus` | Non-2xx response that did not carry a recognized JSON error envelope. |
| `NrovaError.InvalidUrl` | A URL could not be constructed from the provided inputs (programmer error). |

```kotlin
try {
    client.distribution.listApplications()
} catch (e: NrovaError.Api) {
    when (e.error.code) {
        "invalid_api_key" -> showAuthError()
        "not_found"       -> showEmptyState()
        else              -> showGenericError(e.error.message)
    }
} catch (e: NrovaError.Transport) {
    showOfflineState()
}
```

## Custom transports & testing

The HTTP layer goes through a `NrovaTransport` interface so you can
swap in canned responses for unit tests, instrument outgoing requests,
or use a custom OkHttp configuration (interceptors, certificate
pinning, custom DNS):

```kotlin
val pinned = OkHttpClient.Builder()
    .certificatePinner(CertificatePinner.Builder().add("…", "sha256/…").build())
    .build()

val client = NrovaClient(
    apiKey = "sk_live_…",
    transport = OkHttpTransport(pinned),
)
```

Unit tests can use the SDK's `MockTransport` (in `src/test`) directly,
or roll their own implementation of `NrovaTransport`. See
[`nrova/src/test/kotlin/com/nrova/sdk/NrovaClientTest.kt`](./nrova/src/test/kotlin/com/nrova/sdk/NrovaClientTest.kt)
for ready-to-copy examples.

## Configuration reference

| Property         | Default                                                | Notes                                                                 |
| ---------------- | ------------------------------------------------------ | --------------------------------------------------------------------- |
| `apiKey`         | _required_                                             | Bearer + `X-API-Key`.                                                 |
| `organizationId` | `NrovaConfiguration.DEFAULT_ORGANIZATION_ID`           | **Decoy** — production apps must override via manifest or constructor.|
| `endpoints`      | `ServiceEndpoints.production`                          | Override to point at staging or a proxy.                              |
| `defaultHeaders` | `emptyMap()`                                           | Added to every request.                                               |
| `timeout`        | `30.seconds`                                           | Wraps every request — used when you bring your own `OkHttpClient`.    |

## Project layout

```
.
├── settings.gradle.kts          ← includes the :nrova module
├── build.gradle.kts             ← root project (plugins only)
├── gradle.properties            ← Maven coordinates, POM metadata
├── gradle/
│   ├── libs.versions.toml       ← version catalog
│   └── wrapper/gradle-wrapper.properties
├── nrova/                       ← the published Android library module
│   ├── build.gradle.kts         ← com.android.library + maven-publish config
│   ├── consumer-rules.pro       ← R8 rules applied to consumer apps
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/nrova/sdk/
│       │       ├── NrovaClient.kt
│       │       ├── core/        ← HttpClient, NrovaError, transports, …
│       │       ├── distribution/
│       │       ├── support/
│       │       ├── sales/
│       │       ├── careers/
│       │       ├── newsroom/
│       │       ├── featureflags/
│       │       ├── systemstatus/
│       │       └── legal/
│       └── test/kotlin/com/nrova/sdk/
│           ├── MockTransport.kt
│           └── NrovaClientTest.kt
├── samples/Usage.kt             ← illustrative consumer-side usage
├── Nrova Swift/                 ← reference iOS/Swift package (do not edit)
└── README.md
```

## Building and contributing

This repository ships only the source — generate the Gradle wrapper
once and then everything Just Works:

```bash
# 1. Initialize the Gradle wrapper (creates ./gradlew + jar)
gradle wrapper --gradle-version 8.10.2 --distribution-type bin

# 2. Build the library
./gradlew :nrova:assembleRelease

# 3. Run unit tests
./gradlew :nrova:test

# 4. Generate KDoc HTML to nrova/build/dokka
./gradlew :nrova:dokkaHtml
```

The release AAR ends up at
`nrova/build/outputs/aar/nrova-release.aar`.

## Publishing releases

The build uses the **vanniktech maven-publish plugin**, which talks to
the new Sonatype Central Portal natively, signs every artifact with
GPG, and produces sources + Javadoc jars. Three one-time setup steps:

### 1. Bump the version

Edit `gradle.properties`:

```properties
VERSION_NAME=1.1.0
```

`Nrova.VERSION` is regenerated automatically at build time so the
runtime constant cannot drift from the artifact coordinate.

### 2. Configure publishing credentials

Add to your `~/.gradle/gradle.properties` (never commit these):

```properties
# Sonatype Central Portal (https://central.sonatype.com/)
mavenCentralUsername=your-portal-username
mavenCentralPassword=your-portal-token

# GPG signing (https://central.sonatype.org/publish/requirements/gpg/)
signing.keyId=ABCDEF12
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/Users/you/.gnupg/secring.gpg

# OR, for CI:
signingInMemoryKey=base64-encoded-private-key
signingInMemoryKeyId=ABCDEF12
signingInMemoryKeyPassword=your-gpg-passphrase
```

### 3. Publish

```bash
# Stage to Central Portal then release automatically once validated.
./gradlew :nrova:publishToMavenCentral

# (or, to dry-run locally first)
./gradlew :nrova:publishToMavenLocal
```

Tag the commit and push:

```bash
git tag -a v1.1.0 -m "Release 1.1.0"
git push origin v1.1.0
```

### Publishing to GitHub Packages instead

Replace the Central Portal config in `nrova/build.gradle.kts` with a
GitHub Packages target — only the publishing block changes:

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Nrova-LLC/Nrova-Android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

…then run `./gradlew :nrova:publishReleasePublicationToGitHubPackagesRepository`.

## Versioning

The library follows **semantic versioning** (`MAJOR.MINOR.PATCH`):

| Bump  | When                                                          |
| ----- | ------------------------------------------------------------- |
| MAJOR | Breaking changes to the public Kotlin API.                    |
| MINOR | New endpoints, new optional parameters, new convenience APIs. |
| PATCH | Bug fixes, dependency bumps, documentation.                   |

The Maven coordinate (`com.nrova:nrova-android`), the
`Nrova.VERSION` constant, and the git tag (`vX.Y.Z`) are bumped
together. Use `1.x` constraints in consuming apps to stay on a single
major:

```kotlin
implementation("com.nrova:nrova-android:1.+") // any 1.x release
```

## SPM ↔ Gradle: what to know

Coming from the Swift side? Here's a quick translation table:

| Concept                  | Swift Package Manager                                         | Gradle / Maven                                                                         |
| ------------------------ | ------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| Package descriptor       | `Package.swift`                                               | `settings.gradle.kts` + per-module `build.gradle.kts`                                  |
| Library declaration      | `.library(name:, targets:)`                                   | `plugins { id("com.android.library") }`                                                |
| Dependency coordinate    | `https://github.com/Foo/Bar.git`                              | `group:artifact:version` (Maven coordinates)                                           |
| Pinning                  | `from: "1.0.0"` (next-major)                                  | `1.+` (any minor), exact, or `[1.0.0, 2.0.0)` for next-major equivalent                 |
| Registry                 | GitHub (or any git host)                                      | Maven Central, Google Maven, GitHub Packages, JitPack, Artifactory, …                  |
| Resolution               | `Package.resolved` lockfile                                   | Gradle dependency cache; optional dependency locking (`gradle.lockfile`)               |
| Async/await              | `async throws`                                                | `suspend fun` (Kotlin coroutines) — propagates errors as ordinary `Exception` throws   |
| JSON                     | `Codable` (reflection)                                        | `@Serializable` (kotlinx-serialization, compile-time codegen)                          |
| Test runner              | XCTest                                                        | JUnit 4 + Robolectric (unit) / AndroidX Test (instrumentation)                         |
| Conditional source       | `#if canImport(FoundationNetworking)`                         | Source sets (`src/main`, `src/test`, `src/androidTest`) + variant filters              |
| Versioning               | Git tag → SPM resolves via tag                                | Git tag and explicit `VERSION_NAME` in `gradle.properties`                             |
| Publishing               | Push tag → automatically available                            | `./gradlew publishToMavenCentral` (and sign with GPG) → release on the Central Portal  |

Other things that surprise iOS developers:

- **You _must_ tag _and_ publish.** Unlike SPM, pushing a tag is not
  enough. The artifact has to be uploaded to a Maven repository, and on
  Central Portal it must additionally be signed and released.
- **Sources and KDoc ship as separate `-sources.jar` and
  `-javadoc.jar` artifacts.** The vanniktech plugin builds them
  automatically. Without them, IDEs won't show inline docs to your
  consumers.
- **R8 / ProGuard rules ship inside the AAR** via `consumer-rules.pro`,
  the analog of "headers" you'd expose from a Swift package. Keep those
  rules minimal but accurate — they apply to every consuming app.

## License

MIT. See [`LICENSE`](./LICENSE).
