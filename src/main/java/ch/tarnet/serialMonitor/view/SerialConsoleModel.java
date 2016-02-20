package ch.tarnet.serialMonitor.view;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor;

public class SerialConsoleModel {

	public static final String SELECTED_PORT = "selectedPort";
	public static final String WATCHED_PORTS = "watchedPorts";
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	private LogDocument logDocument = new LogDocument();
	
	/** le port sélectionné actuellement */
	private SerialPortDescriptor selectedPort = null;
	
	/** l'ensemble des ports actuellement surveillés */
	private HashSet<SerialPortDescriptor> watchedPorts = new HashSet<>();
	
	/** la map regroupant les ports par nom et la configuration spécifique à la console qui les concerne.
	 *  On lie la configuration au moyen du nom du port et nom pas du descriptor pour concerver la config
	 *  même après avoir connecté puis déconnecté un port */
	private HashMap<SerialPortDescriptor, ConsoleSpecPortDescriptor> knownConfig = new HashMap<>();
	
	private PropertyChangeListener selectedPortListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String name = evt.getPropertyName();
			PropertyChangeEvent newEvent = new PropertyChangeEvent(this, SELECTED_PORT + "." + name, evt.getOldValue(), evt.getNewValue());
			newEvent.setPropagationId(evt.getPropagationId());
			pcs.firePropertyChange(newEvent);
		}
	};
	public LogDocument getLogDocument() {
		return logDocument;
	}

	public SerialPortDescriptor getSelectedPort() {
		return selectedPort;
	}
	
	public void setSelectedPort(SerialPortDescriptor port) {
		SerialPortDescriptor oldValue = getSelectedPort();
		this.selectedPort = port;
		if(oldValue != null) {
			oldValue.removePropertyChangeListener(selectedPortListener);
		}
		if(port != null) {
			port.addPropertyChangeListener(selectedPortListener);
		}
		
		// notifie que le port sélectionné à changé
		pcs.firePropertyChange(SELECTED_PORT, oldValue, port);
		
		// notifie que les propriétés du port selectionné ont aussi certainnement changé, le pcs se charge de filtrer
		pcs.firePropertyChange(SELECTED_PORT + "." + SerialPortDescriptor.NAME, oldValue==null?null:oldValue.getName(), port==null?null:port.getName());
		pcs.firePropertyChange(SELECTED_PORT + "." + SerialPortDescriptor.SPEED, oldValue==null?null:oldValue.getSpeed(), port==null?null:port.getSpeed());
		pcs.firePropertyChange(SELECTED_PORT + "." + SerialPortDescriptor.STATUS, oldValue==null?null:oldValue.getStatus(), port==null?null:port.getStatus());
	}
	
	public void addWatchedPort(SerialPortDescriptor port) {
		watchedPorts.add(port);
		pcs.firePropertyChange(WATCHED_PORTS, null, watchedPorts);
	}
	
	public boolean isPortWatched(SerialPortDescriptor port) {
		return watchedPorts.contains(port);
	}

	public void removeWatchedPort(SerialPortDescriptor port) {
		watchedPorts.remove(port);
		pcs.firePropertyChange(WATCHED_PORTS, null, watchedPorts);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName, listener);
	}

	public void addSelectedPortPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(SELECTED_PORT + "." + propertyName, listener);
	}
	
	/**
	 * Retour l'objet contenant la config spécifique à un port pour cette console.
	 * S'il n'existe pas, il est créé avant d'être retourné.
	 * @param descriptor Le descriptor qui permettera d'identifier le bon descripteur spécifique
	 * @return le descripteur du port spécifique à cette fenêtre.
	 */
	public ConsoleSpecPortDescriptor getSpecDescriptor(SerialPortDescriptor port) {
		ConsoleSpecPortDescriptor specDescriptor = knownConfig.get(port);
		if(specDescriptor == null) {
			specDescriptor = new ConsoleSpecPortDescriptor();
			knownConfig.put(port, specDescriptor);
		}
		return specDescriptor;
	}
	
	/**
	 * Stock les valeurs liés à un port pour une fenêtre donnée.
	 * @author tarrask
	 *
	 */
	public class ConsoleSpecPortDescriptor {
		private Color color;
		private Invocable filter;
		
		public ConsoleSpecPortDescriptor() {
			color = Color.decode(Pref.get("defaultForeground", "#000"));
			
//			ScriptEngine engine = scriptEngineFactory.getEngineByName("JavaScript");
//			filter = (Invocable) engine;
//			try {
//				engine.eval("function filter(args) { "
//						  + "  if(args.message.indexOf('Hello') >= 0) {"
//						  + "    args.style = javax.swing.text.StyleContext.getDefaultStyleContext().addStyle('pink', args.style);"
//						  + "    javax.swing.text.StyleConstants.setForeground(args.style, java.awt.Color.pink);"
//						  + "  }; "
//						  + "  return args; "
//						  + "}");
//			}
//			catch(ScriptException e) {
//				System.err.println(e.getMessage());
//			}
		}
		
		public Invocable getFilter() {
			return filter;
		}

		public Color getColor() {
			return color;
		}
		
		public void setColor(Color color) {
			this.color = color;
		}
	}
}
