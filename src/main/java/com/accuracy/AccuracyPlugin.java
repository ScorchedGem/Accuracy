package com.accuracy;

import com.google.gson.*;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

class Beast {
    public String name = "Invalid by any other name would sound as sweet";
    public int level = -1;
    public int defense = -1;
    public int ranged = -1;
    public int magic = -1;
    public int attack = -1;
    public int id = -1;
}

@PluginDescriptor(
        name = "Accuracy",
        description = "Never tell me the odds",
        enabledByDefault = true
)

public class AccuracyPlugin extends Plugin {
	private static final String CHECK_TARGET = ColorUtil.wrapWithColorTag("Check Target", JagexColors.MENU_TARGET);

    @Inject
    private Client client;

    @Inject
    private AccuracyConfig config;

    @Getter(AccessLevel.PACKAGE)
    private TileObject interactedTile;

    private Beast beastRef;

    @Override
    protected void startUp() throws Exception
    {
        String path = "http://services.runescape.com/m=itemdb_rs/bestiary/beastData.json?beastid=89";

        // Request URL and query for redirects
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        connection.setInstanceFollowRedirects(false);
        System.out.println("Request Url... " + url);

        // Check for redirects
        int responseCode = connection.getResponseCode();
        System.out.println("Redirect Url... " + connection.getHeaderField("Location"));

        // Handle redirect
        if (responseCode == 302 || responseCode == 301){
            path = connection.getHeaderField("Location");
            connection = (HttpURLConnection) new URL(path).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
        }

        // Get JSON file from path
        InputStream is = new URL(path).openStream();
        connection.disconnect();
        String contents = new String(is.readAllBytes());
        System.out.println(contents);

        Gson gson = new Gson();
        beastRef = gson.fromJson(contents, Beast.class);
        System.out.println("Name: " + beastRef.name + " | Level : " + beastRef.level + " | Defense: " + beastRef.defense + " | Attack: " + beastRef.attack + " | Ranged: " + beastRef.ranged + " | Magic: " + beastRef.magic);
    }

    @Override
    protected void shutDown() throws Exception
    {
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws Exception {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Never tell me the odds...", null);
            String path = "http://services.runescape.com/m=itemdb_rs/bestiary/beastData.json?beastid=89";
            URL url = new URL(path);
            URLConnection req = url.openConnection();
            req.connect();
            beastRef = new GsonBuilder().create().fromJson(new InputStreamReader((InputStream) req.getContent()), Beast.class);

            System.out.print(beastRef.id);

/*            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(new InputStreamReader((InputStream) req.getContent()));
            JsonObject obj = root.getAsJsonObject();
            int def = obj.get("defense").getAsInt();

            System.out.print(readUrl(path));
            System.out.print(def);*/

/*            String json = readUrl(path);
            Gson gson = new Gson();
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);

            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", obj.getAsString(), null);*/

            //client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "File Read", null);
            //client.addChatMessage(ChatMessageType.GAMEMESSAGE, beastRef.name, "Level : " + beastRef.level + " Defense: " + beastRef.defense + " Attack: " + beastRef.attack + " Ranged: " + beastRef.ranged + " Magic: " + beastRef.magic, null);
        }
        else if (gameStateChanged.getGameState() == GameState.LOADING) {
            beastRef = new Beast();
        }
    }

    @Subscribe
	public void onMenuOptionClicked(MenuOptionClicked optionClicked) throws Exception {
		if (client.getLocalPlayer() == null) return;

		if (optionClicked.getMenuAction() == MenuAction.NPC_SECOND_OPTION)
		{
            int id = optionClicked.getId();
			NPC tempRef = findNpc(id);
            id = tempRef.getId();

			if (tempRef != null && beastRef.id != id)
            {
                String path = "http://services.runescape.com/m=itemdb_rs/bestiary/beastData.json?beastid=" + id;
                String json = readUrl(path);
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", json, null);

                Gson gson = new Gson();
                beastRef = gson.fromJson(json, Beast.class);
                //client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "File Read", null);
				//client.addChatMessage(ChatMessageType.GAMEMESSAGE, beastRef.name, "Level : " + beastRef.level + " Defense: " + beastRef.defense + " Attack: " + beastRef.attack + " Ranged: " + beastRef.ranged + " Magic: " + beastRef.magic, null);
			}
            else
            {
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No NPC found with value " + id, null);
			}
		}
	}

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    NPC findNpc(int id) {
        return client.getCachedNPCs()[id];
    }

    @Provides
    AccuracyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AccuracyConfig.class);
    }
}

