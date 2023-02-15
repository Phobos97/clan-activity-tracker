package com.clanactivitytracker;

import com.google.common.collect.Lists;
import com.google.inject.Provides;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.*;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@PluginDescriptor(
		name = "Clan Activity Tracker",
		description = "Track activity of clan members."
)
public class ClanActivityTrackerPlugin extends Plugin {
	private static final String BASE_DIRECTORY = RuneLite.RUNELITE_DIR + "/clan-activity-tracker/";

	private static final String[] HEADERS = { "rsn", "rank", "message count", "last message timestamp", "last seen online"};

	@Provides
	ClanActivityTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ClanActivityTrackerConfig.class);
	}

	@Inject
	private ClanActivityTrackerConfig config;

	@Inject
	private Client client;

	@Subscribe()
	public void onClanChannelChanged(ClanChannelChanged event) throws IOException {
		if (config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.CLAN_CHAT && config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.BOTH){
			return;
		}

		if (event.getClanChannel() == null) {
			return;
		}

		// clanID 0 is your actual clan as apposed to GIM or guest channels
		if (event.getClanId() != 0) {
			return;
		}
		// we need the clan settings to get the rank names
		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings == null){
			return;
		}

		String localRsn = "";
		if (config.rsnSpecificLog()) {
			localRsn = "_" + Objects.requireNonNull(client.getLocalPlayer()).getName();
		}

		String pathname = BASE_DIRECTORY + event.getClanChannel().getName().replaceAll(" ", "_") + localRsn + config.fileSuffix();
		createFile(pathname);

		Iterable<CSVRecord> records = readAll(pathname);

		try {
			BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(pathname),
					StandardOpenOption.WRITE);
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
					.withHeader(HEADERS));

			List<ClanChannelMember> memberList = event.getClanChannel().getMembers();
			List<String> clanRsns = new ArrayList<String>();
			List<String> updatedRsns = new ArrayList<String>();
			for (ClanChannelMember member : memberList) {
				clanRsns.add(cleanRsn(member.getName()));
			}

			for (CSVRecord record : records) {
				// if recorded member is currently online
				if (record.size() == 5) {
					String rsn = record.get(0);
					if (clanRsns.contains(rsn)) {
						int index = clanRsns.indexOf(rsn);
						// update rank and last seen online time
						csvPrinter.printRecord(rsn,
								Objects.requireNonNull(clanSettings.titleForRank(memberList.get(index).getRank())).getName(),
								record.get(2), record.get(3),
								formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
						updatedRsns.add(rsn);
					}
					// if member not currently online, don't change data
					else {
						csvPrinter.printRecord(record);
					}
				}
			}
			// if online player has not been recorded yet create new entry
			for (ClanChannelMember member : memberList) {
				String rsn = cleanRsn(member.getName());
				if (!updatedRsns.contains(rsn)) {
					csvPrinter.printRecord(cleanRsn(member.getName()),
							Objects.requireNonNull(clanSettings.titleForRank(member.getRank())).getName(), 0,
							"-", formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
				}
			}

			csvPrinter.flush();
			csvPrinter.close();

		} catch (IOException IOE) {
			log.debug("Clan Activity Tracker encountered an IO exception, likely since the .CSV file was open.");
		}
	}

	@Subscribe()
	public void onClanMemberJoined(ClanMemberJoined event) throws IOException {
		if (config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.CLAN_CHAT  && config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.BOTH){
			return;
		}

		if (event.getClanChannel() == null) {
			return;
		}

		// we need the clan settings to get the rank names
		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings == null){
			return;
		}

		String localRsn = "";
		if (config.rsnSpecificLog()) {
			localRsn = "_" + Objects.requireNonNull(client.getLocalPlayer()).getName();
		}

		String pathname = BASE_DIRECTORY + event.getClanChannel().getName().replaceAll(" ", "_") + localRsn + config.fileSuffix();
		createFile(pathname);

		Iterable<CSVRecord> records = readAll(pathname);

		try {
			BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(pathname),
					StandardOpenOption.WRITE);
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
					.withHeader(HEADERS));

			String rsn = cleanRsn(event.getClanMember().getName());

			boolean found = false;
			for (CSVRecord record : records) {
				if (record.size() == 5) {
					if (rsn.equals(record.get(0))) {
						// update rank and last seen online time
						csvPrinter.printRecord(record.get(0),
								Objects.requireNonNull(clanSettings.titleForRank(event.getClanMember().getRank())).getName(),
								record.get(2), record.get(3),
								formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
						found = true;
					}
					else {
						csvPrinter.printRecord(record);
					}
				}
			}
			if (!found) {
				csvPrinter.printRecord(rsn,
						Objects.requireNonNull(clanSettings.titleForRank(event.getClanMember().getRank())).getName(), 0,
						"-", formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
			}
			csvPrinter.flush();
			csvPrinter.close();

		} catch (IOException IOE) {
			log.debug("Clan Activity Tracker encountered an IO exception, likely since the .CSV file was open.");
		}
	}

	@Subscribe()
	public void onFriendsChatMemberJoined(FriendsChatMemberJoined event) throws IOException {
		if (config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.FRIEND_CHAT  && config.chatTrackType() != ClanActivityTrackerConfig.ChatTrackType.BOTH){
			return;
		}

		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager == null){
			return;
		}

		String localRsn = "";
		if (config.rsnSpecificLog()) {
			localRsn = "_" + Objects.requireNonNull(client.getLocalPlayer()).getName();
		}

		String pathname = BASE_DIRECTORY + friendsChatManager.getName().replaceAll(" ", "_") + localRsn + config.fileSuffix();
		createFile(pathname);

		Iterable<CSVRecord> records = readAll(pathname);

		try {
			BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(pathname),
					StandardOpenOption.WRITE);
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
					.withHeader(HEADERS));

			String rsn = cleanRsn(event.getMember().getName());

			boolean found = false;
			for (CSVRecord record : records) {
				if (record.size() == 5) {
					if (rsn.equals(record.get(0))) {
						// update rank and last seen online time
						csvPrinter.printRecord(record.get(0),
								event.getMember().getRank(),
								record.get(2), record.get(3),
								formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
						found = true;
					}
					else {
						csvPrinter.printRecord(record);
					}
				}
			}
			if (!found) {
				csvPrinter.printRecord(rsn,
						event.getMember().getRank(), 0,
						"-", formatTimestamp((int) Instant.now().getEpochSecond(), "yyyy-MM-dd HH:mm:ss"));
			}

			csvPrinter.flush();
			csvPrinter.close();

		} catch (IOException IOE) {
			log.debug("Clan Activity Tracker encountered an IO exception, likely since the .CSV file was open.");
		}
	}

	@Subscribe()
	public void onChatMessage(ChatMessage chatMessage) throws IOException {
		List<ChatMessageType> chatTypesToTrack = new ArrayList<>();
		switch (config.chatTrackType()) {
			case CLAN_CHAT:
				chatTypesToTrack.add(ChatMessageType.CLAN_CHAT);
				break;
			case FRIEND_CHAT:
				chatTypesToTrack.add(ChatMessageType.FRIENDSCHAT);
				break;
			case BOTH:
				chatTypesToTrack.add(ChatMessageType.CLAN_CHAT);
				chatTypesToTrack.add(ChatMessageType.FRIENDSCHAT);
				break;
		}


		if (chatTypesToTrack.contains(chatMessage.getType())) {
			String localRsn = "";
			if (config.rsnSpecificLog()) {
				localRsn = "_" + Objects.requireNonNull(client.getLocalPlayer()).getName();
			}

			String pathname = BASE_DIRECTORY + chatMessage.getSender().replaceAll("[  ]", "_") + localRsn + config.fileSuffix();
			createFile(pathname);

			String rsn = cleanRsn(chatMessage.getName());

			Iterable<CSVRecord> records = readAll(pathname);

			try {
				BufferedWriter writer = Files.newBufferedWriter(
						Paths.get(pathname),
						StandardOpenOption.WRITE);
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
						.withHeader(HEADERS));

				for (CSVRecord record : records) {
					if (record.size() == 5) {
						if (record.get("rsn").equals(rsn)) {
							int newcount = Integer.parseInt(record.get("message count")) + 1;
							csvPrinter.printRecord(record.get(0), record.get(1), newcount,
									formatTimestamp(chatMessage.getTimestamp(), "yyyy-MM-dd HH:mm:ss"), record.get(4));
						}
						else {
							csvPrinter.printRecord(record);
						}
					}
				}

				csvPrinter.flush();
				csvPrinter.close();
			} catch (IOException IOE) {
				log.debug("Clan Activity Tracker encountered an IO exception, likely since the .CSV file was open.");
			}
		}
	}

	public Iterable<CSVRecord> readAll(String pathname) throws IOException {
		Reader in = new FileReader(pathname);
		return CSVFormat.DEFAULT
				.withHeader(HEADERS)
				.withFirstRecordAsHeader()
				.parse(in);
	}

	public String cleanRsn(String inputString) {
		String cleanString = inputString.replace(" ", " ");
		cleanString = cleanString.replaceAll("(<.+>)", "");
		return cleanString;
	}

	public void createFile(String Pathname) throws IOException {
		File basedir = new File(BASE_DIRECTORY);
		if (!basedir.exists()){
			basedir.mkdir();
		}

		File file = new File(Pathname);
		if (!file.exists()) {
			FileWriter outputfile = new FileWriter(file);
			try (CSVPrinter printer = new CSVPrinter(outputfile, CSVFormat.DEFAULT
					.withHeader(HEADERS))) {
				;
			}
			;
		}
	}

	public String formatTimestamp(int timestamp, String pattern) {
		DateTimeFormatter dateFormatter =
				DateTimeFormatter.ofPattern(pattern);
		return Instant.ofEpochSecond(timestamp)
				.atZone(ZoneId.of("UTC"))
				.format(dateFormatter);
	}

}
