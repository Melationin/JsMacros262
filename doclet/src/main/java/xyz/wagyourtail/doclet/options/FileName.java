package xyz.wagyourtail.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.util.List;

/**
 * Base name of the generated file, defaults to "JsMacros" so addons can generate
 * their own header named e.g. "my-addon-1.0.0.d.ts" instead of clashing with the
 * main mod's "JsMacros-&lt;version&gt;.d.ts".
 */
public class FileName implements Doclet.Option {
    public static String fileName = "JsMacros";

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "base name of the output file (default: JsMacros)";
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-name", "--name");
    }

    @Override
    public String getParameters() {
        return "<name: String>";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        fileName = arguments.get(0);
        return true;
    }

}
