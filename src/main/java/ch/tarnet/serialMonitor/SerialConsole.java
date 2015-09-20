package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

/**
 * Une fen�tre permettant d'afficher les communications s�ries. Plusieurs SerialConsole
 * peuvent �tre ouverte simultan�ment, elle partage toute un unique SerialManager, qui
 * g�re les communications bas niveau. Cela permet � plusieurs fen�tre d'afficher le m�me
 * flux alors que les communications s�ries sont normalement exclusive.
 * 
 * @author tarrask
 */
public class SerialConsole extends JFrame implements SerialMessageListener {

	private static final Logger logger = Logger.getLogger(SerialConsole.class.getName());
	
	/**
	 * Le manager en charge des communications
	 */
	private SerialManager manager;
	/**
	 * L'ensemble des ports actuellement �cout�, afin de filtrer les messages retourn� par le manager.
	 */
	private Set<SerialPortDescriptor> watchedPorts;
	
	// Main gui components
	private JMenuBar menuBar;
	private JToolBar toolBar;
	private StyledDocument logDocument;
	private Style systemStyle;
	private JComboBox<Integer> speedCombo;
	private JButton openButton;
	private JButton closeButton;
	private JButton unwatchButton;
	private JButton watchButton;
	private JScrollPane consoleScrollPane;
	private JTextPane textPane;
	
	public SerialConsole(SerialManager manager) {
		this.manager = manager;
		this.manager.addSerialMessageListener(this);
		this.watchedPorts = new HashSet<>();
		buildGUI();
	}
	
	private void buildGUI() {
		JComponent component = (JComponent)this.getContentPane();
		component.setLayout(new BorderLayout());
		
		// la barre de menu
		this.setJMenuBar(buildMenuBar());
		
		// la barre d'outil
		component.add(buildToolBar(), BorderLayout.PAGE_START);
		
		// une zone centrale, pour ne pas occuper les bords qui pourrait �tre utilis�
		// par la barre d'outil.
		JPanel centerPanel = new JPanel(new BorderLayout());
		component.add(centerPanel, BorderLayout.CENTER);
		
		// la zone de texte
		consoleScrollPane = buildTextPane();
		centerPanel.add(consoleScrollPane, BorderLayout.CENTER);
		
		// une barre sur le bas
		Box bottomBox = new Box(BoxLayout.LINE_AXIS);
		bottomBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		centerPanel.add(bottomBox, BorderLayout.PAGE_END);
		
		bottomBox.add(new JCheckBox("Auto scroll"));
		bottomBox.add(Box.createHorizontalStrut(5));
		bottomBox.add(new JTextField());
		bottomBox.add(Box.createHorizontalStrut(5));
		bottomBox.add(new JButton("Send"));
		
		this.pack();
	}
	
