package ch.tarnet.serialMonitor;

import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.tarnet.serialMonitor.services.SerialMessageEvent;
import ch.tarnet.serialMonitor.services.SerialMessageListener;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortEvent;
import ch.tarnet.serialMonitor.services.SerialPortListener;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor.Status;
import ch.tarnet.serialMonitor.services.rxtx.RxtxPortWatcher;
import ch.tarnet.serialMonitor.services.rxtx.RxtxSerialService;

/**
 * Pièce maitresse du programme, elle maintient la liste des ports série disponibles, centralise 
 * la récupération du contenu des communications et distribue ces messages aux différentes
 * fenêtres (sortie) souhaitant les afficher.
 * 
 * Cette classe arbitre différents threads permettant un déroulement non-bloquant du reste du programme.
 * Un premier thread, {@link RxtxPortWatcher} scrute à interval régulier la disponibilité de ports de 
 * communication serie, déclanchant des évènements lorsque un nouveau port est détecté ou qu'un port est
 * retiré. Les {@link SerialPortListener} permettent d'être prévenu lorsque d'un de ces évènements ce produit.
 * 
 * Un deuxième type de thread est utilisé, les {@link SerialWorker}, un nouveau thread est créé à chaque
 * ouverture d'un port, il a pour mission de gérer l'écoute du port et de transmettre au manager les 
 * messages reçus. Le manager appelle les {@link SerialMessageListener} pour faire suivre ces messages. 
 * 
 * @author tarrask
 *
 */
@Deprecated
public class SerialManager extends RxtxSerialService {
	public static final int PORT_WATCHER_WAIT = 60000;
	public static final int DEFAULT_SPEED = 9600;
	private static final Charset UTF8 = Charset.forName("utf8");
	
	
	private List<SerialMessageListener> messageListeners = new ArrayList<SerialMessageListener>();
	private List<SerialPortListener> portListeners = new ArrayList<SerialPortListener>();

	private RxtxPortWatcher portWatcher;
	private Map<SerialPortDescriptorImpl, SerialWorker> workers = new HashMap<SerialPortDescriptorImpl, SerialManager.SerialWorker>();
	
	public SerialManager() {
		this(new RxtxPortWatcher());
	}
	
	public SerialManager(RxtxPortWatcher portWatcher) {
		// démarre le port watcher qui surveille l'état des différents ports série
		this.portWatcher = portWatcher;
		this.portWatcher.setSerialManager(this);
		this.portWatcher.start();
	}
	/**
	 * Retourne la liste des ports disponible actuellement en possession du portWatcher.
	 * Cette fonction ne déclanche pas un refresh.
	 * @return
	 */
	public List<? extends SerialPortDescriptor> getAvailablePorts() {
		return portWatcher.getAvailablePorts();
	}
	
	/**
	 * Indique au PortWatcher qu'il doit interrompre son attente et procéder de suite à la récupération des
	 * ports série disponible.
	 */
	public void refreshPorts() {
		portWatcher.interrupt();
	}

	/**
	 * Ouvre un port série s'il ne l'est pas déjé. Démarre un SerialWorker pour récupérer les données
	 * transmises par le port.
	 * @param descriptor Tout ce qu'il faut savoir pour ouvrir et configurer le port.
	 */
	public void openPort(SerialPortDescriptorImpl descriptor) {
		// si le port est déjà ouvert par nos soins, on ne fait rien
		if(descriptor.getStatus() == Status.OPEN) {
			return;
		}
		SerialWorker worker = new SerialWorker(this, descriptor);
		workers.put(descriptor, worker);
		worker.start();
	}
	
	/**
	 * Ferme le port identifié par le descriptor.
	 * @param descriptor 
	 */
	public void closePort(SerialPortDescriptorImpl descriptor) {
		// recherche le worker qui aurait ouvert le port
		SerialWorker worker = workers.get(descriptor);
		if(worker != null) {
			worker.closePort();
		}
	}
	
	public void closeAllPorts() {
		for(SerialWorker worker : workers.values()) {
			worker.closePort();
		}
		workers.clear();
	}

	public void addSerialPortListener(SerialPortListener listener) {
		portListeners.add(listener);
	}
	
