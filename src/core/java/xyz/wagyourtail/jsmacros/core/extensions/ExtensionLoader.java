package xyz.wagyourtail.jsmacros.core.extensions;

import xyz.wagyourtail.jsmacros.api.LibraryExtension;

import xyz.wagyourtail.jsmacros.api.Extension;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.Pair;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtensionLoader {
    private final Set<Extension> extensions = new HashSet<>();
    private final Set<LanguageExtension> languageExtensions = new HashSet<>();
    private final Set<LibraryExtension> libraryExtensions = new HashSet<>();

    private final Core<?, ?> core;

    private ExtensionClassLoader classLoader;

    private LanguageExtension highestPriorityExtension;

    private boolean loadingDone;

    private final Path extPath;

    public ExtensionLoader(Core<?, ?> core) {
        this.core = core;
        this.extPath = core.config.configFolder.toPath().resolve("Extensions");
    }

    public boolean isExtensionLoaded(String name) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().anyMatch(e -> e.getExtensionName().equals(name));
    }

    public boolean notLoaded() {
        return !loadingDone;
    }

    public LanguageExtension getHighestPriorityExtension() {
        if (notLoaded()) {
            loadExtensions();
        }
        if (highestPriorityExtension == null) {
            highestPriorityExtension = languageExtensions.stream().max(Comparator.comparingInt(LanguageExtension::getPriority)).orElse(null);
        }
        return highestPriorityExtension;
    }

    public Set<Extension> getAllExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions;
    }

    public Set<LanguageExtension> getAllLanguageExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions;
    }

    public Set<LibraryExtension> getAllLibraryExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return libraryExtensions;
    }

    public @Nullable LanguageExtension getExtensionForFile(File file) {
        if (notLoaded()) {
            loadExtensions();
        }
        List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensions = this.languageExtensions.stream().map(e -> new Pair<>(e.extensionMatch(file), e)).filter(p -> p.getT().isMatch()).collect(Collectors.toList());
        if (extensions.size() > 1) {
            List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensionsByName = extensions.stream().filter(p -> p.getT() == LanguageExtension.ExtMatch.MATCH_WITH_NAME).collect(Collectors.toList());
            if (!extensionsByName.isEmpty()) {
                extensionsByName.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
                return extensionsByName.get(0).getU();
            }
        }
        if (!extensions.isEmpty()) {
            extensions.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
            return extensions.get(0).getU();
        }
        return null;
    }

    public Extension getExtensionForName(String extName) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().filter(e -> e.getExtensionName().equals(extName)).findFirst().orElse(null);
    }

    public synchronized void loadExtensions() {
        if (classLoader != null) {
            System.err.println("Extensions already loaded");
            return;
        }
        if (!Files.exists(extPath)) {
            try {
                Files.createDirectories(extPath);
            } catch (Exception e) {
                throw new RuntimeException("Could not create LanguageExtensions directory", e);
            }
        }

        URL[] urls;
        try (Stream<Path> files = Files.list(extPath)) {
            urls = files.filter(Files::isRegularFile).map(e -> {
                try {
                    return e.toUri().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        classLoader = new ExtensionClassLoader(urls);

        // extract lib to dependencies folder
        Path dependenciesPath = extPath.resolve("tmp");
        try {
            Files.createDirectories(dependenciesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add internal extensions
        Set<URL> internalExtensions = getDependenciesInternal(ExtensionLoader.class, "jsmacros.extension.json");
        for (URL lib : internalExtensions) {
            System.out.println("Adding internal extension: " + lib);
            // copy resource to dependencies folder
            Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
            try (InputStream stream = lib.openStream()) {
                Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                System.out.println("Extracted dependency " + path);
                classLoader.addURL(path.toUri().toURL());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // load extensions
        extensions.addAll(ServiceLoader.load(Extension.class, classLoader)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toSet()));

        System.out.println("Loaded " + extensions.size() + " extensions");

        // load extension deps (both from the extension's jsmacros.ext.<name>.json and
        // from an explicit Extension#getDependencies override)
        for (Extension extension : extensions) {
            try {
                Set<URL> deps = new HashSet<>(getDependenciesInternal(extension.getClass(), "jsmacros.ext." + extension.getExtensionName() + ".json"));
                for (String dep : extension.getDependencies()) {
                    URL lib = extension.getClass().getResource("/" + dep);
                    if (lib != null) {
                        deps.add(lib);
                    }
                }
                if (deps.isEmpty()) {
                    System.out.println("No dependencies for extension: " + extension.getClass().getName());
                }
                for (URL lib : deps) {
                    // copy resource to dependencies folder
                    Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
                    try (InputStream stream = lib.openStream()) {
                        Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                        System.out.println("Extracted dependency " + path);
                        classLoader.addURL(path.toUri().toURL());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension dependencies for: " + extension.getExtensionName(), e);
            }
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        for (Extension extension : extensions) {
            try {
                extension.init(core);
                if (extension instanceof LibraryExtension libExt) {
                    libraryExtensions.add(libExt);
                    for (Class<? extends BaseLibrary> lib : libExt.getLibraries()) {
                        core.libraryRegistry.addLibrary(lib);
                    }
                }
                if (extension instanceof LanguageExtension langExt) {
                    languageExtensions.add(langExt);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension: " + extension.getExtensionName(), e);
            }
        }
        loadingDone = true;
    }

    public boolean isGuestObject(Object obj) {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions.stream().anyMatch(e -> e.isGuestObject(obj));
    }

    /**
     * Parses a {@code dependencies} JSON resource (jsmacros.extension.json or
     * jsmacros.ext.<name>.json) into a set of jar-internal URLs.
     */
    public static Set<URL> getDependenciesInternal(Class<?> clazz, String fname) {
        try (InputStream stream = clazz.getResourceAsStream("/" + fname)) {
            if (stream == null) {
                return new HashSet<>();
            }
            JsonElement json = new JsonParser().parse(new InputStreamReader(stream));
            JsonElement dependencies = json.getAsJsonObject().get("dependencies");
            if (dependencies == null) {
                return new HashSet<>();
            }
            String[] dependenciesArray;
            if (dependencies.isJsonPrimitive()) {
                String dependenciesString = dependencies.getAsString();
                if (dependenciesString.equals("${dependencies}")) {
                    return new HashSet<>();
                }
                dependenciesArray = dependenciesString.split(" ");
                for (int i = 0; i < dependenciesArray.length; i++) {
                    dependenciesArray[i] = "META-INF/jsmacrosdeps/" + dependenciesArray[i].trim();
                }
            } else if (dependencies.isJsonArray()) {
                dependenciesArray = new String[dependencies.getAsJsonArray().size()];
                for (int i = 0; i < dependenciesArray.length; i++) {
                    dependenciesArray[i] = dependencies.getAsJsonArray().get(i).getAsString();
                }
            } else {
                throw new RuntimeException("Invalid dependencies format");
            }
            Set<URL> dependenciesSet = new HashSet<>();
            for (String dependency : dependenciesArray) {
                URL resource = clazz.getResource("/" + dependency);
                if (resource != null) {
                    dependenciesSet.add(resource);
                } else {
                    System.err.println("[JsMacrosExtensionManager] Could not find dependency: " + dependency);
                }
            }
            return dependenciesSet;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
