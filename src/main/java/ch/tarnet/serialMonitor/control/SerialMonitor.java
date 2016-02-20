package ch.tarnet.serialMonitor.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.serialMonitor.services.SerialService;
import ch.tarnet.serialMonitor.view.SerialConsoleManager;

public class SerialMonitor {

	private static final Logger logger = LoggerFactory.getLogger(SerialMonitor.class);
	
	private SerialService			serialService;
	private SerialConsoleManager	serialConsoleManager;

	public SerialMonitor() {
	}

	public SerialConsoleManager getSerialConsoleManager() {
		return this.serialConsoleManager;
	}

	public SerialService getSerialService() {
		return serialService;
	}

	public void setSerialConsoleManager(SerialConsoleManager serialConsoleManager) {
		this.serialConsoleManager = serialConsoleManager;
	}

	public void setSerialService(SerialService serialService) {
		this.serialService = serialService;
	}

	public void quit() {
		serialService.closeAllPorts();
		logger.info("Serial monitor stopped.");
		System.exit(0);
	}
}
