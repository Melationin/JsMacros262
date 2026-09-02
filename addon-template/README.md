# JsMacrosPlus Addon Template

Minimal example of a JsMacrosPlus **API extension** (addon) as an independent Fabric mod.
The addon only depends on `jsmacrosplus-api` at compile time.

## What this template shows

| File | Shows |
|---|---|
| `TemplateAddon.java` | The addon main class implementing `api.Extension`, registered via the `"jsmacrosplus"` fabric entrypoint |
| `ExampleNbtIoLibrary.java` | A script library (`@Library("NbtIo")`) wrapping Minecraft's NBT IO — available in scripts as `NbtIo` |
| `EventOpenContainerExample.java` | A custom event (`@Event("ExampleOpenContainer")`) scripts can listen to |
| `mixin/GuiMixin.java` | A mixin hooking vanilla `Gui.setScreen` to fire the custom event (MC event hooks) |

## Building

1. Build & install the JsMacrosPlus API to your local maven repository (once):

   ```
   cd ..   # JsMacrosPlus repo root
   ./gradlew :jsm-api:publishToMavenLocal
   ```

2. Build this addon:

   ```
   ./gradlew build
   ```

3. Copy `build/libs/jsmacrosplus-addon-template-1.0.0.jar` into your game's `mods/`
   folder, alongside the JsMacrosPlus mod (any 26.1.2 build).

## Usage in scripts

```js
// NBT IO library
const tag = NbtIo.readFile("data/my.nbt");   // relative to the macro folder
NbtIo.writeFile("data/out.nbt", tag);

// Custom event fired when a container screen opens
JsMacros.on("ExampleOpenContainer", (e) => {
    Chat.log("opened container: " + e.title);
});
```

## Writing your own addon

- The addon is a regular Fabric mod: `fabric.mod.json` declares the `"jsmacrosplus"`
  entrypoint pointing at your `Extension` implementation.
- In `init(Core)` you can:
  - `runner.addLibrary(...)` — register script libraries
  - `runner.addEvent(...)` — register events
  - `runner.registerHelper(...)` — wrap objects for scripts
  - `runner.getConfig().addOptions(...)` — config classes
- Mixins are supported, so addons can hook vanilla MC events.
- Compile-time dependency: only `xyz.wagyourtail.jsmacros:jsmacrosplus-api`.
- Optional: generate TypeScript API docs for your `@Library`/`@Event` classes with
  `./gradlew genTSDoc` (needs `./gradlew :doclet:publishToMavenLocal` in the repo root once).
