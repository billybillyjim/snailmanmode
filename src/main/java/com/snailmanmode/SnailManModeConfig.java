package com.snailmanmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("snail")
public interface SnailManModeConfig extends Config
{
	@ConfigItem(
		keyName = "xPos",
		name = "X Position",
		description = "The snail's x coordinate."
	)
	default int xPos()
	{
		return SnailManModePlugin.snailXPosition;
	}

	@ConfigItem(
			keyName = "yPos",
			name = "Y Position",
			description = "The snail's y coordinate."
	)
	default int yPos()
	{
		return SnailManModePlugin.snailYPosition;
	}

	@ConfigItem(
			keyName = "spawnSnail",
			name = "Spawn Snail",
			description = "Spawn the Snail."
	)
	default void spawnSnail()
	{
		SnailManModePlugin.spawnSnail();
	}
}
