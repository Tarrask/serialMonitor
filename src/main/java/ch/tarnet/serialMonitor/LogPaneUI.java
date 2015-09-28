package ch.tarnet.serialMonitor;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;
import javax.swing.text.View;

public class LogPaneUI extends TextUI {
	
	private static final Logger logger = Logger.getLogger(LogPaneUI.class.getName());
	
	private int fontWidth = 7;
	private int lineHeight = 14;
	private int fontDescent = 0;
	
	/**
     * Creates a UI for a LogPane.
     *
     * @param c the log pane
     * @return the UI
     */
	public static ComponentUI createUI(JComponent c) {
		return new LogPaneUI();
	}
	
	@Override
	public Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException {
		System.out.println("LogPaneUI.modelToView(" + t + ", " + pos + ")");
		return null;
	}

	@Override
	public Rectangle modelToView(JTextComponent t, int pos, Bias bias) throws BadLocationException {
		Document document = t.getDocument();
		Element root = document.getDefaultRootElement();
		int lineIndex = 0;
		for(; lineIndex < root.getElementCount() && root.getElement(lineIndex).getEndOffset() < pos; lineIndex++);
		Element line = root.getElement(lineIndex);
		Insets margin = t.getMargin();
		if(margin == null) margin = new Insets(0,  0,  0, 0);
		return new Rectangle(margin.left + (pos - line.getStartOffset()) * fontWidth, margin.top + fontDescent + lineIndex * lineHeight, fontWidth, lineHeight);
	}

	@Override
	public int viewToModel(JTextComponent t, Point pt) {
		System.out.println("LogPaneUI.viewToModel(" + t + ", " + pt + ")");
		return 0;
	}

	@Override
	public int viewToModel(JTextComponent t, Point pt, Bias[] biasReturn) {
		Insets margin = t.getMargin();
		int lineIndex = (pt.y - margin.top - fontDescent) / lineHeight;
		Document document = t.getDocument();
		Element root = document.getDefaultRootElement();
		
		// le point se trouve plus haut que le texte.
		if(lineIndex < 0) {
			return 0;
		}
		// le point est plus bas que le texte, la position se trouve donc à la fin du texte
		if(lineIndex >= root.getElementCount()) {
			return document.getLength();
		}
		
		Element line = root.getElement(lineIndex);
		// TODO mieux, la position ici est le début de la ligne
		return Math.min(line.getStartOffset() + (pt.x - margin.left) / fontWidth, line.getEndOffset());
	}

	@Override
	public int getNextVisualPositionFrom(JTextComponent t, int pos, Bias b, int direction, Bias[] biasRet) throws BadLocationException {
		System.out.println("LogPaneUI.getNextVisualPositionFrom");
		return 0;
	}

	@Override
	public void damageRange(JTextComponent t, int p0, int p1) {
		logger.entering(LogPaneUI.class.getName(), "damageRange", new Object[] {t, p0, p1});
		
		t.repaint();
	}

	@Override
	public void damageRange(JTextComponent t, int p0, int p1, Bias firstBias, Bias secondBias) {
		System.out.println("LogPaneUI.damageRange(" + t + ", " + p0 + ", " + p1 + ", " + firstBias + ", " + secondBias + ")");
		t.repaint();
	}

	@Override
	public EditorKit getEditorKit(JTextComponent t) {
		System.out.println("LogPaneUI.getEditorKit");
		return null;
	}

	@Override
	public View getRootView(JTextComponent t) {
		System.out.println("LogPaneUI.getRootView");
		return null;
	}

}
