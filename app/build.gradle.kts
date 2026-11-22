plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.bechir.authentic"
 compileSdk = 35
 defaultConfig { applicationId = "com.bechir.authentic"; minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "0.3.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("com.google.mlkit:text-recognition:16.0.1") }