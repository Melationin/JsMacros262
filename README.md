# JsMacrosPlus 26.1.2

基于 [JsMacros](https://github.com/wagyourtail/JsMacros) 的 **Minecraft 26.1.2 (Fabric)** 移植分支（即 **JsMacrosPlus**），并在其上扩展了**客户端命令脚本**与**独立 API 扩展（Addon）**体系。

通过脚本与游戏深度交互：聊天、世界、实体、渲染、事件……脚本语言支持 **JavaScript / TypeScript**。

---

## 目录

- [功能特性](#功能特性)
- [环境要求](#环境要求)
- [安装](#安装)
- [快速上手](#快速上手)
- [命令脚本（/js）](#命令脚本js)
- [扩展系统（Addon）](#扩展系统addon)
- [脚本 API 速览](#脚本-api-速览)
- [开发者：构建](#开发者构建)
- [目录结构](#目录结构)
- [兼容性](#兼容性)
- [常见问题（FAQ）](#常见问题faq)

---

## 功能特性

### 脚本系统
| 语言 | 引擎 | 说明 |
|---|---|---|
| JavaScript | GraalJS（由 `js-backend-graaljs` Lib Mod 提供） | 默认语言，完整支持 |
| TypeScript | GraalJS 回退 | 无独立 TS 引擎时按 JS 执行（类型语法报错，JS 语法可用） |

### 核心能力
- **事件系统**：按键、聊天、世界、实体、容器、渲染等 70+ 内置事件 + 自定义事件
- **宏管理 GUI**（按 `K` 打开）：按键宏 / 事件宏 / 服务 / **命令脚本** / 设置
- **脚本编辑器**：内置编辑器、自动补全、语法高亮、外部编辑器支持
- **渲染**：2D HUD 覆盖层、3D 世界渲染（Gizmos / FrameGraph，26.1.2 渲染管线）
- **客户端命令**：脚本可注册/注销自定义客户端命令（`CommandBuilder`）
- **命令脚本**（本分支新增）：`/js <command> [args...]` 一键触发绑定脚本
- **扩展系统**（本分支新增）：独立 Addon jar 提供库类 / 事件 / helper / 配置，**只依赖 jsmacrosplus-api 编译**

---

## 环境要求

| 项目 | 要求 |
|---|---|
| Minecraft | 26.1.2（Fabric） |
| Fabric Loader | ≥ 0.15.0（推荐 0.19.3） |
| Java | **JDK 25 / 26**（普通 JDK 即可，如 Zulu 26；游戏与 Graal 脚本均已在 Zulu 26 验证） |
| 内存 | 建议 ≥ 4GB（含 Graal 引擎） |

> **关于 JVM 选择**：普通 JDK（无 JVMCI）下 Graal 引擎以**解释模式**运行（可正常使用，性能略低）；GraalVM JDK 25 下可启用 JIT。两者均可用，无崩溃。

---

## 安装

1. 下载 `jsmacrosplus-26.1.2-2.0.1-fabric.jar`
2. 下载 `js-backend-graaljs-0.1.0.jar`（共享 JS 后端）
3. 两者都放入游戏 `mods/` 文件夹
4. 启动游戏（需要 Java 25/26 运行时）

可选依赖（建议安装以获得最佳体验）：
- [ModMenu](https://modrinth.com/mod/modmenu)（`18.0.0-alpha.8`）
- [Sodium](https://modrinth.com/mod/sodium)（`mc26.1.2-0.9.0-beta.1`，已做兼容处理）

**Addon 扩展**（可选）：把第三方 `*-addon-*.jar` 放入 `mods/` 文件夹，随游戏加载（见[扩展系统](#扩展系统addon)）。

---

## 快速上手

### 第一个脚本

1. 游戏内按 `K` 打开 JsMacros 界面 → **按键** → `+` 添加宏 → 选择文件（如 `Macros/hello.js`）→ 设置触发键
2. 编辑脚本：

```js
// hello.js —— 按键触发时执行
Chat.log("Hello, JsMacros!");
```

3. 回游戏按触发键，聊天栏输出。

### 事件脚本

```js
// 事件宏：K → 事件 → + → 选事件 → 选文件
// 触发时整个文件执行，event 全局变量可用
if (event.key === "key.keyboard.o") {
    Chat.log("按下了 O");
}
```

或注册式监听（回调必须用 `JavaWrapper.methodToJava` 包装）：

```js
JsMacros.on("Key", JavaWrapper.methodToJava((e) => {
    if (e.key === "key.keyboard.o") {
        Chat.log("按下了 O");
    }
}));
```

> **注意**：`JsMacros.on()` 的回调参数是 `MethodWrapper`，JS 函数必须用 `JavaWrapper.methodToJava(fn)` 包装（详见 FAQ）。

### 运行脚本文件

```js
// 在任意脚本里运行其他脚本
FJsMacros.runScript("other.js");
```

---

## 命令脚本（/js）

**本分支新增**：通过 `/js` 客户端命令一键触发指定脚本。

### 命令格式

```
/js <command> [args...]
```

- `<command>`：命令名（GUI 中注册）
- `[args...]`：参数，按空白拆分后以**字符串数组**传入脚本（支持 Tab 补全命令名）

### 使用步骤

1. 按 `K` → **命令**（新标签页）→ `+` → 输入命令名 → 列表出现条目（文件默认为 `./`）
2. 点击文件按钮选择脚本文件
3. 脚本中通过 `event` 全局变量接收命令数据：

```js
// 绑定到命令名 foo 的脚本
if (event.command === "foo") {
    Chat.log("args: " + event.args.join(", "));   // event.args 是字符串数组
}
```

4. 游戏内输入 `/js foo hello world` → 脚本执行，输出 `args: hello, world`

### GUI 管理
- **命令名**：点击可重命名（字母/数字/`_`/`-`，不可重复）
- **文件**：点击选择脚本文件（相对宏文件夹或绝对路径）
- **删除**：`X` 按钮（确认后删除）
- 配置持久化于 `options.json`（`commandscripts.commands`）

---

## 扩展系统（Addon）

**本分支新增**：第三方可以开发**独立 Addon**，为脚本提供新的库类、事件、helper 和配置。Addon 是标准 Fabric mod，编译时**只依赖 `jsmacrosplus-api`**（独立构件，纯 JDK，无 MC/fabric 依赖）。

> 📘 **Addon 开发详细指南**：[docs/扩展开发指南.md](docs/扩展开发指南.md)（环境搭建、API 详解、事件/库/Helper/Config、Mixin、genTSDoc、发布 JitPack、FAQ）

### 架构

```
┌─────────────────────────────────────────────────────────┐
│  Addon (fabric mod)                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ 库类 @Library │  │ 事件 @Event  │  │ mixin (MC hook)│  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                  │           │
│  fabric.mod.json entrypoints: "jsmacrosplus" │           │
└─────────┼─────────────────┼──────────────────┼───────────┘
          ▼                 ▼                  ▼
    jsmacrosplus (主 mod)  ── FabricLoader.getEntrypointContainers("jsmacrosplus")
          │                 │                  │
          ▼                 ▼                  ▼
    Core.addLibrary   Core.addEvent    事件触发 → 脚本
```

### 发现方式（两种）

| 方式 | 位置 | 机制 |
|---|---|---|
| **Fabric mod（推荐）** | `mods/` 文件夹 | `fabric.mod.json` 声明 `"jsmacrosplus"` entrypoint，jsmacrosplus 启动时发现并初始化 |
| **传统扩展** | `config/jsMacros/Extensions/` | `META-INF/services/xyz.wagyourtail.jsmacros.api.Extension` 声明实现类 |

### Addon 能做什么

| 能力 | API |
|---|---|
| 注册脚本库 | `core.addLibrary(MyLib.class)`（`@Library("名字")` + `extends BaseLibrary`） |
| 注册事件 | `core.addEvent(MyEvent.class)`（`@Event("名字")` + `extends BaseEvent`） |
| 注册 helper | `core.registerHelper(Class, HelperClass)` |
| 注册配置 | `core.getConfig().addOptions("key", ConfigClass.class)` |
| 触发事件 | `new MyEvent(core, ...).trigger()`（从库方法或 mixin 中） |
| MC 事件 hook | **mixin**（Addon 是标准 fabric mod，自带 mixin 能力） |

### 快速开始（参考模板 `addon-template/`）

**1. 获取 jsmacrosplus-api**（仓库内构建并安装到本地 maven）：

```bash
cd JsMacros2622
./gradlew :jsm-api:publishToMavenLocal
```

**2. 创建 Addon 工程**（模板：`addon-template/`，依赖 `xyz.wagyourtail.jsmacros:jsmacrosplus-api:2.0.1`）：

```jsonc
// fabric.mod.json
{
  "id": "my-addon",
  "environment": "client",
  "entrypoints": { "jsmacrosplus": ["com.example.MyAddon"] },
  "depends": { "jsmacrosplus": ">=2.0.0", "minecraft": "26.1.2" }
}
```

```java
// 主类
public class MyAddon implements Extension {
    public static Core core;

    @Override
    public String getExtensionName() { return "my-addon"; }

    @Override
    public void init(Core runner) {
        core = runner;
        runner.addLibrary(MyLib.class);
        runner.addEvent(MyEvent.class);
    }
}
```

**3. 库类**：

```java
@Library("MyLib")   // 脚本里用 MyLib.xxx()
public class MyLib extends BaseLibrary {
    public MyLib(Core runner) { super(runner); }

    public String hello(String name) {
        return "Hello " + name;
    }
}
```

**4. 事件 + mixin 触发**（示例见 `addon-template/`：`EventOpenContainerExample` + `GuiMixin`）：

```java
@Event("MyEvent")
public class MyEvent extends BaseEvent {
    public final String data;
    public MyEvent(Core runner, String data) { super(runner); this.data = data; }
}
```

**5. 构建并安装**：`./gradlew build` → `build/libs/*.jar` 放入游戏 `mods/` 文件夹。

**6.（可选）生成 TS API 文档**：`./gradlew genTSDoc` → `build/typescript/headers/jsmacrosplus-addon-template-1.0.0.d.ts`，包含本 addon 的 `@Library` / `@Event` 声明（`-no-globals` 模式，可与主 mod 的 `JsMacros-*.d.ts` 合并使用）。

### 脚本中使用

```js
MyLib.hello("world");                              // 库方法
JsMacros.on("MyEvent", JavaWrapper.methodToJava((e) => {
    Chat.log(e.data);
}));                                              // 事件
```

> 模板 `addon-template/` 附带完整示例：NBT IO 库（`NbtIo.readFile/writeFile`）+ 打开容器事件（mixin 触发）。`./gradlew -p addon-template build` 构建。

### jsmacrosplus-api 构件

- 坐标：`xyz.wagyourtail.jsmacros:jsmacrosplus-api:2.0.1`
- 内容：`Extension` / `LibraryExtension` / `Core`（接口）/ `Config` / `BaseLibrary`+`@Library` / `BaseEvent`+`@Event` / `EventFilterer` / `BaseHelper`
- 零依赖（纯 JDK），无 MC/fabric 引用 → **无类名映射问题**，Addon 用 loom/unimined 构建 + remap 后与其他 mod 互操作安全

---

## 脚本 API 速览

| 全局对象 | 用途 | 常用成员 |
|---|---|---|
| `Chat` | 聊天 | `log(msg)`、`say(msg)`、`createCommandBuilder(name)` |
| `World` | 世界 | 区块、实体、时间、boss 栏、计分板 |
| `Player` | 玩家 | 移动、交互、物品、状态 |
| `Hud` | 渲染 | 2D 覆盖层、3D 元素（`Draw3D`） |
| `FJsMacros` | 核心 | `runScript(file)`、`on/once/off`、`openGui` |
| `JavaWrapper` | 桥接 | `methodToJava(fn)`（**回调必用**） |
| `NbtIo` | NBT 读写 | 由 addon 模板示例提供（`readFile/writeFile`） |
| `event` | 全局变量 | 事件脚本执行时注入的事件对象 |

---

## 开发者：构建

### 环境

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 运行 Gradle 构建（Gradle 8.14.4 不支持 JDK 25/26 作为守护 JVM） |
| JDK | 25/26 | 编译 toolchain（自动下载）与游戏运行 |
| Gradle | 8.14.4（wrapper） | 内置 |

### 构建命令

```bash
# 完整构建（产出发布 jar）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew build -x test

# 仅发布 jar（跳过文档等）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew fabricJar

# 发布 jsmacrosplus-api 到本地 maven（Addon 开发用）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew :jsm-api:publishToMavenLocal

# 发布 doclet（TS/Web 文档生成器）到本地 maven（Addon 的 genTSDoc 用）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew :doclet:publishToMavenLocal

# 开发运行（Zulu 26 JVM，自动加载 mods 文件夹与依赖）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew fabricRunClient

# 构建模板 addon
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew -p addon-template build

# 模板 addon：生成 TS API 声明（build/typescript/headers/*.d.ts）
JAVA_HOME="C:\Program Files\Java\jdk-21" ./gradlew -p addon-template genTSDoc
```

### 产物

| 文件 | 说明 |
|---|---|
| `build/libs/jsmacrosplus-26.1.2-2.0.1-fabric.jar` | 发布 jar（remap 后，放 mods 使用） |
| `build/libs/jsmacrosplus-26.1.2-2.0.1-fabric-dev.jar` | 开发 jar |
| `jsm-api/build/libs/jsmacrosplus-api-2.0.1.jar` | API 构件 |
| `doclet/build/libs/jsmacrosplus-doclet-2.0.1.jar` | TS/Web 文档生成 doclet |
| `addon-template/build/libs/jsmacrosplus-addon-template-1.0.0.jar` | 模板 Addon |
| `addon-template/build/typescript/headers/*.d.ts` | 模板 Addon 的 TS API 声明（`genTSDoc` 产出） |

---

## 目录结构

```
├── jsm-api/                  # ★ API 构件（纯 JDK，扩展编译只依赖它）
│   └── src/main/java/xyz/wagyourtail/jsmacros/api/
│       ├── Core.java         # 扩展视角的运行时接口
│       ├── Extension.java    # 扩展入口接口
│       ├── BaseLibrary.java / Library.java
│       ├── BaseEvent.java / Event.java / EventFilterer.java
│       └── BaseHelper.java / Config.java / LibraryExtension.java
├── src/
│   ├── core/                 # 核心（配置/事件/库/扩展加载器）
│   ├── client/               # 客户端（GUI/渲染/事件/脚本 API）
│   ├── fabric/               # Fabric 适配（命令注册、addon 发现）
│   └── main/                 # 共享（FJavaUtils 等内置库）
├── src/core/.../backend/     # 共享 JS 后端适配层（JsBackend -> LanguageExtension）
├── doclet/                   # TS/Web 文档生成 doclet（Addon 的 genTSDoc 复用）
├── addon-template/           # ★ Addon 开发模板（NBT IO + mixin 事件示例）
└── build.gradle.kts          # 构建脚本
```

### 关键代码位置

| 功能 | 位置 |
|---|---|
| `/js` 命令注册 | `src/fabric/.../client/commands/JsCommand.java` |
| 命令脚本执行 | `src/client/.../commands/CommandScriptManager.java` |
| 命令脚本 GUI | `src/client/.../gui/screens/CommandScriptsScreen.java` |
| Addon 发现 | `src/fabric/.../client/extensions/FabricExtensionLoader.java` |
| 扩展加载器 | `src/core/.../extensions/ExtensionLoader.java` |
| 事件 `Command` | `src/client/.../api/event/impl/EventCommand.java` |

---

## 兼容性

| 项目 | 状态 |
|---|---|
| Sodium | ✅ 正常（含专用兼容 mixin：`MixinSodiumVideoSettingsScreen`，修复 getWidth/getHeight 语义冲突） |
| ModMenu | ✅ 正常 |
| 普通 JDK（Zulu/微软等） | ✅ Graal 解释模式运行 |
| GraalVM JDK 25 | ✅ 可 JIT（graal-sdk 需与 JDK 匹配，见 FAQ） |
| 其他 mod | 常规 fabric mod，无冲突；Addon 可安全引用其他 mod 类（loom/unimined remap） |

### 已知说明
- **3D 渲染**（Draw3D）基于 26.1.2 的 FrameGraph + Gizmos 管线；与 Sodium 共存已验证启动/渲染无异常，实际效果建议实测

---

## 常见问题（FAQ）

### 1. `JsMacros.on("X", fn)` 报 `no applicable overload found`
回调参数是 `MethodWrapper`（含多个抽象方法），JS 函数不能直接传入。**必须包装**：
```js
JsMacros.on("X", JavaWrapper.methodToJava((e) => { ... }));
```

### 2. 事件脚本不触发
- 事件宏（GUI 添加的「事件 + 文件」）触发时**整个文件执行**，用 `event` 全局变量取数据，不需要 `JsMacros.on`
- 服务（Services）只启动时执行一次，不是事件触发
- 确认事件名与内置/addon 事件一致（事件列表在 `K → 事件 → +` 中查看）

### 3. 游戏启动报 `NoClassDefFoundError: org/graalvm/polyglot/Engine`
JS 后端由独立 Lib Mod `js-backend-graaljs` 提供。请确认 `mods/` 中同时安装了 JsMacrosPlus 和 `js-backend-graaljs-*.jar`。

### 4. 日志里的 `401 /player/certificates` 是什么？
开发环境（runClient）离线账号的正常噪音，不影响单机功能。

### 5. GraalVM JDK 与普通 JDK 的区别
- 普通 JDK（无 JVMCI）：Graal 引擎**解释模式**运行 —— 可用，性能略低
- GraalVM JDK 25：可 JIT —— 但 graal-sdk 必须与 JDK 版本匹配（本分支使用 graal 24.0.1，配套普通 JDK 或 GraalVM 21/22）
- **推荐**：普通 JDK 25/26（Zulu 26 已验证）

### 6. 如何给脚本加 API？
开发 Addon（见[扩展系统](#扩展系统addon)），编译只依赖 `jsmacrosplus-api`。

### 7. Addon 里能用 mixin 吗？
可以。Addon 是标准 Fabric mod，声明 `mixins` 即可；jsmacrosplus 启动时通过 `"jsmacrosplus"` entrypoint 发现并初始化。

### 8. 构建报错 `generateWebDoc` 失败
文档任务与代码无关，构建发布 jar 用 `./gradlew fabricJar` 或 `build -x test -x generateWebDoc`。

---

## License

本项目基于 [MPL-2.0](https://www.mozilla.org/en-US/MPL/2.0/) 许可（继承自上游 JsMacros）。

上游项目：[wagyourtail/JsMacros](https://github.com/wagyourtail/JsMacros) · [文档](https://jsmacros.wagyourtail.xyz)
