package ch.tarnet.serialMonitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * le point d'entr�e du programme, il cr�� juste une premi�re console et attend que
 * la derni�re soit ferm� pour terminer le programme.
 * @author tarrask
 *
 */
public class Launcher {
	
	private static Launcher instance;
	private SerialManager manager;
	private List<SerialConsole> consoles;
	private int consoleIndex = 0;
	private ExitWhenLastClosing closingListener;
	
	public Launcher() {
		instance = this;
		manager = new SerialManager();
		consoles = new ArrayList<SerialConsole>();
		closingListener = new ExitWhenLastClosing();
	}
	
	public static Launcher getInstance() {
		return instance;
	}
	
	public SerialConsole newConsole() {
		SerialConsole console = new SerialConsole(manager);
		console.setTitle("SerialMonitor Window #" + consoleIndex++);
		consoles.add(console);
		console.addWindowListener(closingListener);
		console.setLocationRelativeTo(null);
		console.setVisible(true);
		
		return console;
	}
	
	public void run() {
		// planifie la cr�ation et l'affichage d'une premi�re fenetre console
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				newConsole();
			}
		});
	}
	
	/** 
	 * on cr�� une instance et l'execute, pas forc�ment utile pour l'instant ... 
	 */
	public static void main(String[] args) {
		try {
            // Set cross-platform Java L&F (also called "Metal")
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (UnsupportedLookAndFeelException e) {
	       // handle exception
	    }
	    catch (ClassNotFoundException e) {
	       // handle exception
	    }
	    catch (InstantiationException e) {
	       // handle exception
	    }
	    catch (IllegalAccessException e) {
	       // handle exception
	    }
		
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