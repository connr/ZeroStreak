package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("zerostreak")
public interface ZeroStreakConfig extends Config
{
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Volume (0-100)",
			position = 0
	)
	@Range(
			min = 0,
			max = 100
	)
	default int volume()
	{
		return 80;
	}

	@ConfigItem(
			keyName = "consecutiveZerosRequired",
			name = "Zeros Required",
			description = "How many 0s in a row before playing",
			position = 1
	)
	@Range(
			min = 1,
			max = 10
	)
	default int consecutiveZerosRequired()
	{
		return 4;
	}

	@ConfigItem(
			keyName = "useCustomSound",
			name = "Use Custom Sound",
			description = "Use a custom sound file instead (must be .wav).",
			position = 2
	)
	default boolean useCustomSound()
	{
		return false;
	}
}