package com.trialselector.client;

import com.trialselector.TrialSelectorMod;
import com.trialselector.client.gui.TrialSelectorHud;
import com.trialselector.client.gui.TrialSelectorScreen;
import com.trialselector.config.TrialSelectorConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TrialSelectorClient implements ClientModInitializer {
	public static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of(TrialSelectorMod.MOD_ID, "general"));

	private static KeyBinding openGuiKey;
	private static KeyBinding toggleAutoKey;
	private static KeyBinding lockVaultKey;

	@Override
	public void onInitializeClient() {
		// 1. Register Keybindings
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.trialselector.open_gui",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				KEY_CATEGORY
		));

		toggleAutoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.trialselector.toggle_auto",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KEY_CATEGORY
		));

		lockVaultKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.trialselector.lock_vault",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				KEY_CATEGORY
		));

		// 2. Client Tick Handler
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				return;
			}

			// Keybind checks
			while (openGuiKey.wasPressed()) {
				client.setScreen(new TrialSelectorScreen());
			}

			while (toggleAutoKey.wasPressed()) {
				TrialSelectorConfig config = TrialSelectorMod.getConfig();
				config.toggleAutoUnlock();
				boolean on = config.isAutoUnlockEnabled();
				Text msg = Text.literal("[Trial Selector] Auto-Unlock: ").formatted(Formatting.GOLD)
						.append(Text.literal(on ? "ENABLED" : "DISABLED").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
				client.player.sendMessage(msg, true);
				client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, on ? 1.4f : 0.8f);
			}

			while (lockVaultKey.wasPressed()) {
				VaultTracker tracker = VaultTracker.getInstance();
				tracker.toggleLockCurrentTarget();
				boolean locked = tracker.isLocked();
				Text msg = locked
						? Text.literal("✦ Locked onto target Vault!").formatted(Formatting.YELLOW, Formatting.BOLD)
						: Text.literal("✦ Vault target unlocked.").formatted(Formatting.GRAY);
				client.player.sendMessage(msg, true);
			}

			// Update tracker & handle auto-unlock
			VaultTracker.getInstance().tick(client);
		});

		// 3. Register HUD rendering & per-frame vault tracker check
		HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
			VaultTracker.getInstance().onRenderFrame(net.minecraft.client.MinecraftClient.getInstance());
			TrialSelectorHud.render(drawContext, renderTickCounter);
		});
	}
}
