plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    signing
}

val nativeLibraries = layout.buildDirectory.dir("generated/boko/jniLibs")
val buildBoko by tasks.registering(Exec::class) {
    inputs.files(rootProject.file("Cargo.toml"), rootProject.file("Cargo.lock"), rootProject.file("Dockerfile.android"))
    inputs.dir(rootProject.file("native"))
    inputs.dir(project.file("jni"))
    inputs.dir(rootProject.file("scripts"))
    inputs.dir(rootProject.file("tests/fixtures"))
    outputs.dir(nativeLibraries)
    doFirst { delete(nativeLibraries.get().asFile) }
    if (providers.gradleProperty("bokoBuildMode").getOrElse("docker") == "native") {
        commandLine("sh", rootProject.file("scripts/build-android.sh").absolutePath, nativeLibraries.get().asFile.absolutePath)
    } else {
        commandLine(
            "docker", "build", "--platform", "linux/amd64",
            "-f", rootProject.file("Dockerfile.android").absolutePath,
            "--output", "type=local,dest=${nativeLibraries.get().asFile.absolutePath}",
            rootProject.projectDir.absolutePath,
        )
    }
}

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("JniLibFolders")) dependsOn(buildBoko)
}

android {
    namespace = "io.github.bilec.boko"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets.getByName("main").jniLibs.srcDir(nativeLibraries)
    sourceSets.getByName("androidTest").assets.srcDir(rootProject.file("tests/fixtures"))
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "boko-android"
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("boko Android bindings")
                description.set("Kotlin and JNI bindings for the boko ebook conversion engine")
                url.set("https://github.com/bilec/boko-bindings")
                licenses {
                    license {
                        name.set("GNU General Public License, version 3 only")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/bilec/boko-bindings.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bilec/boko-bindings.git")
                    url.set("https://github.com/bilec/boko-bindings")
                }
                developers {
                    developer {
                        id.set("bilec")
                        name.set("bilec")
                        url.set("https://github.com/bilec")
                    }
                }
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey")
    val signingPassword = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications)
    }
}
