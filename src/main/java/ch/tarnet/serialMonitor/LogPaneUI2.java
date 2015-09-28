package ch.tarnet.serialMonitor;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.Element;
import javax.swing.text.View;

public class LogPaneUI2 extends BasicTextUI {
	
	/**
     * Creates a UI for a LogPane.
     *
     * @param c the log pane
     * @return the UI
     */
	public static ComponentUI createUI(JComponent c) {
		return new LogPaneUI2();
	}
	
	@Override
	protected String getPropertyPrefix() {
		return "TextArea";
	}

	@Override
	public View create(Element elem) {
		System.out.println("LogPaneUI2.create");
		return super.create(elem);
	}
	
	@Override
	public View create(Element elem, int p0, int p1) {
		System.out.println("LogPaneUI2.create");
		return super.create(elem, p0, p1);
	}
}
