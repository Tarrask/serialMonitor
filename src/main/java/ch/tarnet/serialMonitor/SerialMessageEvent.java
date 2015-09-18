package ch.tarnet.serialMonitor;

public class SerialMessageEvent {

	private long when;
	private SerialPortDescriptor descriptor;
	private String message;
	
	public SerialMessageEvent(long when, SerialPortDescriptor descriptor, String message) {
		this.when = when;
		this.descriptor = descriptor;
		this.message = message;
	}

	public SerialPortDescriptor getDescriptor() {
		return descriptor;
	}
	
	public String getMessage() {
		return message;		
	}
}
