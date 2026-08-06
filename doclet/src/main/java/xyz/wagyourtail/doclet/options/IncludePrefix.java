package xyz.wagyourtail.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.util.ArrayList;
import java.util.List;

/**
 * Package prefix(es) whose classes are expanded into the Packages tree in
 * -no-globals (addon) mode. Types outside these prefixes are rendered as
 * {@code /* qualified.name *&#47; any} instead of being expanded, keeping the
 * generated addon header small. Repeatable. When empty (default), everything
 * is expanded (backwards compatible).
 */
public class IncludePrefix implements Doclet.Option {
    public static final List<String> prefixes = new ArrayList<>();

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "package prefix to expand into the Packages tree in -no-globals mode (repeatable)";
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-include", "--include");
    }

    @Override
    public String getParameters() {
        return "<prefix: String>";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        prefixes.add(arguments.get(0));
        return true;
    }

    public static boolean isIncluded(String qualifiedName) {
        if (prefixes.isEmpty()) return true;
        for (String p : prefixes) {
            if (qualifiedName.equals(p) ||
                (qualifiedName.startsWith(p) && qualifiedName.charAt(p.length()) == '.')) {
                return true;
            }
        }
        return false;
    }

}
