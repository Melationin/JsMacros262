# Kotlin 后端实施计划(JsMacrosPlus 26.2)

> 状态:已定稿待实施。目标仓库:`F:\java\JsMacros262`。
> 结论:架构上已存在完整的语言后端接缝(`LanguageExtension` / `BaseLanguage` / `BaseScriptContext` / `ExtensionLoader` / `extension/*` 自动接线),因此**核心代码零改动**,只需新增一个 `extension/kotlin/` 子项目 + 少量构建/GUI 微调即可。

---

## 1. 目标与范围

### 1.1 目标
为 JsMacrosPlus 引入 Kotlin 脚本语言支持:`.kt` / `.kts` 脚本可以像 JS 一样,通过**按键宏、事件宏、服务、命令脚本(`/js`)、`FJsMacros.runScript`、字符串 exec** 触发执行,访问与 JS 完全相同的全局库(`Chat` / `World` / `Player` / `Hud` / `FJsMacros` / `JavaWrapper` / `Time` / `FS` 等),以及 `event` / `context` / `file` 注入。

### 1.2 形态(已与用户确认)
- **仓库内 `extension/kotlin/` 子项目**(不做独立 Lib Mod,不改 js-backend)。
- 直接实现 `xyz.wagyourtail.jsmacros.api.Extension` 的 `LanguageExtension` 子接口。

### 1.3 本期不做(Non-goals)
- 独立 Lib Mod / 新 api 仓库(形态 A 已定)。
- Kotlin 专属 API 文档生成(doclet 目前只产出 TS/Web 声明);自动补全仅复用现有"方法名+库名"建议树。
- 跨脚本 import/包管理(先单文件,后续可在编译 classpath 里加入宏目录以支持同目录 import)。
- 运行时异常行号精确回映到用户源码(编译期错误会精确回映;运行期栈帧先指向生成的 wrapper 文件)。

---

## 2. 现状盘点(已核实的接缝)

| 接缝 | 位置 | 作用 |
|---|---|---|
| `LanguageExtension` | `src/core/java/xyz/wagyourtail/jsmacros/core/extensions/LanguageExtension.java` | `extensionMatch(File)` / `defaultFileExtension()` / `getLanguage(Core)` / `wrapException(Throwable)` / `isGuestObject(Object)` / `getPriority()` |
| `BaseLanguage<U,T>` | `src/core/java/xyz/wagyourtail/jsmacros/core/language/BaseLanguage.java:173-186` | 只需实现 `exec(EventContainer, ScriptTrigger, BaseEvent)`、`exec(..., String lang, String script, BaseEvent)`、`createContext(BaseEvent, File)`;`retrieveLibs(ctx)` 返回 `Map<String, BaseLibrary>` |
| `BaseScriptContext<T>` | `src/core/java/xyz/wagyourtail/jsmacros/core/language/BaseScriptContext.java` | `isMultiThreaded()` / `wrapSleep()` / `closeContext()` / 线程绑定 / `triggeringEvent` / `getFile()` |
| 扩展加载 | `src/core/java/xyz/wagyourtail/jsmacros/core/extensions/ExtensionLoader.java` | ServiceLoader 加载 `config/jsMacros/Extensions/*.jar` + 主 jar 内嵌扩展;`jsmacros.ext.<name>.json` 声明扩展自带依赖 jar;`ExtensionClassLoader` 的 parent 是游戏类加载器 |
| 语言路由 | `src/core/java/xyz/wagyourtail/jsmacros/core/Core.java:134-144` | `selectLanguageExtension(file, requestedBackend)`:显式 backend 名 → 文件扩展名匹配 → 最高优先级兜底 |
| 默认后端 | `src/core/java/xyz/wagyourtail/jsmacros/core/config/CoreConfigV2.java:28` | `defaultBackend = "auto"` |
| 构建接线 | `settings.gradle.kts:37-47`、`build.gradle.kts:106-116`、`extension/build.gradle.kts:25-84` | `extension/<name>/` 自动 include、自动 runtimeOnly、自动 java+shadow、自动打包依赖到 `META-INF/jsmacrosdeps` |
| 编辑器高亮 | `src/client/.../highlighting/Prism.java:53`、`EditorScreen.java:64,130-131` | Prism_kotlin 已打包;语言下拉已含 "kotlin";`.kts` 已自动映射 |

