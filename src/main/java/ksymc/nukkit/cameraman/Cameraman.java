package ksymc.nukkit.cameraman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.MovePlayerPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import ksymc.nukkit.cameraman.movement.Movement;
import ksymc.nukkit.cameraman.movement.StraightMovement;
import ksymc.nukkit.cameraman.task.AutoSaveTask;
import ksymc.nukkit.utils.Messages;
import ksymc.nukkit.utils.Util;

public class Cameraman extends PluginBase implements Listener {

	private static Cameraman instance;

	public static Cameraman getInstance() {
		return instance;
	}

	/* ====================================================================================================================== *
	 *                                                    GLOBAL VARIABLES                                                    *
	 * ====================================================================================================================== */

	public static final int TICKS_PER_SECOND = 10;
	public static final int DELAY = 70;

	private Map<String, List<Location>> waypointMap = new HashMap<>();

	private Map<String, Camera> cameras = new HashMap<>();

	/* ====================================================================================================================== *
	 *                                                    EVENT LISTENERS                                                     *
	 * ====================================================================================================================== */

	@Override
	public void onDisable() {
		saveConfigs();
	}

	@Override
	public void onEnable() {
		loadConfigs();
		loadMessages();
		
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().scheduleRepeatingTask(new AutoSaveTask(this), 20 * 60 * 15); // 15m
	}

