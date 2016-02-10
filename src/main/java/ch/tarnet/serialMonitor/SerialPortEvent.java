package ch.tarnet.serialMonitor;

public class SerialPortEvent {

	private SerialPortDescriptor descriptor;
	
	public SerialPortEvent(SerialPortDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public SerialPortDescriptor getDescriptor() {
		return descriptor;
	}

}
