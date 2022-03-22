package com.clanactivitytracker;

import net.runelite.api.ChatMessageType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clanactivitytracker")
public interface ClanActivityTrackerConfig extends Config
{
	enum ChatTrackType {
		CLAN_CHAT,
		FRIEND_CHAT
	}

	@ConfigItem(
		keyName = "filesuffix",
		name = "File suffix",
		description = "Last part of filename to save clan log to. First part of the file name will be the name of your clan/ friend chat."
	)
	default String fileSuffix()
	{
		return "_Log1.csv";
	}


	@ConfigItem(
			keyName = "chattracktype",
			name = "Chat Type",
			description = "Choice to track either Clan or Friend chat."
	)
	default ChatTrackType chatTrackType()
	{
		return ChatTrackType.CLAN_CHAT;
	}
}
