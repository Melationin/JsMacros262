package xyz.wagyourtail.jsmacros.client.mixin.access;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevel(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderLevel,
            CameraRenderState cameraRenderState,
            Matrix4fc matrix4fc,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean bl,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci
    ) {
        try {
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

            PoseStack matrixStack = new PoseStack();

            for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                d.render(matrixStack, tickDelta);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}