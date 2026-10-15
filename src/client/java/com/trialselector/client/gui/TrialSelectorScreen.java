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
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class TrialSelectorScreen extends Screen {

	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 220;
	private static final int CELL_SIZE = 26;
	private static final int COLUMNS = 12;
	private static final int ROWS = 3;

	private int panelLeft;
	private int panelTop;
	private int scrollRowOffset = 0;

	private TextFieldWidget searchField;
	private VaultRewardRegistry.Category currentCategory = VaultRewardRegistry.Category.SELECTED;
	private List<ItemStack> displayedItems = new ArrayList<>();

	private ButtonWidget autoUnlockBtn;
	private ButtonWidget autoSwitchKeyBtn;
	private ButtonWidget hudBtn;

	public TrialSelectorScreen() {
		super(Text.literal("Trial Vault Reward Selector"));
	}

	@Override
	protected void init() {
		this.panelLeft = (this.width - PANEL_WIDTH) / 2;
		this.panelTop = (this.height - PANEL_HEIGHT) / 2;

		// 1. Search Box
		int searchX = panelLeft + 190;
		int searchY = panelTop + 10;
		this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, 138, 16, Text.literal("Search..."));
		this.searchField.setPlaceholder(Text.literal("Search item...").formatted(Formatting.DARK_GRAY));
		this.searchField.setChangedListener(text -> {
			this.scrollRowOffset = 0;
			updateItemList();
		});
		this.addDrawableChild(this.searchField);

		// 2. Category Tabs
		int tabWidth = 58;
		int tabHeight = 18;
		int tabSpacing = 4;
		int startX = panelLeft + 12;
		int tabY = panelTop + 32;

		VaultRewardRegistry.Category[] categories = {
				VaultRewardRegistry.Category.SELECTED,
				VaultRewardRegistry.Category.ALL_VAULT,
				VaultRewardRegistry.Category.OMINOUS,
				VaultRewardRegistry.Category.STANDARD,
				VaultRewardRegistry.Category.ALL_ITEMS
		};

		for (int i = 0; i < categories.length; i++) {
			VaultRewardRegistry.Category cat = categories[i];
			String label = switch (cat) {
				case SELECTED -> "Targets";
				case ALL_VAULT -> "All Vault";
				case OMINOUS -> "Ominous";
				case STANDARD -> "Standard";
				case ALL_ITEMS -> "All Items";
			};
			int bx = startX + i * (tabWidth + tabSpacing);
			ButtonWidget btn = ButtonWidget.builder(Text.literal(label), b -> {
				this.currentCategory = cat;
				this.scrollRowOffset = 0;
				updateItemList();
			}).dimensions(bx, tabY, tabWidth, tabHeight).build();
			this.addDrawableChild(btn);
		}

		// 3. Bottom Settings & Action Buttons
		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		int bottomY = panelTop + PANEL_HEIGHT - 48;

		this.autoUnlockBtn = ButtonWidget.builder(getAutoUnlockText(), b -> {
			config.toggleAutoUnlock();
			b.setMessage(getAutoUnlockText());
		}).dimensions(panelLeft + 12, bottomY, 100, 18).build();
		this.addDrawableChild(this.autoUnlockBtn);

		this.autoSwitchKeyBtn = ButtonWidget.builder(getAutoSwitchText(), b -> {
			config.setAutoSwitchKey(!config.isAutoSwitchKey());
			b.setMessage(getAutoSwitchText());
		}).dimensions(panelLeft + 116, bottomY, 104, 18).build();
		this.addDrawableChild(this.autoSwitchKeyBtn);

		this.hudBtn = ButtonWidget.builder(getHudText(), b -> {
			config.setShowHud(!config.isShowHud());
			b.setMessage(getHudText());
		}).dimensions(panelLeft + 224, bottomY, 104, 18).build();
		this.addDrawableChild(this.hudBtn);

		// Lower row buttons
		int bottomRow2Y = bottomY + 22;
		ButtonWidget clearBtn = ButtonWidget.builder(Text.literal("Clear All").formatted(Formatting.RED), b -> {
			config.clearRewards();
			updateItemList();
		}).dimensions(panelLeft + 12, bottomRow2Y, 68, 18).build();
		this.addDrawableChild(clearBtn);

		// Opens the dedicated screen to manually choose from every single Trial Vault and Ominous Vault drop
		ButtonWidget selectRaresBtn = ButtonWidget.builder(Text.literal("Select Rares").formatted(Formatting.GOLD), b -> {
			if (this.client != null) {
				this.client.setScreen(new VaultRewardsSelectionScreen(this));
			}
		}).dimensions(panelLeft + 84, bottomRow2Y, 86, 18).build();
		this.addDrawableChild(selectRaresBtn);

		ButtonWidget doneBtn = ButtonWidget.builder(Text.literal("Done").formatted(Formatting.GREEN), b -> {
			this.close();
		}).dimensions(panelLeft + PANEL_WIDTH - 84, bottomRow2Y, 72, 18).build();
		this.addDrawableChild(doneBtn);

		updateItemList();
	}

	private Text getAutoUnlockText() {
		boolean on = TrialSelectorMod.getConfig().isAutoUnlockEnabled();
		return Text.literal("Auto-Unlock: ")
				.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
	}

	private Text getAutoSwitchText() {
		boolean on = TrialSelectorMod.getConfig().isAutoSwitchKey();
		return Text.literal("Auto-Key: ")
				.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
	}

	private Text getHudText() {
		boolean on = TrialSelectorMod.getConfig().isShowHud();
		return Text.literal("HUD: ")
				.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
	}

	public void updateItemList() {
		String query = this.searchField != null ? this.searchField.getText() : "";
		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		this.displayedItems = VaultRewardRegistry.getItemsForCategory(this.currentCategory, config.getSelectedRewardIds(), query);

		int totalRows = (int) Math.ceil((double) displayedItems.size() / COLUMNS);
		int maxScroll = Math.max(0, totalRows - ROWS);
		if (this.scrollRowOffset > maxScroll) {
			this.scrollRowOffset = maxScroll;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		// 1. Render main modal dialog background and border FIRST so buttons are drawn cleanly on top
		context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xF0121722);
		drawBoxOutline(context, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFF35889E);

		// 2. Render all button widgets and input fields on top of background
		super.render(context, mouseX, mouseY, deltaTicks);

		// 3. Title & Header Info
		Text title = Text.literal("✦ Trial Vault Selector ✦").formatted(Formatting.GOLD, Formatting.BOLD);
		context.drawTextWithShadow(this.textRenderer, title, panelLeft + 12, panelTop + 14, 0xFFFFFFFF);

		TrialSelectorConfig config = TrialSelectorMod.getConfig();
		String targetCount = "Targets: " + config.getSelectedRewardIds().size();
		context.drawTextWithShadow(this.textRenderer, Text.literal(targetCount).formatted(Formatting.GRAY), panelLeft + 12, panelTop + 56, 0xFFAAAAAA);

		// 4. Render Items Grid
		int gridStartX = panelLeft + 14;
		int gridStartY = panelTop + 68;
		int startIndex = scrollRowOffset * COLUMNS;
		int endIndex = Math.min(displayedItems.size(), startIndex + (COLUMNS * ROWS));

		ItemStack hoveredStack = null;

		for (int i = startIndex; i < endIndex; i++) {
			int slotIndex = i - startIndex;
			int col = slotIndex % COLUMNS;
			int row = slotIndex / COLUMNS;
			int slotX = gridStartX + col * (CELL_SIZE + 2);
			int slotY = gridStartY + row * (CELL_SIZE + 2);

			ItemStack stack = displayedItems.get(i);
			Identifier itemId = Registries.ITEM.getId(stack.getItem());
			boolean isSelected = itemId != null && config.isRewardSelected(itemId.toString());

			boolean isHovered = mouseX >= slotX && mouseX < slotX + CELL_SIZE && mouseY >= slotY && mouseY < slotY + CELL_SIZE;
			if (isHovered) {
				hoveredStack = stack;
			}

			// Slot Background
			int cellBg = isSelected ? 0xFF2A1515 : 0xFF1C2230;
			if (isHovered) {
				cellBg = isSelected ? 0xFF441818 : 0xFF2A3449;
			}
			context.fill(slotX, slotY, slotX + CELL_SIZE, slotY + CELL_SIZE, cellBg);

			// Slot Border
			int slotBorder = isSelected ? 0xFFFF3333 : (isHovered ? 0xFF88AAFF : 0xFF313B4E);
			drawBoxOutline(context, slotX, slotY, CELL_SIZE, CELL_SIZE, slotBorder);

			// Draw Item Icon
			context.drawItem(stack, slotX + (CELL_SIZE - 16) / 2, slotY + (CELL_SIZE - 16) / 2);

			// Selected Red Cross Marker
			if (isSelected) {
				context.drawTextWithShadow(this.textRenderer, Text.literal("✖").formatted(Formatting.RED, Formatting.BOLD), slotX + 2, slotY + 2, 0xFFFF2222);
			}
		}

		// Scroll info if many items
		int totalRows = (int) Math.ceil((double) displayedItems.size() / COLUMNS);
		if (totalRows > ROWS) {
			String scrollText = String.format("%d / %d", scrollRowOffset + 1, Math.max(1, totalRows - ROWS + 1));
			context.drawTextWithShadow(this.textRenderer, Text.literal(scrollText).formatted(Formatting.GRAY), panelLeft + PANEL_WIDTH - 50, panelTop + 56, 0xFF888888);
		}

		// 5. Render Hovered Item Tooltip
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
			int slotX = gridStartX + col * (CELL_SIZE + 2);
			int slotY = gridStartY + row * (CELL_SIZE + 2);

			if (mx >= slotX && mx < slotX + CELL_SIZE && my >= slotY && my < slotY + CELL_SIZE) {
				ItemStack stack = displayedItems.get(i);
				Identifier itemId = Registries.ITEM.getId(stack.getItem());
				if (itemId != null) {
					TrialSelectorConfig config = TrialSelectorMod.getConfig();
					config.toggleReward(itemId.toString());
					if (this.client != null) {
						this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					}
					// If in SELECTED view, refresh list immediately
					if (this.currentCategory == VaultRewardRegistry.Category.SELECTED) {
						updateItemList();
					}
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

	private static void drawBoxOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y + 1, x + 1, y + height - 1, color);
		context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}
}
