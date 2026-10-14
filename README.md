---
post_title: boko-bindings
author1: Martin Bilka
post_slug: boko-bindings
microsoft_alias: not-applicable
featured_image: ""
categories: []
tags: [rust, android, python]
ai_note: Created with AI assistance.
summary: Android and Python bindings over a shared boko conversion core.
post_date: 2026-10-14
---

## boko-bindings

Thin Android and Python bindings for [boko](https://github.com/zacharydenton/boko),
pinned to engine version 0.5.0. This project maintains bindings, not a fork of
the engine. The conversion wrapper was extracted from eConverter.

| Directory | Responsibility |
| --- | --- |
| `native/` | Shared conversion logic and format validation |
| `android/` | Kotlin API, JNI, and Maven AAR packaging |
| `python/` | PyO3 extension and PyPI wheel packaging |
| `tests/fixtures/` | Source-only EPUB fixture shared by all tests |

Input: EPUB, AZW3, MOBI, KFX. Output: EPUB, AZW3, KFX, Markdown (`.md`).
Use lowercase output extensions. Neither binding implements Calibre options,
DRM removal, UI, or backend selection.

## Android

Requires JDK 17+, Android SDK platform 35, and Docker. Rust, cargo-ndk, and the
NDK remain inside Docker. The AAR supports API 24+ on arm64-v8a, armeabi-v7a,
and x86_64, with 16 KB ELF page alignment.

```sh
./gradlew :android:publishReleasePublicationToStagingRepository
./gradlew :android:connectedDebugAndroidTest
```

Set `ANDROID_HOME` or a local `sdk.dir` when the SDK is not already configured.
Artifacts are staged under `build/maven/io/github/bilec/boko-android/0.1.0/`.
The coordinates are `io.github.bilec:boko-android:0.1.0`; this version has not
been published to a public Maven repository.

```kotlin
import io.github.bilec.boko.Boko

Boko.convert(inputFile.absolutePath, outputFile.absolutePath)
```

The call is synchronous and throws `IllegalStateException` on conversion errors.
Run it off the UI thread. Use a separate worker process when immediate
cancellation is needed; stopping a coroutine does not stop native conversion.

The app should retain file selection, conversion routing, temporary-file
ownership, and cancellation. It can consume the staged AAR for local integration
or pin a published version after this repository has a public release.

## Python

Build and test a Linux wheel without installing Rust on the host:

```sh
docker build -f Dockerfile.python --output type=local,dest=dist/python .
```

The wheel targets Python 3.11+ using the stable ABI. Docker produces a Linux
wheel for its build architecture, not a macOS wheel. CI is configured to build
Linux, macOS, and Windows wheels; those CI jobs must run before claiming the
platforms are verified.

```python
from pathlib import Path
from boko_python import convert

convert(Path("book.epub"), Path("book.kfx"))
```

The import is `boko_python`; the distribution name is `boko-python`.
`convert()` accepts strings or path-like objects, returns `None` on success,
releases the GIL during conversion, and raises `RuntimeError` on engine errors.
Input and output must be different paths. Existing output files may be replaced.

## Source Builds

For build infrastructure such as F-Droid, the same Android build script can run
without Docker when Rust 1.91.0, cargo-ndk 4.1.2, the three Android Rust targets,
and NDK r30 are supplied by the build environment:

```sh
export ANDROID_NDK_HOME=/path/to/android-ndk-r30
./gradlew -PbokoBuildMode=native :android:assembleRelease
```

This is a source-build entrypoint, not a validated F-Droid build recipe or an
approval guarantee. Native builds use the committed Cargo lockfile.

## Releases

Before a release, synchronize the binding version in `Cargo.toml`,
`build.gradle.kts`, and `python/pyproject.toml`. Verify both artifacts and all
target platforms. Maven Central and PyPI publication, credentials, signing,
and a remote Git repository are intentionally not configured yet.

The Android app consumes the external library and supports
`-PbokoBindingsDir=../boko-bindings` for local source substitution until Maven
publication is available. No desktop application behavior is changed by
creating these bindings.

## License

Bindings: GPL-3.0-only; see `LICENSE`. Upstream boko is GPL-3.0-or-later.
Both distributions include the binding license. Dependencies retain their
respective licenses and upstream attribution.