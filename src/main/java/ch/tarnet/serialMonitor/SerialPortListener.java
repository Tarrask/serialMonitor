package ch.tarnet.serialMonitor;

public interface SerialPortListener {
	public void portAdded(SerialPortEvent event);
	public void portRemoved(SerialPortEvent event);
}
