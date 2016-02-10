package ch.tarnet.serialMonitor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import gnu.io.CommPortIdentifier;

public class SerialPortDescriptor {
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
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
		int oldValue = this.speed;
		this.speed = speed;
		pcs.firePropertyChange("speed", oldValue, speed);
	}
	
	public void setStatus(Status status) {
		Status oldValue = this.status;
		this.status = status;
		pcs.firePropertyChange("status", oldValue, status);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
