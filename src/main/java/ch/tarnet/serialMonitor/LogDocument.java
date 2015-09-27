package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class LogDocument implements StyledDocument {

	private ArrayList<DocumentListener> documentListeners = new ArrayList<DocumentListener>();
	private ArrayList<UndoableEditListener> undoableEditListeners = new ArrayList<UndoableEditListener>();
	private HashMap<Object, Object> properties = new HashMap<Object, Object>();
	// un contexte personel, pour qu'un changement de couleur d'un style ne modifie pas la couleur des autres fenêtres
	private StyleContext styleContext = new StyleContext();
	
	private Position startPosition = new LogPosition(0);
	private Position endPosition = new EndPosition();
	private RootElement rootElement = new RootElement();
	
	private StringBuilder text = new StringBuilder();
	
	@Override
	public int getLength() {
		return text.length();
	}

	@Override
	public void addDocumentListener(DocumentListener listener) {
		documentListeners.add(listener);
	}

	@Override
	public void removeDocumentListener(DocumentListener listener) {
		documentListeners.remove(listener);
	}

	@Override
	public void addUndoableEditListener(UndoableEditListener listener) {
		undoableEditListeners.add(listener);
	}

	@Override
	public void removeUndoableEditListener(UndoableEditListener listener) {
		undoableEditListeners.remove(listener);
	}

	@Override
	public Object getProperty(Object key) {
		return properties.get(key);
	}

	@Override
	public void putProperty(Object key, Object value) {
		properties.put(key, value);
	}

	@Override
	public void remove(int offs, int len) throws BadLocationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
		throw new UnsupportedOperationException("Use append string instead.");
	}
	
	public void appendString(String str, AttributeSet attributes) {
		// on recherche le premier changement de ligne. Si une nouvelle ligne est trouvé, on découpe la
		// chaine en deux. Dans un premier temps, on ne s'occupe que de la première partie, à ajouter
		// à la dernière ligne actuelle.
		int firstNewLine = str.indexOf('\n');
		String nextLine = null;
		if(firstNewLine >= 0) {
			nextLine = str.substring(firstNewLine+1);
			str = str.substring(0, firstNewLine);
		}
		
		text.append(str);
		
		// met à jour le model
		LineElement lastLine = (LineElement)rootElement.getElement(rootElement.getElementCount()-1);
		BlockElement lastBlock = null;
		if(lastLine.getElementCount() > 0) {
			lastBlock = (BlockElement)lastLine.getElement(lastLine.getElementCount()-1);
		}
		
		if(lastBlock != null && lastBlock.getAttributes() == attributes) {
			lastBlock.endOffset += str.length();
			lastLine.endOffset += str.length();
		}
		else {
			lastBlock = new BlockElement(lastLine, lastLine.endOffset, lastLine.endOffset+str.length(), attributes);
			lastLine.blocks.add(lastBlock);
			lastLine.endOffset += str.length();
		}

		fireDocumentChanged(lastBlock);
		
		// Si une deuxième ligne existait dans la chaine à ajouter, on s'en occupe maintenant. Premièrement
		// on ajoute la nouvelle ligne au model.
		if(nextLine != null) {
			text.append('\n');
			rootElement.lines.add(new LineElement(rootElement, lastLine.endOffset+1, lastLine.endOffset+1));
			// et s'il y a du texte sur cette nouvelle ligne, on l'ajoute aussi, de façon recursive
			if(nextLine.length() > 0) {
				appendString(nextLine, attributes);
			}
		}
		
	}

	private void fireDocumentChanged(BasicElement changedElement) {
		DocumentEvent e = new LogDocumentEvent(changedElement);
		
		for(DocumentListener l : documentListeners) {
			l.changedUpdate(e);
		}
	}

	@Override
	public String getText(int offset, int length) throws BadLocationException {
		return text.substring(offset, offset + length);
	}

	@Override
	public void getText(int offset, int length, Segment txt) throws BadLocationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Position getStartPosition() {
		return startPosition;
	}

	@Override
	public Position getEndPosition() {
		return endPosition;
	}

	@Override
	public Position createPosition(int offset) throws BadLocationException {
		return new LogPosition(offset);
	}

	@Override
	public Element[] getRootElements() {
		return new Element[] {rootElement};
	}

	@Override
	public Element getDefaultRootElement() {
		return rootElement;
	}

	@Override
	public void render(Runnable r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Style addStyle(String name, Style parent) {
		return styleContext.addStyle(name, parent);
	}

	@Override
	public void removeStyle(String name) {
		styleContext.removeStyle(name);
	}

	@Override
	public Style getStyle(String name) {
		return styleContext.getStyle(name);
	}

	@Override
	public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLogicalStyle(int pos, Style s) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public Style getLogicalStyle(int p) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getParagraphElement(int pos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getCharacterElement(int pos) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Color getForeground(AttributeSet attr) {
		return styleContext.getForeground(attr);
	}

	@Override
	public Color getBackground(AttributeSet attr) {
		return styleContext.getBackground(attr);
	}

	@Override
	public Font getFont(AttributeSet attr) {
		return styleContext.getFont(attr);
	}

	private class EndPosition implements Position {
		@Override
		public int getOffset() {
			return text.length();
		}
		
	}
	private static class LogPosition implements Position {

		private int offset;
		
		public LogPosition(int offset) {
			this.offset = offset;
		}
		
		@Override
		public int getOffset() {
			return offset;
		}
	}
	
	public abstract class BasicElement implements Element {

		private int width = 0;
		
		public int getWidth() {
			return width;
		}
		
		public void setWidth(int width) {
			this.width = width;
		}
		
		public String getText() {
			try {
				return getDocument().getText(getStartOffset(), getEndOffset()-getStartOffset());
			}
			catch(BadLocationException e) {
				e.printStackTrace();
				return "";
			}
		}
	}
	
	private class RootElement extends BasicElement {
		
		private ArrayList<LineElement> lines = new ArrayList<LineElement>();
		private AttributeSet attributes = new SimpleAttributeSet();
		
		public RootElement() {
			lines.add(new LineElement(this, 0, 0));
		}
		
		
		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public Element getParentElement() {
			return null;
		}

		@Override
		public String getName() {
			return "logRoot";
		}

		@Override
		public AttributeSet getAttributes() {
			return attributes;
		}

		@Override
		public int getStartOffset() {
			return 0;
		}

		@Override
		public int getEndOffset() {
			return LogDocument.this.getLength();
		}

		@Override
		public int getElementIndex(int offset) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getElementCount() {
			return lines.size();
		}

		@Override
		public Element getElement(int index) {
			return lines.get(index);
		}

		@Override
		public boolean isLeaf() {
			return false;
		}
	}
	
	private class LineElement extends BasicElement {

		private RootElement parent;
		private int startOffset, endOffset;
		private ArrayList<BlockElement> blocks = new ArrayList<BlockElement>();
		private AttributeSet attributes = new SimpleAttributeSet();

		public LineElement(RootElement parent, int startOffset, int endOffset) {
			this.parent = parent;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		
		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public Element getParentElement() {
			return parent;
		}

		@Override
		public String getName() {
			return "line";
		}

		@Override
		public AttributeSet getAttributes() {
			return attributes;
		}

		@Override
		public int getStartOffset() {
			return startOffset;
		}

		@Override
		public int getEndOffset() {
			return endOffset;
		}

		@Override
		public int getElementIndex(int offset) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getElementCount() {
			return blocks.size();
		}

		@Override
		public Element getElement(int index) {
			return blocks.get(index);
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

	}
	
	private class BlockElement extends BasicElement {

		private LineElement parent;
		private int startOffset, endOffset;
		private AttributeSet attributes;
		
		public BlockElement(LineElement parent, int startOffset, int endOffset, AttributeSet attributes) {
			this.parent = parent;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.attributes = attributes;
		}
		
		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public Element getParentElement() {
			return parent;
		}

		@Override
		public String getName() {
			return "block";
		}

		@Override
		public AttributeSet getAttributes() {
			return attributes;
		}

		@Override
		public int getStartOffset() {
			return startOffset;
		}

		@Override
		public int getEndOffset() {
			return endOffset;
		}

		@Override
		public int getElementIndex(int offset) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getElementCount() {
			return 0;
		}

		@Override
		public Element getElement(int index) {
			return null;
		}

		@Override
		public boolean isLeaf() {
			return true;
		}
	}
	
	public class LogDocumentEvent implements DocumentEvent {
		private BasicElement element;

		public LogDocumentEvent(BasicElement element) {
			this.element = element;
		}
		
		public BasicElement getElement() {
			return element;
		}
		
		@Override
		public int getOffset() {
			return element.getStartOffset();
		}

		@Override
		public int getLength() {
			return element.getEndOffset() - element.getStartOffset();
		}

		@Override
		public Document getDocument() {
			return LogDocument.this;
		}

		@Override
		public EventType getType() {
			return EventType.CHANGE;
		}

		@Override
		public ElementChange getChange(Element elem) {
			return null;
		}
	}
}
