package ch.tarnet.serialMonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialArray;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyledDocument;

import ch.tarnet.common.Pref;

/**
 * Une fenêtre permettant d'afficher les communications séries. Plusieurs SerialConsole
 * peuvent être ouverte simultanément, elle partage toute un unique SerialManager, qui
 * gère les communications bas niveau. Cela permet à plusieurs fenêtre d'afficher le même
 * flux alors que les communications séries sont normalement exclusive.
 * 
 * @author tarrask
 */
public class SerialConsole extends JFrame {

	private static final Logger logger = Logger.getLogger(SerialConsole.class.getName());
	
	/**
	 * Le manager en charge des communications
	 */
	private SerialManager manager;
	
	// Main gui components
	private SerialConsoleModel consoleModel;
	private JMenuBar menuBar;
	private JToolBar toolBar;
	private Action refreshPortAction = new AbstractAction("U") {
		@Override
		public void actionPerformed(ActionEvent e) {
			manager.refreshPorts();
		}
	};
	private JCheckBox autoScrollCheckBox;
	
	public SerialConsole(SerialManager manager) {
		this.manager = manager;
		this.consoleModel = new SerialConsoleModel(this.manager);
		buildGUI();
	}
	
	/**
	 * Construit toute la fenêtre
	 */
	private void buildGUI() {
		JComponent mainContainer = (JComponent)this.getContentPane();
		mainContainer.setLayout(new BorderLayout());
		
		// la barre de menu
		this.setJMenuBar(buildMenuBar());
		
		// la barre d'outil
		mainContainer.add(buildToolBar(), BorderLayout.PAGE_START);
		
		// une zone centrale, pour ne pas occuper les bords qui pourrait être utilisé
		// par la barre d'outil.
		JPanel centerPanel = new JPanel(new BorderLayout());
		mainContainer.add(centerPanel, BorderLayout.CENTER);
		
		// la zone de texte
		centerPanel.add(buildTextPane(), BorderLayout.CENTER);
		
		// une barre sur le bas
		Box bottomBox = new Box(BoxLayout.LINE_AXIS);
		bottomBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		centerPanel.add(bottomBox, BorderLayout.PAGE_END);
		
		// une checkbox, qui permet de faire défiler le texte
		autoScrollCheckBox = new JCheckBox("Auto scroll");
		autoScrollCheckBox.setSelected(Pref.getBoolean("autoScrollSelected", true));
		bottomBox.add(autoScrollCheckBox);
		bottomBox.add(Box.createHorizontalStrut(5));
		
		// un textField pour pouvoir envoyer du text par les ports séries
		bottomBox.add(new JTextField(consoleModel.getCommandModel(), "", 100));
		bottomBox.add(Box.createHorizontalStrut(5));
		
		// le bouton qui déclanche effectivement l'envoi de texte
		JButton sendButton = new JButton(consoleModel.getSendAction());
		bottomBox.add(sendButton);
		
		this.pack();
	}
	
	/**
	 * Contruit la barre de menu
	 * @return
	 */
	private JMenuBar buildMenuBar() {
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		fileMenu.add(new JMenuItem(new AbstractAction("New console ...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Launcher.getInstance().newConsole();
			}
		}));
		fileMenu.add(new JMenuItem(new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		}));
		return menuBar;
	}
	
	/**
	 * Construit la barre d'outils
	 * @return La barre d'outil, prête à être intégrée dans le gui
	 */
	private JToolBar buildToolBar() {
		toolBar = new JToolBar(this.getTitle() + " toolbar");
		toolBar.add(new JComboBox<SerialPortDescriptor>(consoleModel.getAvailablePortsModel()))
			.setMaximumSize(new Dimension(Pref.getInt("serialPortComboWidth", 100), Integer.MAX_VALUE));
		toolBar.add(new JButton(refreshPortAction));
		toolBar.addSeparator();
		
		toolBar.add(new JComboBox<Integer>(consoleModel.getActivePortSpeedModel()))
			.setMaximumSize(new Dimension(Pref.getInt("serialSpeedComboWidth", 100), Integer.MAX_VALUE));
		toolBar.addSeparator();
		
		toolBar.add(new JButton(consoleModel.getOpenCloseAction()));
		toolBar.add(new JButton(consoleModel.getWatchUnwatchAction()));
		toolBar.addSeparator();
		
		toolBar.add(new ColorButton(consoleModel.new ColorAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(SerialConsole.this, "Pick a color", Color.black);
				putValue("Color", newColor);
			}
		}));
		
		return toolBar;
	}
	
	/**
	 * Construit le panneau de texte, contenu dans un JScrollPane
	 * @return
	 */
	private JScrollPane buildTextPane() {
		StyledDocument doc = consoleModel.getLogDocument();
		final JTextPane textPane = new JTextPane(doc);
		doc.addDocumentListener(new DocumentListener() {
			@Override public void removeUpdate(DocumentEvent e) { autoScroll(); }
			@Override public void insertUpdate(DocumentEvent e) { autoScroll(); }
			@Override public void changedUpdate(DocumentEvent e) { autoScroll(); }
			private void autoScroll() {
				if(autoScrollCheckBox.isSelected()) {
					textPane.setCaretPosition(textPane.getDocument().getLength());
				}
			}
		});
		textPane.setEditable(false);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return scrollPane;
	}
	
	/**
	 * Quand la fenêtre est renommé, on renomme aussi la toolbar, comme ça si elle est
	 * détachée, on sait à quel fenêtre elle se ratache.
	 */
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		toolBar.setName(title + " toolbar");
	}
}