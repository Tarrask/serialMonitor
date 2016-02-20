package ch.tarnet.serialMonitor.services;

public class SerialPortEvent {

	private SerialPortDescriptor port;
	
	public SerialPortEvent(SerialPortDescriptor port) {
		this.port = port;
	}
	
	public SerialPortDescriptor getSerialPort() {
		return port;
	}
}
