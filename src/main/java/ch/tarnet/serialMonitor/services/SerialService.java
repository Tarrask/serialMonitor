package ch.tarnet.serialMonitor.services;

import java.util.List;

public interface SerialService {
	public static final int DEFAULT_SPEED = 9600;
	
	List<? extends SerialPortDescriptor> getAvailablePorts();
	void refreshPorts();
	void setPortSpeed(SerialPortDescriptor port, int speed);
	boolean openPort(SerialPortDescriptor port);
	void closePort(SerialPortDescriptor port);
	void closeAllPorts();
	void addSerialPortListener(SerialPortListener listener);
	void removeSerialPortListener(SerialPortListener listener);
	void addSerialMessageListener(SerialMessageListener listener);
	void removeSerialMessageListener(SerialMessageListener listener);
}