---

## 3. 总体设计

### 3.1 数据流
```
脚本触发 (按键/事件/服务/命令/runScript)
  → Core.exec / Core.runMacro → selectLanguageExtension(file, backend)
      → .kt/.kts 命中 KotlinLanguageExtension
  → KotlinLanguageDefinition.exec(...)
      → retrieveLibs(ctx) 得到 Map<String,BaseLibrary>
      → 生成 wrapper Kotlin 源码(顶层用户代码 + libs/event/context 注入)
      → 编译缓存命中? 否则 K2JVMCompiler 编译到 <configFolder>/kotlin/<id>/classes
      → 自定义 URLClassLoader(parent=扩展类加载器)加载 Script_<id>
      → 实例化 KotlinScript,注入 libs,调 run(ctx)
```

### 3.2 与 js-backend 的关系(不改 js-backend)
- js-backend 仍是独立 mod,经 Fabric entrypoint `"jsbackend"` → `registerJsBackend` 注册,与 Kotlin 后端**同处** `ExtensionLoader.languageExtensions` 集合,平级并行。
- 路由零冲突:`.js`/`.ts` 仅 JS 认领,`.kt`/`.kts` 仅 Kotlin 认领。
- **关键约束**:`KotlinLanguageExtension.getPriority()` 返回 `Integer.MIN_VALUE`,保证低于 JS 后端,保持"未知文件默认走 JS"的现状(见 `Core.java:143` 的 `getHighestPriorityExtension()` 兜底)。
- `wrapException` / `isGuestObject` 各自只认自己的对象,互不污染(JS 适配器对非 JS 异常已返回 null,见 `JsBackendLanguageExtension.java:72-76`)。
- 两者共享 `LibraryRegistry`,库注入行为一致。

---

## 4. 详细设计(逐文件)

新增目录:`extension/kotlin/`。

### 4.1 `extension/kotlin/build.gradle.kts`
```kotlin
dependencies {
    implementation(libs.kotlin.compiler.embeddable)
}
```
- 父 `extension/build.gradle.kts` 的 `subprojects` 块已自动:应用 `java` + `shadow`、设 archivesName、把 `implementation` 依赖打进 `META-INF/jsmacrosdeps`、展开 `jsmacros.ext.*.json`、配好 `test` 的 `-XX:-EnableJVMCI`。
- 无需 `repositories`(`settings.gradle.kts` 的 `dependencyResolutionManagement` 已覆盖 mavenCentral)。

### 4.2 `gradle/libs.versions.toml`(根,新增 2 行)
```toml
[versions]
kotlin = "2.2.20"            # 需支持在 JDK 25 上运行;遇兼容问题升最新 2.2.x
[libraries]
kotlin-compiler-embeddable = { module = "org.jetbrains.kotlin:kotlin-compiler-embeddable", version.ref = "kotlin" }
```

### 4.3 `extension/build.gradle.kts`(父,修 3 行——必要)
当前 `subprojects` 块把**主 mod 的全部实现依赖**(minecraft/fabric-api/sodium/malilib/prism4j/... 以及 `rootProject.sourceSets.main.output`)塞进每个扩展的 `implementation`,导致它们被一起打进 `META-INF/jsmacrosdeps`(该路径在无扩展子项目时从未被执行过)。改为:
```kotlin
dependencies {
    compileOnly(rootProject.sourceSets.main.get().output)              // 原 implementation
    for (dependency in rootProject.configurations.implementation.get().dependencies) {
        compileOnly(dependency)                                        // 原 implementation
        runtimeOnly(dependency)                                        // 新增:保留 dev/test 运行时类路径(game + jsm-api + js-backend-api),不打包
    }
    testImplementation(testFixtures(project(":extension")))
}
```
效果:扩展 jar 的 `META-INF/jsmacrosdeps` **只含扩展自己声明的实现依赖(kotlin 编译器及其传递依赖)**;dev/test 运行时不受影响(游戏依赖本来就在运行类路径上,且 `testFixtures(project(":extension"))` 已 `testFixturesApi` 透传 main output 与 `jsm-api`)。

