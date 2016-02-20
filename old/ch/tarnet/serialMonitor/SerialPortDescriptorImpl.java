package ch.tarnet.serialMonitor;

import ch.tarnet.serialMonitor.services.DefaultSerialPortDescriptor;
import gnu.io.CommPortIdentifier;

@Deprecated
public class SerialPortDescriptorImpl extends DefaultSerialPortDescriptor {
	protected CommPortIdentifier portId;
	
	public SerialPortDescriptorImpl(CommPortIdentifier portId, int speed) {
		super(portId.getName(), Status.UNKNOWN, speed);
		this.portId = portId;
	}

	public CommPortIdentifier getPortIdentifier() {
		return portId;
	}
	
	public void setSpeed(int speed) {
		this.speed = speed;
	}
}
