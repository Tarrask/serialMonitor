package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class FilterEditor extends JDialog {
	private JMenuBar menuBar;

	public FilterEditor(Frame owner) {
		super(owner);
		buildGUI();
	}
	
	void buildGUI() {
		JComponent mainContainer = (JComponent)this.getContentPane();
		mainContainer.setLayout(new BorderLayout());
		
		// la barre de menu
		this.setJMenuBar(buildMenuBar());
		
		JTextArea textEditor = new JTextArea();
		textEditor.setPreferredSize(new Dimension(400, 400));
		mainContainer.add(new JScrollPane(textEditor), BorderLayout.CENTER);
		
		this.pack();
	}

	private JMenuBar buildMenuBar() {
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		fileMenu.add(new JMenuItem(new AbstractAction("Save") {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		}));
		fileMenu.add(new JMenuItem(new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				FilterEditor.this.setVisible(false);
			}
		}));
		return menuBar;
	}
}
