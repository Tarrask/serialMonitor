package ch.tarnet.serialMonitor;

import java.util.logging.Logger;

import javax.swing.text.Element;

public class BenchLogDocument {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(BenchLogDocument.class.getName());
	
	public static final String[] LINES_TEMPLATE = {
		"Hello world\n",
		"Comment va tu!\n",
		"\n",
		"42\n",
		"Les wistiti, j'adore ce mot !\n"
	};
	public static final int LINES_COUNT = 100000;
	public static final int ITERATIONS = 10000;

	private static Element root;
	
	public static int naiveGetElementIndex(int offset) {
		int lineIndex = 0;
		for(; lineIndex < root.getElementCount() && root.getElement(lineIndex).getEndOffset() < offset; lineIndex++);
		return lineIndex;
	}
//	
	
	public static void main(String[] args) {
		LogDocument doc = new LogDocument();
		for(int i = 0; i < LINES_COUNT-1; i++) {
			doc.appendString(LINES_TEMPLATE[(int)(Math.random()*LINES_TEMPLATE.length)], null);
		}
		System.out.println("Document created: " + doc.getDefaultRootElement().getElementCount() + " lines.");
		
		root = doc.getDefaultRootElement();
		int docLength = root.getEndOffset();
		System.out.println("Starting benchmark");
		long start = System.currentTimeMillis();
		for(int i = 0; i < ITERATIONS; i++) {
			root.getElementIndex((int)(Math.random()*docLength));
		}
		long end = System.currentTimeMillis();
		System.out.println("Benchmark ended. Duration: " + (end-start) + " ms.");
		

		System.out.println("Starting benchmark");
		start = System.currentTimeMillis();
		for(int i = 0; i < ITERATIONS; i++) {
			naiveGetElementIndex((int)(Math.random()*docLength));
		}
		end = System.currentTimeMillis();
		System.out.println("Benchmark ended. Duration: " + (end-start) + " ms.");
	}
}
