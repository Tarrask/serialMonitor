package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

/**
 * Contient toutes les données requises par une SerialConsole pour fonctionner. La majorité des données
 * est stockée sous forme de model de données Swing (par ex.: ComboBoxModel, Document)
 * @author tarrask
 *
 */
public class SerialConsoleModel {

	private static final Logger logger = LoggerFactory.getLogger(SerialConsoleModel.class.getName());
	
	/** une référence au SerialManager global à toute l'application */
	private SerialManager manager;
	
	/** la fabrique de moteur de script */
	private ScriptEngineManager scriptEngineFactory;

	/** le port actuellement sélectionné par la comboBox, cette champ est définit dans updateActivePortListener
	 *  après avoir transférer le activePortListener vers le nouveau port sélectionné */ 
	private SerialPortDescriptor activePort = null;
	/** la liste des ports visibles par RXTX, surveillé par le SerialManager et mis à jour par le bias
	 *  du serialPortListener */
	private DefaultComboBoxModel<SerialPortDescriptor> availablePorts = new DefaultComboBoxModel<SerialPortDescriptor>();
	/** la liste des vitesses disponibles TODO devrait être éditable pour accepter n'importe quelle vitesse
	 *  TODO la liste de base devrait provenir d'un fichier de configuration */
	private DefaultComboBoxModel<Integer> activePortSpeed = new DefaultComboBoxModel<Integer>(new Integer[] {4800, 9600, 19200, 38400, 57600, 115200, 230400, 250000});
	/** la l'ensemble des ports actuellement surveillé */
	private HashSet<SerialPortDescriptor> watchedPorts = new HashSet<SerialPortDescriptor>();
	/** la map regroupant les ports par nom et la configuration spécifique à la console qui les concerne.
	 *  On lie la configuration au moyen du nom du port et nom pas du descriptor pour concerver la config
	 *  même après avoir connecté puis déconnecté un port */
	private HashMap<String, ConsoleSpecPortDescriptor> knownConfig = new HashMap<String, ConsoleSpecPortDescriptor>();
	/** Le document contenant l'ensemble du texte affiché par la console. Les capacités du document sont
	 *  limité à l'ajout en fin de document, TODO la suppression total du contenu et la suppression de ligne pour 
	 *  se conformer à une limite fixé par l'utilisateur. */
	private LogDocument logDocument = new LogDocument();
	
	/** le style de base utilisé par les messages provenant des ports série */
	private Style logStyle;
	/** le style utilisé pour les messages systemes */
	private Style systemStyle;

	/** l'action d'ouvrir ou de fermer le port série actif. Si l'ouverture se déroule
	 *  correctement, le port actif signalera son changement de status par le bais
	 *  du activePortListener.
	 *  Le port est automatiquement surveillé par la console l'ayant ouvert.
	 *  TODO la fin de la surveillance devrait survenir suite au changement de status du port et
	 *  TODO non lors de la fermeture du port. Afin que toutes les consoles arrêtent de surveiller le
	 *  TODO port lors de sa fermeture. */
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
	
	/** l'action de surveillance d'un port. Les ports surveillés sont affiché dans la console.
	 *  Les autres sont ignorés */
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
	
