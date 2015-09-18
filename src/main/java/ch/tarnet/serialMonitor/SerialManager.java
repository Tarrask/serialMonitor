package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

/**
 * Pièce maitresse du programme, elle maintient la liste des ports série disponibles, centralise 
 * la récupération du contenu des communications et distribue ces messages aux différentes
 * fenêtres (sortie) souhaitant les afficher.
 * 
 * Cette classe arbitre différents threads permettant un déroulement non-bloquant du reste du programme.
 * Un premier thread, {@link PortWatcher} scrute à interval régulier la disponibilité de ports de 
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
public class SerialManager {
	private static final int PORT_WATCHER_WAIT = 60000;
	public 	static final int DEFAULT_SPEED = 9600;
	
	
	private List<SerialMessageListener> messageListeners = new ArrayList<SerialMessageListener>();
	private List<SerialPortListener> portListeners = new ArrayList<SerialPortListener>();

	private PortWatcher portWatcher;
	private Map<SerialPortDescriptor, SerialWorker> workers = new HashMap<SerialPortDescriptor, SerialManager.SerialWorker>();
	
	public SerialManager() {
		// démarre le port watcher qui surveille l'état des différents ports série
		portWatcher = new PortWatcher();
		portWatcher.start();
	}
	
	/**
	 * Retourne la liste des ports disponible actuellement en possession du portWatcher.
	 * Cette fonction ne déclanche pas un refresh.
	 * @return
	 */
	public List<SerialPortDescriptor> getAvailablePorts() {
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
	 * Ouvre un port série s'il ne l'est pas déjà. Démarre un SerialWorker pour récupérer les données
	 * transmises par le port.
	 * @param descriptor Tout ce qu'il faut savoir pour ouvrir et configurer le port.
	 */
	public void openPort(SerialPortDescriptor descriptor) {
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
	public void closePort(SerialPortDescriptor descriptor) {
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
	
	
	private void fireSystemMessageEvent(SerialPortDescriptor descriptor, String message) {
		System.err.println(descriptor.getName() + " - " + message);
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSystemMessage(event);
		}
	}
	
	private void fireSerialMessageEvent(SerialPortDescriptor descriptor, String message) {
		System.out.print(descriptor.getName() + " - " + message);
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSerialMessage(event);
		}
	}

	private void firePortAddedEvent(SerialPortDescriptor descriptor) {
		System.out.println("Fire port added event: " + descriptor.getName());
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portAdded(event);
		}
	}

	private void firePortRemovedEvent(SerialPortDescriptor descriptor) {
		System.out.println("Fire port removed event: " + descriptor.getName());
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portRemoved(event);
		}
	}
	
	private void firePortStatusChangeEvent(SerialPortDescriptor descriptor) {
		System.out.println("Fire port status change event: " + descriptor.getName());
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portStatusChanged(event);
		}
	}

	
	static private class SerialWorker extends Thread {
		private static int openIndex = 0;
		
		private SerialManager manager;
		private SerialPortDescriptor descriptor;
		private SerialPort port;
		
		private boolean keepRunning = true;
		
		public SerialWorker(SerialManager manager, SerialPortDescriptor descriptor) {
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
			manager.firePortStatusChangeEvent(descriptor);

			// boucle de lecture, si une erreur survient, on ferme le port simplement. A Chaque série de donnée, on
			// envoie un event.
			try {
				byte[] buffer = new byte[256];
				while(keepRunning) {
					if(in.available() > 0) {
						int count = in.read(buffer);
						manager.fireSerialMessageEvent(descriptor, new String(buffer, 0, count));
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
				manager.firePortStatusChangeEvent(descriptor);
			}
		}
	}
	
	private class PortWatcher extends Thread {
		/**
		 * Le <code>PortWatcher</code> sera en écoute temps que <code>keepRunning</code> est vrai.
		 */
		private boolean keepRunning = true;
		/**
		 * La list des ports détecté par le Watcher
		 */
		private HashMap<CommPortIdentifier, SerialPortDescriptor> portsMap = new HashMap<CommPortIdentifier, SerialPortDescriptor>();
		/**
		 * la liste suivante détecté par le Watcher, on interverti les deux listes pour limiter la création d'objet
		 * volumineux
		 */
		private HashMap<CommPortIdentifier, SerialPortDescriptor> nextPortsMap = new HashMap<CommPortIdentifier, SerialPortDescriptor>();
		
		public PortWatcher() {
			super("SerialManager.PortWatcher");
			this.setDaemon(true);
		}
		
		synchronized public List<SerialPortDescriptor> getAvailablePorts() {
			return new ArrayList<SerialPortDescriptor>(portsMap.values()); 
		}

		@Override
		public void run() {
			while(keepRunning) {
				// On récupère la liste des ports com visible par RXTX
				Enumeration<CommPortIdentifier> ports = unsafeCast(CommPortIdentifier.getPortIdentifiers());
				
				// On test chaque port régulièrement
				while(ports.hasMoreElements()) {
					CommPortIdentifier portId = ports.nextElement();
					// on ne s'interesse qu'aux ports série
					if(portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
						if(portsMap.containsKey(portId)) {
							nextPortsMap.put(portId, portsMap.get(portId));
						}
						else {
							SerialPortDescriptor descriptor = new SerialPortDescriptor(portId, DEFAULT_SPEED);
							nextPortsMap.put(portId, descriptor);
							firePortAddedEvent(descriptor);
						}
					}
				}
				
				// ainsi que ceux qu'on connait déjà
				for(CommPortIdentifier oldPortId : portsMap.keySet()) {
					if(nextPortsMap.containsKey(oldPortId)) {
						// tout va bien
					}
					else {
						firePortRemovedEvent(portsMap.get(oldPortId));
					}
				}
				
				// on inverse la liste actuelle avec la nouvelle liste et on vide la future nouvelle liste.
				HashMap<CommPortIdentifier, SerialPortDescriptor> tempPortsMap = portsMap;
				portsMap = nextPortsMap;
				nextPortsMap = tempPortsMap;
				nextPortsMap.clear();
				
				// On veille un certain temps
				try {
					synchronized(this) {
						this.wait(PORT_WATCHER_WAIT);
					}
				} catch (InterruptedException e) {
					System.out.println("SerialWatcher force refresh.");
				}
			}
		}
		
		/**
		 * Juste pour ne pas avoir de warrning dans l'éditeur et limiter la portée de <code>SuppressWarnings</code>
		 * @param e une Enumeration contenant des <code>CommPortIdentifier</code>
		 * @return l'<code>Enumeration</code> casté de façon non sure et <code>Enumeration&lt;CommPortIdentifer></code>
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Enumeration<CommPortIdentifier> unsafeCast(Enumeration e) {
			return (Enumeration<CommPortIdentifier>)e;
		}
	}

}


