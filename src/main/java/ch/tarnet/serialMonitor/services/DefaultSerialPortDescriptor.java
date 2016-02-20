package ch.tarnet.serialMonitor.services;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class DefaultSerialPortDescriptor implements SerialPortDescriptor {
	
	protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	protected String name = null;
	protected Status status = Status.UNKNOWN;
	protected int speed = SerialService.DEFAULT_SPEED;
	
	public DefaultSerialPortDescriptor() {}
	
	public DefaultSerialPortDescriptor(String name, Status status, int speed) {
		this.name = name;
		this.status = status;
		this.speed = speed;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public int getSpeed() {
		return speed;
	}

	public void setName(String name) {
		String oldName = getName();
		this.name = name;
		pcs.firePropertyChange("name", oldName, name);
	}

	public void setStatus(Status status) {
		Status oldStatus = getStatus();
		this.status = status;
		pcs.firePropertyChange("status", oldStatus, status);
	}
	
	public void setSpeed(int speed) {
		int oldSpeed = getSpeed();
		this.speed = speed;
		pcs.firePropertyChange("speed", oldSpeed, speed);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	};
	
	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	@Override
	public String toString() {
		return getName();
	}
	
	@Override
    public int hashCode() {
//	 	int hash = 1;
//		hash = hash * 17 + speed;
//		hash = hash * 31 + name.hashCode();
//		hash = hash * 13 + status.hashCode();
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SerialPortDescriptor) {
			SerialPortDescriptor port = (SerialPortDescriptor) obj;
			return this.name.equals(port.getName()); // && this.status.equals(port.getStatus()) && this.speed == port.getSpeed();
		}
		else {
			return false;
		}
	}
}