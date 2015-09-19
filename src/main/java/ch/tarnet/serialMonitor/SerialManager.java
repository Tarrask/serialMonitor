package ch.tarnet.serialMonitor;

import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

/**
 * Pi�ce maitresse du programme, elle maintient la liste des ports s�rie disponibles, centralise 
 * la r�cup�ration du contenu des communications et distribue ces messages aux diff�rentes
 * fen�tres (sortie) souhaitant les afficher.
 * 
 * Cette classe arbitre diff�rents threads permettant un d�roulement non-bloquant du reste du programme.
 * Un premier thread, {@link PortWatcher} scrute � interval r�gulier la disponibilit� de ports de 
 * communication serie, d�clanchant des �v�nements lorsque un nouveau port est d�tect� ou qu'un port est
 * retir�. Les {@link SerialPortListener} permettent d'�tre pr�venu lorsque d'un de ces �v�nements ce produit.
 * 
 * Un deuxi�me type de thread est utilis�, les {@link SerialWorker}, un nouveau thread est cr�� � chaque
 * ouverture d'un port, il a pour mission de g�rer l'�coute du port et de transmettre au manager les 
 * messages re�us. Le manager appelle les {@link SerialMessageListener} pour faire suivre ces messages. 
 * 
 * @author tarrask
 *
 */
public class SerialManager {
	public static final int PORT_WATCHER_WAIT = 60000;
	public static final int DEFAULT_SPEED = 9600;
	
	
	private List<SerialMessageListener> messageListeners = new ArrayList<SerialMessageListener>();
	private List<SerialPortListener> portListeners = new ArrayList<SerialPortListener>();

	private PortWatcher portWatcher;
	private Map<SerialPortDescriptor, SerialWorker> workers = new HashMap<SerialPortDescriptor, SerialManager.SerialWorker>();
	
	public SerialManager() {
		this(new PortWatcher());
	}
	
	public SerialManager(PortWatcher portWatcher) {
		// d�marre le port watcher qui surveille l'�tat des diff�rents ports s�rie
		this.portWatcher = portWatcher;
		this.portWatcher.setSerialManager(this);
		this.portWatcher.start();
	}
	/**
	 * Retourne la liste des ports disponible actuellement en possession du portWatcher.
	 * Cette fonction ne d�clanche pas un refresh.
	 * @return
	 */
	public List<SerialPortDescriptor> getAvailablePorts() {
		return portWatcher.getAvailablePorts();
	}
	
	/**
	 * Indique au PortWatcher qu'il doit interrompre son attente et proc�der de suite � la r�cup�ration des
	 * ports s�rie disponible.
	 */
	public void refreshPorts() {
		portWatcher.interrupt();
	}

	/**
	 * Ouvre un port s�rie s'il ne l'est pas d�j�. D�marre un SerialWorker pour r�cup�rer les donn�es
	 * transmises par le port.
	 * @param descriptor Tout ce qu'il faut savoir pour ouvrir et configurer le port.
	 */
	public void openPort(SerialPortDescriptor descriptor) {
		// si le port est d�j� ouvert par nos soins, on ne fait rien
		if(descriptor.getStatus() == Status.OPEN) {
			return;
		}
		SerialWorker worker = new SerialWorker(this, descriptor);
		workers.put(descriptor, worker);
		worker.start();
	}
	
	/**
	 * Ferme le port identifi� par le descriptor.
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
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSystemMessage(event);
		}
	}
	
	private void fireSerialMessageEvent(SerialPortDescriptor descriptor, String message) {
		SerialMessageEvent event = new SerialMessageEvent(System.currentTimeMillis(), descriptor, message);
		for(SerialMessageListener listener : messageListeners) {
			listener.newSerialMessage(event);
		}
	}

	void firePortAddedEvent(SerialPortDescriptor descriptor) {
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portAdded(event);
		}
	}

	void firePortRemovedEvent(SerialPortDescriptor descriptor) {
		SerialPortEvent event = new SerialPortEvent(descriptor);
		for(SerialPortListener listener : portListeners) {
			listener.portRemoved(event);
		}
	}
	
	private void firePortStatusChangeEvent(SerialPortDescriptor descriptor) {
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
			// tente l'ouverture d'un port flux d'entr� provenant du port s�rie. En reste l� si quoi que ce soit
			// se d�roule mal
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
			manager.firePortStatusChangeEvent(descriptor);

			// boucle de lecture, si une erreur survient, on ferme le port simplement. A Chaque s�rie de donn�e, on
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
					} catch (InterruptedException e) { /* ignor� */ }
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
}


