package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

/**
 * Une fenêtre permettant d'afficher les communications séries. Plusieurs SerialConsole
 * peuvent être ouverte simultanément, elle partage toute un unique SerialManager, qui
 * gère les communications bas niveau. Cela permet à plusieurs fenêtre d'afficher le même
 * flux alors que les communications séries sont normalement exclusive.
 * 
 * @author tarrask
 */
public class SerialConsole extends JFrame implements SerialMessageListener {

	/**
	 * Le manager en charge des communications
	 */
	private SerialManager manager;
	/**
	 * L'ensemble des ports actuellement écouté, afin de filtrer les messages retourné par le manager.
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
		
		// un scrollPane pour afficher les données reçues par les SerialWorkers
		JComponent consoleScrollPane = buildTextPane();
		consoleScrollPane.setPreferredSize(new Dimension(400, 300));
		component.add(consoleScrollPane, BorderLayout.CENTER);
			
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
		
		// La comboBox Listant les différents ports sériel
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
		speedCombo.setSelectedItem(SerialManager.DEFAULT_SPEED);
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
		
		// On écoute les changements de port serie sélectionné et met à jour la vitesse et les boutons
		portComboModel.addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {}
			@Override public void intervalAdded(ListDataEvent e) {}
			@Override public void contentsChanged(ListDataEvent e) {
				setSelectedPort((SerialPortDescriptor)portComboModel.getSelectedItem());
			}
		});
		
		// On écoute les changements de vitesse, qu'on répercute dans le SerialPortDescriptor actuel
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
		
		// On écoute les changements de status des ports, et on met à jour les boutons le cas échéant.
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

	private JComponent buildTextPane() {
		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		
		
		logDocument = textPane.getStyledDocument();
		Style defaultStyle = logDocument.getStyle("default");
		defaultStyle.addAttribute(StyleConstants.FontFamily, "Courier New");
		
		systemStyle = logDocument.addStyle("red", defaultStyle);
		StyleConstants.setForeground(systemStyle, Color.red);
		
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
	public void newSystemMessage(SerialMessageEvent event) {
		System.err.println(event.getMessage() + "\n");
		try {
			logDocument.insertString(logDocument.getLength(), event.getMessage() + "\n", systemStyle);
		}
		catch(BadLocationException e) {
			// ne devrait jamais survenir, ne semble pas grave, au pire aucun text ne sera plus écrit.
			// on log l'erreur mais poursuivons l'execution du programme.
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void newSerialMessage(SerialMessageEvent event) {
		System.out.print(event.getMessage());
		if(watchedPorts.contains(event.getDescriptor())) {
			try {
				logDocument.insertString(logDocument.getLength(), event.getMessage(), null);
			}
			catch(BadLocationException e) {
				// ne devrait jamais survenir, ne semble pas grave, au pire aucun text ne sera plus écrit.
				// on log l'erreur mais poursuivons l'execution du programme.
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
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