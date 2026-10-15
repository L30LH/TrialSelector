package com.trialselector;

import com.trialselector.config.TrialSelectorConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrialSelectorMod implements ModInitializer {
	public static final String MOD_ID = "trialselector";
	public static final String MOD_NAME = "Trial Selector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private static TrialSelectorConfig config;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing {}", MOD_NAME);
		config = TrialSelectorConfig.load();
	}

	public static TrialSelectorConfig getConfig() {
		if (config == null) {
			config = TrialSelectorConfig.load();
		}
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
