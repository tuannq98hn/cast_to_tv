plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.casttotv.screen.mirroring"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.casttotv.screenmirroring.smarttv.castvideo"
        minSdk = 24
        targetSdk = 35
        versionCode = 147402
        versionName = "14.7.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    // Grandfathers the lint errors inherited from the shipped APK's resources; new errors still fail the gate.
    // Regenerate only on purpose: ./gradlew :app:updateLintBaseline
    lint {
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":connectsdk"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("io.socket:socket.io-client:2.1.1")
    implementation("dev.mobile:dadb:1.2.9")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // UI libraries used by the original layouts
    implementation("com.airbnb.android:lottie:6.6.2")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation("com.jsibbold:zoomage:1.3.1")
    implementation("com.github.yuriy-budiyev:code-scanner:2.3.2")
    implementation("com.github.ome450901:SimpleRatingBar:1.5.1")
    implementation("com.github.siyamed:android-shape-imageview:0.9.3")

    testImplementation("junit:junit:4.13.2")
}

configurations.configureEach {
    exclude(group = "org.junit.platform")
    exclude(group = "org.junit.jupiter")
    exclude(group = "org.opentest4j")
    exclude(group = "org.apiguardian")
}
