package ch.tarnet.serialMonitor.services;

public class SerialMessageEvent {

	private long when;
	private SerialPortDescriptor port;
	private String message;

	public SerialMessageEvent(long when, SerialPortDescriptor port, String message) {
		this.when = when;
		this.port = port;
		this.message = message;
	}
	
	public SerialPortDescriptor getSerialPort() {
		return port;
	}
	
	public String getMessage() {
		return message;		
	}
	
	public long getWhen() {
		return when;
	}
}
