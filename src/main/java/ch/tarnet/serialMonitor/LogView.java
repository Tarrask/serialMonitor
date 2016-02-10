package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.logging.Logger;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.ViewFactory;
import javax.swing.text.Position.Bias;
import javax.swing.text.View;

import ch.tarnet.serialMonitor.LogDocument.BasicElement;

public class LogView extends View {

	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(LogView.class.getName());
	
	protected Font font;
	protected FontMetrics metrics;
	private int lineHeight;
	private int charWidth;
	private int longestLineCharCount = 0;
	
	public LogView(Element elem) {
		super(elem);
	}
	
	protected void updateMetrics() {
		font = getContainer().getFont();
		metrics = getContainer().getFontMetrics(font);
		lineHeight = metrics.getHeight();
		// on prend la largeur de 'm' comme largueur pour tous les caractères. Avec une fonte
		// monospace, ça devrait le faire. getMaxAdvance ne retourne pas toujours la bonne valeur
		// par exemple avec une font Courier New en 12pt, il retourne 8 au lieu de 7.
		charWidth = metrics.charWidth('m');
	}

	@Override
	public float getPreferredSpan(int axis) {
		updateMetrics();
		Document document = getDocument();
		Element root = document.getDefaultRootElement();
		if(axis == View.X_AXIS) {
			return longestLineCharCount * charWidth;
		}
		else {
			return root.getElementCount() * lineHeight;
		}
	}

	@Override
	public void paint(Graphics g, Shape allocation) {
		updateMetrics();
        JTextComponent host = (JTextComponent) getContainer();
        Rectangle bounds = g.getClipBounds();
		Insets margin = host.getMargin();
		
		g.setColor(host.getForeground());
		g.setFont(host.getFont());
		
		Document doc = getDocument();
		Element root = doc.getDefaultRootElement();
		
		int firstLine = Math.max((bounds.y-margin.top) / lineHeight, 0);
		int lastLine = Math.min(firstLine + (int)Math.ceil(bounds.getHeight() / lineHeight), root.getElementCount());

		Highlighter h = host.getHighlighter();
 		LayeredHighlighter dh = (h instanceof LayeredHighlighter) ? (LayeredHighlighter)h : null;
		for(int i = firstLine; i < lastLine; i++) {
			Element line = root.getElement(i);
			if (dh != null) {
				dh.paintLayeredHighlights(g, line.getStartOffset(), line.getEndOffset(), allocation, host, this);
			}
			paintLine(g, line, margin.left, margin.top + lineHeight * i + metrics.getAscent());
		}
	}

	private void paintLine(Graphics g, Element line, int x, int y) {
		FontMetrics fm = g.getFontMetrics();
		for(int i = 0; i < line.getElementCount(); i++) {
			BasicElement block = (BasicElement)line.getElement(i);
			Color c = (Color)block.getAttributes().getAttribute(StyleConstants.Foreground);
			String blockText = block.getText(); //getDocument().getText(block.getStartOffset(), block.getEndOffset()-block.getStartOffset());
			g.setColor(c);
			g.drawString(blockText, x, y);
			x += fm.stringWidth(blockText);
//			x += SwingUtilities2.stringWidth((JTextComponent)getContainer(), g.getFontMetrics(), blockText);
		}
	}
	
	@Override
	public Shape modelToView(int pos, Shape a, Bias b) throws BadLocationException {
		Document document = getDocument();
		Element root = document.getDefaultRootElement();
		int lineIndex = root.getElementIndex(pos);
		
		Rectangle bounds = a.getBounds();
		Element line = root.getElement(lineIndex);
		
		return new Rectangle(bounds.x + (pos - line.getStartOffset()) * charWidth, bounds.y +  lineIndex * lineHeight, 1, lineHeight);
	}

	@Override
	public int viewToModel(float fx, float fy, Shape a, Bias[] biasReturn) {
		Rectangle bounds = a.getBounds();
		int x = (int)fx, y = (int)fy;
		if(y < bounds.y) {
			return getStartOffset();
		}
		else if(y > bounds.y + bounds.height) {
			return getEndOffset();
		}
		else {
			Document document = getDocument();
			Element root = document.getDefaultRootElement();
			int lineIndex = (int)((y - bounds.y) / lineHeight);
			
			if(lineIndex < 0) {
				return getStartOffset();
			}
			// le point est plus bas que le texte, la position se trouve donc à la fin du texte
			else if(lineIndex >= root.getElementCount()) {
				return getEndOffset();
			}
			else {
				Element line = root.getElement(lineIndex);
				return Math.min(line.getStartOffset() + (x - bounds.x) / charWidth, line.getEndOffset());
			}
		}
	}
	
	@Override
	public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
		System.out.println("in insertUpdate");
		super.insertUpdate(e, a, f);
	}
	@Override
	public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
		System.out.println("in removeUpdate");
		super.removeUpdate(e, a, f);
	}
	/** 
	 * Met à jour la valeur de la plus longe ligne, en fonction de la longueur de la dernière ligne,
	 * la seule pouvant être modifiée
	 * .
	 * @see javax.swing.text.View#changedUpdate(javax.swing.event.DocumentEvent, java.awt.Shape, javax.swing.text.ViewFactory)
	 */
	@Override
	public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
		Element root = getDocument().getDefaultRootElement();
		Element line = root.getElement(root.getElementCount() - 1);
		longestLineCharCount = Math.max(longestLineCharCount, line.getEndOffset() - line.getStartOffset());
		preferenceChanged(null, true, true);
		getContainer().repaint();
	}
}
