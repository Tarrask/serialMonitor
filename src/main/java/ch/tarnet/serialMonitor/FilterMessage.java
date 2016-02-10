package ch.tarnet.serialMonitor;

import javax.swing.text.Style;

public class FilterMessage {
	public boolean display = true;
	public String message = "Hello world";
	public Style style = null;
	
	public FilterMessage(String message, Style style) {
		this.message = message;
		this.style = style;
	}
}