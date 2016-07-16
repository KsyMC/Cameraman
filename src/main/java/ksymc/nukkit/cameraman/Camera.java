package ksymc.nukkit.cameraman;

import java.util.List;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import ksymc.nukkit.cameraman.Cameraman;
import ksymc.nukkit.cameraman.movement.Movement;
import ksymc.nukkit.cameraman.task.CameraTask;

public class Camera {
	private Player target;
	
	private List<Movement> movements;
	
	private float slowness;
	
	private int taskId = -1;
	
	private int newGamemode, oldGamemode;
	
	private Location location;
	
	public Camera(Player target, List<Movement> movements, float slowness, int newGamemode) {
		this.target = target;
		this.movements = movements;
		this.slowness = slowness;
		this.newGamemode = newGamemode;
	}
	
	public Player getTarget() {
		return target;
	}
	
	public List<Movement> getMovements() {
		return movements;
	}
	
	public Movement getMovement(int index) {
		return movements.get(index);
	}
	
	public float getSlowness() {
		return slowness;
	}
	
	public boolean isRunning() {
		return taskId != -1;
	}
	
	public void start() {
		if (!isRunning()) {
			Cameraman.getInstance().sendMessage(getTarget(), "message-travelling-will-start");
			
			location = getTarget().getLocation();
			oldGamemode = getTarget().getGamemode();
			
			getTarget().setGamemode(newGamemode);
			
			taskId = Cameraman.getInstance().getServer().getScheduler().scheduleDelayedRepeatingTask(
					new CameraTask(Cameraman.getInstance(), this), Cameraman.DELAY, 20 / Cameraman.TICKS_PER_SECOND).getTaskId();
		}
	}
	
	public void stop() {
		if (isRunning()) {
			Cameraman.getInstance().getServer().getScheduler().cancelTask(taskId);
			taskId = -1;
			
			getTarget().teleport(location);
			getTarget().setGamemode(oldGamemode);
			
			Cameraman.getInstance().sendMessage(getTarget(), "message-travelling-finished");
		}
	}
}
