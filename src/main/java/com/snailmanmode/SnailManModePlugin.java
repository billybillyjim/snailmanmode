package com.snailmanmode;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;


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
	public static int ticksSinceLastMove = 0;
	public static Model currentSnailModel;
	public static int previousRotation = 0;

	public static int currentFloor = 0;
	public static int playerLastTickPlane;
	public static boolean changedPlanes;
	public static boolean lostGame = false;
	public static ArrayList planeChangeLocations = new ArrayList();
	public static List<WorldPoint> path = new ArrayList();
	public static WorldPoint playerLastPosition;
	public static boolean recalculatePath = false;
	private int snailmanIconOffset = -1;
	private static boolean firstGameTick = true;
	//public static int speed = 1;
	//public static boolean diagonalMovement = true;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SnailManModeConfig config;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SnailManModeOverlay overlay;

	@Inject
	private SnailManModeLoseOverlay loseOverlay;

	@Inject
	private ChatMessageManager chatMessageManager;


	@Subscribe
	public void onSessionOpen(SessionOpen event)
	{
		if(snailObject == null){
			clientThread.invoke(this::createSnailObject);
		}
		if(SNAIL_LOCATION_ICON == null){
			LoadSnailImage();
		}
		snailXPosition = Integer.parseInt(configManager.getConfiguration("Snail", "xPos"));
		snailYPosition = Integer.parseInt(configManager.getConfiguration("Snail", "yPos"));
		lostGame = Boolean.parseBoolean(configManager.getConfiguration("Snail", "lost"));
		log.debug("Setting Snail pos:" + snailXPosition + "," + snailYPosition);
		worldMapPointManager.add(WorldMapPoint.builder()
				.worldPoint(new WorldPoint(snailXPosition, snailYPosition,0))
				.image(SNAIL_LOCATION_ICON)
				.tooltip("The snail is here.")
				.build());
	}

	public void LoadSnailImage(){

		try
		{
			SNAIL_LOCATION_ICON = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
			BufferedImage snailCurrentLocation = ImageUtil.loadImageResource(SnailManModePlugin.class, "/snail.png");
			if(snailCurrentLocation != null){
				SNAIL_LOCATION_ICON.getGraphics().drawImage(snailCurrentLocation, 1, 1, null);
			}
		}
		catch(Exception e){
			SNAIL_LOCATION_ICON = null;
		}

	}

	public void createSnailObject(){

		WorldPoint wp = new WorldPoint(snailXPosition, snailYPosition, currentFloor);
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		if(lp != null){
			snailObject = client.createRuneLiteObject();
			if(currentSnailModel == null){
				currentSnailModel = client.loadModel(4108);
			}

			snailObject.setModel(currentSnailModel);
			snailObject.setAnimation(client.loadAnimation(1274));
			snailObject.setShouldLoop(true);
			snailObject.setLocation(lp, currentFloor);
			snailObject.setActive(true);
			previousRotation = 0;
			final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
			int xDist = Math.abs(playerPos.getX() - snailXPosition);
			int yDist = Math.abs(playerPos.getY() - snailYPosition);
			boolean xMove = xDist > yDist;
			if(xMove){
				if(playerPos.getX() - snailXPosition > 0){
					currentSnailModel.rotateY270Ccw();
					previousRotation = 270;
				}
				else{
					currentSnailModel.rotateY90Ccw();
					previousRotation = 90;
				}
			}
			else{
				if(playerPos.getY() - snailYPosition > 0){
				}
				else{
					currentSnailModel.rotateY180Ccw();
					previousRotation = 180;
				}
			}
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(loseOverlay);
		configManager.setConfiguration("Snail", "lost", false);
		snailXPosition = 3400;
		snailYPosition = 3400;
		currentFloor = 0;
		lostGame = false;
		try
		{

			snailXPosition = Integer.parseInt(configManager.getConfiguration("Snail", "xPos"));
			snailYPosition = Integer.parseInt(configManager.getConfiguration("Snail", "yPos"));
			lostGame = Boolean.parseBoolean(configManager.getConfiguration("Snail", "lost"));
			currentFloor = Integer.parseInt(configManager.getConfiguration("Snail", "currentFloor"));
			LoadWorldPointListFromString(configManager.getConfiguration("Snail", "worldPoints"));
		}
		catch(Exception e){

		}
		log.debug("Setting Snail pos:" + snailXPosition + "," + snailYPosition);
		loadResources();
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN) {

					setChatboxName(getNameChatbox());

			}
		});
	}
	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(loseOverlay);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if(lostGame){
			return;
		}
		if(firstGameTick){
			firstGameTick = false;
			return;
		}
		if(SNAIL_LOCATION_ICON == null){
			LoadSnailImage();
		}
		final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		if (playerPos == null)
		{
			//log.debug("Player pos was null");
			return;
		}
		if(changedPlanes && planeChangeLocations.size() > 0){
			WorldPoint pcl = (WorldPoint)planeChangeLocations.get(0);
			if(pcl != null){
				moveSnailTowardPoint(pcl);
			}
			else{
				log.debug("PCL was null!");
				moveSnailTowardPoint(playerPos);
			}

		}
		else{

			if(playerPos.getX() == snailXPosition &&
			   playerPos.getY() == snailYPosition)
			{
				lostGame = true;
				configManager.setConfiguration("Snail", "lost", true);
				log.debug("Game Over");
			}
			moveSnailTowardPoint(playerPos);
		}

		final LocalPoint playerPosLocal = LocalPoint.fromWorld(client, playerPos);
		if (playerPosLocal == null)
		{
			//log.debug("Player local pos was null");
			return;
		}
		if(currentMapPoint != null){
			//log.debug("Map point not null");
			worldMapPointManager.remove(currentMapPoint);
		}
		if(SNAIL_LOCATION_ICON != null){
			currentMapPoint = WorldMapPoint.builder()
					.worldPoint(new WorldPoint(snailXPosition, snailYPosition,currentFloor))
					.image(SNAIL_LOCATION_ICON)
					.tooltip("The snail is here.")
					.build();
			worldMapPointManager.add(currentMapPoint);

		}
		if(snailObject != null){
			WorldPoint wp = new WorldPoint(snailXPosition, snailYPosition, currentFloor);
			LocalPoint lp = LocalPoint.fromWorld(client, wp);
			try
			{
				snailObject.setLocation(lp, currentFloor);
				Point minimapLocation = new Point(wp.getX(), wp.getY());
				Color color = Color.RED;
				OverlayUtil.renderMinimapLocation(SNAIL_LOCATION_ICON.createGraphics(), minimapLocation, color);
			}
			catch(Exception e){
				log.debug(e.getMessage());
				log.debug("Failed to update snail position");
			}

		}
		else{
			clientThread.invoke(this::createSnailObject);
		}

		if(playerLastTickPlane != playerPos.getPlane()){
			changedPlanes = true;
			planeChangeLocations.add(playerLastPosition);

		}
		else if(currentFloor == playerPos.getPlane()){
			changedPlanes = false;
		}
		if(playerPos.getPlane() == currentFloor){
			planeChangeLocations.clear();
		}
		playerLastTickPlane = playerPos.getPlane();
		playerLastPosition = playerPos;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::createSnailObject);
			recalculatePath = true;
		}
	}

	private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final double borderWidth, final Color fillColor)
	{
		if (dest == null)
		{
			return;
		}

		final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

		if (poly == null)
		{
			return;
		}

		OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
	}

	public void moveSnailTowardPoint(WorldPoint point)
	{
		ticksSinceLastMove++;
		int xDist = Math.abs(point.getX() - snailXPosition);
		int yDist = Math.abs(point.getY() - snailYPosition);
		boolean moveX = true;

		if(!config.diagonalMovement()){
			moveX = xDist > yDist;
		}
		if(recalculatePath){
			calculatePath(LocalPoint.fromWorld(client, point));
			recalculatePath = false;
		}
		else if(ticksSinceLastMove % config.speed() == 0){

			if(path.size() > 1){

				WorldPoint wp = path.get(1);

				snailXPosition = wp.getX();
				snailYPosition = wp.getY();
				ticksSinceLastMove = 0;
				path.remove(0);
			}
			else{
				double dist = Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
				log.debug("Distance: {}, {}, {}", xDist, yDist, dist);
				int moveTileCount = (int)Math.max((dist / 1000) + 1, 1);
				int x = moveTileCount;
				int ticks = 0;
				double tileDist = dist;
				while(tileDist > 0){
					ticks++;
					tileDist -= x;
					x = (int)(tileDist / 1000) + 1;
				}
				log.debug("Ticks to you: {}", ticks);
				if(point.getX() > snailXPosition && (moveX || config.diagonalMovement())){
					snailXPosition += moveTileCount;
					ticksSinceLastMove = 0;
				}
				else if(point.getX() < snailXPosition && (moveX || config.diagonalMovement())){
					snailXPosition -= moveTileCount;
					ticksSinceLastMove = 0;
				}
				if(point.getY() > snailYPosition && (!moveX || config.diagonalMovement())){
					snailYPosition += moveTileCount;
					ticksSinceLastMove = 0;
				}
				else if(point.getY() < snailYPosition && (!moveX || config.diagonalMovement())){
					snailYPosition -= moveTileCount;
					ticksSinceLastMove = 0;
				}
			}
			if(ticksSinceLastMove == 0){
				LocalPoint lp = LocalPoint.fromWorld(client, point);
				if(lp != null){
					calculatePath(lp);
				}
			}

			updateRotation(point);

			if(snailYPosition == point.getY() && snailXPosition == point.getX()){
				if(planeChangeLocations.size() > 0){
					if(planeChangeLocations.size() > 1){
						currentFloor = ((WorldPoint)planeChangeLocations.get(1)).getPlane();
					}
					else{
						currentFloor = client.getLocalPlayer().getWorldLocation().getPlane();
					}

					planeChangeLocations.remove(0);
					clientThread.invoke(this::createSnailObject);
				}
				else
				{
					lostGame = true;
					configManager.setConfiguration("Snail", "lost", true);
					log.debug("Game Over");
				}
			}
		}
		configManager.setConfiguration("Snail", "xPos", snailXPosition);
		configManager.setConfiguration("Snail", "yPos", snailYPosition);
		configManager.setConfiguration("Snail", "currentFloor", currentFloor);
		configManager.setConfiguration("Snail", "worldPoints", WorldPointListToString());
		log.debug("Snail Pos: {}, {}, {}", snailXPosition, snailYPosition, currentFloor);
		log.debug("Time to pos: {}", Math.sqrt(Math.pow(point.getX() - snailXPosition, 2) + Math.pow(point.getY() - snailYPosition, 2)));
	}
	public void updateRotation(WorldPoint targetPos){
		if(currentSnailModel == null){
			return;
		}
		if(previousRotation == 90){
			currentSnailModel.rotateY270Ccw();
		}
		else if(previousRotation == 180){
			currentSnailModel.rotateY180Ccw();
		}
		else if(previousRotation == 270){
			currentSnailModel.rotateY90Ccw();
		}

		int xDist = snailXPosition - targetPos.getX();
		int yDist = snailYPosition - targetPos.getY();

		if(Math.abs(xDist) > Math.abs(yDist)){
			if(xDist > 0){

			}
			else{
				currentSnailModel.rotateY180Ccw();
				previousRotation = 180;
			}
		}
		else{
			if(yDist > 0){
				currentSnailModel.rotateY270Ccw();
				previousRotation = 270;
			}
			else{
				currentSnailModel.rotateY90Ccw();
				previousRotation = 90;
			}
		}
	}

	public void calculatePath(LocalPoint point){

		LocalPoint startPos = LocalPoint.fromWorld(client, snailXPosition, snailYPosition);
		if(startPos == null || point == null){
			log.debug("No Starting Position or Point was null.");
			return;
		}

		CollisionData[] data = client.getCollisionMaps();
		CollisionData floorData = data[currentFloor];
		WorldPoint worldPoint = new WorldPoint(snailXPosition, snailYPosition, currentFloor);

		WorldPoint goal = WorldPoint.fromLocal(client, point);
		path = new ArrayList();
		ArrayDeque frontier = new ArrayDeque();

		frontier.add(new WorldPointPath(worldPoint, path));
		int[][] flags = floorData.getFlags();

		boolean[][] visited = new boolean[255][255];
		int offset = 105;
		int maxIterations = 65100;
		int iterations = 0;
		while(!frontier.isEmpty())
		{
			WorldPointPath current = (WorldPointPath)frontier.removeFirst();
			val currentPath = (ArrayList) ((ArrayList) current.path).clone();
			currentPath.add(current.point);

			if(current.isEqual(goal)){
				path = currentPath;
				break;
			}
			int count = 0;

			for (LocalPoint lp:
				 getNeighbors(current.x, current.y, flags)) {
				if(!visited[lp.getSceneX() + offset][lp.getSceneY() + offset]){
					frontier.add(new WorldPointPath(WorldPoint.fromLocal(client, lp), currentPath));
					visited[lp.getSceneX() + offset][lp.getSceneY() + offset] = true;
					count++;
				}

			}

			iterations++;
			if(iterations > maxIterations){
				break;
			}
		}


		if(data == null){
			return;
		}

	}

	public static List<WorldPoint> getDirectPath(){
		ArrayList directPath = new ArrayList();
		WorldPoint p = new WorldPoint(snailXPosition, snailYPosition, currentFloor);
		WorldPoint player = playerLastPosition;

		if(planeChangeLocations.size() > 0){
			if(planeChangeLocations.get(0) != null){
				player = (WorldPoint)planeChangeLocations.get(0);
			}

		}

		int x = snailXPosition;
		int y = snailYPosition;
		while(!worldPointsAreEqual(p, player, false)){
			if(player.getX() > p.getX()){
				x++;
			}
			else if(player.getX() < p.getX()){
				x--;
			}
			if(player.getY() > p.getY()){
				y++;
			}
			else if(player.getY() < p.getY()){
				y--;
			}
			p = new WorldPoint(x,y, currentFloor);
			directPath.add(p);
		}
		return directPath;
	}

	public static boolean worldPointsAreEqual(WorldPoint wp1, WorldPoint wp2, boolean plane){
		if(wp1 == null || wp2 == null){
			log.debug("1:{}, 2:{}", wp1 == null, wp2 == null);
		}
		if(plane){
			return wp1.getX() == wp2.getX() && wp1.getY() == wp2.getY() && wp1.getPlane() == wp2.getPlane();
		}
		return wp1.getX() == wp2.getX() && wp1.getY() == wp2.getY();
	}

	public class WorldPointPath {
		public int x;
		public int y;
		public WorldPoint point;
		public List<WorldPoint> path;
		public WorldPointPath(WorldPoint wp, List<WorldPoint> p){
			x = wp.getX();
			y = wp.getY();
			point = wp;
			path = p;
		}

		public boolean isEqual(WorldPoint p)
		{
			return p.getX() == x && p.getY() == y && point.getPlane() == p.getPlane();

		}


		@Override
		public String toString() {
			String output = "";
			for(WorldPoint wp:
			    path){
				output += wp.toString();
			}
			return "WorldPointPath{" +
					"point=" + x + "," + y +
					", path=" + output +
					'}';
		}
	}

	public List<LocalPoint> getNeighbors(int worldX, int worldY, int[][] flags){
		List<LocalPoint> neighbors = new ArrayList();
		int x = worldX;
		int y = worldY;
		LocalPoint lp = LocalPoint.fromWorld(client,x, y);
		int flag = flags[lp.getSceneX()][lp.getSceneY()];
		if((flag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0){
			return neighbors;
		}
		if((flag & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL) != 0){
			return neighbors;
		}
		else if((flag & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0){
			return neighbors;
		}
		LocalPoint N = LocalPoint.fromWorld(client,x, y + 1);
		LocalPoint S = LocalPoint.fromWorld(client,x, y - 1);
		LocalPoint E = LocalPoint.fromWorld(client,x + 1, y);
		LocalPoint W = LocalPoint.fromWorld(client,x - 1, y);

		LocalPoint NE = LocalPoint.fromWorld(client,x + 1, y + 1);
		LocalPoint SW = LocalPoint.fromWorld(client,x - 1, y - 1);
		LocalPoint SE = LocalPoint.fromWorld(client,x + 1, y - 1);
		LocalPoint NW = LocalPoint.fromWorld(client,x - 1, y + 1);
		boolean NBlocked = false;
		boolean WBlocked = false;
		boolean SBlocked = false;
		boolean EBlocked = false;
		if(W != null){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_WEST) != flag &&
					(flag | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST) != flag){

				neighbors.add(W);
			}
			else{
				WBlocked = true;
			}
		}
		if(S != null){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != flag &&
					(flag | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH) != flag){
				neighbors.add(S);
			}else{
				SBlocked = true;
			}
		}
		if(N != null){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != flag &&
					(flag | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH) != flag){
				neighbors.add(N);
			}else{
				NBlocked = true;
			}
		}
		if(E != null){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_EAST) != flag &&
					(flag | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST) != flag){
				neighbors.add(E);
			}else{
				EBlocked = true;
			}
		}


		if(NW != null && !NBlocked && !WBlocked){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != flag){
				neighbors.add(NW);
			}
		}
		if(SE != null && !SBlocked && !EBlocked){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != flag){
				neighbors.add(SE);
			}

		}
		if(NE != null && !NBlocked && !EBlocked){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != flag){
				neighbors.add(NE);
			}
		}
		if(SW != null && !SBlocked && !WBlocked){
			if((flag | CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != flag){
				neighbors.add(SW);
			}
		}
		return neighbors;
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent scriptCallbackEvent)
	{
		if (scriptCallbackEvent.getEventName().equals("setChatboxInput"))
		{
			setChatboxName(getNameChatbox());
		}
	}
	private void setChatboxName(String name)
	{
		Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatboxInput != null)
		{
			String text = chatboxInput.getText();
			int idx = text.indexOf(':');
			if (idx != -1)
			{
				String newText = name + text.substring(idx);
				chatboxInput.setText(newText);
			}
		}
	}
	private String getNameChatbox()
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			return getNameWithIcon(snailmanIconOffset, player.getName());
		}
		return null;
	}

	private static String getNameWithIcon(int iconIndex, String name)
	{
		String icon = "<img=" + iconIndex + ">";
		return icon + name;
	}

	public String WorldPointListToString(){
		StringBuilder w = new StringBuilder();
		for(Object pcl:planeChangeLocations){
			WorldPoint wp = (WorldPoint)pcl;
			if(wp != null){
				w.append(wp.getX() + "," + wp.getY() + "," + wp.getPlane() + ";");
			}

		}
		return w.toString();
	}

	public void LoadWorldPointListFromString(String data){
		String[] lines = data.split(";");
		for(String line:lines){
			String[] d = line.split(",");
			int x = Integer.parseInt(d[0]);
			int y = Integer.parseInt(d[1]);
			int p = Integer.parseInt(d[2]);
			planeChangeLocations.add(new WorldPoint(x,y,p));
		}
	}

	private void loadResources()
	{
		final IndexedSprite[] modIcons = client.getModIcons();


		BufferedImage image = ImageUtil.loadImageResource(getClass(), "/snailman-icon.png");
		IndexedSprite indexedSprite = ImageUtil.getImageIndexedSprite(image, client);

		snailmanIconOffset = modIcons.length;

		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 1);
		newModIcons[newModIcons.length - 1] = indexedSprite;

		client.setModIcons(newModIcons);
	}

	@Provides
    SnailManModeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SnailManModeConfig.class);
	}
}
