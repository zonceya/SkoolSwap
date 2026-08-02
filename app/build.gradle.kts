plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "za.co.skoolswap"
    compileSdk = 36

    defaultConfig {
        applicationId = "za.co.skoolswap"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kapt {
        correctErrorTypes = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")

    }
    packagingOptions {
        // Fix INDEX.LIST conflict
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")

        // Fix Netty conflict
        exclude("META-INF/io.netty.versions.properties")

        // Prevent other common conflicts
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/AL2.0")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")

        // Use pickFirst for critical files
        pickFirst("META-INF/licenses/ASL-2.0.txt")
        pickFirst("META-INF/licenses/LICENSE-2.0.txt")
    }
    buildFeatures {
      viewBinding = true
      dataBinding = true
      buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)
    implementation(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.crashlytics)
    // Authentication
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image Loading
    implementation(libs.glide)
    implementation(libs.androidx.viewpager2)
    implementation(libs.facebook.shimmer)
    implementation(libs.subsampling.scale.image.view)
    implementation(libs.lottie)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.hilt.work)
     implementation(libs.play.services.maps3d)
    kapt(libs.androidx.hilt.compiler)
    kapt(libs.room.compiler)
    implementation(libs.kotlinx.datetime)
    implementation(libs.play.services.cast.framework)

    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.fragment)
    implementation(libs.androidx.hilt.work)
    kapt(libs.hilt.compiler)


    // Work Manager
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}