package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.JToolBar;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class SerialConsole extends JFrame {

	private SerialManager manager;
	
	// Main gui components
	private JMenuBar menuBar;
	private JToolBar toolBar;
	
	public SerialConsole(SerialManager manager) {
		this.manager = manager;
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
		JScrollPane consoleScrollPane = new JScrollPane();
		component.add(consoleScrollPane, BorderLayout.CENTER);
		
		this.pack();
	}
	
	private JMenuBar buildMenuBar() {
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add(new JMenuItem(new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		}));
		return menuBar;
	}
	
	private JToolBar buildToolBar() {
		toolBar = new JToolBar();
		
		// La comboBox Listant les différents ports sériel
		final PortComboModel portComboModel = new PortComboModel(manager);
		final JComboBox<SerialPortDescriptor> portCombo = new JComboBox<SerialPortDescriptor>(portComboModel);
		toolBar.add(portCombo);
		toolBar.add(new JButton(new AbstractAction("U") {
			@Override
			public void actionPerformed(ActionEvent e) {
				portComboModel.refresh();
			}
		}));
		toolBar.addSeparator();
		
		// La comboBox listant les vitesses disponibles
		final JComboBox<Integer> speedCombo = new JComboBox<Integer>(new Integer[] {9600, 19200});
		toolBar.add(speedCombo);
		portComboModel.addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {}
			@Override public void intervalAdded(ListDataEvent e) {}
			@Override public void contentsChanged(ListDataEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				if(selectedPort != null) {
					speedCombo.setSelectedItem(selectedPort.getSpeed());
				}
				else {
					speedCombo.setSelectedItem(SerialManager.DEFAULT_SPEED);
				}
			}
		});
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
		toolBar.addSeparator();
		toolBar.add(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.openPort(selectedPort);
			}
		});
		toolBar.add(new JButton("Display"));
		toolBar.add(new JButton(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.closePort(selectedPort);
			}
		}));
		return toolBar;
	}
	
	@Override
	public void setName(String name) {
		super.setName(name);
		toolBar.setName(name + " toolbar");
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
}