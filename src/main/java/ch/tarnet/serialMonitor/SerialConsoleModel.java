package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

public class SerialConsoleModel {

	private static final Logger logger = Logger.getLogger(SerialConsoleModel.class.getName());
	
	private SerialManager manager;
	
	private SerialPortDescriptor activePort = null;
	private DefaultComboBoxModel<SerialPortDescriptor> availablePorts = new DefaultComboBoxModel<SerialPortDescriptor>();
	private DefaultComboBoxModel<Integer> activePortSpeed = new DefaultComboBoxModel<Integer>(new Integer[] {4800, 9600, 19200, 38400, 57600, 115200, 230400, 250000});
	private HashSet<SerialPortDescriptor> watchedPorts = new HashSet<SerialPortDescriptor>();
	private HashMap<String, ConsoleSpecPortDescriptor> knownConfig = new HashMap<String, ConsoleSpecPortDescriptor>();
	private StyledDocument logDocument = new DefaultStyledDocument();
	
	private Style logStyle;
	private Style systemStyle;

	private PlainDocument commandText = new PlainDocument();
	private Action openCloseAction = new AbstractAction("Open") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if("open".equals(getValue("Action"))) {
				manager.openPort(activePort);
				watchedPorts.add(activePort);
			}
			else {
				manager.closePort(activePort);
				watchedPorts.remove(activePort);
			}
		}
	};
	
	private Action watchUnwatchAction = new AbstractAction("Watch") {
		@Override 
		public void actionPerformed(ActionEvent e) {
			if("watch".equals(getValue("Action"))) {
				if(activePort.getStatus() == Status.OPEN) {
					watchedPorts.add(activePort);
				}
			}
			else {
				watchedPorts.remove(activePort);
			}
			updateWatchUnwatchAction();
		}
	};
	
	private Action sendAction = new AbstractAction("Send") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
		}
	};

	/**
	 * Gère les évenements liés aux ports provenant du SerialManager
	 */
	private final SerialPortListener serialPortlistener = new SerialPortListener() {
		@Override 
		public void portAdded(SerialPortEvent event) {
			SerialPortDescriptor descriptor = event.getDescriptor();
			
			// trouve l'index trié
			int sortIndex = 0;
			while(sortIndex < availablePorts.getSize() &&
					descriptor.getName().compareToIgnoreCase(availablePorts.getElementAt(sortIndex).getName()) > 0) {
				sortIndex++;
			}
			
			// insert le port dans la liste
			availablePorts.insertElementAt(descriptor, sortIndex);
			
			// si aucune configuration pour ce port n'existe on en créé une par défaut
			if(!knownConfig.containsKey(descriptor.getName())) {
				knownConfig.put(descriptor.getName(), new ConsoleSpecPortDescriptor());
			}
		}


		@Override
		public void portRemoved(SerialPortEvent event) {
			SerialPortDescriptor descriptor = event.getDescriptor();
			availablePorts.removeElement(descriptor);
		}


		@Override
		public void portStatusChanged(SerialPortEvent event) {
		//	updateActivePortDependantModels();
			System.out.println("Should not call this anymore");
		}
	};
	
	/**
	 * Gère la reception des messages provenant des ports série géré par le manager
	 */
	private final SerialMessageListener serialMessageListener = new SerialMessageListener() {
		@Override
		public void newSystemMessage(final SerialMessageEvent event) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					printText(event.getMessage() + "\n", systemStyle);
				}
			});
		}

		@Override
		public void newSerialMessage(final SerialMessageEvent event) {
			if(watchedPorts.contains(event.getDescriptor())) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						printText(event.getMessage(), logStyle);
					}
				});
			}
		}
	};

	/**
	 * Gère le ComboBox listant les ports séries disponibles
	 */
	private final ListDataListener selectedPortListener = new ListDataListener() {
		@Override
		public void intervalAdded(ListDataEvent e) {
			if(activePort == null) {
				availablePorts.setSelectedItem(availablePorts.getElementAt(e.getIndex0()));
			}
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			if(availablePorts.getSize() > 0 && activePort == null) {
				availablePorts.setSelectedItem(availablePorts.getElementAt(0));
			}
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			updateActivePortDependantModels();
		}
	};
	
	private final ListDataListener selectedSpeedListener = new ListDataListener() {
		@Override
		public void intervalRemoved(ListDataEvent e) {
			logger.info("in selectedSpeedListener.intervalRemoved: Le speed ComboBox n'étant pas modifiable pour l'instant. Ce message ne devrait pas apparaitre.");
		}
		
		@Override
		public void intervalAdded(ListDataEvent e) {
			logger.info("in selectedSpeedListener.intervalAdded: Le speed ComboBox n'étant pas modifiable pour l'instant. Ce message ne devrait pas apparaitre.");
		}
		@Override
		public void contentsChanged(ListDataEvent e) {
			if(activePort != null) {
				activePort.setSpeed((Integer)activePortSpeed.getSelectedItem());
			}
		}
	};
	
	/**
	 * Ecoute les changements des valeurs du port actif actuel, comme la vitesse de transmission
	 * ou le status
	 */
	private final PropertyChangeListener activePortListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updateActivePortDependantModels();
		}
	};
	
	private final PropertyChangeListener portColorListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if(event.getPropertyName() == "Color" && activePort != null) {
				Object obj = event.getNewValue();
				if(obj instanceof Color) {
					ConsoleSpecPortDescriptor consoleDesc = knownConfig.get(activePort.getName());
					consoleDesc.setColor((Color)obj);
				}
			}
		}
	};

	public SerialConsoleModel(SerialManager manager) {
		// On se lie au manager
		this.manager = manager;
		this.manager.addSerialPortListener(serialPortlistener);
		this.manager.addSerialMessageListener(serialMessageListener);
		
		
		// On initialise et lie les listeners pour les différents model contenant les données
		// activePort: rien à faire, null au lancement de la console, est défini lors du peuplement des ports disponibles
		
		// availablePorts: la liste des ports série disponible qu'on peuple avec les valeurs provenant du manager
		availablePorts.addListDataListener(selectedPortListener);
		List<SerialPortDescriptor> ports = this.manager.getAvailablePorts();
		Collections.sort(ports, new Comparator<SerialPortDescriptor>() {
			@Override public int compare(SerialPortDescriptor o1, SerialPortDescriptor o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for(SerialPortDescriptor descriptor : ports) {
			availablePorts.addElement(descriptor);
		}
		
		// activePortSpeed: la liste des vitesses disponible, initialisé avec la valeur de l'activePort
		activePortSpeed.addListDataListener(selectedSpeedListener);
		if(activePortSpeed.getSelectedItem() == null) {
			activePortSpeed.setSelectedItem(Pref.getInt("defaultSerialSpeed", SerialManager.DEFAULT_SPEED));
			activePortSpeed.setSelectedItem(1000);
		}
		
		// watchedPorts: rien à faire, la liste est vide au lancement de la console
		
		// logDocument: initialise les styles
		Enumeration<?> styles = ((DefaultStyledDocument)logDocument).getStyleNames();
		while(styles.hasMoreElements()) {
			System.out.println(styles.nextElement());
		}
		logStyle = logDocument.addStyle("log", null);
		StyleConstants.setFontFamily(logStyle, Pref.getString("defaultFontFamily", "Courier New"));
		StyleConstants.setFontSize(logStyle, Pref.getInt("defaultFontSize", 12));
		StyleConstants.setForeground(logStyle, Color.decode(Pref.getString("defaultForeground", "#222222")));
		
		systemStyle = logDocument.addStyle("red", logStyle);
		StyleConstants.setForeground(systemStyle, Color.decode(Pref.getString("defaultSystemForeground", "#aa0000")));
		
		// commandText: rien à faire, initialement vide, servira à qqch si l'utilisateur y saisie qqch
		
		// openCloseAction: rien à faire
		
		// watchUnwatchAction: rien à faire
		
		// sendAction: rien à faire TODO pour l'instant en tout cas
	}

	/**
	 * met à jour tous les modeles liés au port actif, en commençant par la variable activePort
	 */
	private void updateActivePortDependantModels() {
		updateActivePortListener();
		updateActivePortSpeed();
		updateOpenCloseAction();
		updateWatchUnwatchAction();
	}
	
	/**
	 * Appelé suite à un changement du port actif, met à jour la variable et transmet le listener
	 */
	private void updateActivePortListener() {
		SerialPortDescriptor oldPort = activePort;
		SerialPortDescriptor newPort = (SerialPortDescriptor) availablePorts.getSelectedItem();
		if(oldPort != newPort) {
			if(oldPort != null) {
				oldPort.removePropertyChangeListener(activePortListener);
			}
			activePort = newPort;
			activePort.addPropertyChangeListener(activePortListener);
		}
	}

	/**
	 * Appelé suite à un changement du port actif
	 */
	private void updateActivePortSpeed() {
		if(activePort == null) {
			activePortSpeed.setSelectedItem(Pref.getInt("defaultSerialSpeed", SerialManager.DEFAULT_SPEED));	//$NON-NLS-1$
		}
		else {
			activePortSpeed.setSelectedItem(activePort.getSpeed());
		}
	}

	/**
	 * Appelé suite au changement du port actif, met à jour le bouton open/close en fonction du
	 * nouveau port série selectionné
	 */
	private void updateOpenCloseAction() {
		if(activePort == null) {
			openCloseAction.setEnabled(false);
			openCloseAction.putValue("Name", "Open"); 		//$NON-NLS-1$
			openCloseAction.putValue("Action", "open");		//$NON-NLS-1$ //$NON-NLS-2$
		}
		else if(activePort.getStatus() == Status.OPEN) {
			openCloseAction.setEnabled(true);
			openCloseAction.putValue("Name", "Close");		//$NON-NLS-1$
			openCloseAction.putValue("Action", "close");	//$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			openCloseAction.setEnabled(true);
			openCloseAction.putValue("Name", "Open");		//$NON-NLS-1$
			openCloseAction.putValue("Action", "open");		//$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private void updateColorAction() {
		
	}
	
	/**
	 * Appelé suite au changement du port actif, met à jour le bouton watch/unwatch en fonction
	 * du nouveau port sélectionné.
	 */
	private void updateWatchUnwatchAction() {
		if(activePort == null) {
			watchUnwatchAction.setEnabled(false);
			watchUnwatchAction.putValue("Name", "Watch");		//$NON-NLS-1$
			watchUnwatchAction.putValue("Action", "watch");		//$NON-NLS-1$ //$NON-NLS-2$
		}
		else if(watchedPorts.contains(activePort)) {
			watchUnwatchAction.setEnabled(true);
			watchUnwatchAction.putValue("Name", "Unwatch");		//$NON-NLS-1$
			watchUnwatchAction.putValue("Action", "unwatch");	//$NON-NLS-1$ //$NON-NLS-2$
		}
		else if(activePort.getStatus() == Status.OPEN) {
			watchUnwatchAction.setEnabled(true);
			watchUnwatchAction.putValue("Name", "Watch");		//$NON-NLS-1$
			watchUnwatchAction.putValue("Action", "watch");		//$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			watchUnwatchAction.setEnabled(false);
			watchUnwatchAction.putValue("Name", "Watch");		//$NON-NLS-1$
			watchUnwatchAction.putValue("Action", "watch");		//$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Affiche du text dans la zone de log, le style étant specifié
	 * 
	 * @param text Le texte à afficher
	 * @param style le style à employer
	 */
	private void printText(String text, Style style) {
		try {
			logDocument.insertString(logDocument.getLength(), text, style);
		}
		catch(BadLocationException e) {
			// ne devrait jamais survenir, ne semble pas grave, au pire aucun text ne sera plus écrit.
			// on log l'erreur mais poursuivons l'execution du programme.
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Un accès au différent model, utilisé par SerialConsole lors de l'initialisation du GUI
	 * @return
	 */
	public ComboBoxModel<SerialPortDescriptor> getAvailablePortsModel() {
		return availablePorts;
	}
	public ComboBoxModel<Integer> getActivePortSpeedModel() {
		return activePortSpeed;
	}
	public Action getOpenCloseAction() {
		return openCloseAction;
	}
	public Action getWatchUnwatchAction() {
		return watchUnwatchAction;
	}
	public StyledDocument getLogDocument() {
		return logDocument;
	}
	public Document getCommandModel() {
		return commandText;
	}
	public Action getSendAction() {
		return sendAction;
	}
	public abstract class ColorAction extends AbstractAction {
		public ColorAction() {
			addPropertyChangeListener(portColorListener);
		}
	}
	
	private class ConsoleSpecPortDescriptor {
		private Color color;
		
		public ConsoleSpecPortDescriptor() {
			color = Color.decode(Pref.get("defaultForeground", "#000"));
		}
		
		public Color getColor() {
			return color;
		}
		
		public void setColor(Color color) {
			this.color = color;
		}
	}
}
