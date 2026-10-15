package com.trialselector.client.mixin;

import com.trialselector.client.VaultTracker;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.client.render.block.entity.VaultBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.VaultBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VaultBlockEntityRenderer.class)
public class VaultBlockEntityRendererMixin {

	@Inject(method = "updateRenderState", at = @At("HEAD"))
	private void trialselector$onUpdateRenderState(
			VaultBlockEntity vaultBlockEntity,
			VaultBlockEntityRenderState vaultBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand,
			CallbackInfo ci
	) {
		VaultTracker.getInstance().onVaultBlockEntityRender(vaultBlockEntity);
	}
}
