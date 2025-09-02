plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gilad.shabbas_clock_kt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gilad.shabbas_clock_kt"
        minSdk = 26
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
//        compose = true
        viewBinding = true  // אופציונלי אבל מומלץ
    }
}

dependencies {

// Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Layouts
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycle (למרות שלא משתמשים ישירות, טוב שיהיה)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // JSON handling
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines (עבור AlarmService)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Work Manager (אופציונלי - אם תרצה לשפר את הניהול של השעונים)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.ui.graphics)
    implementation(libs.material3)

    // Testing (אפשר להשאיר את אלה)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}