### 4.4 `KotlinScript.java`(接口)
```java
package xyz.wagyourtail.jsmacros.kotlin;
public interface KotlinScript {
    void run(KotlinScriptContext ctx) throws Exception;
}
```

### 4.5 `KotlinScriptContext.java`
```java
package xyz.wagyourtail.jsmacros.kotlin;
public class KotlinScriptContext extends BaseScriptContext<Void> {
    private final Map<String, BaseLibrary> libs;
    public KotlinScriptContext(Core<?, ?> runner, BaseEvent event, File file, Map<String, BaseLibrary> libs) { ... }
    public BaseLibrary lib(String name) { return libs.get(name); }
    @Override public boolean isMultiThreaded() { return true; }   // 纯 JVM 字节码
    // wrapSleep 用基类默认(直接 sleep);closeContext 基类默认即可
}
```
- 注入点:库在 `exec` 内通过 `retrieveLibs(ctx)` 取得后 set;`event` 用基类 `triggeringEvent`。

### 4.6 `KotlinLanguageExtension.java`(实现 `LanguageExtension`)
- `getExtensionName()` → `"kotlin"`。
- `getPriority()` → `Integer.MIN_VALUE`(理由见 §3.2)。
- `extensionMatch(File)` → 文件名(小写)以 `.kt` 或 `.kts` 结尾返回 `MATCH`,否则 `NOT_MATCH`。
- `defaultFileExtension()` → `"kt"`。
- `getLanguage(Core)` → `new KotlinLanguageDefinition(this, runner)`(单例)。
- `wrapException(Throwable)` → 仅当 `t instanceof KotlinCompilationException` 时构造 `BaseWrappedException`(`message` = 首条诊断;`location` = `GuestLocation(file, 0, 0, line, column)` 或 `HostLocation("<file>:<line>:<col>")`);否则返回 `null`。
- `isGuestObject(Object)` → `false`(Kotlin 产物全是宿主对象)。
- `init(api.Core)` → 空(v1 不注册配置)。

### 4.7 `KotlinCompilationException.java`(内部 RuntimeException)
携带 `List<Diagnostic{File file; int line; int column; String message;}>` 与 `sourceName`。

### 4.8 `KotlinLanguageDefinition.java`(extends `BaseLanguage<Void, KotlinScriptContext>`,工作主体)
字段:`compileLock`、`Map<String, CompiledScript> cache`、`K2JVMCompiler compiler`(复用单例,编译串行)、`AtomicInteger compileCount`(测试用)。

`exec(ctx, macro, event)` / `exec(ctx, lang, script, event)` 都汇入:
```
source = 读文件 / 字符串
key    = 文件绝对路径 或 "<mem>:"+contentHash
CompiledScript cs = cache.get(key)
if (cs == null || cs.sourceFingerprint != fingerprint(source)) cs = compile(key, source, ctx)
ctx.getCtx().setLibs(retrieveLibs(ctx.getCtx()))
KotlinScript s = cs.cls.getDeclaredConstructor().newInstance()
s.run(ctx.getCtx())
```

#### 4.8.1 wrapper 源码生成
```
package ktmacros

import xyz.wagyourtail.jsmacros.kotlin.KotlinScript
import xyz.wagyourtail.jsmacros.kotlin.KotlinScriptContext

class Script_<id> : KotlinScript {
    override fun run(ctx: KotlinScriptContext) {
        val `Chat` = ctx.lib("Chat") as <Chat类的FQCN>
        ... (每个库一行;标识符一律反引号转义,防关键字冲突)
        val `event` = ctx.getTriggeringEvent() as <event.getClass().getName()>   // 仅 event != null 时生成
        val `context` = ctx
        val `file` = ctx.getFile()

// ---- 用户代码(逐字保留,不缩进改写,记录起始行 offset) ----
<user source>
    }
}
```
- `id` = 脚本文件基名(去非 `[A-Za-z0-9_]`)+ `-` + 内容 hash 前 8 位;源码变了 id 变 → 新类名 → 旧 loader/类整体被 GC,无泄漏。
- **行号回映**:编译时用"用户代码起始行之前的实际行数"作为 `offset`;诊断/栈帧行号 `- offset` 即用户源码行。
- 类 FQCN 的 `$`(内部类)需映射为 `.`(Kotlin 语法);现有库类均为顶层类,预计不会遇到,代码里做兜底替换。

