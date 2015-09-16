package ch.tarnet.serialMonitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * le point d'entrée du programme, il créé juste une première console et attend que
 * la dernière soit fermé pour terminer le programme.
 * @author tarrask
 *
 */
public class Launcher {
	
	SerialManager manager;
	List<SerialConsole> consoles;
	
	public Launcher() {
		manager = new SerialManager();
		consoles = new ArrayList<SerialConsole>();
	}
	
	public void run() {
		// planifie la création et l'affichage d'une première fenetre console
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SerialConsole console = new SerialConsole(manager);
				console.setTitle("SerialMonitor Window #1");
				consoles.add(console);
				console.addWindowListener(new ExitWhenLastClosing());
				console.setLocationRelativeTo(null);
				console.setVisible(true);
			}
		});
	}
	
	/** 
	 * on créé une instance et l'execute, pas forcément utile pour l'instant ... 
	 */
	public static void main(String[] args) {
		new Launcher().run();
	}
	
	private class ExitWhenLastClosing extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			consoles.remove(e.getWindow());
			if(consoles.isEmpty()) {
				manager.closeAllPorts();
				System.exit(0);
			}
			super.windowClosing(e);
		}
	}
}