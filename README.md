# boko-bindings

Android and Python bindings for [boko](https://github.com/zacharydenton/boko) 0.5.0.

| Directory | Contents |
| --- | --- |
| `native/` | Shared conversion logic |
| `android/` | Kotlin API, JNI, AAR |
| `python/` | PyO3 extension, wheel |
| `tests/fixtures/` | EPUB fixture for all tests |

Input: EPUB, AZW3, MOBI, KFX. Output (lowercase extension): EPUB, AZW3, KFX, `.md`.
No Calibre options or DRM removal.

## Android

Requires JDK 17+, Android SDK 35 (`ANDROID_HOME` or `sdk.dir`), and Docker for the
Rust/NDK toolchain. AAR: API 24+, arm64-v8a, armeabi-v7a, x86_64, 16 KB pages.

```sh
./gradlew :android:publishReleasePublicationToMavenLocal
./gradlew :android:connectedDebugAndroidTest
```

Maven coordinates: `io.github.bilec:boko-android:<version>`.

```kotlin
import io.github.bilec.boko.Boko

Boko.convert(inputFile.absolutePath, outputFile.absolutePath)
```

Synchronous; throws `IllegalStateException` on failure. Run off the UI thread.
Cancelling a coroutine does not stop native conversion; use a separate process
if you need that.

## Python

Build and test a Linux wheel in Docker:

```sh
docker build -f Dockerfile.python --output type=local,dest=dist/python .
```

Python 3.11+ stable ABI, built for the Docker host architecture. macOS and Windows
wheels are configured in CI but unverified.

```python
from pathlib import Path
from boko_python import convert

convert(Path("book.epub"), Path("book.kfx"))
```

Package `boko-python`. `convert()` takes `str` or path-like, releases the GIL,
raises `RuntimeError` on failure, and may overwrite output. Input and output must differ.

## Source Builds

Without Docker (e.g. F-Droid), supply Rust 1.91.0, cargo-ndk 4.1.2, the three
Android Rust targets, and NDK r30:

```sh
export ANDROID_NDK_HOME=/path/to/android-ndk-r30
./gradlew -PbokoBuildMode=native :android:assembleRelease
```

Uses the committed Cargo lockfile. Not a validated F-Droid recipe.

## Releases

Keep the version in sync across `Cargo.toml`, `build.gradle.kts`, and
`python/pyproject.toml`.

`v*` tags publish the AAR to Maven Central. Required secrets: `OSSRH_USERNAME` and
`OSSRH_PASSWORD` (Central Portal user token), `ORG_GRADLE_PROJECT_signingInMemoryKey`
and `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` (armored GPG key).

The eConverter Android app accepts `-PbokoBindingsDir=../boko-bindings` to build against local source.

## License

Bindings: GPL-3.0-only (`LICENSE`, shipped in both artifacts). boko: GPL-3.0-or-later.
