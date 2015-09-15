package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SerialManager {
	
	static int openIndex = 0;
	
	private List<SerialListener> listeners = new ArrayList<SerialListener>();

	public static List<CommPortIdentifier> getAvailablePorts() {
		Enumeration<?> ports = (Enumeration<?>)CommPortIdentifier.getPortIdentifiers();
		List<CommPortIdentifier> list = new ArrayList<CommPortIdentifier>(); 
		while(ports.hasMoreElements()) {
			CommPortIdentifier port = (CommPortIdentifier)ports.nextElement();
			if(port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				list.add(port);
			}
		}
		
		return list;
	}

	public void openPort(CommPortIdentifier portId, int speed) {
		SerialWorker worker = new SerialWorker(this, portId, speed);
		worker.start();
	}
	
	public void addSerialListener(SerialListener listener) {
		
	}
	
	private synchronized void fireSystemSerialEvent(String message) {
		System.err.println(message);
	}
	
	private synchronized void fireSerialEvent(CommPortIdentifier identifier, String message) {
		System.out.println(identifier.getName() + " - " + message);
	}
	
	static class SerialWorker extends Thread {
		private SerialManager manager;
		private CommPortIdentifier portId;
		private SerialPort port;
		private int speed;
		
		private boolean stopRequested;
		
		public SerialWorker(SerialManager manager, CommPortIdentifier portId, int speed) {
			this.manager = manager;
			this.portId = portId;
			this.speed = speed;
		}

		@Override
		public void run() {
			BufferedInputStream in;
			try {
				port = (SerialPort)portId.open(getClass().getName(), openIndex++);
				port.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				in = new BufferedInputStream(port.getInputStream());
			}
			catch(PortInUseException e) {
				manager.fireSystemSerialEvent("Port " + portId.getName() + " is already in use. Owner is " + portId.getCurrentOwner() + ".");
				return;
			}
			catch(ClassCastException e) {
				manager.fireSystemSerialEvent("Port " + portId.getName() + " is not a serial port.");
				port.close();
				return;
			}
			catch(IOException e) {
				manager.fireSystemSerialEvent("Unable to open input stream for port " + port.getName() + ".");
				port.close();
				return;
			}
			catch(UnsupportedCommOperationException e) {
				manager.fireSystemSerialEvent("Incompatible params for port " + port.getName() + ".");
				port.close();
				return;
			}

			try {
				byte[] buffer = new byte[256];
				while(!stopRequested) {
					if(in.available() > 0) {
						int count = in.read(buffer);
						manager.fireSerialEvent(portId, new String(buffer, 0, count));
					}
				}
			}
			catch(IOException e) {
				
			}
			finally {
				port.close();
				System.out.println("Connection with port " + port.getName() + "is closed.");
			}
		}
	}
}


