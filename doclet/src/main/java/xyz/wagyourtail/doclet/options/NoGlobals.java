package xyz.wagyourtail.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

/**
 * When set, skips the global declarations (declare const context/event/file,
 * interface EventFilterers, interface Events and the enum type aliases) so the
 * generated header only contains this project's own {@code @Library} /
 * {@code @Event} declarations and can be merged with the main mod's header
 * without duplicate identifiers. Used by addon projects.
 */
public class NoGlobals implements Doclet.Option {
    public static boolean noGlobals = false;

    @Override
    public int getArgumentCount() {
        return 0;
    }

    @Override
    public String getDescription() {
        return "skip global declarations, only emit this project's own libraries/events (for addon headers)";
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-no-globals", "--no-globals");
    }

    @Override
    public String getParameters() {
        return "";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        noGlobals = true;
        return true;
    }

}
