package ksymc.nukkit.cameraman.task;

import cn.nukkit.scheduler.PluginTask;
import ksymc.nukkit.cameraman.Cameraman;

public class AutoSaveTask extends PluginTask<Cameraman> {

	public AutoSaveTask(Cameraman owner) {
		super(owner);
	}

	@Override
	public void onRun(int currentTick) {
		getOwner().saveConfig();
	}

}
