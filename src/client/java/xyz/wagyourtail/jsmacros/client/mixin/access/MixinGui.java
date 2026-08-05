package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IScreen;
import xyz.wagyourtail.jsmacros.client.api.classes.render.ScriptScreen;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventOpenContainer;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventOpenScreen;

import java.util.function.Consumer;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Shadow
    private Screen screen;

    @Shadow
    public abstract void setScreen(Screen screen);

    @Unique
    private Screen jsmacros$prevScreen;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"), method = "setScreen")
    public void onCloseScreen(Screen screen, CallbackInfo ci) {
        Consumer<IScreen> onClose = ((IScreen) this.screen).getOnClose();
        try {
            if (onClose != null) onClose.accept((IScreen) screen);
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD), method = "setScreen")
    public void onOpenScreen(Screen screen, CallbackInfo info) {
        if (this.screen != screen) {
            jsmacros$prevScreen = screen;
            new EventOpenScreen(screen).trigger();
        }
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void onRenderScreen(Screen instance, GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        instance.extractRenderStateWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        if (!(Minecraft.getInstance().gui.screen() instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Inject(at = @At("TAIL"), method = "setScreen")
    public void afterOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof AbstractContainerScreen<?>) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode.getPlayerMode().isCreative() && !(screen instanceof CreativeModeInventoryScreen)) {
                return;
            }
            EventOpenContainer event = new EventOpenContainer(((AbstractContainerScreen<?>) screen));
            event.trigger();
            if (event.isCanceled()) {
                this.setScreen(jsmacros$prevScreen);
            }
        }
        jsmacros$prevScreen = null;
    }
}
