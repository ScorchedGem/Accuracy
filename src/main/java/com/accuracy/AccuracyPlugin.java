package com.accuracy;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.swing.*;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Beast {
    public String name = "Invalid by any other name would sound as sweet";
    public int Defense_level = -1;
    public int Ranged_level = -1;
    public int Magic_level = -1;
    public int Attack_level = -1;
    public int id = -1;
    public int Strength_level = -1;
}

class AnswerWorker extends SwingWorker<Void, Integer>
{
    public int id;
    public int savedSpace = 0;
    public URL lUrl;
    public String contents;

    AnswerWorker(int nId, int sSpot, URL nUrl){ id = nId; savedSpace = sSpot; lUrl = nUrl; }

    protected Void doInBackground() throws Exception {
        requestNpc();
        return null;
    }

    private Void requestNpc()
    {
        try {
            InputStream is = lUrl.openStream();
            contents = new String(is.readAllBytes());
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}


@PluginDescriptor(name = "Accuracy", description = "Never tell me the odds", enabledByDefault = true)
public class AccuracyPlugin extends Plugin {
    static final String URL_START = "https://oldschool.runescape.wiki/api.php?action=ask&query=[[NPC%20ID::";
    static final String URL_END = "]]%7C%3FAttack%20level%7C%3FStrength%20level%7C%3FDefence%20level%7C%3FMagic%20level%7C%3FRanged%20level&format=json";

    @Inject private Client client;
    @Inject private AccuracyConfig config;

    private Beast[] beastRefs;
    private int indexer = 0;

    private List<AnswerWorker> workers;

    @Override
    protected void startUp() throws Exception {
        workers = new ArrayList<>();
        beastRefs = new Beast[config.groupCount()];
        for (int i = 0; i < config.groupCount(); i++) {
            beastRefs[i] = new Beast();
        }

        //constructWorker(3313);
        //constructWorker(2837);
        //constructWorker(3295);
        //constructWorker(3260);
        //constructWorker(2837);
        //constructWorker(2830);
    }

    private void constructWorker(int nId) throws Exception {
        AnswerWorker worker = new AnswerWorker(nId, indexer, buildUrl(nId));
        workers.add(worker);

        if (worker.lUrl != null)
        {
            // Initialise values for worker
            worker.execute();

            // Iterate index for next worker request
            indexer = indexer + 1 >= beastRefs.length ? 0 : indexer;
        }
    }

    @Subscribe public void onGameTick(GameTick tick){
        for (var worker : workers)
        {
            if (worker.isDone()){
                buildNpcStats(worker.contents , worker.id, worker.savedSpace);
                workers.remove(worker);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked optionClicked) throws Exception {
        if (client.getLocalPlayer() == null) return;
        if (optionClicked.getMenuAction() == MenuAction.EXAMINE_NPC) {
            NPC npc = optionClicked.getMenuEntry().getNpc();

            if (npc != null) {
                NPCComposition nc = npc.getTransformedComposition();
                constructWorker(nc.getId());
            }
        }
    }

    private void buildNpcStats(String incoming, int id, int index) {
        String results = incoming.substring(incoming.indexOf("results"));

        try {
            // NAME
            int b = results.indexOf("{") + 2; int e = results.indexOf(":", b) - 1;
            String nameTemp = results.substring(b, e);

            if (nameTemp.contains("#"))
            { nameTemp = nameTemp.replace("#", " ("); nameTemp += ")"; }

            beastRefs[index].name = nameTemp;
            beastRefs[index].id = id;

            // ATTACK LEVEL
            b = results.indexOf("[") + 1; e = results.indexOf("]", b);
            if (b != -1 && b != e) beastRefs[index].Attack_level = Integer.parseInt(results.substring(b, e));
            else beastRefs[index].Attack_level = -100;

            // STRENGTH LEVEL
            b = results.indexOf("[", e) + 1; e = results.indexOf("]", b);
            if (b != -1 && b != e) beastRefs[index].Strength_level = Integer.parseInt(results.substring(b, e));

            // DEFENSE LEVEL
            b = results.indexOf("[", e) + 1; e = results.indexOf("]", b);
            if (b != -1 && b != e) beastRefs[index].Defense_level = Integer.parseInt(results.substring(b, e));

            // MAGIC LEVEL
            b = results.indexOf("[", e) + 1; e = results.indexOf("]", b);
            if (b != -1 && b != e) beastRefs[index].Magic_level = Integer.parseInt(results.substring(b, e));

            // RANGED LEVEL
            b = results.indexOf("[", e) + 1; e = results.indexOf("]", b);
            if (b != -1 && b != e) beastRefs[index].Ranged_level = Integer.parseInt(results.substring(b, e));

            printStats(index);
            return;
        }
        catch (Exception x) {
            System.out.println("It broke, at iter " + index + " using source " + results);
            return;
        }
    }

    private URL buildUrl(int id) throws Exception
    {
        int storedPos = checkStored(id);
        if (storedPos == -1)
        {
            // Reserve the position for thread
            beastRefs[indexer].id = id;

            // Generate json link
            return new URL(URL_START + id + URL_END);
        }
        else
        {
            // Print out our locally stored version
            printStats(storedPos);
            return null;
        }
    }

    public void printStats(int pos)
    {
        String stats = "";
        if (beastRefs[pos].Attack_level > -100)
        {
            stats = beastRefs[pos].name +
                    " | Strength: " + beastRefs[pos].Strength_level +
                    " | Defense: " + beastRefs[pos].Defense_level +
                    " | Attack: " + beastRefs[pos].Attack_level +
                    " | Ranged: " + beastRefs[pos].Ranged_level +
                    " | Magic: " + beastRefs[pos].Magic_level;
        }
        else
        {
            stats = beastRefs[pos].name + " | Description: Would prefer if you didn't check their stats";
        }

        if (client.getLocalPlayer() != null) client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", stats, null);
    }

    private int checkStored(int newId)
    {
        for (int i = 0; i < beastRefs.length; i++)
        { if (beastRefs[i].id == newId) return i; }
        return -1;
    }

    @Provides
    AccuracyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AccuracyConfig.class);
    }
}

