package com.trialselector.data;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public class VaultRewardRegistry {

	public enum Category {
		ALL_VAULT("All Vault"),
		OMINOUS("Ominous"),
		STANDARD("Standard"),
		SELECTED("Targets"),
		ALL_ITEMS("All Items");

		private final String displayName;

		Category(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	// Full loot list for Ominous Trial Vault based on https://minecraft.wiki/w/Vault#Loot & Ominous_Vault#Loot
	public static final List<Item> OMINOUS_REWARDS = List.of(
			Items.HEAVY_CORE,
			Items.ENCHANTED_GOLDEN_APPLE,
			Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
			Items.FLOW_BANNER_PATTERN,
			Items.MUSIC_DISC_CREATOR,
			Items.DIAMOND_BLOCK,
			Items.EMERALD_BLOCK,
			Items.IRON_BLOCK,
			Items.GOLDEN_APPLE,
			Items.ENCHANTED_BOOK,
			Items.DIAMOND_CHESTPLATE,
			Items.DIAMOND_AXE,
			Items.DIAMOND,
			Items.EMERALD,
			Items.CROSSBOW,
			Items.OMINOUS_BOTTLE,
			Items.WIND_CHARGE,
			Items.TIPPED_ARROW
	);

	// Full loot list for Standard Trial Vault based on https://minecraft.wiki/w/Vault#Loot
	public static final List<Item> STANDARD_REWARDS = List.of(
			Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
			Items.GUSTER_BANNER_PATTERN,
			Items.MUSIC_DISC_PRECIPICE,
			Items.TRIDENT,
			Items.GOLDEN_APPLE,
			Items.GOLDEN_CARROT,
			Items.ENCHANTED_BOOK,
			Items.DIAMOND_CHESTPLATE,
			Items.DIAMOND_AXE,
			Items.DIAMOND,
			Items.EMERALD,
			Items.IRON_CHESTPLATE,
			Items.IRON_AXE,
			Items.IRON_INGOT,
			Items.CROSSBOW,
			Items.BOW,
			Items.SHIELD,
			Items.OMINOUS_BOTTLE,
			Items.WIND_CHARGE,
			Items.HONEY_BOTTLE,
			Items.TIPPED_ARROW,
			Items.ARROW
	);

	public static final List<Item> ALL_VAULT_REWARDS;

	static {
		Set<Item> combined = new LinkedHashSet<>(OMINOUS_REWARDS);
		combined.addAll(STANDARD_REWARDS);
		ALL_VAULT_REWARDS = List.copyOf(combined);
	}

	public static List<ItemStack> getItemsForCategory(Category category, Set<String> selectedIds, String searchQuery) {
		List<Item> baseList;

		switch (category) {
			case OMINOUS -> baseList = OMINOUS_REWARDS;
			case STANDARD -> baseList = STANDARD_REWARDS;
			case ALL_VAULT -> baseList = ALL_VAULT_REWARDS;
			case SELECTED -> {
				List<ItemStack> selectedStacks = new ArrayList<>();
				for (String idStr : selectedIds) {
					Identifier id = Identifier.tryParse(idStr);
					if (id != null && Registries.ITEM.containsId(id)) {
						Item item = Registries.ITEM.get(id);
						ItemStack stack = new ItemStack(item);
						if (matchesQuery(stack, searchQuery)) {
							selectedStacks.add(stack);
						}
					}
				}
				selectedStacks.sort(Comparator.comparing(s -> s.getName().getString()));
				return selectedStacks;
			}
			case ALL_ITEMS -> {
				List<ItemStack> allStacks = new ArrayList<>();
				for (Item item : Registries.ITEM) {
					if (item == Items.AIR) continue;
					ItemStack stack = new ItemStack(item);
					if (matchesQuery(stack, searchQuery)) {
						allStacks.add(stack);
					}
				}
				allStacks.sort(Comparator.comparing(s -> s.getName().getString()));
				return allStacks;
			}
			default -> baseList = ALL_VAULT_REWARDS;
		}

		List<ItemStack> result = new ArrayList<>();
		for (Item item : baseList) {
			ItemStack stack = new ItemStack(item);
			if (matchesQuery(stack, searchQuery)) {
				result.add(stack);
			}
		}
		return result;
	}

	private static boolean matchesQuery(ItemStack stack, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String cleanQuery = query.toLowerCase(Locale.ROOT).trim();
		String name = stack.getName().getString().toLowerCase(Locale.ROOT);
		Identifier id = Registries.ITEM.getId(stack.getItem());
		String idStr = id != null ? id.toString().toLowerCase(Locale.ROOT) : "";
		return name.contains(cleanQuery) || idStr.contains(cleanQuery);
	}
}
