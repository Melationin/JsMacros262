package com.example.addon.mixin;

import com.example.addon.EventOpenContainerExample;
import com.example.addon.TemplateAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Example mixin: fires {@link EventOpenContainerExample} whenever a container screen
 * is opened. This is how addons hook into vanilla/MC events.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void jsmacrosTemplate$onOpenScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof AbstractContainerScreen<?> container && TemplateAddon.core != null) {
            new EventOpenContainerExample(TemplateAddon.core, container.getTitle().getString()).trigger();
        }
    }

}