	/** Action permettant de modifier la couleur d'un port. Sont fonctionnement est lié
	 *  au bouton de type ColorButton. Ces boutons définissent la propriété ACTION_COLOR_KEY
	 *  pour transmettre la nouvelle couleur choisie */
	private Action colorAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(activePort != null) {
				ConsoleSpecPortDescriptor specDescriptor = getSpecDescriptor(activePort);
				specDescriptor.setColor((Color)getValue(ColorButton.ACTION_COLOR_KEY));
			}
		}
	};
	
	/** Le document lié au TextField de saisi de commande */
	private PlainDocument commandText = new PlainDocument();

	private Action sendAction = new AbstractAction("Send") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
		}
	};

	/**
	 * Gère les évenements liés aux ports provenant du SerialManager. Lorsque un nouveau port est
	 * détecté par le SerialManager, on l'ajoute à la liste de cette fenêtre trié par ordre alphabetique
	 * du nom de ports.
	 * Si un port n'est plus disponible, on le retire simplement.
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
		}


		@Override
		public void portRemoved(SerialPortEvent event) {
			SerialPortDescriptor descriptor = event.getDescriptor();
			availablePorts.removeElement(descriptor);
		}
	};
	
	/**
	 * Gêre la reception des messages provenant des ports série géré par le manager
	 */
	private final SerialMessageListener serialMessageListener = new SerialMessageListener() {
		@Override
		public void newSystemMessage(final SerialMessageEvent event) {
			StringBuilder sb = new StringBuilder();
			try {
				// s'assure que le message apparaisse sur une ligne unique.
				if(logDocument.getLength() > 0 && !"\n".equals(logDocument.getText(logDocument.getLength()-1, 1))) {
					sb.append("\n");
				}
			}
			catch(BadLocationException e) {
				// ne devrait pas arriver
				e.printStackTrace();
			}
			sb.append(event.getMessage()).append("\n");
			printText(sb.toString(), systemStyle);
		}

		@Override
		public void newSerialMessage(final SerialMessageEvent event) {
			if(watchedPorts.contains(event.getDescriptor())) {
				SerialPortDescriptor descriptor = event.getDescriptor();
				ConsoleSpecPortDescriptor specDescriptor = getSpecDescriptor(descriptor);
				Style style = logDocument.getStyle(descriptor.getName());
				if(style == null) {
					style = logDocument.addStyle(descriptor.getName(), logStyle);
				}
				StyleConstants.setForeground(style, specDescriptor.getColor());
				
				Invocable inv = specDescriptor.getFilter();
				boolean display = true;
				String message = event.getMessage();
				if(inv != null) {
					try {
						FilterMessage mes = new FilterMessage(message, style);
						mes =  (FilterMessage)inv.invokeFunction("filter", mes);
						display = mes.display;
						message = mes.message;
						style = mes.style;
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if(display) {
					printText(message, style);
				}
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

	public SerialConsoleModel(SerialManager manager) {
		// On se lie au manager
		this.manager = manager;
		this.manager.addSerialPortListener(serialPortlistener);
		this.manager.addSerialMessageListener(serialMessageListener);

		scriptEngineFactory = new ScriptEngineManager();
		
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
		logStyle = logDocument.addStyle("log", null);
		StyleConstants.setFontFamily(logStyle, Pref.getString("defaultFontFamily", "Courier New"));
		StyleConstants.setFontSize(logStyle,   Pref.getInt("defaultFontSize", 12));
		StyleConstants.setForeground(logStyle, Color.decode(Pref.getString("defaultForeground", "#000")));
		
		systemStyle = logDocument.addStyle("red", logStyle);
		StyleConstants.setForeground(systemStyle, Color.decode(Pref.getString("defaultSystemForeground", "#aa0000")));
		
		// commandText: rien à faire, initialement vide, servira à qqch si l'utilisateur y saisie qqch
		
		// openCloseAction: rien à faire
		
		// watchUnwatchAction: rien à faire
		
		// colorAction: rien à faire
		
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
		updateColorAction();
	}
	
	/**
	 * Appelé suite à un changement du port actif, met à jour la variable et transmet le listener
	 */
	private void updateActivePortListener() {
		SerialPortDescriptor oldDescriptor = activePort;
		SerialPortDescriptor newDescriptor = (SerialPortDescriptor) availablePorts.getSelectedItem();
		if(oldDescriptor != newDescriptor) {
			if(oldDescriptor != null) {
				oldDescriptor.removePropertyChangeListener(activePortListener);
				//knownConfig.get(oldDescriptor).removePropertyChangeListener();
			}
			newDescriptor.addPropertyChangeListener(activePortListener);
			activePort = newDescriptor;
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
		if(activePort == null) {
			colorAction.putValue(ColorButton.ACTION_COLOR_KEY, Color.decode(Pref.get("defaultForeground", "#000")));
		}
		else {
			ConsoleSpecPortDescriptor specDescriptor = getSpecDescriptor(activePort);
			colorAction.putValue(ColorButton.ACTION_COLOR_KEY, specDescriptor.getColor());
		}
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
	 * @throws IOException 
	 */
	private void printText(final String text, final Style style) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				logDocument.appendString(text, style);
			}
		});
	}

	/**
	 * Retour l'objet contenant la config spécifique à un port pour cette console.
	 * S'il n'existe pas, il est créé avant d'être retourné.
	 * @param descriptor Le descriptor qui permettera d'identifier le bon descripteur spécifique
	 * @return le descripteur du port spécifique à cette fenêtre.
	 */
	private ConsoleSpecPortDescriptor getSpecDescriptor(SerialPortDescriptor descriptor) {
		ConsoleSpecPortDescriptor specDescriptor = knownConfig.get(descriptor.getName());
		if(specDescriptor == null) {
			specDescriptor = new ConsoleSpecPortDescriptor();
			knownConfig.put(descriptor.getName(), specDescriptor);
		}
		return specDescriptor;
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
	public Action getColorAction() {
		return colorAction;
	}
	public StyledDocument getLogDocument() {
		return logDocument;
	}
	public void setLogDocument(LogDocument logDocument) {
		this.logDocument = logDocument;
	}
	public Document getCommandModel() {
		return commandText;
	}
	public Action getSendAction() {
		return sendAction;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/* TODO devrait peut-être disparaitre. */
	public SerialPortDescriptor getActivePort() {
		return activePort;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Stock les valeurs liés à un port pour une fenêtre donnée.
	 * @author tarrask
	 *
	 */
	private class ConsoleSpecPortDescriptor {
		private Color color;
		private Invocable filter;
		
		public ConsoleSpecPortDescriptor() {
			color = Color.decode(Pref.get("defaultForeground", "#000"));
			
			ScriptEngine engine = scriptEngineFactory.getEngineByName("JavaScript");
			filter = (Invocable) engine;
			try {
				engine.eval("function filter(args) { "
						  + "  if(args.message.indexOf('Hello') >= 0) {"
						  + "    args.style = javax.swing.text.StyleContext.getDefaultStyleContext().addStyle('pink', args.style);"
						  + "    javax.swing.text.StyleConstants.setForeground(args.style, java.awt.Color.pink);"
						  + "  }; "
						  + "  return args; "
						  + "}");
			}
			catch(ScriptException e) {
				System.err.println(e.getMessage());
			}
		}
		
		public void setFilter(String filter) {
			try {
				ScriptEngine engine = scriptEngineFactory.getEngineByName("JavaScript");
				engine.eval(filter);
				this.filter = (Invocable) engine;
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void setFilter(Invocable filter) {
			this.filter = filter;
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