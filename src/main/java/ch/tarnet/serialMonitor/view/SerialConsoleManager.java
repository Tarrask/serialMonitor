package ch.tarnet.serialMonitor.view;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import ch.tarnet.serialMonitor.control.SerialMonitor;

public class SerialConsoleManager {

	private SerialMonitor serialMonitor;
	
	private int consoleIndex = 0;
	private List<SerialConsole> consoles = new ArrayList<>();
	private WindowListener closingListener = new ExitWhenLastClosing();

	public SerialConsoleManager(SerialMonitor serialMonitor) {
		this.serialMonitor = serialMonitor;
	}


	public SerialConsole createConsole() {
		SerialConsole instance = new SerialConsole(serialMonitor, consoleIndex++);
		consoles.add(instance);
		instance.addWindowListener(closingListener);
		instance.setLocationByPlatform(true);
		instance.setVisible(true);
		
		return instance;
	}
	
	private class ExitWhenLastClosing extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			Window console = e.getWindow();
			consoles.remove(console);
			if(consoles.isEmpty()) {
				serialMonitor.quit();
			}
			console.dispose();
		}
	}
}
