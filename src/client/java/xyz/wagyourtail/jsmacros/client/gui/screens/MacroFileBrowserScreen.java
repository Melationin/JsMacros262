package xyz.wagyourtail.jsmacros.client.gui.screens;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.MaLiLibIcons;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.gui.interfaces.IFileBrowserIconProvider;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.gui.IJsMacrosScreen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** File selector for macro scripts built on malilib's file browser widget. */
public final class MacroFileBrowserScreen extends GuiListBase<
    WidgetFileBrowserBase.DirectoryEntry,
    WidgetDirectoryEntry,
    MacroFileBrowserScreen.MacroFileBrowserWidget
> implements IJsMacrosScreen {
    private static final String BROWSER_CONTEXT = "jsmacros.macro_files";
    private static final DirectoryCache DIRECTORY_CACHE = new DirectoryCache();

    private final Path rootDirectory;
    private final Path initialDirectory;
    private final Consumer<File> selectAction;
    private final Consumer<File> editAction;

    private Path selectedPath;
    private ButtonGeneric selectButton;
    private ButtonGeneric editButton;
    private ButtonGeneric renameButton;
    private ButtonGeneric deleteButton;

    public MacroFileBrowserScreen(
        Screen parent,
        File directory,
        File selected,
        Consumer<File> selectAction,
        Consumer<File> editAction
    ) {
        super(10, 26);
        this.setParent(parent);
        this.title = StringUtils.translate("jsmacros.filename");
        this.useTitleHierarchy = false;
        this.selectAction = selectAction;
        this.editAction = editAction;

        Path macroRoot = normalize(JsMacrosClient.clientCore.config.macroFolder.toPath());
        Path requestedDirectory = directory != null ? normalize(directory.toPath()) : macroRoot;
        this.initialDirectory = ensureDirectory(requestedDirectory, macroRoot);
        this.rootDirectory = this.initialDirectory.startsWith(macroRoot)
            ? macroRoot
            : this.initialDirectory.getRoot() != null ? this.initialDirectory.getRoot() : this.initialDirectory;
        this.selectedPath = selected != null ? normalize(selected.toPath()) : null;
        DIRECTORY_CACHE.setCurrentDirectoryForContext(BROWSER_CONTEXT, this.initialDirectory);
    }

    @Override
    public void setJsMacrosParent(Screen parent) {
        this.setParent(parent);
    }

    @Override
    protected int getBrowserWidth() {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(60, this.height - 104);
    }

    @Override
    protected MacroFileBrowserWidget createListWidget(int listX, int listY) {
        return new MacroFileBrowserWidget(
            listX,
            listY,
            this.getBrowserWidth(),
            this.getBrowserHeight(),
            this.rootDirectory,
            this.initialDirectory,
            this::onSelectionChanged
        );
    }

    @Override
    public void initGui() {
        super.initGui();

        int firstRowY = this.height - 54;
        int secondRowY = this.height - 30;
        int x = 10;

        x += this.addActionButton(x, firstRowY, "jsmacros.back", () -> GuiBase.openGui(this.getParent())) + 4;
        x += this.addActionButton(x, firstRowY, "jsmacros.openfolder", this::openCurrentDirectory) + 4;
        this.addActionButton(x, firstRowY, "jsmacros.new", this::openCreateDialog);

        x = 10;
        this.renameButton = this.createActionButton(x, secondRowY, "jsmacros.rename", this::openRenameDialog);
        x += this.renameButton.getWidth() + 4;
        this.deleteButton = this.createActionButton(x, secondRowY, "selectWorld.delete", this::openDeleteDialog);
        x += this.deleteButton.getWidth() + 4;
        this.editButton = this.createActionButton(x, secondRowY, "selectWorld.edit", this::editSelectedFile);
        x += this.editButton.getWidth() + 4;
        this.selectButton = this.createActionButton(x, secondRowY, "jsmacros.select", this::selectFile);

        MacroFileBrowserWidget browser = this.getListWidget();
        if (browser != null && this.selectedPath != null) {
            browser.selectPath(this.selectedPath);
        }
        this.updateActionButtons();
    }

    private int addActionButton(int x, int y, String translationKey, Runnable action) {
        return this.createActionButton(x, y, translationKey, action).getWidth();
    }

    private ButtonGeneric createActionButton(int x, int y, String translationKey, Runnable action) {
        String label = StringUtils.translate(translationKey);
        ButtonGeneric button = new ButtonGeneric(x, y, this.getStringWidth(label) + 20, 20, label);
        this.addButton(button, (clickedButton, mouseButton) -> action.run());
        return button;
    }

    private void onSelectionChanged(WidgetFileBrowserBase.DirectoryEntry entry) {
        this.selectedPath = entry != null && entry.type() == WidgetFileBrowserBase.DirectoryEntryType.FILE
            ? normalize(entry.getFullPath())
            : null;
        this.updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasFile = this.selectedPath != null && Files.isRegularFile(this.selectedPath);
        if (this.selectButton != null) {
            this.selectButton.setEnabled(hasFile && this.selectAction != null);
        }
        if (this.editButton != null) {
            this.editButton.setEnabled(hasFile && this.editAction != null);
        }
        if (this.renameButton != null) {
            this.renameButton.setEnabled(hasFile);
        }
        if (this.deleteButton != null) {
            this.deleteButton.setEnabled(hasFile);
        }
    }

    private void selectFile() {
        if (this.selectedPath != null && this.selectAction != null && Files.isRegularFile(this.selectedPath)) {
            this.selectAction.accept(this.selectedPath.toFile());
            GuiBase.openGui(this.getParent());
        }
    }

    private void editSelectedFile() {
        if (this.selectedPath != null && this.editAction != null && Files.isRegularFile(this.selectedPath)) {
            this.editAction.accept(this.selectedPath.toFile());
        }
    }

    private void openCurrentDirectory() {
        MacroFileBrowserWidget browser = this.getListWidget();
        if (browser != null) {
            Util.getPlatform().openPath(browser.getCurrentDirectory());
        }
    }

    private void openCreateDialog() {
        GuiBase.openGui(new JsMacrosTextInputScreen(
            255,
            "jsmacros.filename",
            "",
            this,
            this::createFile
        ));
    }

    private boolean createFile(String requestedName) {
        MacroFileBrowserWidget browser = this.getListWidget();
        if (browser == null) {
            return false;
        }

        String fileName = validateFileName(requestedName);
        if (fileName == null) {
            return false;
        }

        File candidate = browser.getCurrentDirectory().resolve(fileName).toFile();
        if (JsMacrosClient.clientCore.extensions.getExtensionForFile(candidate) == null) {
            fileName += "." + JsMacrosClient.clientCore.extensions
                .getHighestPriorityExtension()
                .defaultFileExtension();
        }

        Path file = normalize(browser.getCurrentDirectory().resolve(fileName));
        try {
            if (!Files.exists(file)) {
                Files.createFile(file);
            } else if (!Files.isRegularFile(file)) {
                return false;
            }
        } catch (IOException e) {
            JsMacrosClient.LOGGER.error("Failed to create macro file {}", file, e);
            return false;
        }

        this.selectedPath = file;
        browser.refreshEntries();
        browser.selectPath(file);
        return true;
    }

    private void openRenameDialog() {
        if (this.selectedPath == null) {
            return;
        }
        GuiBase.openGui(new JsMacrosTextInputScreen(
            255,
            "jsmacros.filename",
            this.selectedPath.getFileName().toString(),
            this,
            this::renameSelectedFile
        ));
    }

    private boolean renameSelectedFile(String requestedName) {
        if (this.selectedPath == null || !Files.isRegularFile(this.selectedPath)) {
            return false;
        }

        String fileName = validateFileName(requestedName);
        if (fileName == null) {
            return false;
        }

        Path target = normalize(this.selectedPath.resolveSibling(fileName));
        if (target.equals(this.selectedPath)) {
            return true;
        }
        if (Files.exists(target)) {
            return false;
        }

        try {
            Files.move(this.selectedPath, target);
        } catch (IOException e) {
            JsMacrosClient.LOGGER.error("Failed to rename macro file {} to {}", this.selectedPath, target, e);
            return false;
        }

        this.selectedPath = target;
        MacroFileBrowserWidget browser = this.getListWidget();
        if (browser != null) {
            browser.refreshEntries();
            browser.selectPath(target);
        }
        return true;
    }

    private void openDeleteDialog() {
        if (this.selectedPath == null || !Files.isRegularFile(this.selectedPath)) {
            return;
        }

        GuiBase.openGui(new JsMacrosConfirmScreen(
            300,
            "jsmacros.confirmdeletefile",
            new IConfirmationListener() {
                @Override
                public boolean onActionConfirmed() {
                    return deleteSelectedFile();
                }

                @Override
                public boolean onActionCancelled() {
                    return true;
                }
            },
            this,
            "jsmacros.confirmdeletefile"
        ));
    }

    private boolean deleteSelectedFile() {
        if (this.selectedPath == null || !Files.isRegularFile(this.selectedPath)) {
            return false;
        }

        try {
            Files.delete(this.selectedPath);
        } catch (IOException e) {
            JsMacrosClient.LOGGER.error("Failed to delete macro file {}", this.selectedPath, e);
            return false;
        }

        this.selectedPath = null;
        MacroFileBrowserWidget browser = this.getListWidget();
        if (browser != null) {
            browser.refreshEntries();
        }
        this.updateActionButtons();
        return true;
    }

    private static String validateFileName(String requestedName) {
        String name = requestedName.trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return null;
        }

        try {
            Path path = Path.of(name);
            return path.getNameCount() == 1 && path.getFileName().toString().equals(name) ? name : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static Path ensureDirectory(Path requested, Path fallback) {
        if (Files.isDirectory(requested)) {
            return requested;
        }
        try {
            Files.createDirectories(requested);
            return requested;
        } catch (IOException e) {
            JsMacrosClient.LOGGER.warn("Unable to use macro directory {}, falling back to {}", requested, fallback, e);
            return fallback;
        }
    }

    static final class MacroFileBrowserWidget extends WidgetFileBrowserBase {
        private final Path rootDirectory;

        private MacroFileBrowserWidget(
            int x,
            int y,
            int width,
            int height,
            Path rootDirectory,
            Path initialDirectory,
            fi.dy.masa.malilib.gui.interfaces.ISelectionListener<DirectoryEntry> selectionListener
        ) {
            super(
                x,
                y,
                width,
                height,
                DIRECTORY_CACHE,
                BROWSER_CONTEXT,
                initialDirectory,
                selectionListener,
                FileBrowserIcons.INSTANCE
            );
            this.rootDirectory = rootDirectory;
            this.switchToDirectory(initialDirectory);
        }

        @Override
        protected Path getRootDirectory() {
            // WidgetFileBrowserBase queries this from its constructor, before this
            // subclass can assign rootDirectory.
            return this.rootDirectory != null ? this.rootDirectory : this.currentDirectory;
        }

        @Override
        protected FileFilter getFileFilter() {
            return new FileFilter();
        }

        private void selectPath(Path path) {
            Path normalized = normalize(path);
            for (int i = 0; i < this.listContents.size(); i++) {
                DirectoryEntry entry = this.listContents.get(i);
                if (normalize(entry.getFullPath()).equals(normalized)) {
                    this.onEntryClicked(entry, i);
                    return;
                }
            }
        }
    }

    private static final class DirectoryCache implements IDirectoryCache {
        private final Map<String, Path> directories = new ConcurrentHashMap<>();

        @Override
        public Path getCurrentDirectoryForContext(String context) {
            return this.directories.get(context);
        }

        @Override
        public void setCurrentDirectoryForContext(String context, Path dir) {
            this.directories.put(context, dir);
        }
    }

    private enum FileBrowserIcons implements IFileBrowserIconProvider {
        INSTANCE;

        @Override
        public IGuiIcon getIconRoot() {
            return MaLiLibIcons.ARROW_UP;
        }

        @Override
        public IGuiIcon getIconUp() {
            return MaLiLibIcons.ARROW_UP;
        }

        @Override
        public IGuiIcon getIconCreateDirectory() {
            return MaLiLibIcons.PLUS;
        }

        @Override
        public IGuiIcon getIconSearch() {
            return MaLiLibIcons.SEARCH;
        }

        @Override
        public IGuiIcon getIconDirectory() {
            return MaLiLibIcons.ARROW_DOWN;
        }

        @Override
        public IGuiIcon getIconForFile(Path file) {
            return null;
        }
    }
}