	public void addSerialMessageListener(SerialMessageListener listener) {
		messageListeners.add(listener);
	}
	
	
	private void fireSystemMessageEvent(SerialPortDescriptorImpl descriptor, String message) {
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSystemMessage(event);
		}
	}
	
	private void fireSerialMessageEvent(SerialPortDescriptorImpl descriptor, String message) {
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSerialMessage(event);
		}
	}

	public void firePortAddedEvent(SerialPortDescriptorImpl descriptor) {
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portAdded(event);
		}
	}

	public void firePortRemovedEvent(SerialPortDescriptorImpl descriptor) {
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portRemoved(event);
		}
	}

	
	static private class SerialWorker extends Thread {
		private static int openIndex = 0;
		
		private SerialManager manager;
		private SerialPortDescriptorImpl descriptor;
		private SerialPort port;
		
		private boolean keepRunning = true;
		
		public SerialWorker(SerialManager manager, SerialPortDescriptorImpl descriptor) {
			this.setDaemon(false);
			this.manager = manager;
			this.descriptor = descriptor;
		}
		
		public void closePort() {
			keepRunning = false;
		}

		@Override
		public void run() {
			BufferedInputStream in;
			// tente l'ouverture d'un port flux d'entré provenant du port série. En reste là si quoi que ce soit
			// se déroule mal
			try {
				System.out.println(descriptor.getPortIdentifier().getClass());
				port = (SerialPort)descriptor.getPortIdentifier().open(getClass().getName(), openIndex++);
				port.setSerialPortParams(descriptor.getSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				in = new BufferedInputStream(port.getInputStream());
			}
			catch(PortInUseException e) {
				manager.fireSystemMessageEvent(descriptor, "Port " + descriptor.getName() + " is already in use. Owner is " + descriptor.getPortIdentifier().getCurrentOwner() + ".");
				System.err.println(e.getMessage());
				return;
			}
			catch(ClassCastException e) {
				manager.fireSystemMessageEvent(descriptor, "Port " + descriptor.getName() + " is not a serial port.");
				System.err.println(e.getMessage());
				port.close();
				return;
			}
			catch(UnsupportedCommOperationException e) {
				manager.fireSystemMessageEvent(descriptor, "Incompatible params for port " + descriptor.getName() + ".");
				System.err.println(e.getMessage());
				port.close();
				return;
			}
			catch(IOException e) {
				manager.fireSystemMessageEvent(descriptor, "Unable to open input stream for port " + descriptor.getName() + ".");
				System.err.println(e.getMessage());
				port.close();
				return;
			}
			
			// Aquite de l'ouverture du port.
			descriptor.setStatus(Status.OPEN);
			manager.fireSystemMessageEvent(descriptor, "Connection with serial port " + descriptor.getName() + " is open.");
			
			// On écoute les changements de vitesse
			descriptor.addPropertyChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if(event.getPropertyName() == "speed") {
						try {
							port.setSerialPortParams(descriptor.getSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
						}
						catch(UnsupportedCommOperationException e) {
							manager.fireSystemMessageEvent(descriptor, "Incompatible params for port " + descriptor.getName() + ".");
							System.err.println(e.getMessage());
							port.close();
							return;
						}
					}
				}
			});

			// boucle de lecture, si une erreur survient, on ferme le port simplement. A Chaque série de donnée, on
			// envoie un event.
			try {
				byte[] buffer = new byte[256];
				while(keepRunning) {
					if(in.available() > 0) {
						int count = in.read(buffer);
						manager.fireSerialMessageEvent(descriptor, new String(buffer, 0, count, UTF8));
					}
					// On attend une petite millisecond pour ne pas burner le processeur
					try {
						synchronized(this) {
							this.wait(1);
						}
					} catch (InterruptedException e) { /* ignoré */ }
				}
			}
			catch(IOException e) {
				manager.fireSystemMessageEvent(descriptor, "Error reading port " + descriptor.getName() + ".");
				System.err.println(e.getMessage());
			}
			finally {
				port.close();
				descriptor.setStatus(Status.CLOSE);
				//retire le worker de la liste des workers actifs.
				manager.workers.remove(descriptor);
				manager.fireSystemMessageEvent(descriptor, "Connection with serial port " + descriptor.getName() + " is close.");
			}
		}
	}
}


