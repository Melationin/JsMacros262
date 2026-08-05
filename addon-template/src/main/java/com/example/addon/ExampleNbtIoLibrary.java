package com.example.addon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import xyz.wagyourtail.jsmacros.api.BaseLibrary;
import xyz.wagyourtail.jsmacros.api.Core;
import xyz.wagyourtail.jsmacros.api.Library;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Example script library: read/write compressed NBT files.
 * <p>
 * In scripts this is available as {@code NbtIo}, e.g.:
 * <pre>
 * const tag = NbtIo.readFile("data/file.nbt");
 * NbtIo.writeFile("data/out.nbt", tag);
 * </pre>
 * CompoundTags returned to scripts are automatically wrapped by JsMacros' existing
 * NBT helpers, so methods like {@code tag.get("key")} work directly.
 */
@Library("NbtIo")
public class ExampleNbtIoLibrary extends BaseLibrary {

    public ExampleNbtIoLibrary(Core runner) {
        super(runner);
    }

    /**
     * Reads a compressed .nbt file, relative to the macro folder or absolute.
     */
    public CompoundTag readFile(String path) throws IOException {
        return NbtIo.readCompressed(resolve(path), NbtAccounter.unlimitedHeap());
    }

    /**
     * Writes a compound tag to a compressed .nbt file, relative to the macro folder
     * or absolute.
     */
    public void writeFile(String path, CompoundTag tag) throws IOException {
        NbtIo.writeCompressed(tag, resolve(path));
    }

    private Path resolve(String path) {
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(runner.getMacroFolder(), path);
        }
        return f.toPath();
    }

}
