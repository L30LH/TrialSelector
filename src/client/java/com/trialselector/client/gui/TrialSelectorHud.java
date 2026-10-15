package com.trialselector.client.gui;

import com.trialselector.TrialSelectorMod;
import com.trialselector.client.VaultTracker;
import com.trialselector.config.TrialSelectorConfig;
import net.minecraft.block.enums.VaultState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TrialSelectorHud {

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options.hudHidden) {
			return;
		}

		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		if (!config.isShowHud()) {
			return;
		}

		VaultTracker tracker = VaultTracker.getInstance();
		if (!tracker.hasTarget()) {
			return;
		}

		TextRenderer textRenderer = client.textRenderer;
		int x = 12;
		int y = 12;
		int width = 210;
		int height = 72;

		boolean matched = tracker.isTargetMatched();
		boolean ominous = tracker.isVaultOminous();
		int borderColor = matched ? 0xFF00FF66 : (ominous ? 0xFFBA55D3 : 0xFF00CCCC);
		int bgColor = 0xDD12151D;

		// Background box
		context.fill(x, y, x + width, y + height, bgColor);
		// Border lines
		drawBoxOutline(context, x, y, width, height, borderColor);

		// Header: Title & Vault Type
		String vaultTypeName = ominous ? "OMINOUS VAULT" : "TRIAL VAULT";
		Formatting vaultColor = ominous ? Formatting.LIGHT_PURPLE : Formatting.AQUA;
		Text title = Text.literal("✦ ").formatted(Formatting.GOLD)
				.append(Text.literal(vaultTypeName).formatted(vaultColor, Formatting.BOLD))
				.append(Text.literal(tracker.isLocked() ? " [LOCKED]" : "").formatted(Formatting.YELLOW));
		context.drawTextWithShadow(textRenderer, title, x + 8, y + 6, 0xFFFFFFFF);

		// Auto unlock status
		boolean autoEnabled = config.isAutoUnlockEnabled();
		Text autoStatus = Text.literal("Auto: ").formatted(Formatting.GRAY)
				.append(Text.literal(autoEnabled ? "ACTIVE" : "OFF").formatted(autoEnabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
				.append(Text.literal(String.format(" (%.1fm)", tracker.getDistance())).formatted(Formatting.DARK_GRAY));
		context.drawTextWithShadow(textRenderer, autoStatus, x + width - textRenderer.getWidth(autoStatus) - 8, y + 6, 0xFFFFFFFF);

		// Current displayed item row
		ItemStack displayStack = tracker.getCurrentDisplayItem();
		int itemRowY = y + 22;
		if (!displayStack.isEmpty()) {
			context.drawItem(displayStack, x + 8, itemRowY);
			String itemName = displayStack.getName().getString();
			if (matched) {
				Text matchedText = Text.literal("✖ ").formatted(Formatting.RED, Formatting.BOLD)
						.append(Text.literal(itemName).formatted(Formatting.GREEN, Formatting.BOLD));
				context.drawTextWithShadow(textRenderer, matchedText, x + 30, itemRowY + 4, 0xFF55FF55);
			} else {
				Text normalText = Text.literal(itemName).formatted(Formatting.WHITE);
				context.drawTextWithShadow(textRenderer, normalText, x + 30, itemRowY + 4, 0xFFDDDDDD);
			}
		} else {
			VaultState state = tracker.getVaultState();
			String stateMsg = switch (state) {
				case INACTIVE -> "Vault Inactive (Already Looted)";
				case UNLOCKING -> "Unlocking Reward...";
				case EJECTING -> "Ejecting Items...";
				default -> "Waiting for item cycle...";
			};
			context.drawTextWithShadow(textRenderer, Text.literal(stateMsg).formatted(Formatting.GRAY), x + 10, itemRowY + 4, 0xFFAAAAAA);
		}

		// Key requirement row
		int keyRowY = y + 46;
		ItemStack keyStack = new ItemStack(tracker.getRequiredKey());
		context.drawItem(keyStack, x + 8, keyRowY);

		Text keyStatusText;
		if (tracker.isKeyHeld()) {
			keyStatusText = Text.literal("Key: Ready in Hand").formatted(Formatting.GREEN, Formatting.BOLD);
		} else if (tracker.hasKeyInInventory()) {
			keyStatusText = Text.literal("Key: In Inventory (Auto-Switch)").formatted(Formatting.YELLOW);
		} else {
			keyStatusText = Text.literal("Key: None Available").formatted(Formatting.RED);
		}
		context.drawTextWithShadow(textRenderer, keyStatusText, x + 30, keyRowY + 4, 0xFFFFFFFF);
	}

	private static void drawBoxOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}
}
