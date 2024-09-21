package com.CAT;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CAT")
public interface CATConfig extends Config
{
	enum ChatTrackType {
		CLAN_CHAT,
		FRIEND_CHAT,
		BOTH
	}

	@ConfigItem(
		keyName = "filesuffix",
		name = "File Suffix",
		description = "Last part of filename to save clan log to. First part of the file name will be the name of your clan/friend chat."
	)
	default String fileSuffix() { return "_log.csv"; }

	@ConfigItem(
			keyName = "chattracktype",
			name = "Chat Type",
			description = "Choice to track either clan or friends chat."
	)
	default ChatTrackType chatTrackType()
	{
		return ChatTrackType.CLAN_CHAT;
	}

	@ConfigItem(
			keyName = "rsnspecificlog",
			name = "Use RSN in filename",
			description = "Advised for playing on multiple clients. Stops double counting of messages and some more" +
					" inconsistencies when multi-logging. Each account will generate its own log file."
	)
	default boolean rsnSpecificLog() { return true; }
}
