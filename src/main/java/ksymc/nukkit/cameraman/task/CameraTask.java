package ksymc.nukkit.cameraman.task;

import java.util.HashMap;
import java.util.Map;

import cn.nukkit.level.Location;
import cn.nukkit.scheduler.PluginTask;
import ksymc.nukkit.cameraman.Camera;
import ksymc.nukkit.cameraman.Cameraman;

public class CameraTask extends PluginTask<Cameraman> {

	private Camera camera;

	private int index = -1;

	public CameraTask(Cameraman owner, Camera camera) {
		super(owner);
		this.camera = camera;
	}

	@Override
	public void onRun(int currentTick) {
		if (index < 0) {
			Map<String, String> format = new HashMap<>();
			format.put("slowness", Double.toString(getCamera().getSlowness()));
			getOwner().sendMessage(getCamera().getTarget(), "message-travelling-started", format);
			index = 0;
		}
		if (index >= getCamera().getMovements().size()) {
			getCamera().stop();
			return;
		}

		Location location = getCamera().getMovement(index).tick(getCamera().getSlowness());
		if (location == null) {
			index++;
			return;
		}
		
		getCamera().getTarget().teleport(location);
		//getCamera().getTarget().setPositionAndRotation(location, location.getYaw(), location.getPitch());
		//Cameraman.sendMovePlayerPacket(getCamera().getTarget());
	}

	public Camera getCamera() {
		return camera;
	}
}
