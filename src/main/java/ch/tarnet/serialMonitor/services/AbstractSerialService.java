package ch.tarnet.serialMonitor.services;

import java.util.ArrayList;

public abstract class AbstractSerialService implements SerialService {

	private ArrayList<SerialPortListener> portListeners = new ArrayList<>();
	private ArrayList<SerialMessageListener> messageListeners = new ArrayList<>();
	
	@Override
	public void addSerialPortListener(SerialPortListener listener) {
		portListeners.add(listener);
	}

	@Override
	public void removeSerialPortListener(SerialPortListener listener) {
		portListeners.remove(listener);
	}

	@Override
	public void addSerialMessageListener(SerialMessageListener listener) {
		messageListeners.add(listener);
	}

	@Override
	public void removeSerialMessageListener(SerialMessageListener listener) {
		messageListeners.remove(listener);
	}
	
	protected void fireSerialPortAdded(SerialPortDescriptor port) {
		SerialPortEvent event = new SerialPortEvent(port);
		for(SerialPortListener l: portListeners) {
			l.portAdded(event);
		}
	}
	
	protected void fireSerialPortRemoved(SerialPortDescriptor port) {
		SerialPortEvent event = new SerialPortEvent(port);
		for(SerialPortListener l: portListeners) {
			l.portRemoved(event);
		}
	}
	
	protected void fireSerialMessage(SerialPortDescriptor port, String message) {
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), port, message);
		for(SerialMessageListener l: messageListeners) {
			l.newSerialMessage(event);
		}
	}
	
	protected void fireSystemMessage(SerialPortDescriptor port, String message) {
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), port, message);
		for(SerialMessageListener l: messageListeners) {
			l.newSystemMessage(event);
		}
	}
}
