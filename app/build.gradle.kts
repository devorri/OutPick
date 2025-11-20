plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Firebase / Google Services plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.outpick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.outpick"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // ✅ Java + Kotlin compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // ✅ Enable ViewBinding (recommended for your UI-heavy app)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --------------------------------
    // ✅ Core Android Dependencies
    // --------------------------------

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // --------------------------------
    // ✅ Firebase (Managed via BoM)
    // --------------------------------
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // --------------------------------
    // ✅ Image Handling & UI
    // --------------------------------
    // Glide for loading images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.squareup.picasso:picasso:2.71828")

    // ✅ UCrop for image cropping (GitHub JitPack)
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Flexbox for responsive layouts
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // --------------------------------
    // ✅ JSON & Networking
    // --------------------------------
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // --------------------------------
    // ✅ Testing Dependencies
    // --------------------------------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
