package ch.tarnet.serialMonitor.services;

import java.util.EventListener;

public interface SerialMessageListener extends EventListener {
	void newSystemMessage(SerialMessageEvent event);
	void newSerialMessage(SerialMessageEvent event);
}
