package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;

import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import sun.swing.SwingUtilities2;
import ch.tarnet.serialMonitor.LogDocument.BasicElement;
import ch.tarnet.serialMonitor.LogDocument.LogDocumentEvent;

public class LogPane extends JTextComponent implements Scrollable, DocumentListener {
    
	/**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "LogPaneUI";
    
	static {
		UIManager.put("LogPaneUI", LogPaneUI.class.getName());
	}

	private int maxWidth = 0;
	private Font courier;
	private FontMetrics courierMetrics;
	
	public LogPane(StyledDocument doc) {
		setDocument(doc);
		doc.addDocumentListener(this);
		setOpaque(true);
		setBackground(Color.white);
		courier = new Font("Courier new", Font.PLAIN, 12);
		courierMetrics = getFontMetrics(courier);
	}
	
	
	@Override
	protected void paintComponent(Graphics g) {
		Rectangle bounds = g.getClipBounds();
		if(isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
		Insets margin = getMargin();
		FontMetrics metrics = g.getFontMetrics(courier);
		g.setColor(Color.black);
		g.setFont(courier);
		
		Document doc = getDocument();
		Element root = doc.getDefaultRootElement();
//		dumpElement(root, "");
		int firstLine = bounds.y / metrics.getHeight();
		int lastLine = Math.min(firstLine + (int)bounds.getHeight() / metrics.getHeight(), root.getElementCount());
		for(int i = firstLine; i < lastLine; i++) {
			paintLine(g, root.getElement(i), margin.left, margin.top + metrics.getHeight() * (i+1));
		}
	}
	
	private void paintLine(Graphics g, Element line, int x, int y) {
		for(int i = 0; i < line.getElementCount(); i++) {
			try {
				Element block = line.getElement(i);
				Color c = (Color)block.getAttributes().getAttribute(StyleConstants.Foreground);
				String blockText = getDocument().getText(block.getStartOffset(), block.getEndOffset()-block.getStartOffset());
				g.setColor(c);
				g.drawString(blockText, x, y);
				Rectangle2D bounds = g.getFontMetrics().getStringBounds(blockText, g);
				x += bounds.getWidth();
			}
			catch(BadLocationException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void setCaretPosition(int position) {
		
	}
	
	@Override
	public String getUIClassID() {
		return uiClassID;
	}
	
	private void dumpElement(Element elem, String indent) {
		try {
			int start = elem.getStartOffset();
			int end = elem.getEndOffset();
			
				System.out.println(indent + (elem.isLeaf()?"#":"") + elem.getName() + " (" + start + ".." + end + ") = " + elem.getDocument().getText(start, end-start));
			
			AttributeSet attrs = elem.getAttributes();
			Enumeration<?> names = attrs.getAttributeNames();
			while(names.hasMoreElements()) {
				Object attrName = names.nextElement();
				System.out.println(indent + " -> " + attrName + ": " + attrs.getAttribute(attrName));
			}
			for(int i = 0; i < elem.getElementCount(); i++) {
				dumpElement(elem.getElement(i), indent + "    ");
			}
		} 
		catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		Insets margin = getMargin();
		return new Dimension(
				((BasicElement)getDocument().getDefaultRootElement()).getWidth() + margin.left + margin.right, 
				getDocument().getDefaultRootElement().getElementCount() * 14 +  margin.top + margin.bottom);
	}
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		 throw new UnsupportedOperationException();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		 throw new UnsupportedOperationException();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		BasicElement root = (BasicElement)e.getDocument().getDefaultRootElement();
		BasicElement lastLine = (BasicElement)root.getElement(root.getElementCount()-1);
		BasicElement changedElement = ((LogDocumentEvent)e).getElement();
		
		// calcule la largeur de l'élément
		int width = SwingUtilities2.stringWidth(this, courierMetrics, changedElement.getText());
		changedElement.setWidth(width);
		
		// calcule la largeur de la ligne
		width = 0;
		for(int i = 0; i < lastLine.getElementCount(); i++) {
			width += ((BasicElement)lastLine.getElement(i)).getWidth();
		}
		lastLine.setWidth(width);
		
		// calcule la largeur du document
		if(width > root.getWidth()) {
			root.setWidth(width);
		}
		
		revalidate();
		repaint();
	}
}
