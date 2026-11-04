plugins {
    id("com.android.library")
    id("maven-publish")
    signing
}

abstract class BuildBoko : Exec() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}

val buildBoko = tasks.register<BuildBoko>("buildBoko") {
    inputs.files(rootProject.file("Cargo.toml"), rootProject.file("Cargo.lock"), rootProject.file("Dockerfile.android"))
    inputs.dir(rootProject.file("native"))
    inputs.dir(project.file("jni"))
    inputs.dir(rootProject.file("scripts"))
    inputs.dir(rootProject.file("tests/fixtures"))
    outputDirectory.set(layout.buildDirectory.dir("generated/boko/jniLibs"))
    doFirst { delete(outputDirectory.get().asFile) }
    if (providers.gradleProperty("bokoBuildMode").getOrElse("docker") == "native") {
        commandLine("sh", rootProject.file("scripts/build-android.sh").absolutePath, outputDirectory.get().asFile.absolutePath)
    } else {
        commandLine(
            "docker", "build", "--platform", "linux/amd64",
            "-f", rootProject.file("Dockerfile.android").absolutePath,
            "--output", "type=local,dest=${outputDirectory.get().asFile.absolutePath}",
            rootProject.projectDir.absolutePath,
        )
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(buildBoko, BuildBoko::outputDirectory)
    }
}

android {
    namespace = "io.github.bilec.boko"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        named("androidTest") {
            assets.directories.add(rootProject.file("tests/fixtures").absolutePath)
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
