package ch.tarnet.serialMonitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.common.Pref;

/**
 * le point d'entrée du programme, il créé juste une première console et attend que
 * la dernière soit fermé pour terminer le programme.
 * 
 * @author tarrask
 */
public class Launcher {
	
	private final static Logger logger = LoggerFactory.getLogger(Launcher.class);
	
	private static Launcher instance;
	private SerialManager manager;
	private List<SerialConsole> consoles;
	private int consoleIndex = 0;
	private ExitWhenLastClosing closingListener; 
	
	public Launcher() {
		this(new PortWatcher());
	}
	
	public Launcher(PortWatcher portWatcher) {
		System.setProperty("java.util.prefs.PreferencesFactory", "ch.tarnet.common.ApplicationPreferencesFactory");
		Pref.loadPreferences();
		
		instance = this;
		manager = new SerialManager(portWatcher);
		consoles = new ArrayList<SerialConsole>();
		closingListener = new ExitWhenLastClosing();
	}
	
	public static Launcher getInstance() {
		return instance;
	}
	
	public SerialConsole newConsole() {
		SerialConsole console = new SerialConsole(manager);
		console.setTitle(MessageFormat.format(Pref.get("title", "No title {0}"), consoleIndex++));
		consoles.add(console);
		console.addWindowListener(closingListener);
		console.setSize(Pref.getInt("consoleWidth", 400), Pref.getInt("consoleHeight", 300));
		console.setLocationByPlatform(true);
		console.setVisible(true);
		
		return console;
	}
	
	public void run() {
		// planifie la création et l'affichage d'une première fenêtre console
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				setSystemLookAndFeel();
				newConsole();
			}
		});
	}
	
	private void setSystemLookAndFeel() {
		try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch(UnsupportedLookAndFeelException|ClassNotFoundException|InstantiationException|IllegalAccessException e) {
	    	logger.info("Impossible d'utiliser le look&feel system, celui par défaut sera utilisé", e);
	    }
	}
	
	/** 
	 * on créé une instance et l'execute, pas forcément utile pour l'instant ... 
	 */
	public static void main(String[] args) {
		logger.info("Starting SerialMonitor ...");
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