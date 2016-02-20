package ch.tarnet.serialMonitor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.control.SerialMonitor;
import ch.tarnet.serialMonitor.services.SerialService;
import ch.tarnet.serialMonitor.services.rxtx.RxtxSerialService;
import ch.tarnet.serialMonitor.view.SerialConsoleManager;

public class Launcher {

	private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
	
	private final Runnable buildGUI = new Runnable() {
		@Override
		public void run() {
			setSystemLookAndFeel();
			serialConsoleManager.createConsole();
			logger.info("Serial monitor is up and running.");
		}
	};

	
	private SerialMonitor serialMonitor;
	private SerialService serialService;
	private SerialConsoleManager serialConsoleManager;

	public static void main(String[] args) {
		new Launcher().launch();
	}
	
	protected void launch() {
		logger.info("Starting serial monitor ...");
		Pref.loadPreferences();
		
		serialMonitor = createSerialMonitor();
		serialService = createSerialService();
		serialConsoleManager = createSerialConsoleManager();
		
		serialMonitor.setSerialService(serialService);
		serialMonitor.setSerialConsoleManager(serialConsoleManager);
		
		SwingUtilities.invokeLater(buildGUI);
	}
	
	protected SerialMonitor createSerialMonitor() {
		return new SerialMonitor();
	}
	
	protected SerialService createSerialService() {
		return new RxtxSerialService();
	}
	
	protected SerialConsoleManager createSerialConsoleManager() {
		return new SerialConsoleManager(serialMonitor);
	}
	
	private void setSystemLookAndFeel() {
		try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch(UnsupportedLookAndFeelException|ClassNotFoundException|InstantiationException|IllegalAccessException e) {
	    	logger.info("Impossible d'utiliser le look&feel system, celui par défaut sera utilisé", e);
	    }
	}
}