#### 4.8.2 编译(串行,复用单例 K2JVMCompiler)
```java
synchronized (compileLock) {
    Path base  = runner.config.configFolder.toPath().resolve("kotlin").resolve(id);
    Path src   = base.resolve("Script_" + id + ".kt");
    Path out   = base.resolve("classes");
    Files.writeString(src, wrapper);
    CompilerConfiguration conf = new CompilerConfiguration();
    conf.put(CommonConfigurationKeys.MODULE_NAME, "ktmacros_" + id);
    conf.put(CommonConfigurationKeys.CLASSPATH, classpath());          // List<File>
    conf.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, out.toFile());
    MessageCollector collector = new MessageCollector();
    ExitCode code = compiler.exec(collector, Services.EMPTY, conf, src.toAbsolutePath().toString());
    if (code != ExitCode.OK) throw new KotlinCompilationException(collector.getErrors(), ...);
    // 加载 out/ktmacros/ 下所有 .class(含 Script_<id>$1.class 等 lambda 类)
    URLClassLoader loader = new URLClassLoader(new URL[]{ out.toUri().toURL() }, getClass().getClassLoader());
    Class<?> cls = Class.forName("ktmacros.Script_" + id, true, loader);
    cache.put(key, new CompiledScript(cls, loader, fingerprint, offset));
}
```

#### 4.8.3 编译 classpath 收集
```java
List<File> cp = new ArrayList<>();
for (ClassLoader cl = getClass().getClassLoader(); cl != null; cl = cl.getParent())
    if (cl instanceof URLClassLoader u) for (URL u2 : u.getURLs()) cp.add(new File(u2.toURI()));
// 兜底:System.getProperty("java.class.path") + 去重 + 过滤不存在
```
- 覆盖 prod(`ExtensionClassLoader` = `Extensions/tmp/*.jar` 里的 kotlin 依赖,parent = Knot 类加载器)与 dev(扩展类在 app classpath)。
- **风险点**:Fabric `KnotClassLoader` 是否 `URLClassLoader` 子类需实现时确认;否则走 `java.class.path` + `mods/*.jar` 兜底。

### 4.9 资源文件
- `extension/kotlin/src/main/resources/META-INF/services/xyz.wagyourtail.jsmacros.api.Extension` → 内容 `xyz.wagyourtail.jsmacros.kotlin.KotlinLanguageExtension`。
- `extension/kotlin/src/main/resources/jsmacros.ext.kotlin.json` → `{"dependencies": ["${dependencies}"]}`(数组形式,匹配父 build 的 `joinToString("\", \"")` 展开,见 `ExtensionLoader.getDependenciesInternal` 的数组分支)。

---

## 5. GUI 小改(约 4 行)

| 文件 | 位置 | 改动 |
|---|---|---|
| `src/client/java/.../gui/screens/EditorScreen.java` | `getDefaultLanguage()` 约 130 行 | 增加 `case "kt": return "kotlin";`(现仅映射了 `kts`) |
| `src/client/java/.../gui/editor/highlighting/impl/AutoCompleteSuggester.java` | 约 22-34 行 switch | 增加 `case "kotlin": this.language = ".kt"; this.method_separator = "."; break;` |

说明:`FileChooser` / `MacroFileBrowserScreen` 已通过 `getExtensionForFile` 自动识别 `.kt`/`.kts`,无需改;`.kts` 高亮已就绪。

---

## 6. 测试计划

测试目录 `extension/kotlin/src/test/java/xyz/wagyourtail/jsmacros/kotlin/`,复用 `extension/src/testFixtures` 的 `BaseTest`(其 `runTestScript` 走 `core.exec(getLang(), script, null, event, ...)`)。

- `getLang()` 返回 `"kotlin"`(经 `getLanguageExtensionForName("kotlin")` 命中)。
- 库注入:测试内定义 `@Library("TestLib")` 的纯 JDK 库并 `core.libraryRegistry.addLibrary(...)` 注册(若 `BaseTest.core` 不可见,则测试自行 `CoreInstanceCreator.createCore()` 或用内置纯 JDK 库 `FS`/`Time`)。
- 用例:
  1. 顶层代码执行(`val x = 1 + 1; TestLib.log("" + x)`,断言输出)。
  2. `event` 注入(脚本把 `event` 传给 `TestLib.received(event)`,断言同一实例)。
  3. 库方法调用返回(如 `TestLib.hello("k") == "hi k"`)。
  4. 编译错误行号回映(脚本第 N 行写非法语法,断言 `KotlinCompilationException` / `wrapException` 定位到 N)。
  5. 缓存命中(同一 source 连续 run 两次,断言 `compileCount == 1`)。
  6. 字符串 exec(`core.exec("kotlin", "TestLib.log(\"str\")", null, event, ...)`)。

