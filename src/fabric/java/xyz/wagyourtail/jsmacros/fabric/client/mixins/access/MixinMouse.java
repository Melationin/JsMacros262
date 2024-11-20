package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;

@Mixin(Mouse.class)
public class MixinMouse {

    @Redirect(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"))
    private boolean onMouseClicked(Screen screen, double x, double y, int button) {
        ((IScreenInternal) screen).jsmacros_mouseClicked(x, y, button);
        return screen.mouseClicked(x, y, button);
    }

    @Redirect(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseReleased(DDI)Z"))
    private boolean onMouseReleased(Screen screen, double x, double y, int button) {
        ((IScreenInternal) screen).jsmacros_mouseReleased(x, y, button);
        return screen.mouseReleased(x, y, button);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseDragged(DDIDD)Z"))
    private boolean onMouseDragged(Screen screen, double x, double y, int activeButton, double dx, double dy) {
        ((IScreenInternal) screen).jsmacros_mouseDragged(x, y, activeButton, dx, dy);
        return screen.mouseDragged(x, y, activeButton, dx, dy);
    }

    @Redirect(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseScrolled(DDDD)Z"))
    private boolean onMouseScrolled(Screen instance, double x, double y, double horiz, double vert) {
        ((IScreenInternal) instance).jsmacros_mouseScrolled(x, y, horiz, vert);
        return instance.mouseScrolled(x, y, horiz, vert);
    }

}
