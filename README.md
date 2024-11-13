# KCEF

**Kotlin equivalent of jcefmaven with a more modern setup and depending on JetBrains/jcef**

Visit the JCEF repo at [JetBrains/jcef](https://github.com/JetBrains/jcef) and the jcefmaven repo at [jcefmaven/jcefmaven](https://github.com/jcefmaven/jcefmaven/)

## Supports

Please take a look at [JetBrains/JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime/releases) for a full list of supported targets.

| OS | Arch |
|----|------|
|![Linux](https://cdn.jsdelivr.net/npm/simple-icons@v12/icons/linux.svg)    | amd64, aarch64 |
|![Windows](https://cdn.jsdelivr.net/npm/simple-icons@v12/icons/windows.svg)| amd64, aarch64 |
|![MacOS](https://cdn.jsdelivr.net/npm/simple-icons@v12/icons/macos.svg)    | amd64, aarch64 |

## Installation

### Repository

This library is published to [Maven Central](https://mvnrepository.com/artifact/dev.datlag/kcef).

```gradle
repositories {
    mavenCentral()
    maven("https://jogamp.org/deployment/maven")
}
```

### Version

The version depends on [JetBrains/JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime/releases) and [JetBrains/jcef](https://github.com/JetBrains/jcef).

Take a look at the [releases](https://github.com/DATL4G/KCEF/releases) for more details.

#### Kotlin DSL

```kotlin
dependencies {
    implementation("dev.datlag:kcef:$version")
    
    // or with version catalog
    implementation(libs.kcef)
}
```

#### Version catalog

```toml
[versions]
kcef = "2023.10.11.1" # put your wanted version here

[libraries]
kcef = { group = "dev.datlag", name = "kcef", version.ref = "kcef" }
```

## Usage

View Compose specific usage here [COMPOSE](COMPOSE.md)

### Initialize

It's recommended to initialize `KCEF` directly after starting the application.

This way users don't have to wait when the `CefBrowser` is used in another UI page.

<details open>
<summary>Kotlin</summary>

This is recommended to be called in a **Coroutine** with **IO** scope.

```kotlin
KCEF.init(
    builder = {
        progress {
            onDownloading {
                println("Download progress: $it%")
            }
        }
        release(true)
    }
)
```

</details>

<details>
<summary>Java</summary>

This is recommended to be called in a **IO** Thread.

```java
KCEF.initBlocking(
    new KCEFBuilder().progress(
        new KCEFBuilder.InitProgress.Builder().onDownloading(progress -> {
            System.out.println("Download progress: " + progress + "%");
        }).build()
    ).release(true),
    throwable -> {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    },
    () -> {
        System.out.println("Restart required");
    }
);
```

</details>

### Create client

<details open>
<summary>Kotlin</summary>

If you listen to the `onInitialized` progress in the `KCEF.init` method, you can get the client blocking on the **Main** Thread.

```kotlin
if (initialized) {
    val client = KCEF.newClientBlocking()
}
```

Otherwise, you should run this in a Coroutine which is not using the **Main** scope.

```kotlin
KCEF.newClient()
```

The above methods may throw a `CefException`, you can use the nullable equivalent instead.

```kotlin
if (initialized) {
    val client: KCEFClient? = KCEF.newClientOrNullBlocking { throwable ->
        throwable?.printStackTrace()
    }
}
```

```kotlin
/** Needs to be called in  a coroutine */
val client: KCEFClient? = KCEF.newClientOrNull { throwable ->
    throwable?.printStackTrace()
}
```

</details>

<details>
<summary>Java</summary>

If you listen to the `onInitialized` progress in the `KCEF.init` method, you can get the client blocking on the **Main** Thread.

```java
if (initialized) {
    KCEFClient client = KCEF.newClientBlocking();
}
```

Otherwise, you should run this in a new Thread.

```java
/** Run in a new Thread */
KCEFClient client = KCEF.newClientBlocking();
```

The above methods may throw a `CefException`, you can use the nullable equivalent instead.

```java
if (initialized) {
    KCEFClient client = KCEF.newClientOrNullBlocking(throwable -> {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    });
}
```

```java
/** Should be called in a new Thread */
KCEFClient client = KCEF.newClientOrNullBlocking(throwable -> {
    if (throwable != null) {
        throwable.printStackTrace();
    }
});
```

</details>

### Dispose

If the `CefClient` is no longer used, make sure to dispose it.

`client.dispose()`

If you no longer need any CEF instance, cleanup up using the `KCEF` class.

`KCEF.disposeBlocking()`

or, if you're not sure if the `KCEF.init` process is finished

`KCEF.dispose()`

## Flags

Some platforms require the addition of specific flags. To use on MacOSX, add the following JVM flags:

```
--add-opens java.desktop/sun.awt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED
```

For gradle project, you can configure it in the build.gradle.kts like that:

```kotlin
afterEvaluate {
    tasks.withType<JavaExec> {
        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
```

## ProGuard

If your application build type uses ProGuard, commonly used for release builds in Java and Kotlin applications, you have to add ProGuard rules for KCEF to work.

```
-keep class org.cef.** { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory
```

## Support the project

[![Github-sponsors](https://img.shields.io/badge/sponsor-30363D?style=for-the-badge&logo=GitHub-Sponsors&logoColor=#EA4AAA)](https://github.com/sponsors/DATL4G)
[![PayPal](https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white)](https://paypal.me/datlag)

### This is a non-profit project!

Sponsoring to this project means sponsoring to all my projects!
So the further text is not to be attributed to this project, but to all my apps and libraries.

Supporting this project helps to keep it up-to-date. You can donate if you want or contribute to the project as well.
This shows that the library is used by people, and it's worth to maintain.
