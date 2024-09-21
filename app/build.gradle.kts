plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.gpstracker"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.gpstracker"
        minSdk = 24
        targetSdk = 34
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
            // Отключение Conscrypt для релиза
            buildConfigField("String", "CONSCRYPT_PROVIDER_ENABLE", "\"false\"")
        }
        debug {
            isMinifyEnabled = false
            // Отключение Conscrypt для режима отладки
            buildConfigField("String", "CONSCRYPT_PROVIDER_ENABLE", "\"false\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Добавлено для jTDS
    implementation(libs.net.jtds)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
