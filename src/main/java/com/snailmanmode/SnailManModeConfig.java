package com.snailmanmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import java.awt.*;

@ConfigGroup("snail")
public interface SnailManModeConfig extends Config
{
	@ConfigItem(
			keyName = "showTile",
			name = "Show Tile",
			description = "Show the snail's tile"
	)
	default boolean showTile()
	{
		return true;
	}
	@ConfigItem(
			keyName = "showPath",
			name = "Show Path",
			description = "Show the snail's path"
	)
	default boolean showPath()
	{
		return false;
	}
	@ConfigItem(
			keyName = "tileBorderColor",
			name = "Tile Border Color",
			description = "The snail's tile border color."
	)
	default Color color()
	{
		return new Color(255, 0,0,250);
	}

	@ConfigItem(
			keyName = "fillColor",
			name = "Fill Color",
			description = "The snail's tile fill color."
	)
	default Color fillColor()
	{
		return new Color(255, 0,0,50);
	}

	@Range(
			min = 1
	)
	@ConfigItem(
			keyName = "speed",
			name = "Speed",
			description = "The snail's speed in ticks."
	)
	default int speed()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "diagonalMovement",
			name = "Move Diagonally",
			description = "The snail can move X and Y on the same tick."
	)
	default boolean diagonalMovement()
	{
		return true;
	}
}
