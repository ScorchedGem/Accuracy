package com.accuracy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface AccuracyConfig extends Config
{
	@ConfigItem(
			keyName = "groupCount",
			name = "Remember for me",
			description = "The amount of id's you store locally per session for reuse later. (Saves FPS, Costs RAM) [Requires Plugin Restart]"
	)
	default int groupCount() { return 5; }
}
