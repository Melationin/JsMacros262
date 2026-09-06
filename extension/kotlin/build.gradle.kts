dependencies {
    implementation(libs.kotlin.compiler.embeddable)

    // deps to bundle into the extension jar's META-INF/jsmacrosdeps go into the
    // parent-declared `jsmacrosExtensionInclude` configuration. Because
    // `implementation extendsFrom(jsmacrosExtensionInclude)`, this also puts the
    // compiler on compile/runtime classpaths, so it is both usable and packaged.
    project.dependencies.add("jsmacrosExtensionInclude", libs.kotlin.compiler.embeddable)
}
