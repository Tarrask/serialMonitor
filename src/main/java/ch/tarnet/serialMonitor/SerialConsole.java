package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Enumeration;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import ch.tarnet.serialMonitor.SerialPortDescriptor.Status;

public class SerialConsole extends JFrame implements SerialMessageListener {

	private SerialManager manager;
	
	// Main gui components
	private JMenuBar menuBar;
	private JToolBar toolBar;
	
	public SerialConsole(SerialManager manager) {
		this.manager = manager;
		this.manager.addSerialMessageListener(this);
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
		
		// La comboBox listant les vitesses disponibles
		final JComboBox<Integer> speedCombo = new JComboBox<Integer>(new Integer[] {9600, 19200});
		speedCombo.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		toolBar.add(speedCombo);
		
		// les boutons
		toolBar.addSeparator();
		final JButton openButton = new JButton(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.openPort(selectedPort);
			}
		});
		toolBar.add(openButton);
		final JButton closeButton = new JButton(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor selectedPort = (SerialPortDescriptor)portComboModel.getSelectedItem();
				manager.closePort(selectedPort);
			}
		});
		closeButton.setVisible(false);
		toolBar.add(closeButton);
		toolBar.add(new JButton("Display"));
		
		// On relie le tout au moyen d'events.
		
		// On écoute les changements de port serie sélectionné et met à jour la vitesse et les boutons
		portComboModel.addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {}
			@Override public void intervalAdded(ListDataEvent e) {}
			@Override public void contentsChanged(ListDataEvent e) {
				SerialPortDescriptor descriptor = (SerialPortDescriptor)portComboModel.getSelectedItem();
				if(descriptor.getStatus() == Status.OPEN) {
					openButton.setVisible(false);
					closeButton.setVisible(true);
				}
				else {
					openButton.setVisible(true);
					closeButton.setVisible(false);
				}
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
			@Override public void portRemoved(SerialPortEvent event) {}
			@Override public void portAdded(SerialPortEvent event) {}
			@Override public void portStatusChanged(SerialPortEvent event) {
				SerialPortDescriptor descriptor = event.getDescriptor();
				if(descriptor.getStatus() == Status.OPEN) {
					openButton.setVisible(false);
					closeButton.setVisible(true);
				}
				else {
					openButton.setVisible(true);
					closeButton.setVisible(false);
				}
			}
		});
		
		
		return toolBar;
	}
	
	private JComponent buildTextPane() {
		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		
		StyledDocument styledDoc = textPane.getStyledDocument();
		StyleContext styleContext = StyleContext.getDefaultStyleContext();
		Style defaultStyle = styledDoc.getStyle("default");
		defaultStyle.addAttribute(StyleConstants.FontFamily, "Courier New");
		AttributeSet red = styleContext.addAttribute(styleContext.getEmptySet(), StyleConstants.Foreground, Color.red);
		
		
		try {
			Enumeration<?> styles = styleContext.getStyleNames();
			while(styles.hasMoreElements()) {
				String styleName = styles.nextElement().toString();
				System.out.println(styleName);
				Style style = styleContext.getStyle(styleName);
				Enumeration<?> attrs = style.getAttributeNames();
				while(attrs.hasMoreElements()) {
					System.out.println("   " + attrs.nextElement());
				}
			}
			//AttributeSet red = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.red);
			styledDoc.insertString(0, "Empty for now\n", styledDoc.getStyle("default"));
			styledDoc.insertString(styledDoc.getLength(), "Empty for now", red);
		}
		catch(BadLocationException e) {}
		
		return new JScrollPane(textPane);
	}
	
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		toolBar.setName(title + " toolbar");
	}

	@Override
	public void newSystemMessage(SerialMessageEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newSerialMessage(SerialMessageEvent event) {
		// TODO Auto-generated method stub
		
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