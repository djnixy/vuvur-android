plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ Add the Kotlin Compose plugin alias
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.vuvur"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vuvur"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Or match your project's version if different
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes.all {
        val buildType = this.name
        setProperty("archivesBaseName", "vuvur-v${defaultConfig.versionName}-$buildType")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3) // Main Material 3 library
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Using explicit versions from your file where aliases weren't shown or incorrect
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8") // Or use libs.androidx.compose.material.icons.extended.android if defined
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Or use libs.retrofit if version matches
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Or use libs.converter.gson if version matches
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Or use libs.okhttp.v4120 if preferred
    implementation("io.coil-kt:coil-compose:2.6.0") // Or use libs.coil.compose if version matches
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3") // Or use libs.androidx.lifecycle.viewmodel.compose if version matches
    implementation("androidx.navigation:navigation-compose:2.7.7") // Or use libs.androidx.navigation.compose if version matches
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation(libs.androidx.datastore.preferences)

    // Use the correct Accompanist SwipeRefresh alias from your TOML file
    implementation(libs.accompanist.swiperefresh)

    // Add the Coil GIF dependency explicitly
    implementation("io.coil-kt:coil-gif:2.6.0")
}