plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

// Keep existing task names; F-Droid selects the free source set with -Pfdroid=true.
val fdroidProperty = providers.gradleProperty("fdroid").orNull
require(fdroidProperty == null || fdroidProperty in listOf("true", "false")) { "fdroid must be true or false" }
val isFdroid = fdroidProperty == "true"
if (!isFdroid) apply(from = rootProject.file("google/dependencies.gradle.kts"))

val signingFields = mapOf(
    "ANDROID_KEYSTORE_PATH" to "android.injected.signing.store.file",
    "ANDROID_KEYSTORE_PASSWORD" to "android.injected.signing.store.password",
    "ANDROID_KEY_ALIAS" to "android.injected.signing.key.alias",
    "ANDROID_KEY_PASSWORD" to "android.injected.signing.key.password"
)
val environmentSigning = signingFields.keys.associateWith { providers.environmentVariable(it).orNull }
val releaseSigning = if (environmentSigning.values.all { !it.isNullOrBlank() }) environmentSigning
    else signingFields.mapValues { providers.gradleProperty(it.value).orNull }
val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }

android {
    namespace = "dev.notechat"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "dev.notechat"
        minSdk = 30
        targetSdk = 35
        versionCode = 11
        versionName = "1.5.3"
        testInstrumentationRunner = "dev.notechat.RuntimeChecks"
        ndk { abiFilters += if (providers.gradleProperty("emulator").orNull == "true") listOf("arm64-v8a", "x86_64") else listOf("arm64-v8a") }
    }
    sourceSets.getByName("main").java.srcDir(if (isFdroid) "../fdroid/java" else "../google/java")
    if (!isFdroid) sourceSets.getByName("main").assets.srcDir("../google/assets")
    // Preserve Maven native binaries so an installed NDK cannot change APK bytes.
    packaging { jniLibs.keepDebugSymbols += "**/*.so" }
    signingConfigs {
        if (hasReleaseSigning) create("production") {
            storeFile = file(releaseSigning.getValue("ANDROID_KEYSTORE_PATH")!!)
            storePassword = releaseSigning.getValue("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = releaseSigning.getValue("ANDROID_KEY_ALIAS")
            keyPassword = releaseSigning.getValue("ANDROID_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning && !isFdroid) signingConfig = signingConfigs.getByName("production")
            isMinifyEnabled = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2") }
    }
}

val verifyReleaseSigning by tasks.registering {
    doLast {
        check(hasReleaseSigning) { "Use Android Studio signing or provide: ${releaseSigning.keys.joinToString()}." }
        check(file(releaseSigning.getValue("ANDROID_KEYSTORE_PATH")!!).isFile) { "Release keystore does not exist." }
    }
}
// F-Droid signs its own builds; normal releases still require the existing key.
if (!isFdroid) tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSigning) }

if (isFdroid) {
    val verifyFdroidDependencies by tasks.registering {
        doLast {
            val forbidden = configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.filter {
                val group = it.moduleVersion.id.group
                group.startsWith("com.google.mlkit") || group.startsWith("com.google.android.gms") || group.startsWith("com.google.firebase")
            }
            check(forbidden.isEmpty()) { "Proprietary Google SDKs in F-Droid build: ${forbidden.map { it.moduleVersion.id }}" }
        }
    }
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyFdroidDependencies) }
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
