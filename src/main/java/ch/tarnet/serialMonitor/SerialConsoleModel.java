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
import java.util.logging.Logger;

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

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

public class SerialConsoleModel {

	private static final Logger logger = Logger.getLogger(SerialConsoleModel.class.getName());
	
	private SerialManager manager;
	
	private ScriptEngineManager scriptEngineFactory;

	private SerialPortDescriptor activePort = null;
	private DefaultComboBoxModel<SerialPortDescriptor> availablePorts = new DefaultComboBoxModel<SerialPortDescriptor>();
	private DefaultComboBoxModel<Integer> activePortSpeed = new DefaultComboBoxModel<Integer>(new Integer[] {4800, 9600, 19200, 38400, 57600, 115200, 230400, 250000});
	private HashSet<SerialPortDescriptor> watchedPorts = new HashSet<SerialPortDescriptor>();
	private HashMap<String, ConsoleSpecPortDescriptor> knownConfig = new HashMap<String, ConsoleSpecPortDescriptor>();
	private LogDocument logDocument = new LogDocument();
	
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
	
	private Action colorAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(activePort != null) {
				ConsoleSpecPortDescriptor specDescriptor = knownConfig.get(activePort.getName());
				specDescriptor.setColor((Color)getValue(ColorButton.ACTION_COLOR_KEY));
			}
		}
	};
	
	
	private Action sendAction = new AbstractAction("Send") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
		}
	};

	/**
	 * G�re les �venements li�s aux ports provenant du SerialManager. Lorsque un nouveau port est
	 * d�tect� par le SerialManager, on l'ajoute � la liste de cette fen�tre tri� par ordre alphabetique
	 * du nom de ports.
	 * Si un port n'est plus disponible, on le retire simplement.
	 */
	private final SerialPortListener serialPortlistener = new SerialPortListener() {
		@Override 
		public void portAdded(SerialPortEvent event) {
			SerialPortDescriptor descriptor = event.getDescriptor();
			
			// trouve l'index tri�
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
	 * G�re la reception des messages provenant des ports s�rie g�r� par le manager
	 */
	private final SerialMessageListener serialMessageListener = new SerialMessageListener() {
		@Override
		public void newSystemMessage(final SerialMessageEvent event) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					StringBuilder sb = new StringBuilder();
					try {
						if(logDocument.getLength() > 0 && !"\n".equals(logDocument.getText(logDocument.getLength()-1, 1))) {
							sb.append("\n");
						}
					}
					catch(BadLocationException e) {
						e.printStackTrace();
					}
					sb.append(event.getMessage()).append("\n");
					printText(sb.toString(), systemStyle);
				}
			});
		}

		@Override
		public void newSerialMessage(final SerialMessageEvent event) {
			if(watchedPorts.contains(event.getDescriptor())) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
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
				});
			}
		}
	};

	/**
	 * G�re le ComboBox listant les ports s�ries disponibles
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
			logger.info("in selectedSpeedListener.intervalRemoved: Le speed ComboBox n'�tant pas modifiable pour l'instant. Ce message ne devrait pas apparaitre.");
		}
		
		@Override
		public void intervalAdded(ListDataEvent e) {
			logger.info("in selectedSpeedListener.intervalAdded: Le speed ComboBox n'�tant pas modifiable pour l'instant. Ce message ne devrait pas apparaitre.");
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
		
		// On initialise et lie les listeners pour les diff�rents model contenant les donn�es
		// activePort: rien � faire, null au lancement de la console, est d�fini lors du peuplement des ports disponibles
		
		// availablePorts: la liste des ports s�rie disponible qu'on peuple avec les valeurs provenant du manager
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
		
		// activePortSpeed: la liste des vitesses disponible, initialis� avec la valeur de l'activePort
		activePortSpeed.addListDataListener(selectedSpeedListener);
		if(activePortSpeed.getSelectedItem() == null) {
			activePortSpeed.setSelectedItem(Pref.getInt("defaultSerialSpeed", SerialManager.DEFAULT_SPEED));
			activePortSpeed.setSelectedItem(1000);
		}
		
		// watchedPorts: rien � faire, la liste est vide au lancement de la console
		
		// logDocument: initialise les styles
		logStyle = logDocument.addStyle("log", null);
		StyleConstants.setFontFamily(logStyle, Pref.getString("defaultFontFamily", "Courier New"));
		StyleConstants.setFontSize(logStyle,   Pref.getInt("defaultFontSize", 12));
		StyleConstants.setForeground(logStyle, Color.decode(Pref.getString("defaultForeground", "#000")));
		
		systemStyle = logDocument.addStyle("red", logStyle);
		StyleConstants.setForeground(systemStyle, Color.decode(Pref.getString("defaultSystemForeground", "#aa0000")));
		
		// commandText: rien � faire, initialement vide, servira � qqch si l'utilisateur y saisie qqch
		
		// openCloseAction: rien � faire
		
		// watchUnwatchAction: rien � faire
		
		// colorAction: rien � faire
		
		// sendAction: rien � faire TODO pour l'instant en tout cas
		
	}

	/**
	 * met � jour tous les modeles li�s au port actif, en commen�ant par la variable activePort
	 */
	private void updateActivePortDependantModels() {
		updateActivePortListener();
		updateActivePortSpeed();
		updateOpenCloseAction();
		updateWatchUnwatchAction();
		updateColorAction();
	}
	
	/**
	 * Appel� suite � un changement du port actif, met � jour la variable et transmet le listener
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
	 * Appel� suite � un changement du port actif
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
	 * Appel� suite au changement du port actif, met � jour le bouton open/close en fonction du
	 * nouveau port s�rie selectionn�
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
	 * Appel� suite au changement du port actif, met � jour le bouton watch/unwatch en fonction
	 * du nouveau port s�lectionn�.
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
	 * Affiche du text dans la zone de log, le style �tant specifi�
	 * 
	 * @param text Le texte � afficher
	 * @param style le style � employer
	 * @throws IOException 
	 */
	private void printText(String text, Style style) {
//		try {
//			logDocument.insertString(logDocument.getLength(), text, style);
			logDocument.appendString(text, style);
//			HTMLDocument doc = (HTMLDocument)logDocument;
//			Color color = (Color)style.getAttribute(StyleConstants.Foreground);
//			System.out.println(color.toString());
//			doc.insertBeforeEnd(doc.getElement("body"), "<font color='#f00'>" + text + "</font>");
//		}
//		catch(BadLocationException e) {
//			// ne devrait jamais survenir, ne semble pas grave, au pire aucun text ne sera plus �crit.
//			// on log l'erreur mais poursuivons l'execution du programme.
//			logger.warning(e.getMessage());
//			e.printStackTrace();
//		} 
//		catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private ConsoleSpecPortDescriptor getSpecDescriptor(SerialPortDescriptor descriptor) {
		ConsoleSpecPortDescriptor specDescriptor = knownConfig.get(descriptor.getName());
		if(specDescriptor == null) {
			specDescriptor = new ConsoleSpecPortDescriptor();
			knownConfig.put(descriptor.getName(), specDescriptor);
		}
		return specDescriptor;
	}
	
	
	/**
	 * Un acc�s au diff�rent model, utilis� par SerialConsole lors de l'initialisation du GUI
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
	public void setSerialPortColor(Color color) {
		if(activePort != null) {
			ConsoleSpecPortDescriptor specDescriptor = knownConfig.get(activePort.getName());
			specDescriptor.setColor(color);
		}
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
	public SerialPortDescriptor getActivePort() {
		return activePort;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
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