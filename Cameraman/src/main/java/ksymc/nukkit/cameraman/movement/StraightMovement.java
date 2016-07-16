package ksymc.nukkit.cameraman.movement;

import cn.nukkit.level.Location;
import ksymc.nukkit.cameraman.Cameraman;

public class StraightMovement extends Movement {

	private Location distance;
	
	protected int current = 0, length = 0;
	
	public StraightMovement(Location origin, Location destination) {
		super(origin, destination);
		
		this.distance = new Location(getDestination().getX() - getOrigin().getX(),
				getDestination().getY() - getOrigin().getY(),
				getDestination().getZ() - getOrigin().getZ(),
				getDestination().getYaw() - getOrigin().getYaw(),
				getDestination().getPitch() - getOrigin().getPitch());
		
		double[] nums = {Math.abs(distance.getX()), Math.abs(distance.getY()), Math.abs(distance.getZ()), Math.abs(distance.getYaw())};
		int max = -999999999;
		for (double num : nums)
			if (num > max) max = (int) num;
		
		this.length = Cameraman.TICKS_PER_SECOND * (int) max;
	}

	@Override
	public Location tick(float slowness) {
		double length = this.length * slowness;
		if (length < 0.0000001)
			return null;
		
		double progress = current++ / length;
		if (progress > 1)
			return null;
		
		return new Location(getOrigin().getX() + distance.getX() * progress,
				1.62 + getOrigin().getY() + distance.getY() * progress,
				getOrigin().getZ() + distance.getZ() * progress,
				getOrigin().getYaw() + distance.getYaw() * progress,
				getOrigin().getPitch() + distance.getPitch() * progress);
	}

}
