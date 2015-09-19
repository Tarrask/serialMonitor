package ch.tarnet.serialMonitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ch.tarnet.common.ApplicationPreferences;

/**
 * le point d'entrée du programme, il créé juste une première console et attend que
 * la dernière soit fermé pour terminer le programme.
 * @author tarrask
 *
 */
public class Launcher {
	
	private static Launcher instance;
	private SerialManager manager;
	private List<SerialConsole> consoles;
	private int consoleIndex = 0;
	private ExitWhenLastClosing closingListener;
	private Preferences config;
	
	public Launcher() {
		this(new PortWatcher());
	}
	
	public Launcher(PortWatcher portWatcher) {
		System.setProperty("java.util.prefs.PreferencesFactory", "ch.tarnet.common.ApplicationPreferencesFactory");
		config = Preferences.systemRoot();
		
		instance = this;
		manager = new SerialManager(portWatcher);
		consoles = new ArrayList<SerialConsole>();
		closingListener = new ExitWhenLastClosing();
	}
	
	public static Launcher getInstance() {
		return instance;
	}
	
	public Preferences getConfig() {
		return config;
	}
	
	public SerialConsole newConsole() {
		SerialConsole console = new SerialConsole(manager);
		console.setTitle(MessageFormat.format(getConfig().get("title", "No title {0}"), consoleIndex++));
		consoles.add(console);
		console.addWindowListener(closingListener);
		console.setLocationRelativeTo(null);
		console.setVisible(true);
		
		return console;
	}
	
	public void run() {
		// planifie la création et l'affichage d'une première fenetre console
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setSystemLookAndFeel();
				newConsole();
			}
		});
	}
	
	private void setSystemLookAndFeel() {
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