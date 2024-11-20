package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;keyPressed(III)Z"))
    private boolean onKeyPressed(Screen instance, int keyCode, int scanCode, int modifiers) {
        ((IScreenInternal) instance).jsmacros_keyPressed(keyCode, scanCode, modifiers);
        return instance.keyPressed(keyCode, scanCode, modifiers);
    }

    @Redirect(method = "onChar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;charTyped(CI)Z"), require = 3)
    private boolean onCharTyped(Screen instance, char chr, int modifiers) {
        ((IScreenInternal) instance).jsmacros_charTyped(chr, modifiers);
        return instance.charTyped(chr, modifiers);
    }

}
