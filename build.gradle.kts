import java.text.SimpleDateFormat
import java.util.Date

val kotlinVersion = "1.9.0"
val kspVersion = "$kotlinVersion-1.0.13"
val composeBomVersion = "2025.06.00" // Aligned with libs.versions.toml
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.firebase-perf")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.openapi.generator") version "7.5.0"
    id("com.google.devtools.ksp") version kspVersion
}

// Task to fix Kotlin visibility for LSPosed compatibility
tasks.register<Exec>("fixKotlinVisibility") {
    group = "build"
    description = "Fixes Kotlin visibility issues for LSPosed compatibility"
    
    // Only run if the script exists
    onlyIf { file("${rootProject.projectDir}/fix-visibility.ps1").exists() }
    
    // Command to run the PowerShell script
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "${rootProject.projectDir}/fix-visibility.ps1")
    
    // Set working directory to project root
    workingDir(rootProject.projectDir)
    
    // Log the command being executed
    doFirst {
        logger.lifecycle("Running visibility fixer: ${commandLine}")
    }
    
    // Log completion
    doLast {
        logger.lifecycle("Visibility fixer completed successfully")
    }
}

// Run the visibility fixer before preBuild
tasks.named("preBuild") {
    // dependsOn("fixVisibility")
}

// Add a task to validate build configuration
tasks.register("validateBuildConfig") {
    group = "verification"
    description = "Validates build configuration for compatibility"
    
    doLast {
        println("=== AuraFrameFx Build Configuration Validation ===")
        println("Kotlin Version: $kotlinVersion")
        println("Compose BOM Version: $composeBomVersion")
        println("Hilt Version: $hiltVersion")
        println("Firebase BOM Version: $firebaseBomVersion")
        println("Target SDK: ${android.defaultConfig.targetSdk}")
        println("Compile SDK: ${android.compileSdk}")
        println("Java Version: ${android.compileOptions.sourceCompatibility}")
        
        // Check for critical files
        val xposedApiJar = file("Libs/api-82.jar")
        val visibilityScript = file("${rootProject.projectDir}/fix-kotlin-visibility.sh")
        
        if (!xposedApiJar.exists()) {
            logger.warn("WARNING: Xposed API JAR not found at ${xposedApiJar.absolutePath}")
        } else {
            println("✓ Xposed API JAR found")
        }
        
        if (!visibilityScript.exists()) {
            logger.warn("WARNING: Visibility fixing script not found at ${visibilityScript.absolutePath}")
        } else {
            println("✓ Visibility fixing script found")
        }
        
        println("=== Build Configuration Validation Complete ===")
    }
}

// Repositories are configured in settings.gradle.kts

// Common versions - Aligned with libs.versions.toml
val composeVersion = "1.6.7" // Keep for other Compose libs if needed
val composeCompilerExtensionVersion = composeVersion // Use composeVersion for compiler extension
val hiltVersion = "2.56.2"
val navigationVersion = "2.9.0"
val firebaseBomVersion = "33.15.0"
val lifecycleVersion = "2.9.1"
android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = 34
    ndkVersion = "28.1.13356709"
    // buildToolsVersion is no longer needed with AGP 8.10.1+
    
    defaultConfig {
        testInstrumentationRunnerArguments += mapOf()
        applicationId = "dev.aurakai.auraframefx"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable multidex support
        multiDexEnabled = true

        manifestPlaceholders["appAuthRedirectScheme"] = "auraframefx"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Configure Java toolchain
    kotlin {
        jvmToolchain(17)
    }

    // Configure source sets with explicit paths including OpenAPI generated sources
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs(
                "src/main/java",
                layout.buildDirectory.dir("generated/openapi/src/main/kotlin").get().asFile
            )
            res.srcDirs("src/main/res")
            aidl.srcDirs("src/main/aidl")
            assets.srcDirs("src/main/assets")
            resources.srcDirs("src/main/resources")
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    // Fix for Windows path handling in AIDL compilation
    tasks.withType<com.android.build.gradle.tasks.AidlCompile> {
        // Configure AIDL compilation to avoid path issues
        outputs.upToDateWhen { false } // Force the task to always run
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerExtensionVersion
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Configure sourceSets to exclude duplicate generated models
    // Configure to exclude duplicate model files - using manual approach instead
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            // Add the path to exclude in freeCompilerArgs
            freeCompilerArgs.addAll(
                listOf(
                    // Changed from strict to warning to reduce build noise
                    // Can be set back to "strict" once all visibility modifiers are properly set
                    "-Xexplicit-api=warning"
                    // Removed "-Xno-source=/dev/aurakai/auraframefx/generated/model"
                )
            )
        }
    }
}

