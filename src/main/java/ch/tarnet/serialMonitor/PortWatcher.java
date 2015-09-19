package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

class PortWatcher extends Thread {
	/**
	 * 
	 */
	private SerialManager serialManager;
	/**
	 * Le <code>PortWatcher</code> sera en �coute temps que <code>keepRunning</code> est vrai.
	 */
	private boolean keepRunning = true;
	/**
	 * La list des ports d�tect� par le Watcher
	 */
	private HashMap<CommPortIdentifier, SerialPortDescriptor> portsMap = new HashMap<CommPortIdentifier, SerialPortDescriptor>();
	/**
	 * la liste suivante d�tect� par le Watcher, on interverti les deux listes pour limiter la cr�ation d'objet
	 * volumineux
	 */
	private HashMap<CommPortIdentifier, SerialPortDescriptor> nextPortsMap = new HashMap<CommPortIdentifier, SerialPortDescriptor>();
	
	public PortWatcher() {
		super("SerialPortWatcher");
		this.setDaemon(true);
	}
	
	public void setSerialManager(SerialManager manager) {
		this.serialManager = manager;
	}
	
	synchronized public List<SerialPortDescriptor> getAvailablePorts() {
		return new ArrayList<SerialPortDescriptor>(portsMap.values()); 
	}

	@Override
	public void run() {
		while(keepRunning) {
			// On r�cup�re la liste des ports com visible par RXTX
			Enumeration<CommPortIdentifier> ports = getPortIdentifiers();
			
			// On test chaque port r�guli�rement
			while(ports.hasMoreElements()) {
				CommPortIdentifier portId = ports.nextElement();
				// on ne s'interesse qu'aux ports s�rie
				if(portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					if(portsMap.containsKey(portId)) {
						nextPortsMap.put(portId, portsMap.get(portId));
					}
					else {
						SerialPortDescriptor descriptor = new SerialPortDescriptor(portId, SerialManager.DEFAULT_SPEED);
						nextPortsMap.put(portId, descriptor);
						serialManager.firePortAddedEvent(descriptor);
					}
				}
			}
			
			// ainsi que ceux qu'on connait d�j�
			for(CommPortIdentifier oldPortId : portsMap.keySet()) {
				if(nextPortsMap.containsKey(oldPortId)) {
					// tout va bien
				}
				else {
					serialManager.firePortRemovedEvent(portsMap.get(oldPortId));
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
					this.wait(SerialManager.PORT_WATCHER_WAIT);
				}
			} catch (InterruptedException e) {
				System.out.println("SerialWatcher force refresh.");
			}
		}
	}
	
	/**
	 * Juste pour ne pas avoir de warrning dans l'�diteur et limiter la port�e de <code>SuppressWarnings</code>
	 * @param e une Enumeration contenant des <code>CommPortIdentifier</code>
	 * @return l'<code>Enumeration</code> cast� de fa�on non sure et <code>Enumeration&lt;CommPortIdentifer></code>
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Enumeration<CommPortIdentifier> unsafeCast(Enumeration e) {
		return (Enumeration<CommPortIdentifier>)e;
	}
	
	
	protected Enumeration<CommPortIdentifier> getPortIdentifiers()  {
	/*	Vector<CommPortIdentifier> v = new Vector<CommPortIdentifier>();
		Enumeration<CommPortIdentifier> e = unsafeCast(CommPortIdentifier.getPortIdentifiers());
		while(e.hasMoreElements()) {
			v.add(e.nextElement());
		}
		v.add(new FakeIdentifier("COM91"));
		return v.elements();*/
		return unsafeCast(CommPortIdentifier.getPortIdentifiers());
	}
}