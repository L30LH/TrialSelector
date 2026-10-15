package com.trialselector.client.gui;

import com.trialselector.TrialSelectorMod;
import com.trialselector.config.TrialSelectorConfig;
import com.trialselector.data.VaultRewardRegistry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class VaultRewardsSelectionScreen extends Screen {

	public enum ViewMode {
		ALL("All Vault Drops"),
		OMINOUS("Ominous Vault"),
		STANDARD("Standard Vault");

		private final String label;

		ViewMode(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final Screen parent;
	private ViewMode currentMode = ViewMode.ALL;

	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 230;
	private static final int CELL_SIZE = 26;
	private static final int COLUMNS = 10;
	private static final int ROWS = 4;

	private int panelLeft;
	private int panelTop;
	private int scrollRowOffset = 0;

	private TextFieldWidget searchField;
	private final List<ItemStack> displayedItems = new ArrayList<>();

	public VaultRewardsSelectionScreen(Screen parent) {
		super(Text.literal("Vault Rewards Selection"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.panelLeft = (this.width - PANEL_WIDTH) / 2;
		this.panelTop = (this.height - PANEL_HEIGHT) / 2;

		// 1. Search Box
		int searchX = panelLeft + 190;
		int searchY = panelTop + 10;
		this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, 138, 16, Text.literal("Search..."));
		this.searchField.setPlaceholder(Text.literal("Search drop...").formatted(Formatting.DARK_GRAY));
		this.searchField.setChangedListener(query -> {
			this.scrollRowOffset = 0;
			updateItemList();
		});
		this.addDrawableChild(this.searchField);

		// 2. Mode Category Tabs
		ViewMode[] modes = ViewMode.values();
		int tabWidth = 98;
		int tabHeight = 18;
		int tabSpacing = 6;
		int startX = panelLeft + 12;
		int tabY = panelTop + 32;

		for (int i = 0; i < modes.length; i++) {
			ViewMode mode = modes[i];
			int bx = startX + i * (tabWidth + tabSpacing);
			ButtonWidget btn = ButtonWidget.builder(Text.literal(mode.getLabel()), b -> {
				this.currentMode = mode;
				this.scrollRowOffset = 0;
				updateItemList();
			}).dimensions(bx, tabY, tabWidth, tabHeight).build();
			this.addDrawableChild(btn);
		}

		// 3. Bottom Action Buttons
		int bottomY = panelTop + PANEL_HEIGHT - 28;

		ButtonWidget selectAllBtn = ButtonWidget.builder(Text.literal("Select All").formatted(Formatting.GREEN), b -> {
			TrialSelectorConfig config = TrialSelectorMod.getConfig();
			for (ItemStack stack : displayedItems) {
				Identifier id = Registries.ITEM.getId(stack.getItem());
				if (id != null) {
					config.getSelectedRewardIds().add(id.toString());
				}
			}
			config.save();
			playClickSound();
			updateItemList();
		}).dimensions(panelLeft + 12, bottomY, 74, 18).build();
		this.addDrawableChild(selectAllBtn);

		ButtonWidget deselectAllBtn = ButtonWidget.builder(Text.literal("Deselect All").formatted(Formatting.RED), b -> {
			TrialSelectorConfig config = TrialSelectorMod.getConfig();
			for (ItemStack stack : displayedItems) {
				Identifier id = Registries.ITEM.getId(stack.getItem());
				if (id != null) {
					config.getSelectedRewardIds().remove(id.toString());
				}
			}
			config.save();
			playClickSound();
			updateItemList();
		}).dimensions(panelLeft + 90, bottomY, 80, 18).build();
		this.addDrawableChild(deselectAllBtn);

		ButtonWidget selectRaresPresetBtn = ButtonWidget.builder(Text.literal("Select Rares Only").formatted(Formatting.GOLD), b -> {
			TrialSelectorConfig config = TrialSelectorMod.getConfig();
			// Clear first or add rare set
			config.getSelectedRewardIds().add("minecraft:heavy_core");
			config.getSelectedRewardIds().add("minecraft:enchanted_golden_apple");
			config.getSelectedRewardIds().add("minecraft:flow_armor_trim_smithing_template");
			config.getSelectedRewardIds().add("minecraft:bolt_armor_trim_smithing_template");
			config.getSelectedRewardIds().add("minecraft:flow_banner_pattern");
			config.getSelectedRewardIds().add("minecraft:guster_banner_pattern");
			config.getSelectedRewardIds().add("minecraft:music_disc_creator");
			config.getSelectedRewardIds().add("minecraft:music_disc_precipice");
			config.getSelectedRewardIds().add("minecraft:trident");
			config.getSelectedRewardIds().add("minecraft:diamond_block");
			config.save();
			playClickSound();
			updateItemList();
		}).dimensions(panelLeft + 174, bottomY, 96, 18).build();
		this.addDrawableChild(selectRaresPresetBtn);

		ButtonWidget backBtn = ButtonWidget.builder(Text.literal("Back").formatted(Formatting.AQUA), b -> {
			this.close();
		}).dimensions(panelLeft + PANEL_WIDTH - 64, bottomY, 52, 18).build();
		this.addDrawableChild(backBtn);

		updateItemList();
	}

	private void updateItemList() {
		this.displayedItems.clear();
		String query = this.searchField != null ? this.searchField.getText().toLowerCase(Locale.ROOT).trim() : "";

		List<Item> sourceItems = switch (currentMode) {
			case OMINOUS -> VaultRewardRegistry.OMINOUS_REWARDS;
			case STANDARD -> VaultRewardRegistry.STANDARD_REWARDS;
			case ALL -> VaultRewardRegistry.ALL_VAULT_REWARDS;
		};

		for (Item item : sourceItems) {
			ItemStack stack = new ItemStack(item);
			if (query.isEmpty()
					|| stack.getName().getString().toLowerCase(Locale.ROOT).contains(query)
					|| Registries.ITEM.getId(item).toString().toLowerCase(Locale.ROOT).contains(query)) {
				this.displayedItems.add(stack);
			}
		}
	}

	@Override
	public void close() {
		if (this.client != null) {
			if (this.parent instanceof TrialSelectorScreen screen) {
				screen.updateItemList();
			}
			this.client.setScreen(this.parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		// 1. Draw Background & Outer Frame FIRST so buttons render ON TOP
		context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xF0121722);
		drawBoxOutline(context, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFF35889E);

		// 2. Draw Widgets (Buttons, Text Field) ON TOP of background
		super.render(context, mouseX, mouseY, deltaTicks);

		// 3. Header Title & Count Info
		Text title = Text.literal("✦ Vault Rewards Selection ✦").formatted(Formatting.GOLD, Formatting.BOLD);
		context.drawTextWithShadow(this.textRenderer, title, panelLeft + 12, panelTop + 14, 0xFFFFFFFF);

		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		int selectedCount = 0;
		for (ItemStack stack : displayedItems) {
			Identifier id = Registries.ITEM.getId(stack.getItem());
			if (id != null && config.isRewardSelected(id.toString())) {
				selectedCount++;
			}
		}
		String countInfo = String.format("Selected in view: %d / %d", selectedCount, displayedItems.size());
		context.drawTextWithShadow(this.textRenderer, Text.literal(countInfo).formatted(Formatting.GRAY), panelLeft + 12, panelTop + 56, 0xFFAAAAAA);

		// 4. Render Item Grid
		int gridStartX = panelLeft + 14;
		int gridStartY = panelTop + 68;
		int startIndex = scrollRowOffset * COLUMNS;
		int endIndex = Math.min(displayedItems.size(), startIndex + (COLUMNS * ROWS));

		ItemStack hoveredStack = null;

		for (int i = startIndex; i < endIndex; i++) {
			int slotIndex = i - startIndex;
			int col = slotIndex % COLUMNS;
			int row = slotIndex / COLUMNS;
			int slotX = gridStartX + col * (CELL_SIZE + 5);
			int slotY = gridStartY + row * (CELL_SIZE + 4);

			ItemStack stack = displayedItems.get(i);
			Identifier itemId = Registries.ITEM.getId(stack.getItem());
			boolean isSelected = itemId != null && config.isRewardSelected(itemId.toString());

			boolean isHovered = mouseX >= slotX && mouseX < slotX + CELL_SIZE && mouseY >= slotY && mouseY < slotY + CELL_SIZE;
			if (isHovered) {
				hoveredStack = stack;
			}

			// Slot Background & Highlight
			int cellBg = isSelected ? 0xFF2A1515 : 0xFF1B2332;
			if (isHovered) {
				cellBg = isSelected ? 0xFF441818 : 0xFF26334A;
			}
			context.fill(slotX, slotY, slotX + CELL_SIZE, slotY + CELL_SIZE, cellBg);

			// Slot Border (Red when selected, bright blue on hover, dark blue standard)
			int slotBorder = isSelected ? 0xFFFF3333 : (isHovered ? 0xFF88AAFF : 0xFF35445E);
			drawBoxOutline(context, slotX, slotY, CELL_SIZE, CELL_SIZE, slotBorder);

			// Draw Item Icon
			context.drawItem(stack, slotX + (CELL_SIZE - 16) / 2, slotY + (CELL_SIZE - 16) / 2);

			// Red Cross Badge for Selected Item
			if (isSelected) {
				context.drawTextWithShadow(this.textRenderer, Text.literal("✖").formatted(Formatting.RED, Formatting.BOLD), slotX + 2, slotY + 2, 0xFFFF2222);
			}
		}

		// Scroll indicator if needed
		int totalRows = (int) Math.ceil((double) displayedItems.size() / COLUMNS);
		if (totalRows > ROWS) {
			String scrollText = String.format("%d / %d", scrollRowOffset + 1, Math.max(1, totalRows - ROWS + 1));
			context.drawTextWithShadow(this.textRenderer, Text.literal(scrollText).formatted(Formatting.GRAY), panelLeft + PANEL_WIDTH - 54, panelTop + 56, 0xFF888888);
		}

		// 5. Tooltip on Hover
		if (hoveredStack != null) {
			List<Text> tooltip = new ArrayList<>();
			tooltip.add(hoveredStack.getName());
			Identifier id = Registries.ITEM.getId(hoveredStack.getItem());
			if (id != null) {
				boolean selected = config.isRewardSelected(id.toString());
				if (selected) {
					tooltip.add(Text.literal("✖ Selected as Target (Click to remove)").formatted(Formatting.RED));
				} else {
					tooltip.add(Text.literal("Click to select as Target").formatted(Formatting.GREEN));
				}
				tooltip.add(Text.literal(id.toString()).formatted(Formatting.DARK_GRAY));
			}
			context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		double mx = click.x();
		double my = click.y();

		int gridStartX = panelLeft + 14;
		int gridStartY = panelTop + 68;
		int startIndex = scrollRowOffset * COLUMNS;
		int endIndex = Math.min(displayedItems.size(), startIndex + (COLUMNS * ROWS));

		for (int i = startIndex; i < endIndex; i++) {
			int slotIndex = i - startIndex;
			int col = slotIndex % COLUMNS;
			int row = slotIndex / COLUMNS;
			int slotX = gridStartX + col * (CELL_SIZE + 5);
			int slotY = gridStartY + row * (CELL_SIZE + 4);

			if (mx >= slotX && mx < slotX + CELL_SIZE && my >= slotY && my < slotY + CELL_SIZE) {
				ItemStack stack = displayedItems.get(i);
				Identifier itemId = Registries.ITEM.getId(stack.getItem());
				if (itemId != null) {
					TrialSelectorConfig config = TrialSelectorMod.getConfig();
					config.toggleReward(itemId.toString());
					playClickSound();
					updateItemList();
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int totalRows = (int) Math.ceil((double) displayedItems.size() / COLUMNS);
		int maxScroll = Math.max(0, totalRows - ROWS);

		if (verticalAmount > 0 && scrollRowOffset > 0) {
			scrollRowOffset--;
			return true;
		} else if (verticalAmount < 0 && scrollRowOffset < maxScroll) {
			scrollRowOffset++;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void playClickSound() {
		if (this.client != null) {
			this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}

	private static void drawBoxOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y + 1, x + 1, y + height - 1, color);
		context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}
}