dependencies {
    // Add Guava for utility functions
    implementation("com.google.guava:guava:33.2.1-android")
    
    // OpenAPI Client Dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") // Scalars converter for OpenAPI/Retrofit

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Security (MasterKeys)
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // AppAuth (OAuth utilities)
    implementation("net.openid:appauth:0.11.1")
    // Apache Oltu (OAuth 2.0 client, for generated code compatibility)
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.2") {
        exclude(group = "org.apache.oltu.oauth2", module = "org.apache.oltu.oauth2.common")
    }

    // Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycleVersion")

    // Coroutines & Serialization & DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // Adjusted version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // Adjusted version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // Adjusted version
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // Added kotlinx-datetime

    // Reminder: Xposed/LSPosed might have compatibility issues with certain hot-swapping/Apply Changes features in Android Studio.
    // If you encounter issues during development, try disabling these or performing a full reinstall of the app.
    // Xposed Framework
    // compileOnly("de.robv.android.xposed:api:82")
    // compileOnly("de.robv.android.xposed:api:82:sources")
    // Using local JARs for Xposed API v82 as it's no longer reliably available
    // from standard public repositories (like jCenter/Bintray which are deprecated).
    compileOnly(files("Libs/api-82.jar"))
    compileOnly(files("Libs/api-82-sources.jar"))
    // LSPosed specific
    compileOnly("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    compileOnly("org.lsposed.hiddenapibypass:hiddenapibypass:6.1:sources")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBomVersion"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Exclude firebase-common if needed - Commenting out for now
    // configurations.all {
    // exclude(group = "com.google.firebase", module = "firebase-common")
    // }

    // Firebase ML Kit
    implementation("com.google.firebase:firebase-ml-modeldownloader-ktx")

    // ML Kit
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("com.google.mlkit:translate:17.0.3")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.4")
    // Remove any litert dependencies if present
    configurations.all {
        exclude(group = "com.google.ai.edge.litert")
    }

    // Accompanist for Compose utilities - Using consistent version from libs.versions.toml
    val accompanistVersion = "0.32.0" // Stable version from libs.versions.toml
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-flowlayout:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-webview:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-pager-indicators:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-placeholder-material:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-navigation-material:$accompanistVersion")

    // Room for local database
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Retrofit for network calls
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore for preferences (required by your code)
    implementation("androidx.datastore:datastore-preferences:1.1.1") // Updated version

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1") // Adjusted version
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    // Hilt testing
    kaptTest("com.google.dagger:hilt-android-compiler:$hiltVersion")
    testImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")

    // AndroidX Test
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.14.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:$hiltVersion")

    // Debug implementations
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Desugar JDK libs for Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

// KSP and KAPT configuration
kapt {
    correctErrorTypes = true // KAPT is in maintenance mode, use KSP where possible
}

// OpenAPI Generator configuration
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/../api-spec/aura-framefx-api.yaml".toString())
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.toString())
    apiPackage.set("dev.aurakai.auraframefx.api")
    invokerPackage.set("dev.aurakai.auraframefx.api.invoker")
    modelPackage.set("dev.aurakai.auraframefx.api.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "collectionType" to "list",
            "useCoroutines" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            "serializationLibrary" to "kotlinx_serialization",
            "library" to "jvm-retrofit2",
            "explicitApi" to "true"
        )
    )
}

// Configure Android source sets
android {
    // OpenAPI sources are now included in the main sourceSets configuration above
}

// Set up task dependencies
tasks.named("preBuild") {
    dependsOn("openApiGenerate")
    // Run visibility fixer after OpenAPI generation
    finalizedBy("fixKotlinVisibility")
}

// Ensure OpenAPI generation happens before compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn("openApiGenerate")
}

// Configure the visibility fixer to run after OpenAPI generation
tasks.named("fixKotlinVisibility") {
    mustRunAfter("openApiGenerate")
}

// Register a task to build a jar for Xposed/LSPosed modules after the Android plugin is configured
afterEvaluate {
    android.applicationVariants.all {
        val buildTypeName = buildType.name
        if (buildTypeName == "release" || buildTypeName == "debug") {
            val variant = this
            tasks.register(
                "buildXposedJar${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                Jar::class
            ) {
                archiveBaseName.set("app-xposed-${variant.name}")
                from(variant.javaCompileProvider.get().destinationDirectory)
                destinationDirectory.set(layout.buildDirectory.dir("libs"))
            }
        }
    }
}
