package ch.tarnet.serialMonitor;

import java.util.ArrayList;
import java.util.List;

import ch.tarnet.serialMonitor.services.AbstractSerialService;
import ch.tarnet.serialMonitor.services.DefaultSerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor.Status;
import ch.tarnet.serialMonitor.services.SerialService;

public class TestLauncher extends Launcher {

	@Override
	protected SerialService createSerialService() {
		return new FakeSerialService();
	}
	
	public static void main(String[] args) {
		new TestLauncher().launch();
	}
	
	class FakeSerialService extends AbstractSerialService {
		private ArrayList<SerialPortDescriptor> ports = new ArrayList<>();
		
		public FakeSerialService() {
			ports.add(new DefaultSerialPortDescriptor("COM1", Status.UNKNOWN, 9600));
			ports.add(new DefaultSerialPortDescriptor("COM3", Status.UNKNOWN, 9600));
		}
		
		@Override
		public void setPortSpeed(SerialPortDescriptor port, int speed) {
			if (port instanceof DefaultSerialPortDescriptor) {
				DefaultSerialPortDescriptor fakePort = (DefaultSerialPortDescriptor) port;
				fakePort.setSpeed(speed);
			}
		}
		
		@Override
		public void refreshPorts() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public boolean openPort(SerialPortDescriptor port) {
			if (port instanceof DefaultSerialPortDescriptor) {
				DefaultSerialPortDescriptor fakePort = (DefaultSerialPortDescriptor) port;
				fakePort.setStatus(Status.OPEN);
				fireSystemMessage(port, "Serial port opened");
				return true;
			}
			else {
				return false;
			}
		}
		
		@Override
		public List<? extends SerialPortDescriptor> getAvailablePorts() {
			return ports;
		}
		
		@Override
		public void closePort(SerialPortDescriptor port) {
			if (port instanceof DefaultSerialPortDescriptor) {
				DefaultSerialPortDescriptor fakePort = (DefaultSerialPortDescriptor) port;
				fakePort.setStatus(Status.CLOSE);
			}
		}
		
		@Override
		public void closeAllPorts() {
			// TODO Auto-generated method stub
			
		}
	}
}
