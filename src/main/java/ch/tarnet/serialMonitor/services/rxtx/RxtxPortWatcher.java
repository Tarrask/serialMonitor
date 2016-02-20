package ch.tarnet.serialMonitor.services.rxtx;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.serialMonitor.services.rxtx.RxtxSerialService.RxtxSerialPortDescriptor;

public class RxtxPortWatcher extends Thread {
	
	private static final Logger logger = LoggerFactory.getLogger(RxtxPortWatcher.class);
	
	/**
	 * 
	 */
	private RxtxSerialService serialService;
	/**
	 * Le <code>PortWatcher</code> sera en écoute temps que <code>keepRunning</code> est vrai.
	 */
	private boolean keepRunning = true;
	/**
	 * La liste des ports détectés par le Watcher
	 */
	private HashMap<CommPortIdentifier, RxtxSerialPortDescriptor> portsMap = new HashMap<CommPortIdentifier, RxtxSerialPortDescriptor>();
	/**
	 * la liste suivante détectée par le Watcher, on interverti les deux listes pour limiter la création d'objet
	 * volumineux
	 */
	private HashMap<CommPortIdentifier, RxtxSerialPortDescriptor> nextPortsMap = new HashMap<CommPortIdentifier, RxtxSerialPortDescriptor>();
	
	@Deprecated
	public RxtxPortWatcher() {
		super("SerialPortWatcher");
		this.setDaemon(true);
	}
	
	public RxtxPortWatcher(RxtxSerialService serialService) {
		super("SerialPortWatcher");
		this.serialService = serialService;
		this.setDaemon(true);
	}

	public void setSerialManager(RxtxSerialService serialService) {
		this.serialService = serialService;
	}
	
	synchronized public List<RxtxSerialPortDescriptor> getAvailablePorts() {
		return new ArrayList<RxtxSerialPortDescriptor>(portsMap.values()); 
	}

	@Override
	public void run() {
		logger.info("Start monitoring serial ports ... ");
		while(keepRunning) {
			// On récupère la liste des ports com visible par RXTX
			Enumeration<CommPortIdentifier> ports = getPortIdentifiers();
			
			// On test chaque port actuellement visible
			while(ports.hasMoreElements()) {
				CommPortIdentifier portId = ports.nextElement();
				// on ne s'interesse qu'aux ports série
				if(portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					// si le port est déjà dans notre liste, on le reporte juste dans la nouvelle
					if(portsMap.containsKey(portId)) {
						nextPortsMap.put(portId, portsMap.get(portId));
					}
					// sinon on l'ajoute à la nouvelle liste et on signal son apparission
					else {
						RxtxSerialPortDescriptor descriptor = new RxtxSerialPortDescriptor(
								portId, RxtxSerialService.DEFAULT_SPEED);
						nextPortsMap.put(portId, descriptor);
						serialService.firePortAddedEvent(descriptor);
					}
				}
			}
			
			// ainsi que ceux qu'on connait déjà
			for(CommPortIdentifier oldPortId : portsMap.keySet()) {
				if(nextPortsMap.containsKey(oldPortId)) {
					// tout va bien
				}
				else {
					serialService.firePortRemovedEvent(portsMap.get(oldPortId));
				}
			}
			
			// on inverse la liste actuelle avec la nouvelle liste et on vide la future nouvelle liste.
			HashMap<CommPortIdentifier, RxtxSerialPortDescriptor> tempPortsMap = portsMap;
			portsMap = nextPortsMap;
			nextPortsMap = tempPortsMap;
			nextPortsMap.clear();
			
			// On veille un certain temps
			try {
				synchronized(this) {
					this.wait(RxtxSerialService.PORT_WATCHER_WAIT);
				}
			} catch (InterruptedException e) {
				logger.debug("Forcing serial port list refresh.");
			}
		}
	}
	
	protected Enumeration<CommPortIdentifier> getPortIdentifiers()  {
		return unsafeCast(CommPortIdentifier.getPortIdentifiers());
	}

	/**
	 * Juste pour ne pas avoir de warning dans l'éditeur et limiter la portée de <code>SuppressWarnings</code>
	 * @param e une Enumeration contenant des <code>CommPortIdentifier</code>
	 * @return l'<code>Enumeration</code> casté de façon non sure en <code>Enumeration&lt;CommPortIdentifer></code>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Enumeration<CommPortIdentifier> unsafeCast(Enumeration e) {
		return (Enumeration<CommPortIdentifier>)e;
	}
}