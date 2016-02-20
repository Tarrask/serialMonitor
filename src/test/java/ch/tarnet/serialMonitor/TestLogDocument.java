package ch.tarnet.serialMonitor;

import static org.junit.Assert.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Style;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.tarnet.serialMonitor.view.LogDocument;

public class TestLogDocument {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void testGetLength() {
		LogDocument doc = new LogDocument();
		Style style = doc.getStyle("default");
		Style red = doc.addStyle("red", style);
		
		doc.appendString("Hello world", style);
		assertEquals(11, doc.getLength());
		
		doc.appendString("less", red);
		assertEquals(15, doc.getLength());
		
		doc.appendString("ive\nhaha", style);
		assertEquals(23, doc.getLength());
	}

	@Test
	public void testLogDocument() {
		LogDocument doc = new LogDocument();
		assertEquals(0, 	doc.getLength());
		assertEquals(0, 	doc.getStartPosition().getOffset());
		assertEquals(0, 	doc.getEndPosition().getOffset());

		Element root = doc.getDefaultRootElement();
		assertNotNull(root);
		assertEquals("logRoot", root.getName());
		assertEquals(0,			root.getStartOffset());
		assertEquals(0, 		root.getEndOffset());
		assertEquals(1, 		root.getElementCount());
		
		Element line = root.getElement(0);
		assertNotNull(line);
		assertEquals("line", 	line.getName());
		assertEquals(0, 		line.getStartOffset());
		assertEquals(0, 		line.getEndOffset());
		assertEquals(0,			line.getElementCount());
	}
		
