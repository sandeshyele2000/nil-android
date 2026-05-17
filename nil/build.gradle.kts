plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.sandesh.nil"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.sandeshyele2000",
        artifactId = "nil",
        version = "1.0.5"
    )

    pom {
        name.set("Nil")
        description.set("A composable Android library available via Maven Central.")
        inceptionYear.set("2025")
        url.set("https://github.com/sandeshyele2000/nil-android")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("sandeshyele2000")
                name.set("Sandesh Yele")
                url.set("https://github.com/sandeshyele2000")
            }
        }

        scm {
            url.set("https://github.com/sandeshyele2000/nil-android")
            connection.set("scm:git:git://github.com/sandeshyele2000/nil-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/sandeshyele2000/nil-android.git")
        }
    }
}
