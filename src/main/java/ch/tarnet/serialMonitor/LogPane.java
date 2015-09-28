package ch.tarnet.serialMonitor;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

public class LogPane extends JTextComponent implements DocumentListener {
	
	/**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "LogPaneUI2";
	static {
		UIManager.put("LogPaneUI2", LogPaneUI.class.getName());
	}

	@Override
	public String getUIClassID() {
		return uiClassID;
	}
	
	public LogPane(StyledDocument doc) {
		setDocument(doc);
		doc.addDocumentListener(this);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		repaint();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		repaint();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		repaint();
	}
}
