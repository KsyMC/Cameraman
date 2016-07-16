package ksymc.nukkit.cameraman.movement;

import cn.nukkit.level.Location;

abstract public class Movement {

	private Location origin;
	
	private Location destination;
	
	public Movement(Location origin, Location destination) {
		this.origin = origin;
		this.destination = destination;
	}
	
	public Location getOrigin() {
		return origin;
	}
	
	public Location getDestination() {
		return destination;
	}
	
	@Override
	public String toString() {
		return "Movement(" + getOrigin() + " -> " + getDestination() + ")";
	}
	
	public abstract Location tick(float slowness);
}
