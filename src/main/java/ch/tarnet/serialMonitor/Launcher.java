package ch.tarnet.serialMonitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * le point d'entr�e du programme, il cr�� juste une premi�re console et attend que
 * la derni�re soit ferm� pour terminer le programme.
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
		// planifie la cr�ation et l'affichage d'une premi�re fenetre console
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
	 * on cr�� une instance et l'execute, pas forc�ment utile pour l'instant ... 
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