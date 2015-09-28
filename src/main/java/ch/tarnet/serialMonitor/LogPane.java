package ch.tarnet.serialMonitor;

import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

public class LogPane extends JTextComponent {
	
	/**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "LogPaneUI";
	static {
		UIManager.put("LogPaneUI", LogPaneUI.class.getName());
	}

	@Override
	public String getUIClassID() {
		return uiClassID;
	}
	
	public LogPane(StyledDocument doc) {
		setDocument(doc);
	}
}
