package ch.tarnet.serialMonitor.services.rxtx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.serialMonitor.services.AbstractSerialService;
import ch.tarnet.serialMonitor.services.DefaultSerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor.Status;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class RxtxSerialService extends AbstractSerialService {
	
	private static final Logger logger = LoggerFactory.getLogger(RxtxSerialService.class);
	
	public static final int PORT_WATCHER_WAIT = 1000;

	private static final Charset UTF8 = Charset.forName("utf8");

	private RxtxPortWatcher portWatcher;
	private Map<RxtxSerialPortDescriptor, SerialWorker> workers = new HashMap<>();

	public RxtxSerialService() {
		// démarre le port watcher qui surveille l'état des différents ports série
		this.portWatcher = new RxtxPortWatcher(this);
		this.portWatcher.start();
	}
	
	/**
	 * Retourne la liste des ports disponible actuellement en possession du portWatcher.
	 * Cette fonction ne déclanche pas un refresh.
	 * @return
	 */
	@Override
	public List<? extends SerialPortDescriptor> getAvailablePorts() {
		return portWatcher.getAvailablePorts();
	}

	/**
	 * Indique au PortWatcher qu'il doit interrompre son attente et procéder de suite à la récupération des
	 * ports série disponible.
	 */
	@Override
	public void refreshPorts() {
		portWatcher.interrupt();
	}
	
	@Override
	public void setPortSpeed(SerialPortDescriptor port, int speed) {
		if (port instanceof RxtxSerialPortDescriptor) {
			RxtxSerialPortDescriptor rxtxPort = (RxtxSerialPortDescriptor) port;
			rxtxPort.setSpeed(speed);
		}
	}

	/**
	 * Ouvre un port série s'il ne l'est pas déjé. Démarre un SerialWorker pour récupérer les données
	 * transmises par le port. Cette fonction bloque jusque à ce que le port soit ouvert ou que
	 * l'impossibilité de l'ouvrir soit declaré.
	 * @param descriptor Tout ce qu'il faut savoir pour ouvrir et configurer le port.
	 */
	@Override
	public boolean openPort(SerialPortDescriptor port) {
		if(!(port instanceof RxtxSerialPortDescriptor)) {
			throw new IllegalArgumentException("port should be an instance of RxtxSerialPortDescriptor");
		}
		RxtxSerialPortDescriptor descriptor = (RxtxSerialPortDescriptor)port;
		// si le port est déjà ouvert par nos soins, on ne fait rien
		if(port.getStatus() == Status.OPEN) {
			return true;
		}
		
		SerialWorker worker = new SerialWorker(this, descriptor);
		if(worker.openPort()) {
			workers.put(descriptor, worker);
			return true;
		}
		
		return false;
	}

	/**
	 * Ferme le port identifié par le descriptor.
	 * @param descriptor 
	 */
	@Override
	public void closePort(SerialPortDescriptor port) {
		logger.info("Closing port {}.", port);
		// recherche le worker qui aurait ouvert le port
		SerialWorker worker = workers.get(port);
		if(worker != null) {
			worker.closePort();
		}
	}

	@Override
	public void closeAllPorts() {
		while(!workers.isEmpty()) {
			for(SerialWorker worker : workers.values()) {
				logger.info("Closing port {}.", worker.port);
				worker.closePort();
			}
			try {
				synchronized(this) {
					wait(100);
				}
			}
			catch(InterruptedException e) {
				
			}
		}
	}

	public void firePortAddedEvent(RxtxSerialPortDescriptor descriptor) {
		fireSerialPortAdded(descriptor);
	}

	public void firePortRemovedEvent(RxtxSerialPortDescriptor descriptor) {
		fireSerialPortRemoved(descriptor);
	}

	private void fireSerialMessageEvent(RxtxSerialPortDescriptor descriptor, String message) {
		fireSerialMessage(descriptor, message);
	}

	private void fireSystemMessageEvent(RxtxSerialPortDescriptor descriptor, String message) {
		logger.info(message, descriptor);
		fireSystemMessage(descriptor, message);
	}
	
	static private class SerialWorker extends Thread {
		private static final int BUFFER_SIZE = 256;
		private RxtxSerialService manager;
		private RxtxSerialPortDescriptor descriptor;
		private SerialPort port;
		
		private BufferedInputStream portInStream;
		private boolean keepRunning = true;
		private static int openIndex = 0;
		public SerialWorker(RxtxSerialService manager, RxtxSerialPortDescriptor descriptor) {
			this.manager = manager;
			this.descriptor = descriptor;
			this.setDaemon(false);
		}
		
		public boolean openPort() {
			// tente l'ouverture d'un port flux d'entré provenant du port série. En reste là si quoi que ce soit
			// se déroule mal
			try {
				descriptor.setStatus(Status.OPENING);
				port = (SerialPort)descriptor.getPortIdentifier().open(getClass().getName(), openIndex++);
				port.setSerialPortParams(descriptor.getSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				portInStream = new BufferedInputStream(port.getInputStream());
			}
			catch(PortInUseException e) {
				manager.fireSystemMessageEvent(descriptor, 
						"Port " + descriptor.getName() + " is already in use. Owner is " + descriptor.getPortIdentifier().getCurrentOwner() + ". " + e.getMessage());
				descriptor.setStatus(Status.USED);
				return false;
			}
			catch(ClassCastException e) {
				manager.fireSystemMessageEvent(descriptor, "Port " + descriptor.getName() + " is not a serial port.");
				System.err.println(e.getMessage());
				port.close();
				descriptor.setStatus(Status.CLOSE);
				return false;
			}
			catch(UnsupportedCommOperationException e) {
				manager.fireSystemMessageEvent(descriptor, "Incompatible params for port " + descriptor.getName() + ".");
				System.err.println(e.getMessage());
				port.close();
				descriptor.setStatus(Status.CLOSE);
				return false;
			}
			catch(IOException e) {
				manager.fireSystemMessageEvent(descriptor, "Unable to open input stream for port " + descriptor.getName() + ".");
				System.err.println(e.getMessage());
				port.close();
				descriptor.setStatus(Status.CLOSE);
				return false;
			}
			
			// Aquite de l'ouverture du port.
			descriptor.setStatus(Status.OPEN);
			manager.fireSystemMessageEvent(descriptor, "Connection with serial port " + descriptor.getName() + " is open. (" + port.getBaudRate() + ", " + port.getDataBits() + ", " + port.getStopBits() + ", " + port.getParity() + ")");
			
			// On écoute les changements de vitesse
			descriptor.addPropertyChangeListener(SerialPortDescriptor.SPEED, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					try {
						port.setSerialPortParams(descriptor.getSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
						manager.fireSystemMessageEvent(descriptor, "Parameters for port " + descriptor.getName() + " changed. (" + port.getBaudRate() + ", " + port.getDataBits() + ", " + port.getStopBits() + ", " + port.getParity() + ")");
					}
					catch(UnsupportedCommOperationException e) {
						manager.fireSystemMessageEvent(descriptor, "Incompatible params for port " + descriptor.getName() + ".");
						logger.error("Incompatible params for port {}.", descriptor, e.getMessage());
						port.close();
						return;
					}
				}
			});
			
			start();
			return true;
		}

		public void closePort() {
			descriptor.setStatus(Status.CLOSING);
			keepRunning = false;
			this.interrupt();
		}

		@Override
		public void run() {
			// boucle de lecture, si une erreur survient, on ferme le port simplement. A Chaque série de donnée, on
			// envoie un event.
			try {
				byte[] buffer = new byte[BUFFER_SIZE];
				while(keepRunning) {
					if(portInStream.available() > 0) {
						int count = portInStream.read(buffer);
						manager.fireSerialMessageEvent(descriptor, new String(buffer, 0, count, UTF8));
					}
					// On attend une petite millisecond pour ne pas burner le processeur
					try {
						synchronized(this) {
							this.wait(1);
						}
					} catch (InterruptedException e) {
						/* ignoré */ 
					}
				}
			}
			catch(IOException e) {
				manager.fireSystemMessageEvent(descriptor, "Error reading port " + descriptor.getName() + ".");
				logger.error("error reading port {}.", descriptor, e);
			}
			finally {
				port.close();
				descriptor.setStatus(Status.CLOSE);
				//retire le worker de la liste des workers actifs.
				manager.workers.remove(descriptor);
				manager.fireSystemMessageEvent(descriptor, "Connection with serial port " + descriptor.getName() + " is close.");
			}
			logger.info("SerialWorker for port {} termiated.", port.getName());
		}
	}
	
	static class RxtxSerialPortDescriptor extends DefaultSerialPortDescriptor {
		
		private CommPortIdentifier portId;
		
		public RxtxSerialPortDescriptor(CommPortIdentifier portId, int speed) {
			super(portId.getName(), Status.UNKNOWN, speed);
			System.out.println("Owner: " + portId.getCurrentOwner());
			System.out.println("Owned: " + portId.isCurrentlyOwned());
			this.portId = portId;
		}
		
		public CommPortIdentifier getPortIdentifier() {
			return portId;
		}
		
		public void setSpeed(int speed) {
			int oldSpeed = getSpeed();
			this.speed = speed;
			pcs.firePropertyChange(SPEED, oldSpeed, speed);
		}
	}
}
