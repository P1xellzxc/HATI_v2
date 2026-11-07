import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hativ2.web"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hativ2.web"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // A thin WebView shell: nothing worth shrinking, and R8 must not strip
            // the @JavascriptInterface bridge.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

/**
 * Copies the built web app (docs/app, output of web/tools/build.py) into this
 * variant's generated assets as assets/app/. sw.js is left out on purpose: the
 * assets are local, and a service worker cache could serve a stale app after
 * an APK update. The web app already ignores a failed registration.
 */
abstract class CopyWebApp : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val webApp: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(webApp)
            into(outputDir.dir("app"))
            exclude("sw.js")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val copy = tasks.register<CopyWebApp>("copy${name}WebApp") {
            webApp.set(rootProject.layout.projectDirectory.dir("docs/app"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(copy, CopyWebApp::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.webkit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}
