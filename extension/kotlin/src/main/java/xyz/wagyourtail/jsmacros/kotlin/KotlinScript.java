package xyz.wagyourtail.jsmacros.kotlin;

/**
 * Entry point implemented by every generated Kotlin script class.
 * <p>
 * The wrapper class produced by {@link KotlinLanguageDefinition} implements this
 * interface; each trigger instantiates it once and calls {@link #run(KotlinScriptContext)}.
 */
public interface KotlinScript {

    void run(KotlinScriptContext ctx) throws Exception;

}