	private JMenuBar buildMenuBar() {
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		fileMenu.add(new JMenuItem(new AbstractAction("New console ...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Launcher.getInstance().newConsole();
			}
		}));
		fileMenu.add(new JMenuItem(new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		}));
		return menuBar;
	}
	
	private JToolBar buildToolBar() {
		toolBar = new JToolBar(this.getTitle() + " toolbar");
		
		// La comboBox Listant les diff�rents ports s�riel
		final PortComboModel portComboModel = new PortComboModel(manager);
		final JComboBox<SerialPortDescriptor> portCombo = new JComboBox<SerialPortDescriptor>(portComboModel);
		portCombo.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		toolBar.add(portCombo);
		toolBar.add(new JButton(new AbstractAction("U") {
			@Override
			public void actionPerformed(ActionEvent e) {
				portComboModel.refresh();
			}
		}));
		toolBar.addSeparator();
		
		speedCombo = new JComboBox<Integer>(new Integer[] {4800, 9600, 19200, 38400, 57600, 115200, 230400, 250000});
		speedCombo.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		speedCombo.setSelectedItem(Pref.getInt("defaultSerialSpeed", SerialManager.DEFAULT_SPEED));
		toolBar.add(speedCombo);
		
		// les boutons
		toolBar.addSeparator();
		
		openButton = new JButton(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.openPort(selectedPort);
				watchPort(selectedPort);
			}
		});
		toolBar.add(openButton);
		
		closeButton = new JButton(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.closePort(selectedPort);
				unwatchPort(selectedPort);
			}
		});
		toolBar.add(closeButton);
		watchButton = new JButton(new AbstractAction("Watch") {
			@Override public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				watchPort(selectedPort);
				setSelectedPort(selectedPort);
			}
		});
		toolBar.add(watchButton);
		unwatchButton = new JButton(new AbstractAction("Unwatch") {
			@Override public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				unwatchPort(selectedPort);
				setSelectedPort(selectedPort);
			}
		});
		toolBar.add(unwatchButton);
		
		
		
		// On relie le tout au moyen d'events.
		
		// On �coute les changements de port serie s�lectionn� et met � jour la vitesse et les boutons
		portComboModel.addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {}
			@Override public void intervalAdded(ListDataEvent e) {}
			@Override public void contentsChanged(ListDataEvent e) {
				setSelectedPort((SerialPortDescriptor)portComboModel.getSelectedItem());
			}
		});
		
		// On �coute les changements de vitesse, qu'on r�percute dans le SerialPortDescriptor actuel
		speedCombo.getModel().addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {}
			@Override public void intervalAdded(ListDataEvent e) {}
			@Override public void contentsChanged(ListDataEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				if(selectedPort != null) {
					selectedPort.setSpeed((Integer)speedCombo.getSelectedItem());
				}
			}
		});
		
		// On �coute les changements de status des ports, et on met � jour les boutons le cas �ch�ant.
		manager.addSerialPortListener(new SerialPortListener() {
			@Override public void portRemoved(SerialPortEvent event) {
				SerialPortDescriptor descriptor = event.getDescriptor();
				if(watchedPorts.contains(descriptor)) {
					watchedPorts.remove(descriptor);
				}
			}
			@Override public void portAdded(SerialPortEvent event) {}
			@Override public void portStatusChanged(final SerialPortEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						setSelectedPort(event.getDescriptor());
					}
				});
			}
		});
		
		setSelectedPort((SerialPortDescriptor)portComboModel.getSelectedItem());
		
		return toolBar;
	}
	
	/**
	 * appel� quand le port s�rie s�lectionn� au niveau du comboBox est modifi�.
	 * Modifie l'�tat de tous les autres controls pour refleter ce changement.
	 * 
	 * @param descriptor le nouveau port s�lectionn�
	 */
	protected void setSelectedPort(SerialPortDescriptor descriptor) {
		if(descriptor == null) {
			openButton.setVisible(true);
			openButton.setEnabled(false);
			closeButton.setVisible(false);
			watchButton.setEnabled(false);
			unwatchButton.setEnabled(false);
		}
		else {
			if(descriptor.getStatus() == Status.OPEN) {
				openButton.setVisible(false);
				openButton.setEnabled(false);
				closeButton.setVisible(true);
				watchButton.setEnabled(true);
				unwatchButton.setEnabled(true);
			}
			else {
				openButton.setVisible(true);
				openButton.setEnabled(true);
				closeButton.setVisible(false);
				watchButton.setEnabled(false);
				unwatchButton.setEnabled(false);
			}
			if(watchedPorts.contains(descriptor)) {
				watchButton.setVisible(false);
				unwatchButton.setVisible(true);
			}
			else {
				watchButton.setVisible(true);
				unwatchButton.setVisible(false);
			}
		}
	}

	private JScrollPane buildTextPane() {
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		
		textPane.addCaretListener(new CaretListener() {
			@Override public void caretUpdate(CaretEvent e) {
				System.out.println("CaretEvent");
			}
		});
		logDocument = textPane.getStyledDocument();
		Style defaultStyle = logDocument.getStyle("default");
		StyleConstants.setFontFamily(defaultStyle, Pref.getString("defaultFontFamily", "Courier New"));
		StyleConstants.setFontSize(defaultStyle, Pref.getInt("defaultFontSize", 12));
		StyleConstants.setForeground(defaultStyle, Color.decode(Pref.getString("defaultForeground", "#222222")));
		
		systemStyle = logDocument.addStyle("red", defaultStyle);
		StyleConstants.setForeground(systemStyle, Color.decode(Pref.getString("defaultSystemForeground", "#aa0000")));
		
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return scrollPane;
	}
	


	private void watchPort(SerialPortDescriptor descriptor) {
		watchedPorts.add(descriptor);
	}


	private void unwatchPort(SerialPortDescriptor descriptor) {
		watchedPorts.remove(descriptor);
	}
	
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		toolBar.setName(title + " toolbar");
	}
	
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
					printText(event.getMessage(), null);
				}
			});
		}
	}
	
	public void printText(String text, Style style) {
		try {
			logDocument.insertString(logDocument.getLength(), text, style);
			textPane.setCaretPosition(logDocument.getLength());
		}
		catch(BadLocationException e) {
			// ne devrait jamais survenir, ne semble pas grave, au pire aucun text ne sera plus �crit.
			// on log l'erreur mais poursuivons l'execution du programme.
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
	}
}


class PortComboModel implements ComboBoxModel<SerialPortDescriptor>, SerialPortListener {

	private SerialManager manager;
	private List<SerialPortDescriptor> portsList;
	private List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();
	private SerialPortDescriptor selectedItem = null;
	
	public PortComboModel(SerialManager manager) {
		this.manager = manager;
		portsList = manager.getAvailablePorts();
		manager.addSerialPortListener(this);
		if(!portsList.isEmpty()) {
			setSelectedItem(portsList.get(0));
		}
	}
	
	public void refresh() {
		portsList = manager.getAvailablePorts();
		fireContentChanged();
		manager.refreshPorts();
	}

	@Override
	public int getSize() {
		return portsList.size();
	}

	@Override
	public SerialPortDescriptor getElementAt(int index) {
		return portsList.get(index);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		listDataListeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		listDataListeners.remove(l);
	}

	@Override
	public void setSelectedItem(Object anItem) {
		if(anItem == null || anItem instanceof SerialPortDescriptor) {
			selectedItem = (SerialPortDescriptor)anItem;
			fireContentChanged();
		}
	}

	private void fireContentChanged() {
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, portsList.size()-1);
		for(ListDataListener listener : listDataListeners) {
			listener.contentsChanged(e);
		}
	}

	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void portAdded(SerialPortEvent event) {
		portsList.add(event.getDescriptor());
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, portsList.size()-1, portsList.size()-1);
		for(ListDataListener listener : listDataListeners) {
			listener.intervalAdded(e);
		}
		if(selectedItem == null) {
			setSelectedItem(portsList.get(0));
		}
	}

	@Override
	public void portRemoved(SerialPortEvent event) {
		SerialPortDescriptor descriptor = event.getDescriptor();
		int index = portsList.indexOf(descriptor);
		portsList.remove(index);
		
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index);
		for(ListDataListener listener : listDataListeners) {
			listener.intervalRemoved(e);
		}
		
		if(selectedItem == descriptor) {
			setSelectedItem(portsList.isEmpty() ? null : portsList.get(0));
		}
	}
	
	@Override
	public void portStatusChanged(SerialPortEvent event) {}
}