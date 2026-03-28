plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.chaquo.python") version "17.0.0" apply false
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}
