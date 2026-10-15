package com.trialselector.client;

import com.trialselector.TrialSelectorMod;
import com.trialselector.config.TrialSelectorConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.VaultBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.enums.VaultState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class VaultTracker {
	private static final VaultTracker INSTANCE = new VaultTracker();

	private BlockPos targetVaultPos = null;
	private BlockPos lockedVaultPos = null;
	private BlockHitResult targetHit = null;

	private boolean isVaultOminous = false;
	private VaultState vaultState = VaultState.INACTIVE;
	private ItemStack currentDisplayItem = ItemStack.EMPTY;
	private boolean targetMatched = false;
	private Item requiredKey = Items.TRIAL_KEY;
	private boolean hasKeyInInventory = false;
	private boolean isKeyHeld = false;
	private double distance = 0.0;

	// Frame-accurate cycle state
	private boolean clickedForCurrentItem = false;
	private long lastClickTimeMs = 0;
	private int retryClicksSent = 0;
	private int debounceCooldown = 0;

	public static VaultTracker getInstance() {
		return INSTANCE;
	}

	/**
	 * Called during block entity render state generation for this vault in the current frame.
	 * Guarantees zero-delay detection on the exact frame the item appears.
	 */
	public void onVaultBlockEntityRender(VaultBlockEntity vaultEntity) {
		if (vaultEntity == null) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) return;

		BlockPos pos = vaultEntity.getPos();
		// If we already have a target, only track that one; otherwise check if player is near
		if (targetVaultPos != null && !pos.equals(targetVaultPos)) {
			return;
		}

		updateDisplayItemAndCheckUnlock(vaultEntity, client);
	}

	/**
	 * Called immediately when a BlockEntityUpdateS2CPacket is processed on the client.
	 * Catches the new item display at the moment of packet receipt.
	 */
	public void onBlockEntityPacket(BlockPos pos) {
		if (pos == null) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) return;

		if (targetVaultPos != null && !pos.equals(targetVaultPos)) {
			return;
		}

		BlockEntity be = client.world.getBlockEntity(pos);
		if (be instanceof VaultBlockEntity vaultEntity) {
			updateDisplayItemAndCheckUnlock(vaultEntity, client);
		}
	}

	/**
	 * Called on every render frame from HudRenderCallback.
	 */
	public void onRenderFrame(MinecraftClient client) {
		if (client == null || client.player == null || client.world == null || targetVaultPos == null) {
			return;
		}

		BlockEntity be = client.world.getBlockEntity(targetVaultPos);
		if (be instanceof VaultBlockEntity vaultEntity) {
			updateDisplayItemAndCheckUnlock(vaultEntity, client);
		}
	}

	/**
	 * Evaluates the vault's display item, detects the exact frame it appears/changes,
	 * and triggers an instantaneous click without waiting for game ticks.
	 */
	public synchronized void updateDisplayItemAndCheckUnlock(VaultBlockEntity vaultEntity, MinecraftClient client) {
		if (vaultEntity == null || client.player == null || client.world == null) {
			return;
		}

		BlockPos pos = vaultEntity.getPos();
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;

		// Read block state
		BlockState state = vaultEntity.getCachedState();
		if (!(state.getBlock() instanceof VaultBlock)) {
			return;
		}

		this.isVaultOminous = state.get(VaultBlock.OMINOUS);
		this.vaultState = state.get(VaultBlock.VAULT_STATE);
		this.requiredKey = isVaultOminous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;

		// If vault successfully transitioned to unlocking/ejecting, mark cycle complete
		if (this.vaultState == VaultState.UNLOCKING || this.vaultState == VaultState.EJECTING) {
			this.clickedForCurrentItem = true;
			this.debounceCooldown = 30;
			return;
		}

		if (this.vaultState != VaultState.ACTIVE) {
			return;
		}

		// Ensure player is looking towards or within reach of this vault
		double reach = player.getBlockInteractionRange();
		Vec3d eyePos = player.getEyePos();
		double distSq = eyePos.squaredDistanceTo(Vec3d.ofCenter(pos));
		if (distSq > (reach + 0.6) * (reach + 0.6)) {
			return;
		}

		if (this.targetVaultPos == null) {
			this.targetVaultPos = pos;
		}

		this.distance = Math.sqrt(distSq);

		// Proactively keep key held in hand whenever near an active vault
		checkPlayerKeys(player);
		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		if (config.isAutoSwitchKey() && !isKeyHeld && hasKeyInInventory) {
			switchToKey(player);
		}

		// Get current display item
		ItemStack newDisplayItem = vaultEntity.getSharedData().getDisplayItem();
		boolean itemChanged = !ItemStack.areEqual(this.currentDisplayItem, newDisplayItem);

		if (itemChanged) {
			// A NEW ITEM HAS JUST APPEARED ON THIS EXACT FRAME!
			this.currentDisplayItem = newDisplayItem.isEmpty() ? ItemStack.EMPTY : newDisplayItem.copy();
			this.clickedForCurrentItem = false;
			this.retryClicksSent = 0;
			this.targetMatched = !currentDisplayItem.isEmpty() && config.isRewardSelected(currentDisplayItem);

			// TRIGGER CLICK AT THE EXACT FRAME THE ITEM APPEARS!
			if (this.targetMatched && config.isAutoUnlockEnabled() && !this.clickedForCurrentItem) {
				executeInstantClick(client, player, pos);
			}
		} else if (this.targetMatched && config.isAutoUnlockEnabled() && this.vaultState == VaultState.ACTIVE) {
			// Item did not change, but might have been missed or user just enabled auto-unlock
			if (!this.clickedForCurrentItem) {
				executeInstantClick(client, player, pos);
			} else if (this.retryClicksSent < 2 && (System.currentTimeMillis() - this.lastClickTimeMs > 160)) {
				// Failsafe backup retry if network packet was dropped or high ping
				executeInstantClick(client, player, pos);
			}
		}
	}

	/**
	 * Dispatches the interaction packet to the server instantly on this exact frame.
	 */
	private void executeInstantClick(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
		if (client.interactionManager == null || client.currentScreen != null) {
			return;
		}

		double reach = player.getBlockInteractionRange();
		Vec3d eyePos = player.getEyePos();
		Vec3d center = Vec3d.ofCenter(pos);
		if (eyePos.squaredDistanceTo(center) > (reach + 0.5) * (reach + 0.5)) {
			return;
		}

		TrialSelectorConfig config = TrialSelectorMod.getConfig();

		// Ensure key is held in hand
		checkPlayerKeys(player);
		if (!isKeyHeld && config.isAutoSwitchKey() && hasKeyInInventory) {
			switchToKey(player);
		}

		if (!isKeyHeld) {
			return;
		}

		Hand hand = player.getMainHandStack().isOf(requiredKey) ? Hand.MAIN_HAND : Hand.OFF_HAND;

		// Compute hit result
		BlockHitResult hit = this.targetHit;
		if (hit == null || !hit.getBlockPos().equals(pos)) {
			if (client.crosshairTarget instanceof BlockHitResult crossHit && crossHit.getBlockPos().equals(pos)) {
				hit = crossHit;
			} else {
				Direction facing = Direction.getFacing(eyePos.x - center.x, eyePos.y - center.y, eyePos.z - center.z);
				Vec3d hitPos = center.add(Vec3d.of(facing.getVector()).multiply(0.5));
				hit = new BlockHitResult(hitPos, facing, pos, false);
			}
		}

		// FIRE INTERACTION INSTANTLY ON THIS FRAME!
		ActionResult result = client.interactionManager.interactBlock(player, hand, hit);
		player.swingHand(hand);

		this.clickedForCurrentItem = true;
		this.lastClickTimeMs = System.currentTimeMillis();
		this.retryClicksSent++;

		if (config.isPlayAlertSound()) {
			player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.9f, 1.4f);
		}
	}

	/**
	 * Per-tick handler for targeting raycasts, lock tracking, and proactive key holding.
	 */
	public void tick(MinecraftClient client) {
		if (debounceCooldown > 0) {
			debounceCooldown--;
		}

		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;
		if (player == null || world == null) {
			resetTarget();
			return;
		}

		double reach = player.getBlockInteractionRange();
		double reachSq = reach * reach;
		Vec3d eyePos = player.getEyePos();

		BlockPos foundPos = null;
		BlockHitResult hit = null;

		// 1. Check if we have a locked vault
		if (lockedVaultPos != null) {
			double distSq = eyePos.squaredDistanceTo(Vec3d.ofCenter(lockedVaultPos));
			if (isVaultAt(world, lockedVaultPos)) {
				foundPos = lockedVaultPos;
			} else if (distSq > 64.0) {
				lockedVaultPos = null;
			}
		}

		// 2. Direct Crosshair Block Raycast
		if (foundPos == null && client.crosshairTarget instanceof BlockHitResult blockHit && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			if (isVaultAt(world, hitPos)) {
				foundPos = hitPos;
				hit = blockHit;
			}
		}

		// 3. Line-of-sight raycast through entities
		if (foundPos == null) {
			Vec3d lookVec = player.getRotationVec(1.0f);
			Vec3d reachEnd = eyePos.add(lookVec.multiply(reach + 0.5));
			BlockHitResult rayHit = world.raycast(new RaycastContext(
					eyePos, reachEnd, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
			));
			if (rayHit != null && rayHit.getType() == HitResult.Type.BLOCK && isVaultAt(world, rayHit.getBlockPos())) {
				foundPos = rayHit.getBlockPos();
				hit = rayHit;
			}
		}

		// 4. Proximity & viewing angle fallback
		if (foundPos == null) {
			BlockPos playerBlock = player.getBlockPos();
			int radius = (int) Math.ceil(reach);
			BlockPos bestPos = null;
			double bestAngle = 0.4;
			Vec3d lookVec = player.getRotationVec(1.0f);

			for (BlockPos checkPos : BlockPos.iterate(playerBlock.add(-radius, -radius, -radius), playerBlock.add(radius, radius, radius))) {
				if (isVaultAt(world, checkPos)) {
					Vec3d center = Vec3d.ofCenter(checkPos);
					if (eyePos.squaredDistanceTo(center) <= reachSq) {
						Vec3d dir = center.subtract(eyePos).normalize();
						double dot = lookVec.dotProduct(dir);
						if (dot > bestAngle) {
							bestAngle = dot;
							bestPos = checkPos.toImmutable();
						}
					}
				}
			}
			if (bestPos != null) {
				foundPos = bestPos;
			}
		}

		if (foundPos == null) {
			resetTarget();
			return;
		}

		this.targetVaultPos = foundPos;
		this.targetHit = hit;
		this.distance = Math.sqrt(eyePos.squaredDistanceTo(Vec3d.ofCenter(foundPos)));

		BlockState state = world.getBlockState(foundPos);
		this.isVaultOminous = state.get(VaultBlock.OMINOUS);
		this.vaultState = state.get(VaultBlock.VAULT_STATE);
		this.requiredKey = isVaultOminous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;

		checkPlayerKeys(player);

		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		// Proactively switch to key ahead of time so it is already in hand when matching reward appears
		if (config.isAutoSwitchKey() && !isKeyHeld && hasKeyInInventory && vaultState == VaultState.ACTIVE && distance <= reach) {
			switchToKey(player);
		}

		BlockEntity blockEntity = world.getBlockEntity(foundPos);
		if (blockEntity instanceof VaultBlockEntity vaultEntity) {
			updateDisplayItemAndCheckUnlock(vaultEntity, client);
		}
	}

	private void checkPlayerKeys(ClientPlayerEntity player) {
		this.isKeyHeld = player.getMainHandStack().isOf(requiredKey) || player.getOffHandStack().isOf(requiredKey);
		this.hasKeyInInventory = isKeyHeld;

		if (!hasKeyInInventory) {
			for (int i = 0; i < player.getInventory().size(); i++) {
				if (player.getInventory().getStack(i).isOf(requiredKey)) {
					this.hasKeyInInventory = true;
					break;
				}
			}
		}
	}

	private void switchToKey(ClientPlayerEntity player) {
		// 1. Check hotbar (slots 0-8)
		for (int slot = 0; slot < 9; slot++) {
			if (player.getInventory().getStack(slot).isOf(requiredKey)) {
				player.getInventory().setSelectedSlot(slot);
				if (player.networkHandler != null) {
					player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
				}
				this.isKeyHeld = true;
				return;
			}
		}

		// 2. Check main inventory (slots 9-35) and swap to selected hotbar slot
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.interactionManager != null && player.playerScreenHandler != null) {
			int invLimit = Math.min(36, player.getInventory().size());
			for (int slot = 9; slot < invLimit; slot++) {
				if (player.getInventory().getStack(slot).isOf(requiredKey)) {
					int selectedSlot = player.getInventory().getSelectedSlot();
					client.interactionManager.clickSlot(
							player.playerScreenHandler.syncId,
							slot,
							selectedSlot,
							SlotActionType.SWAP,
							player
					);
					this.isKeyHeld = true;
					return;
				}
			}
		}
	}

	private boolean isVaultAt(ClientWorld world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof VaultBlock;
	}

	public void toggleLockCurrentTarget() {
		if (lockedVaultPos != null) {
			lockedVaultPos = null;
		} else if (targetVaultPos != null) {
			lockedVaultPos = targetVaultPos;
		}
	}

	public void resetTarget() {
		this.targetVaultPos = null;
		this.targetHit = null;
		this.isVaultOminous = false;
		this.vaultState = VaultState.INACTIVE;
		this.currentDisplayItem = ItemStack.EMPTY;
		this.targetMatched = false;
		this.hasKeyInInventory = false;
		this.isKeyHeld = false;
		this.distance = 0.0;
		this.clickedForCurrentItem = false;
		this.retryClicksSent = 0;
	}

	public boolean hasTarget() {
		return targetVaultPos != null;
	}

	public BlockPos getTargetVaultPos() {
		return targetVaultPos;
	}

	public boolean isLocked() {
		return lockedVaultPos != null;
	}

	public boolean isVaultOminous() {
		return isVaultOminous;
	}

	public VaultState getVaultState() {
		return vaultState;
	}

	public ItemStack getCurrentDisplayItem() {
		return currentDisplayItem;
	}

	public boolean isTargetMatched() {
		return targetMatched;
	}

	public Item getRequiredKey() {
		return requiredKey;
	}

	public boolean hasKeyInInventory() {
		return hasKeyInInventory;
	}

	public boolean isKeyHeld() {
		return isKeyHeld;
	}

	public double getDistance() {
		return distance;
	}
}
