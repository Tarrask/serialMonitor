package ch.tarnet.serialMonitor;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;
import javax.swing.text.View;

public class LogPaneUI extends TextUI {

	public static ComponentUI createUI(JComponent c) {
		System.out.println("LogPaneUI.createUI");
		return new LogPaneUI();
	}
	
	@Override
	public Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException {
		System.out.println("LogPaneUI.modelToView");
		return null;
	}

	@Override
	public Rectangle modelToView(JTextComponent t, int pos, Bias bias) throws BadLocationException {
		System.out.println("LogPaneUI.modelToView");
		return null;
	}

	@Override
	public int viewToModel(JTextComponent t, Point pt) {
		System.out.println("LogPaneUI.viewToModel");
		return 0;
	}

	@Override
	public int viewToModel(JTextComponent t, Point pt, Bias[] biasReturn) {
		System.out.println("LogPaneUI.viewToModel");
		return 0;
	}

	@Override
	public int getNextVisualPositionFrom(JTextComponent t, int pos, Bias b, int direction, Bias[] biasRet) throws BadLocationException {
		System.out.println("LogPaneUI.getNextVisualPositionFrom");
		return 0;
	}

	@Override
	public void damageRange(JTextComponent t, int p0, int p1) {
		System.out.println("LogPaneUI.damageRange");
	}

	@Override
	public void damageRange(JTextComponent t, int p0, int p1, Bias firstBias, Bias secondBias) {
		System.out.println("LogPaneUI.damageRange");
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
