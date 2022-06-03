package com.snailmanmode;

import com.google.inject.Provides;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.GameState;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Snail Man Mode",
	description = "Spawns an invincible snail who is coming to kill you."
)
public class SnailManModePlugin extends Plugin
{

	public static RuneLiteObject snailObject;
	public static int snailXPosition = 3400;
	public static int snailYPosition = 3400;
	public static WorldMapPoint currentMapPoint;
	public static BufferedImage SNAIL_LOCATION_ICON;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SnailManModeConfig config;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	public static void spawnSnail(){

		snailObject.setActive(true);
		SNAIL_LOCATION_ICON = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
		BufferedImage snailCurrentLocation = ImageUtil.loadImageResource(SnailManModePlugin.class, "/snail.png");
		SNAIL_LOCATION_ICON.getGraphics().drawImage(snailCurrentLocation, 1, 1, null);

	}

	@Override
	protected void startUp() throws Exception
	{
		snailObject = client.createRuneLiteObject();
		snailObject.setModel(client.loadModel(2634));
		snailObject.setAnimation(client.loadAnimation(1274));
		snailObject.setShouldLoop(true);

		WorldPoint wp = new WorldPoint(snailXPosition, snailYPosition, 0);
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		snailObject.setLocation(lp, client.getPlane());

		worldMapPointManager.add(WorldMapPoint.builder()
				.worldPoint(new WorldPoint(3214, 3419,0))
				.image(SNAIL_LOCATION_ICON)
				.tooltip("The snail is here.")
				.build());
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		if (playerPos == null)
		{
			return;
		}
		moveSnailTowardPoint(playerPos);
		final LocalPoint playerPosLocal = LocalPoint.fromWorld(client, playerPos);
		if (playerPosLocal == null)
		{
			return;
		}
		if(currentMapPoint != null){
			worldMapPointManager.remove(currentMapPoint);
		}
		currentMapPoint = WorldMapPoint.builder()
				.worldPoint(new WorldPoint(snailXPosition, snailYPosition,0))
				.image(SNAIL_LOCATION_ICON)
				.tooltip("The snail is here.")
				.build();
		worldMapPointManager.add(currentMapPoint);
		log.debug("Snail moved {} , {}", snailXPosition, snailYPosition);
	}

	public void moveSnailTowardPoint(WorldPoint point)
	{
		if(point.getX() > snailXPosition){
			snailXPosition++;
		}
		else if(point.getX() < snailXPosition){
			snailXPosition--;
		}
		if(point.getY() > snailYPosition){
			snailYPosition++;
		}
		else if(point.getY() < snailYPosition){
			snailYPosition--;
		}
	}

	@Provides
    SnailManModeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SnailManModeConfig.class);
	}
}