	@Test
	public void testAppendString() throws BadLocationException {
		LogDocument doc = new LogDocument();
		Style style = doc.getStyle("default");
		Style red = doc.addStyle("red", style);
		
		// premi�re ajout, sans nouvelle ligne
		String str1 = "Hello world";
		int strLength = str1.length();
		doc.appendString(str1, style);
		
		assertEquals(str1, 		doc.getText(0, doc.getLength()));
		assertEquals(strLength, doc.getLength());
		assertEquals(0, 		doc.getStartPosition().getOffset());
		assertEquals(strLength, doc.getEndPosition().getOffset());

		Element root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(1, 		root.getElementCount());
		
		Element line = root.getElement(0);
		assertEquals(0, 		line.getStartOffset());
		assertEquals(strLength, line.getEndOffset());
		assertEquals(1,			line.getElementCount());
		
		// ajout m�me style m�me ligne
		String str2 = " of tomorrow.";
		strLength += str2.length();
		doc.appendString(str2, style);
		
		assertEquals(str1+str2,	doc.getText(0, doc.getLength()));
		assertEquals(strLength, doc.getLength());
		assertEquals(0, 		doc.getStartPosition().getOffset());
		assertEquals(strLength, doc.getEndPosition().getOffset());
		
		root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(1, 		root.getElementCount());
		
		line = root.getElement(0);
		assertEquals(0, 		line.getStartOffset());
		assertEquals(strLength, line.getEndOffset());
		assertEquals(1,			line.getElementCount());
		
		// ajout diff�rents styles, m�me ligne
		String str3 = " IMPORTANT";
		strLength += str3.length();
		doc.appendString(str3, red);

		assertEquals(str1+str2+str3,	doc.getText(0, doc.getLength()));
		assertEquals(strLength, 		doc.getLength());
		assertEquals(0, 				doc.getStartPosition().getOffset());
		assertEquals(strLength, 		doc.getEndPosition().getOffset());
		
		root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(1, 		root.getElementCount());
		
		line = root.getElement(0);
		assertEquals(0, 		line.getStartOffset());
		assertEquals(strLength, line.getEndOffset());
		assertEquals(2,			line.getElementCount());
		
		// m�me style, nouvelle ligne � la fin
		String str4 = " take care\n";
		strLength += str4.length();
		int line2Start = strLength;
		doc.appendString(str4, red);
		
		assertEquals(str1+str2+str3+str4,	doc.getText(0, doc.getLength()));
		assertEquals(strLength, 			doc.getLength());
		assertEquals(0, 					doc.getStartPosition().getOffset());
		assertEquals(strLength, 			doc.getEndPosition().getOffset());
		
		root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(2, 		root.getElementCount());
		
		line = root.getElement(0);
		assertEquals(0, 			line.getStartOffset());
		assertEquals(strLength-1,	line.getEndOffset());
		assertEquals(2,				line.getElementCount());
		
		line = root.getElement(1);
		assertNotNull(line);
		assertEquals(line2Start, line.getStartOffset());
		assertEquals(line2Start, line.getEndOffset());
		assertEquals(0, 		 line.getElementCount());
		
		// un premier ajout sans nouvelle ligne sur la 2�me ligne
		String str5 = "Monster count: ";
		strLength += str5.length();
		doc.appendString(str5, style);
		
		assertEquals(str1+str2+str3+str4+str5,	doc.getText(0, doc.getLength()));
		assertEquals(strLength, 				doc.getLength());
		assertEquals(0, 						doc.getStartPosition().getOffset());
		assertEquals(strLength, 				doc.getEndPosition().getOffset());
		
		root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(2, 		root.getElementCount());
		
		line = root.getElement(0);
		assertEquals(0, 			line.getStartOffset());
		assertEquals(line2Start-1,	line.getEndOffset());
		assertEquals(2,				line.getElementCount());
		
		line = root.getElement(1);
		assertNotNull(line);
		assertEquals(line2Start, line.getStartOffset());
		assertEquals(strLength,  line.getEndOffset());
		assertEquals(1, 		 line.getElementCount());
		
		// m�me style, avec un retour au milieu de ligne
		String str6_1 = "26";
		String str6_2 = "Kill count: ";
		String str6 = str6_1 + "\n" + str6_2;
		int line3Start = strLength + (str6_1+ "\n").length() ;
		strLength += str6.length();
		doc.appendString(str6, style);
		
		assertEquals(str1+str2+str3+str4+str5+str6, 
								doc.getText(0, doc.getLength()));
		assertEquals(strLength, doc.getLength());
		assertEquals(0, 		doc.getStartPosition().getOffset());
		assertEquals(strLength, doc.getEndPosition().getOffset());
		
		root = doc.getDefaultRootElement();
		assertEquals(0,			root.getStartOffset());
		assertEquals(strLength, root.getEndOffset());
		assertEquals(3, 		root.getElementCount());
		
		line = root.getElement(0);
		assertEquals(0, 			line.getStartOffset());
		assertEquals(line2Start-1,	line.getEndOffset());
		assertEquals(2,				line.getElementCount());
		
		line = root.getElement(1);
		assertNotNull(line);
		assertEquals(line2Start, 	line.getStartOffset());
		assertEquals(line3Start-1,  line.getEndOffset());
		assertEquals(1, 		 	line.getElementCount());
		
		line = root.getElement(2);
		assertNotNull(line);
		assertEquals(line3Start, 	line.getStartOffset());
		assertEquals(strLength,		line.getEndOffset());
		assertEquals(1, 		 	line.getElementCount());
	} 
	
	/**
	 * je sais pas vraiment ce que ca fait de ne pas mettre de style, mais ca ne devrait pas
	 * fonctionner
	 */
	@Test
	public void testAppendString_noStyle() {
		LogDocument doc = new LogDocument();
		doc.appendString("Hello world", null);
	}

	@Test
	public void testGetText() throws BadLocationException {
		LogDocument doc = new LogDocument();
		Style style = doc.getStyle("default");
		Style red = doc.addStyle("red", style);
		
		doc.appendString("Hello world", style);
		assertEquals("Hello", doc.getText(0, 5));
		assertEquals("world", doc.getText(6, 5));
		
		doc.appendString("Some more", style);
		assertEquals("worldSome", doc.getText(6, 9));
		
		doc.appendString("less\nline", red);
		assertEquals("less\nline", doc.getText(20, 9));
	}

}
