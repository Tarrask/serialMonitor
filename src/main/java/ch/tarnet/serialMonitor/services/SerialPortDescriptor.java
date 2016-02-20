package ch.tarnet.serialMonitor.services;

import java.beans.PropertyChangeListener;

public interface SerialPortDescriptor {

	static final String NAME = "name";
	static final String STATUS = "status";
	static final String SPEED = "speed";
	
	enum Status {OPEN, OPENING, CLOSE, CLOSING, USED, UNKNOWN};
	
	String getName();
	Status getStatus();
	int	getSpeed();

	void addPropertyChangeListener(PropertyChangeListener listener);
	void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
	void removePropertyChangeListener(PropertyChangeListener listener);
}
