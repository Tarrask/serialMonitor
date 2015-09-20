package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;

public class SerialPortDescriptor {

	enum Status {OPEN, CLOSE, USED, UNKNOWN};
	private CommPortIdentifier portId;
	private String name;
	private int speed;
	private Status status;
	
	public SerialPortDescriptor(CommPortIdentifier portId, int speed) {
		this.portId = portId;
		this.name = portId.getName();
		this.speed = speed;
		this.status = Status.UNKNOWN;
	}

	public String getName() {
		return name;
	}

	public CommPortIdentifier getPortIdentifier() {
		return portId;
	}

	public int getSpeed() {
		return speed;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