	@Override
	public void onLoad() {
		instance = this;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Camera camera = getCamera(event.getPlayer());
		if (camera != null && camera.isRunning())
			camera.stop();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerReceive(DataPacketReceiveEvent event) {
		Camera camera = getCamera(event.getPlayer());
		if (event.getPacket() instanceof MovePlayerPacket && camera != null && camera.isRunning())
			event.setCancelled(true);
	}

	/* ====================================================================================================================== *
	 *                                                    RESOURCE CONTROL                                                    *
	 * ====================================================================================================================== */

	private Messages messages = null;
	public static final int MESSAGE_VERSION = 2;

	public void loadMessages() {
		getDataFolder().mkdir();
		updateMessages("messages.yml");
		messages = new Messages((new Config(getDataFolder() + "/messages.yml", Config.YAML)).getAll());
	}

	public void updateMessages() {
		updateMessages("mesages.yml");
	}
	
	public void updateMessages(String filename) {
		saveResource(filename, false);
		
		Map<String, Object> messages = (new Config(getDataFolder() + "/" + filename, Config.YAML)).getAll();
		if (!messages.containsKey("version") || (int) messages.get("version") < Cameraman.MESSAGE_VERSION)
			saveResource(filename, true);
	}
	
	public Messages getMessages() {
		return messages;
	}

	public void loadConfigs() {
		getDataFolder().mkdir();
		
		Config config = new Config(getDataFolder() + "/waypoint-map.json", Config.JSON);
		Map<String, Object> waypointMap = config.getAll();
		
		for (String player : waypointMap.keySet()) {
			List<Location> waypoints = new ArrayList<>();
			for (Map<String, Object> waypoint : ((ArrayList<Map<String, Object>>) waypointMap.get(player))) {
				double x = (double) waypoint.get("x");
				double y = (double) waypoint.get("y");
				double z = (double) waypoint.get("z");
				double yaw = (double) waypoint.get("yaw");
				double pitch = (double) waypoint.get("pitch");
				Level level = getServer().getLevelByName((String) waypoint.get("level"));
				
				waypoints.add(new Location(x, y, z, yaw, pitch, level));
			}
			this.waypointMap.put(player, waypoints);
		}
	}

	public void saveConfigs() {
		LinkedHashMap<String, Object> waypointMap = new LinkedHashMap<>();
		
		for (String player : getWaypointMap().keySet()) {
			if (player == null) continue;
			
			List<Map<String, Object>> waypoints = new ArrayList<>();
			for (Location loc : getWaypointMap().get(player)) {
				LinkedHashMap<String, Object> waypoint = new LinkedHashMap<>();
				waypoint.put("x", loc.getX());
				waypoint.put("y", loc.getY());
				waypoint.put("z", loc.getZ());
				waypoint.put("yaw", loc.getYaw());
				waypoint.put("pitch", loc.getPitch());
				waypoint.put("level", loc.isValid() ? loc.getLevel().getName() : null);
				waypoints.add(waypoint);
			}
			waypointMap.put(player, waypoints);
		}
		
		Config config = new Config(getDataFolder() + "/waypoint-map.json", Config.JSON);
		config.setAll(waypointMap);
		config.save();
	}

	/* ====================================================================================================================== *
	 *                                                   GETTERS AND SETTERS                                                  *
	 * ====================================================================================================================== */

	public Map<String, List<Location>> getWaypointMap() {
		return waypointMap;
	}

	public Map<String, List<Location>> setWaypointMap(Map<String, List<Location>> waypointMap) {
		this.waypointMap = waypointMap;
		return waypointMap;
	}

	public List<Location> getWaypoints(Player player) {
		return waypointMap.get(player.getName());
	}

	public List<Location> setWaypoints(Player player, List<Location> waypoints) {
		waypointMap.put(player.getName(), waypoints);
		return waypoints;
	}

	public List<Location> setWaypoint(Player player, Location waypoint) {
		return setWaypoint(player, waypoint, -1);
	}

	public List<Location> setWaypoint(Player player, Location waypoint, int index) {
		List<Location> waypoints = waypointMap.get(player.getName());
		if (index >= 0)
			waypoints.add(index, waypoint);
		else
			waypoints.add(waypoint);
		waypointMap.put(player.getName(), waypoints);

		return waypoints;
	}


	public Map<String, Camera> getCameras() {
		return cameras;
	}

	public Camera getCamera(Player player) {
		return cameras.containsKey(player.getName()) ? cameras.get(player.getName()) : null;
	}

	public Camera setCamera(Player player, Camera camera) {
		cameras.put(player.getName(), camera);
		return camera;
	}

	/* ====================================================================================================================== *
	 *                                                     HELPER METHODS                                                     *
	 * ====================================================================================================================== */

	public static List<Movement> createStraightMovements(List<Location> waypoints) {
		Location lastWaypoint = null;

		List<Movement> movements = new ArrayList<>();
		for (Location waypoint : waypoints) {
			if (lastWaypoint != null && !waypoint.equals(lastWaypoint))
				movements.add(new StraightMovement(lastWaypoint, waypoint));
			lastWaypoint = waypoint;
		}
		return movements;
	}

	public static boolean sendMovePlayerPacket(Player player) {
		MovePlayerPacket packet = new MovePlayerPacket();
		packet.eid = 0;
		packet.x = (float) player.getX();
		packet.y = (float) player.getY();
		packet.z = (float) player.getZ();
		packet.yaw = (float) player.getYaw();
		packet.headYaw = (float) player.getYaw();
		packet.pitch = (float) player.getPitch();
		packet.onGround = false;

		return player.dataPacket(packet);
	}

	/* ====================================================================================================================== *
	 *                                                     MESSAGE SENDERS                                                    *
	 * ====================================================================================================================== */

	private static String colorError = TextFormat.RESET + TextFormat.RED;
	private static String colorLight = TextFormat.RESET + TextFormat.GREEN;
	private static String colorDark = TextFormat.RESET + TextFormat.DARK_GREEN;
	private static String colorTitle = TextFormat.RESET + TextFormat.DARK_GREEN + TextFormat.BOLD;
	private static String colorTITLE = TextFormat.RESET + TextFormat.RED + TextFormat.BOLD;

	private static String[] commands = {
		"p", "start", "stop", "info", "goto", "clear", "help", "about"
	};

	private static String[][] commandMap = {
		{"p", "start", "stop"},
		{"info", "goto", "clear"},
		{"help", "about"}
	};

	public boolean sendMessage(CommandSender sender, String key) {
		return sendMessage(sender, key, new HashMap<>());
	}

	public boolean sendMessage(CommandSender sender, String key, Map<String, String> format) {
		if (sender == null)
			return false;

		String prefix;

		if (key.charAt(0) == '@') {
			key = key.substring(1);
			prefix = Cameraman.colorTitle;
		} else if (key.charAt(0) == '!') {
			key = key.substring(1);
			prefix = Cameraman.colorDark;
		} else if (key.charAt(0) == '?') {
			key = key.substring(1);
			prefix = Cameraman.colorLight;
		} else if (key.charAt(0) == '.') {
			key = key.substring(1);
			prefix = Cameraman.colorTitle + getMessages().getMessage("prefix") + Cameraman.colorDark;
		} else if (key.charAt(0) == '#') {
			key = key.substring(1);
			prefix = Cameraman.colorTITLE + getMessages().getMessage("prefix") + Cameraman.colorError;
		} else {
			prefix = Cameraman.colorTitle + getMessages().getMessage("prefix") + Cameraman.colorLight;
		}
		sender.sendMessage(prefix + getMessages().getMessage(key, format));

		return true;
	}

	public boolean sendAboutMessage(CommandSender sender) {
		Map<String, String> format = new HashMap<>();
		format.put("version", getDescription().getVersion());
		format.put("chalkpe", getDescription().getAuthors().get(0));
		format.put("website", getDescription().getWebsite());
		sendMessage(sender, "@message-about", format);
		return true;
	}

	public boolean sendUnknownCommandErrorMessage(CommandSender sender) {
		sendMessage(sender, "#error-unknown-command-0");
		sendMessage(sender, "#error-unknown-command-1");
		return true;
	}

	public boolean sendHelpMessages(CommandSender sender) {
		return sendHelpMessages(sender, "1");
	}

	public boolean sendHelpMessages(CommandSender sender, String param) {
		param = param.toLowerCase();

		if (param.isEmpty()) {
			for (String command : Cameraman.commands) {
				sendMessage(sender, "command-" + command + "-usage");
				sendMessage(sender, ".command-" + command + "-description");
			}
			return true;
		}

		if (Util.isNumeric(param)) {
			Map<String, String> format = new HashMap<>();
			format.put("current", param);
			format.put("total", Integer.toString(Cameraman.commandMap.length));
			sendMessage(sender, "@message-help", format);

			int index = Integer.parseInt(param);
			if (index > 0 && index <= Cameraman.commandMap.length) {
				for (String command : Cameraman.commandMap[index - 1]) {
					sendMessage(sender, "command-" + command + "-usage");
					sendMessage(sender, ".command-" + command + "-description");
				}
			}
			return true;
		}

		for (String command : commands) {
			if (command.equals(param)) {
				sendMessage(sender, "command-" + param + "-usage");
				sendMessage(sender, ".command-" + param + "-description");

				return true;
			}
		}

		return sendUnknownCommandErrorMessage(sender);
	}

	public boolean sendWaypointMessage(CommandSender sender, Vector3 waypoint, int index) {
		Map<String, String> format = new HashMap<>();
		format.put("index", Integer.toString(index));
		format.put("x", Integer.toString(waypoint.getFloorX()));
		format.put("y", Integer.toString(waypoint.getFloorY()));
		format.put("z", Integer.toString(waypoint.getFloorZ()));
		sendMessage(sender, "message-waypoint-info", format);

		return true;
	}

	/* ====================================================================================================================== *
	 *                                                    COMMAND HANDLERS                                                    *
	 * ====================================================================================================================== */

	private boolean checkIndex(int index, List<Location>array) {
		return checkIndex(index, array);
	}
	
	private boolean checkIndex(int index, List<Location> array, CommandSender sender) {
		if (index < 1 || index > array.size()) {
			Map<String, String> format = new HashMap<>();
			format.put("total", Integer.toString(array.size()));
			sendMessage(sender, "#error-index-out-of-bounds", format);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
		if (!(sender instanceof Player)) {
			sendMessage(sender, "#error-only-in-game");
			return true;
		}
		
		List<String> args = new ArrayList<String>(Arrays.asList(arguments));
		
		if (label.equals("p"))
			args.add("p");
		
		if (args.size() < 1)
			return sendHelpMessages(sender);
		
		Player player = (Player) sender;
		List<Location> waypoints = getWaypoints(player);
		Camera camera = getCamera(player);
		
		switch (args.get(0).toLowerCase()) {
			default:
				sendUnknownCommandErrorMessage(sender);
				break;
			case "help":
				if (args.size() > 1)
					return sendHelpMessages(sender, args.get(1));
				else
					return sendHelpMessages(sender);
			case "about":
				return sendAboutMessage(sender);
			case "p":
				if (waypoints == null)
					waypoints = setWaypoints(player, new ArrayList<>());
				
				if (args.size() > 1 && Util.isNumeric(args.get(1))) {
					int index = Integer.parseInt(args.get(1));
					if (checkIndex(index, waypoints, sender))
						return true;
					
					waypoints = setWaypoint(player, player.getLocation(), index - 1);
					
					Map<String, String> format = new HashMap<>();
					format.put("index", Integer.toString(index));
					format.put("total", Integer.toString(waypoints.size()));
					sendMessage(sender, "message-reset-waypoint", format);
				} else {
					waypoints = setWaypoint(player, player.getLocation());
					
					Map<String, String> format = new HashMap<>();
					format.put("index", Integer.toString(waypoints.size()));
					sendMessage(sender, "message-added-waypoint", format);
				}
				break;
			case "start":
				if (args.size() < 2 || !Util.isNumeric(args.get(1)))
					return sendHelpMessages(sender, args.get(0));
				
				if (waypoints == null || waypoints.size() < 2) {
					sendMessage(sender, "#error-too-few-waypoints");
					return sendHelpMessages(sender, "p");
				}
				
				int gamemode = Player.SPECTATOR;
				if (args.size() >= 3)
					gamemode = Server.getGamemodeFromString(args.get(2));

				float slowness = Float.parseFloat(args.get(1));
				if (slowness < 0.0000001) {
					Map<String, String> format = new HashMap<>();
					format.put("slowness", Double.toString(slowness));
					return sendMessage(sender, "#error-negative-slowness", format);
				}
				
				if (camera != null && camera.isRunning()) {
					sendMessage(sender, ".message-interrupting-current-travel");
					camera.stop();
				}
				
				setCamera(player, new Camera(player, Cameraman.createStraightMovements(waypoints), slowness, gamemode)).start();
				break;
			case "stop":
				if (camera == null || !camera.isRunning())
					return sendMessage(sender, "#error-travels-already-interrupted");
				
				camera.stop();
				setCamera(player, null);
				sendMessage(sender, "message-travelling-interrupted");
				break;
			case "info":
			{
				if (waypoints == null || waypoints.size() == 0)
					return sendMessage(sender, "#error-no-waypoints-to-show");
				
				if (args.size() > 1 && Util.isNumeric(args.get(1))) {
					int index = Integer.parseInt(args.get(1));
					if (checkIndex(index, waypoints, sender))
						return true;
					
					sendWaypointMessage(sender, waypoints.get(index - 1), index);
				} else {
					int index = 0;
					for (Location waypoint : waypoints) {
						sendWaypointMessage(sender, waypoint, index++ + 1);
					}
				}
				break;
			}
			case "goto":
			{
				if (args.size() < 2 || !Util.isNumeric(args.get(1)))
					return sendHelpMessages(sender, args.get(0));
				
				if (waypoints == null || waypoints.size() == 0)
					return sendMessage(sender, "#error-no-waypoints-to-teleport");
				
				int index = Integer.parseInt(args.get(1));
				if (checkIndex(index, waypoints, sender))
					return true;
				
				player.teleport(waypoints.get(index - 1));
				
				Map<String, String> format = new HashMap<>();
				format.put("index", Integer.toString(index));
				sendMessage(sender, "message-teleported", format);
				break;
			}
			case "clear":
			{
				if (waypoints == null || waypoints.size() == 0)
					return sendMessage(sender, "#error-no-waypoints-to-remove");
				
				if (args.size() > 1 && Util.isNumeric(args.get(1))) {
					int index = Integer.parseInt(args.get(1));
					if (checkIndex(index, waypoints, sender))
						return true;
					
					waypoints.remove(index - 1);
					
					Map<String, String> format = new HashMap<>();
					format.put("index", Integer.toString(index));
					format.put("total", Integer.toString(waypoints.size()));
					sendMessage(sender, "message-removed-waypoint", format);
				} else {
					waypoints.clear();
					sendMessage(sender, "message-all-waypoint-removed");
				}
				setWaypoints(player, waypoints);
				break;
			}
		}
		return true;
	}
}
