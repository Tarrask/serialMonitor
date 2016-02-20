package ch.tarnet.serialMonitor.services;

import java.util.EventListener;

public interface SerialPortListener extends EventListener {
	public void portAdded(SerialPortEvent event);
	public void portRemoved(SerialPortEvent event);
}
