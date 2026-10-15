package com.trialselector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trialselector.TrialSelectorMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TrialSelectorConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("trialselector.json");

	private boolean autoUnlockEnabled = true;
	private boolean autoSwitchKey = true;
	private boolean showHud = true;
	private boolean playAlertSound = true;
	private Set<String> selectedRewardIds = new HashSet<>();

	public TrialSelectorConfig() {
		// Default selected rewards (most valuable trial items)
		selectedRewardIds.add("minecraft:heavy_core");
		selectedRewardIds.add("minecraft:enchanted_golden_apple");
		selectedRewardIds.add("minecraft:flow_armor_trim_smithing_template");
		selectedRewardIds.add("minecraft:bolt_armor_trim_smithing_template");
		selectedRewardIds.add("minecraft:flow_banner_pattern");
		selectedRewardIds.add("minecraft:guster_banner_pattern");
		selectedRewardIds.add("minecraft:music_disc_creator");
		selectedRewardIds.add("minecraft:music_disc_precipice");
		selectedRewardIds.add("minecraft:diamond_block");
		selectedRewardIds.add("minecraft:emerald_block");
		selectedRewardIds.add("minecraft:ominous_bottle");
		selectedRewardIds.add("minecraft:wind_charge");
	}

	public static TrialSelectorConfig load() {
		File file = CONFIG_PATH.toFile();
		if (!file.exists()) {
			TrialSelectorConfig config = new TrialSelectorConfig();
			config.save();
			return config;
		}

		try (FileReader reader = new FileReader(file)) {
			TrialSelectorConfig config = GSON.fromJson(reader, TrialSelectorConfig.class);
			if (config == null) {
				config = new TrialSelectorConfig();
			}
			return config;
		} catch (Exception e) {
			TrialSelectorMod.LOGGER.error("Failed to load Trial Selector configuration, using defaults", e);
			return new TrialSelectorConfig();
		}
	}

	public void save() {
		File file = CONFIG_PATH.toFile();
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			try (FileWriter writer = new FileWriter(file)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			TrialSelectorMod.LOGGER.error("Failed to save Trial Selector configuration", e);
		}
	}

	public boolean isAutoUnlockEnabled() {
		return autoUnlockEnabled;
	}

	public void setAutoUnlockEnabled(boolean autoUnlockEnabled) {
		this.autoUnlockEnabled = autoUnlockEnabled;
		save();
	}

	public void toggleAutoUnlock() {
		setAutoUnlockEnabled(!this.autoUnlockEnabled);
	}

	public boolean isAutoSwitchKey() {
		return autoSwitchKey;
	}

	public void setAutoSwitchKey(boolean autoSwitchKey) {
		this.autoSwitchKey = autoSwitchKey;
		save();
	}

	public boolean isShowHud() {
		return showHud;
	}

	public void setShowHud(boolean showHud) {
		this.showHud = showHud;
		save();
	}

	public boolean isPlayAlertSound() {
		return playAlertSound;
	}

	public void setPlayAlertSound(boolean playAlertSound) {
		this.playAlertSound = playAlertSound;
		save();
	}

	public Set<String> getSelectedRewardIds() {
		return selectedRewardIds;
	}

	public boolean isRewardSelected(Item item) {
		Identifier id = Registries.ITEM.getId(item);
		return id != null && isRewardSelected(id.toString());
	}

	public boolean isRewardSelected(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return isRewardSelected(stack.getItem());
	}

	public boolean isRewardSelected(String itemId) {
		return selectedRewardIds.contains(itemId);
	}

	public void toggleReward(String itemId) {
		if (selectedRewardIds.contains(itemId)) {
			selectedRewardIds.remove(itemId);
		} else {
			selectedRewardIds.add(itemId);
		}
		save();
	}

	public void addReward(String itemId) {
		if (selectedRewardIds.add(itemId)) {
			save();
		}
	}

	public void removeReward(String itemId) {
		if (selectedRewardIds.remove(itemId)) {
			save();
		}
	}

	public void clearRewards() {
		selectedRewardIds.clear();
		save();
	}
}
