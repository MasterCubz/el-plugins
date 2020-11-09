/*
 * Copyright (c) 2017, Tyler <https://github.com/tylerthardy>
 * Copyright (c) 2020, dekvall <https://github.com/dekvall>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.ElHerbiboar;

import com.google.common.collect.Iterables;
import com.google.inject.Provides;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import static net.runelite.api.ObjectID.DRIFTWOOD_30523;
import static net.runelite.api.ObjectID.MUSHROOM_30520;
import static net.runelite.api.ObjectID.ROCK_30519;
import static net.runelite.api.ObjectID.ROCK_30521;
import static net.runelite.api.ObjectID.ROCK_30522;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.util.Text;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;
import static net.runelite.client.plugins.ElHerbiboar.ElHerbiboarState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Herbiboar",
	description = "Does herbiboar for you.",
	tags = {"herblore", "hunter", "skilling", "overlay"},
	type = PluginType.SKILLING
)
@Slf4j
@Getter
public class ElHerbiboarPlugin extends Plugin
{
	private static final List<WorldPoint> END_LOCATIONS = List.of(
		new WorldPoint(3693, 3798, 0),
		new WorldPoint(3702, 3808, 0),
		new WorldPoint(3703, 3826, 0),
		new WorldPoint(3710, 3881, 0),
		new WorldPoint(3700, 3877, 0),
		new WorldPoint(3715, 3840, 0),
		new WorldPoint(3751, 3849, 0),
		new WorldPoint(3685, 3869, 0),
		new WorldPoint(3681, 3863, 0)
	);

	private static final Set<Integer> START_OBJECT_IDS = Set.of(
		ROCK_30519,
		MUSHROOM_30520,
		ROCK_30521,
		ROCK_30522,
		DRIFTWOOD_30523
	);

	private static final List<Integer> HERBIBOAR_REGIONS = List.of(
		14652,
		14651,
		14908,
		14907
	);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ElHerbiboarOverlay overlay;

	@Inject
	private ElHerbiboarMinimapOverlay minimapOverlay;

	@Inject
	private BotUtils utils;

	@Inject
	private ElHerbiboarConfig config;

	/**
	 * Objects which appear at the beginning of Herbiboar hunting trails
	 */
	private final Map<WorldPoint, TileObject> starts = new HashMap<>();
	/**
	 * Herbiboar hunting "footstep" trail objects
	 */
	private final Map<WorldPoint, TileObject> trails = new HashMap<>();
	/**
	 * Objects which trigger next trail (mushrooms, mud, seaweed, etc)
	 */
	private final Map<WorldPoint, TileObject> trailObjects = new HashMap<>();
	/**
	 * Tunnel where the Herbiboar is hiding at the end of a trail
	 */
	private final Map<WorldPoint, TileObject> tunnels = new HashMap<>();
	/**
	 * Trail object IDs which should be highlighted
	 */
	private final Set<Integer> shownTrails = new HashSet<>();
	/**
	 * Sequence of herbiboar spots searched along the current trail
	 */
	private final List<ElHerbiboarSearchSpot> currentPath = new ArrayList<>();

	private boolean inHerbiboarArea;
	private TrailToSpot nextTrail;
	private ElHerbiboarSearchSpot.Group currentGroup;
	private int finishId;

	private boolean started;
	private WorldPoint startPoint;
	private ElHerbiboarStart startSpot;
	private boolean ruleApplicable;

	private boolean startHerbiboar;
	private TileObject targetTile;
	private MenuEntry targetMenu;
	private NPC targetNPC;
	private int tickTimer;
	private ElHerbiboarState status;
	private TileObject lastTileObject;
	private final List<TileObject> objectsToClick = new ArrayList<TileObject>();

	@Provides
	ElHerbiboarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElHerbiboarConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);
		setValues();
		startHerbiboar=false;

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				inHerbiboarArea = checkArea();
				updateTrailData();
			});
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);
		resetTrailData();
		clearCache();
		inHerbiboarArea = false;
		setValues();
		startHerbiboar=false;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) throws Exception {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElHerbiboar"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			setValues();
			if (!startHerbiboar)
			{
				startHerbiboar = true;
				targetMenu = null;
			} else {
				startHerbiboar = false;
				targetMenu = null;
			}
		}
	}

	private void setValues()
	{
		objectsToClick.clear();
	}

	private void updateTrailData()
	{
		if (!isInHerbiboarArea())
		{
			return;
		}

		boolean pathActive = false;
		boolean wasStarted = started;

		// Get trail data
		for (ElHerbiboarSearchSpot spot : ElHerbiboarSearchSpot.values())
		{
			for (TrailToSpot trail : spot.getTrails())
			{
				int value = client.getVar(trail.getVarbit());

				if (value == trail.getValue())
				{
					// The trail after you have searched the spot
					currentGroup = spot.getGroup();
					nextTrail = trail;

					// You never visit the same spot twice
					if (!currentPath.contains(spot))
					{
						currentPath.add(spot);
					}
				}
				else if (value > 0)
				{
					// The current trail
					shownTrails.addAll(trail.getFootprintIds());
					pathActive = true;
				}
			}
		}

		finishId = client.getVar(Varbits.HB_FINISH);

		// The started varbit doesn't get set until the first spot of the rotation has been searched
		// so we need to use the current group as an indicator of the rotation being started
		started = client.getVar(Varbits.HB_STARTED) > 0 || currentGroup != null;
		boolean finished = !pathActive && started;

		if (!wasStarted && started)
		{
			startSpot = ElHerbiboarStart.from(startPoint);
		}

		ruleApplicable = ElHerbiboarRule.canApplyRule(startSpot, currentPath);

		if (finished)
		{
			resetTrailData();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuOpt)
	{
		log.info(menuOpt.toString());
		if (!inHerbiboarArea || started || MenuOpcode.GAME_OBJECT_FIRST_OPTION != menuOpt.getMenuOpcode())
		{
			return;
		}

		switch (Text.removeTags(menuOpt.getTarget()))
		{
			case "Rock":
			case "Mushroom":
			case "Driftwood":
				startPoint = WorldPoint.fromScene(client, menuOpt.getParam0(), menuOpt.getParam1(), client.getPlane());
		}

		if(targetMenu!=null){
			menuOpt.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}
	}

	private void resetTrailData()
	{
		log.debug("Reset trail data");
		shownTrails.clear();
		currentPath.clear();
		nextTrail = null;
		currentGroup = null;
		finishId = 0;
		started = false;
		startPoint = null;
		startSpot = null;
		ruleApplicable = false;
	}

	private void clearCache()
	{
		starts.clear();
		trails.clear();
		trailObjects.clear();
		tunnels.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
				resetTrailData();
				break;
			case LOADING:
				clearCache();
				inHerbiboarArea = checkArea();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		updateTrailData();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		onTileObject(null, event.getGameObject());
	}

	@Subscribe
	public void onGameObjectChanged(GameObjectChanged event)
	{
		onTileObject(event.getPrevious(), event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		onTileObject(event.getGameObject(), null);
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		onTileObject(null, event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectChanged(GroundObjectChanged event)
	{
		onTileObject(event.getPrevious(), event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		onTileObject(event.getGroundObject(), null);
	}

	// Store relevant GameObjects (starts, tracks on trails, objects used to trigger next trails, and tunnels)
	private void onTileObject(TileObject oldObject, TileObject newObject)
	{
		if (oldObject != null)
		{
			WorldPoint oldLocation = oldObject.getWorldLocation();
			starts.remove(oldLocation);
			trails.remove(oldLocation);
			trailObjects.remove(oldLocation);
			tunnels.remove(oldLocation);
		}

		if (newObject == null)
		{
			return;
		}

		// Starts
		if (START_OBJECT_IDS.contains(newObject.getId()))
		{
			starts.put(newObject.getWorldLocation(), newObject);
			return;
		}

		// Trails
		if (ElHerbiboarSearchSpot.isTrail(newObject.getId()))
		{
			trails.put(newObject.getWorldLocation(), newObject);
			return;
		}

		// GameObject to trigger next trail (mushrooms, mud, seaweed, etc)
		if (ElHerbiboarSearchSpot.isSearchSpot(newObject.getWorldLocation()))
		{
			trailObjects.put(newObject.getWorldLocation(), newObject);
			return;
		}

		// Herbiboar tunnel
		if (END_LOCATIONS.contains(newObject.getWorldLocation()))
		{
			tunnels.put(newObject.getWorldLocation(), newObject);
		}
	}

	private boolean checkArea()
	{
		final int[] mapRegions = client.getMapRegions();
		for (int region : HERBIBOAR_REGIONS)
		{
			if (ArrayUtils.contains(mapRegions, region))
			{
				return true;
			}
		}
		return false;
	}

	List<WorldPoint> getEndLocations()
	{
		return END_LOCATIONS;
	}

	@Subscribe
	public void onGameTick(GameTick event){
		if (!startHerbiboar)
		{
			return;
		}
		if (!client.isResized())
		{
			utils.sendGameMessage("client must be set to resizable");
			startHerbiboar = false;
			return;
		}
		status = checkPlayerStatus();
		switch (status) {
			case ANIMATING:
			case NULL_PLAYER:
			case TICK_TIMER:
				break;
			case MOVING:
				shouldRun();
				break;
			case UNKNOWN:
				// Draw start objects
				if (config.isStartShown() && (currentGroup == null && finishId == 0))
				{
					int[] startIds = new int[getStarts().values().size()];
					if(startIds.length>0){
						int i = 0;
						for(TileObject object : getStarts().values()){
							startIds[i]=object.getId();
							i++;
						}
						targetTile = utils.findNearestGameObject(startIds);
						if(targetTile!=null){
							if(targetTile.getId()==30523){
								utils.setMenuEntry(new MenuEntry("","",targetTile.getId(),3,targetTile.getLocalLocation().getSceneX()+1,targetTile.getLocalLocation().getSceneY(),false));
							} else {
								utils.setMenuEntry(new MenuEntry("","",targetTile.getId(),3,targetTile.getLocalLocation().getSceneX(),targetTile.getLocalLocation().getSceneY(),false));
							}
							utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
							return;
						}
						//log.info(startIds.toString());
						//log.info("nearest " + utils.findNearestGameObject(startIds).getId());
					}
					//getStarts().values().forEach((obj) -> log.info("1:" + "id:" + obj.getId() + "x:" + obj.getLocalLocation().getSceneX() + "y:" + obj.getLocalLocation().getSceneY()));
				}

				// Draw trails
				if (config.isTrailShown())
				{
					Set<Integer> shownTrailIds = getShownTrails();
					getTrails().values().forEach((x) ->
					{
						int id = x.getId();
						if (shownTrailIds.contains(id) && (finishId > 0 || nextTrail != null && !nextTrail.getFootprintIds().contains(id)))
						{

						}
					});
				}
				if(!objectsToClick.isEmpty()){
					utils.setMenuEntry(new MenuEntry("","",objectsToClick.get(0).getId(),3,objectsToClick.get(0).getLocalLocation().getSceneX(),objectsToClick.get(0).getLocalLocation().getSceneY(),false));
					if(objectsToClick.get(0).getClickbox().getBounds()!=null){
						utils.delayMouseClick(objectsToClick.get(0).getClickbox().getBounds(),sleepDelay());
					} else {
						utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					}
				}
				// Draw trail objects (mushrooms, mud, etc)
				if (config.isObjectShown() && !(finishId > 0 || currentGroup == null))
				{
					if (isRuleApplicable())
					{
						WorldPoint correct = Iterables.getLast(getCurrentPath()).getLocation();
						TileObject object = getTrailObjects().get(correct);
						objectsToClick.add(object);
						utils.sendGameMessage("1. adding game object: " + object.getId());
						//log.info("2:" + "id:" + object.getId() + "x:" + object.getLocalLocation().getSceneX() + "y:" + object.getLocalLocation().getSceneY());
						//orange
					}
					else
					{
						for (WorldPoint trailLoc : ElHerbiboarSearchSpot.getGroupLocations(getCurrentGroup()))
						{
							TileObject object = getTrailObjects().get(trailLoc);
							objectsToClick.add(object);
							utils.sendGameMessage("2. adding game object: " + object.getId());
							//log.info("3:" + "id:" + object.getId() + "x:" + object.getLocalLocation().getSceneX() + "y:" + object.getLocalLocation().getSceneY());
							//black
						}
					}
				}

				// Draw finish tunnels
				if (config.isTunnelShown() && finishId > 0)
				{
					WorldPoint finishLoc = getEndLocations().get(finishId - 1);
					TileObject object = getTunnels().get(finishLoc);
					utils.sendGameMessage("3. adding game object: " + object.getId());
					objectsToClick.add(object);
					//log.info("4:" + "id:" + object.getId() + "x:" + object.getLocalLocation().getSceneX() + "y:" + object.getLocalLocation().getSceneY());
				}
				break;
		}
	}

	private ElHerbiboarState checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return NULL_PLAYER;
		}
		if(utils.iterating){
			return ITERATING;
		}
		if(player.getPoseAnimation()!=813 && player.getPoseAnimation()!=5160 && player.getPoseAnimation()!=808){
			return MOVING;
		}
		if(player.getAnimation()!=-1){
			return ANIMATING;
		}
		if(tickTimer>0)
		{
			tickTimer--;
			return TICK_TIMER;
		}
		tickTimer=tickDelay();
		return UNKNOWN;
	}

	private long sleepDelay()
	{
		if(config.customDelays()){
			return utils.randomDelay(config.sleepWeighted(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		} else {
			return utils.randomDelay(false, 60, 350, 100, 100);
		}

	}

	private int tickDelay()
	{
		if(config.customDelays()){
			return (int) utils.randomDelay(config.tickWeighted(),config.tickMin(), config.tickMax(), config.tickDeviation(), config.tickTarget());
		} else {
			return (int) utils.randomDelay(false,1, 3, 2, 2);
		}

	}

	private void shouldRun()
	{
		if(client.getWidget(160,23)!=null){ //if run widget is visible
			if(Integer.parseInt(client.getWidget(160,23).getText())>(30+utils.getRandomIntBetweenRange(0,20))){ //if run > 30+~20
				if(client.getWidget(160,27).getSpriteId()==1069){ //if run is off
					targetMenu = new MenuEntry("Toggle Run","",1,57,-1,10485782,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				}
			}
		}
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}
}