---

## 7. 验收标准

1. `./gradlew :extension:kotlin:test` 全绿(§6 六条用例)。
2. `jar tf extension/kotlin/build/libs/*.jar` 确认 `META-INF/jsmacrosdeps/` 下**只有 kotlin 相关 jar**,无 minecraft/fabric/sodium 等游戏 jar。
3. 主构建 `./gradlew jar`(或 `build`)不回归;`./gradlew build` 在有 js-backend 时通过。
4. 游戏内手测清单(交付后由用户/本机 runClient 验证):
   - `.kt` 按键宏触发,`Chat.log` 生效;
   - 事件宏注入 `event`(如 Key 事件的 `event.key`);
   - `/js` 命令脚本绑定 `.kt` 文件;
   - 同 profile 下 `.js` 与 `.kt` 宏并存、互不影响;
   - 脚本语法错误时聊天栏报错定位到正确行;
   - 同脚本二次触发无重编译(日志/耗时)。

---

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| K2 编译器在 JDK 25 上运行兼容性 | 选最新 2.2.x;在非模块化 classpath 下通常无需 `--add-opens`;如遇,记录 JVM 参数或升版本(实测确认) |
| `KnotClassLoader` 拿不到 URL | 父链 `URLClassLoader` 收集 + `java.class.path` + `mods/*.jar` 兜底 |
| 编译内存峰值(200-400MB/次) | 复用单例 `K2JVMCompiler` + `compileLock` 串行;缓存命中则零编译开销 |
| 类加载泄漏 | 每脚本独立 `URLClassLoader`,源码变更换新 `id`/新 loader,旧 loader 随缓存替换被 GC |
| 库名/标识符为 Kotlin 关键字或非法 | 生成绑定一律反引号转义 |
| 内部类 FQCN 的 `$` | 生成 cast 时 `$` → `.` 兜底替换 |
| 首次编译延迟 0.5–2s | 磁盘 + 内存双缓存,重复触发零开销;文档说明 |
| 扩展 jar 体积 ~55–65MB | 类加载隔离(Extensions/tmp 内嵌),不与其他 mod 冲突;文档说明 |

---

## 9. 实施步骤与工作量

| # | 任务 | 估算 |
|---|---|---|
| 1 | 修 `extension/build.gradle.kts`(根依赖 compileOnly+runtimeOnly) | 5 行 |
| 2 | `gradle/libs.versions.toml` 加 kotlin-compiler-embeddable | 2 行 |
| 3 | `extension/kotlin/build.gradle.kts` + services + `jsmacros.ext.kotlin.json` | ~10 行 |
| 4 | `KotlinScript` + `KotlinScriptContext` | ~60 行 |
| 5 | `KotlinLanguageExtension` + `KotlinCompilationException` | ~110 行 |
| 6 | `KotlinLanguageDefinition`(wrapper/K2/缓存/加载/exec×2) | ~300 行 |
| 7 | GUI 小改 | 4 行 |
| 8 | 单元测试 | ~150 行 |
| 9 | 构建 + 测试验证 + jar 依赖清单检查 | — |
| 10 | README/文档更新 | ~40 行 |

合计:**新增约 630 行 + 修改约 11 行**,核心(非扩展)源码改动仅限于 GUI 2 处 4 行与父构建 3 行。

---

## 10. 交付物清单

1. `Plan.md`(本文件)。
2. `extension/kotlin/` 全部源码与资源。
3. 修改:`extension/build.gradle.kts`、`gradle/libs.versions.toml`、`EditorScreen.java`、`AutoCompleteSuggester.java`。
4. `README.md` 增补(Kotlin 语言行 + 使用/分发说明)。
5. 验证证据:单测结果 + 扩展 jar 依赖清单。
