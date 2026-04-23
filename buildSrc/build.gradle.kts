plugins {
    `kotlin-dsl`
}

dependencies {
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(plugin(libs.plugins.vanniktechMavenPublish))
    implementation(plugin(libs.plugins.dema.kotlinLibrary))
}

internal fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
