package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;

import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

public class Launcher {
	
	SerialManager manager;
	SerialSelector selector;
	
	public Launcher() {
		manager = new SerialManager();
	}
	
	public void run() {
		// planifie la création du gui
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				selector = new SerialSelector(manager);
				selector.setLocationRelativeTo(null);
				selector.setVisible(true);
			}
		});
		
		// initialise le gestionnaire des ports séries
		SerialManager manager = new SerialManager();
		final List<CommPortIdentifier> ports = SerialManager.getAvailablePorts();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				selector.setAvailablePorts(ports);
			}
		});
	}
	
	public static void main(String[] args) {
		new Launcher().run();
	}
}
