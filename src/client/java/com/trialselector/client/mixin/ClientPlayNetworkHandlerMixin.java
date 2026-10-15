package com.trialselector.client.mixin;

import com.trialselector.client.VaultTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Inject(method = "onBlockEntityUpdate", at = @At("TAIL"))
	private void trialselector$onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
		VaultTracker.getInstance().onBlockEntityPacket(packet.getPos());
	}
}
