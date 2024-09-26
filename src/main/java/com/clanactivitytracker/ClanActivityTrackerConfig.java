package com.clanactivitytracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clanactivitytracker")
public interface ClanActivityTrackerConfig extends Config
{
	enum ChatTrackType {
		CLAN_CHAT,
		FRIEND_CHAT,
		BOTH
	}

	@ConfigItem(
		keyName = "filesuffix",
		name = "File suffix",
		description = "Last part of filename to save clan log to. First part of the file name will be the name of your clan/ friend chat."
	)
	default String fileSuffix() { return "_Log1.csv"; }

	@ConfigItem(
			keyName = "chattracktype",
			name = "Chat Type",
			description = "Choice to track either Clan or Friend chat."
	)
	default ChatTrackType chatTrackType()
	{
		return ChatTrackType.CLAN_CHAT;
	}

	@ConfigItem(
			keyName = "rsnspecificlog",
			name = "Use rsn in filename",
			description = "Advised for playing on multiple clients. Stops double counting of messages and some more" +
					" inconsistencies when multilogging. Each account will generate its own log file."
	)
	default boolean rsnSpecificLog() { return true; }
}
