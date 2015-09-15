package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

public class SerialSelector extends JFrame {
	
	private SerialManager manager;
	
	private JComboBox<CommPortIdentifier> 	portSelectorCombo;
	private JComboBox<Integer>				portSpeedCombo;
	
	public SerialSelector(SerialManager manager) {
		this.manager = manager;
		this.setTitle("Serial port selector");
		
		Container container = this.getContentPane();
		container.setLayout(new FlowLayout());
		
		portSelectorCombo = new JComboBox<CommPortIdentifier>();
		portSelectorCombo.addItem(null);
		portSelectorCombo.setRenderer(new CommPortIdentifierListCellRenderer());
		container.add(portSelectorCombo);
		
		portSpeedCombo = new JComboBox<Integer>();
		int[] speeds = {4800, 9600, 19200, 38400, 57600, 115200, 230400, 2500000};
		for(int speed : speeds) {
			portSpeedCombo.addItem(speed);
		}
		portSpeedCombo.setSelectedItem(9600);
		portSpeedCombo.setEditable(true);
		container.add(portSpeedCombo);
		
		JButton openButton = new JButton(new OpenAction());
		container.add(openButton);
		
		this.pack();
		this.setResizable(false);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void setAvailablePorts(List<CommPortIdentifier> ports) {
		portSelectorCombo.removeAllItems();
		for(CommPortIdentifier port : ports) {
			portSelectorCombo.addItem(port);
		}
	}
	
	class OpenAction extends AbstractAction {
		public OpenAction() {
			super("Open ...");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			CommPortIdentifier port = portSelectorCombo.getItemAt(portSelectorCombo.getSelectedIndex());
			int speed = (Integer)portSpeedCombo.getSelectedItem();
			System.out.println("Openning port " + port.getName() + " @ " + speed + " baud ...");
			manager.openPort(port, speed);
		}
	}
}

class CommPortIdentifierListCellRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if(comp instanceof JLabel) {
			if(value == null) {
				((JLabel)comp).setText("Loading ...");
			}
			else {
				((JLabel)comp).setText(((CommPortIdentifier)value).getName());
			}
		}
		return comp;
	}
}