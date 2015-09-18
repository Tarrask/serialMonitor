package ch.tarnet.serialMonitor;

public interface SerialMessageListener {
	void newSystemMessage(SerialMessageEvent event);
	void newSerialMessage(SerialMessageEvent event);
}
