package ch.tarnet.serialMonitor.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tarnet.common.Pref;
import ch.tarnet.serialMonitor.FilterEditor;
import ch.tarnet.serialMonitor.FilterMessage;
import ch.tarnet.serialMonitor.control.SerialMonitor;
import ch.tarnet.serialMonitor.services.SerialMessageEvent;
import ch.tarnet.serialMonitor.services.SerialMessageListener;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortEvent;
import ch.tarnet.serialMonitor.services.SerialPortListener;
import ch.tarnet.serialMonitor.services.SerialService;
import ch.tarnet.serialMonitor.view.SerialConsoleModel.ConsoleSpecPortDescriptor;
import ch.tarnet.serialMonitor.services.SerialPortDescriptor.Status;

public class SerialConsole extends JFrame {
	
	private static final Logger logger = LoggerFactory.getLogger(SerialConsole.class);

	private static final String FRAME_TITLE_KEY			= "frame.title";
	private static final String FRAME_WIDTH_KEY			= "frame.width";
	private static final String FRAME_HEIGHT_KEY		= "frame.height";
	private static final String MENU_FILE_TEXT_KEY		= "menu.file.text";
	private static final String NEW_CONSOLE_TEXT_KEY	= "menu.file.newConsole.text";
	private static final String CLOSE_CONSOLE_TEXT_KEY	= "menu.file.closeConsole.text";
	private static final String QUIT_TEXT_KEY			= "menu.file.quit.text";
	private static final String TOOLBAR_TITLE_KEY 		= "toolbar.title";
	
	private SerialConsoleModel model;
	
	private SerialMonitor serialMonitor;
	private ResourceBundle res;
	
	private JToolBar toolBar;

	private Style logStyle;
	private Style systemStyle;


	public SerialConsole(SerialMonitor serialMonitor, int consoleIndex) {
		this.model = new SerialConsoleModel();
		this.serialMonitor = serialMonitor; 
		this.res = ResourceBundle.getBundle(SerialConsole.class.getName());
		
		// Construit le gui
		buildGUI(consoleIndex);

		
	}
	
	private void buildGUI(int consoleIndex) {
		
		// construit les différents éléments du GUI
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		// la barre de menu
		setJMenuBar(buildMenuBar());
		
		// la barre d'outil
		contentPane.add(buildToolBar(), BorderLayout.PAGE_START);

		// une zone centrale, pour ne pas occuper les bords qui pourrait être utilisé par la barre d'outil.
		JPanel centerPanel = new JPanel(new BorderLayout());
		contentPane.add(centerPanel, BorderLayout.CENTER);
		
		// la zone de texte principale
		centerPanel.add(buildTextPane(), BorderLayout.CENTER);
		
		// une barre sur le bas
		centerPanel.add(buildBottomBar(), BorderLayout.PAGE_END);
		

		// configure la fenêtre
		setTitle(MessageFormat.format(res.getString(FRAME_TITLE_KEY), consoleIndex++));
		setSize(Integer.parseInt(res.getString(FRAME_WIDTH_KEY)), Integer.parseInt(res.getString(FRAME_HEIGHT_KEY)));
	}

	/**
	 * Construit la barre de menu principale de la console. On y trouve actuellement le menu fichier,
	 * comportant une entrée pour ouvrir une nouvelle console, une entrée pour fermer la console, et
	 * une dernière entrée pour quitter definitivement le programme.
	 * 
	 * @param res le ResourceBundle permettant de récupèrer les textes.
	 * @return  La barre de menu, prête à être installé dans la console.
	 */
	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(res.getString(MENU_FILE_TEXT_KEY));
		menuBar.add(fileMenu);
		
		fileMenu.add(new AbstractAction(res.getString(NEW_CONSOLE_TEXT_KEY)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				serialMonitor.getSerialConsoleManager().createConsole();
			}
		});
		
		fileMenu.add(new AbstractAction(res.getString(CLOSE_CONSOLE_TEXT_KEY)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialConsole.this.setVisible(false);
			}
		});
		
		fileMenu.addSeparator();
		
		fileMenu.add(new AbstractAction(res.getString(QUIT_TEXT_KEY)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				serialMonitor.quit();
			}
		});
		
		return menuBar;
	}
	
	private JToolBar buildToolBar() {
		toolBar = new JToolBar(this.getTitle() + " toolbar");

		toolBar.add(createPortListComboBox());
		toolBar.add(new AbstractAction("U") { public void actionPerformed(ActionEvent e) {
			serialMonitor.getSerialService().refreshPorts();			
		} });
		toolBar.addSeparator();
		toolBar.add(createPortSpeedComboBox());
		toolBar.add(createOpenButton());
		toolBar.add(createWatchButton());
		toolBar.addSeparator();
		toolBar.add(createColorButton());
		toolBar.add(createFilterButton()); //new AbstractAction("Filter ...") { public void actionPerformed(ActionEvent e) {} });
		
		return toolBar;
	}
	
	/**
	 * Construit la comboBox listant les ports disponibles. La lie au <code>SerialService</code> pour garder la liste
	 * des ports à jour, et la lie au <code>SerialConsoleModel</code> pour 
	 * @return
	 */
	private JComboBox<?> createPortListComboBox() {
		final JComboBox<SerialPortDescriptor> combo = new JComboBox<>(new Vector<>(serialMonitor.getSerialService().getAvailablePorts()));
		getModel().setSelectedPort((SerialPortDescriptor)combo.getSelectedItem());
		combo.setMaximumSize(new Dimension(75, combo.getMaximumSize().height));
				
		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.info("Selected port change");
				getModel().setSelectedPort((SerialPortDescriptor)combo.getSelectedItem());
			}
		});
		
		serialMonitor.getSerialService().addSerialPortListener(new SerialPortListener() {
			@Override
			public void portRemoved(final SerialPortEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						combo.removeItem(event.getSerialPort());
					}
				});
			}
			
			@Override
			public void portAdded(final SerialPortEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						combo.addItem(event.getSerialPort());
					}
				});
			}
		});
		
		getModel().addPropertyChangeListener(SerialConsoleModel.SELECTED_PORT, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				combo.setSelectedItem(evt.getNewValue());
			}
		});
		
		return combo;
	}

	private JComboBox<?> createPortSpeedComboBox() {
		final JComboBox<Integer> combo = new JComboBox<>(new Integer[] {4800, 9600, 19200, 38400, 57600, 115200, 230400, 250000});
		combo.setSelectedItem(SerialService.DEFAULT_SPEED);
		combo.setMaximumSize(new Dimension(70, combo.getMaximumSize().height));
		
		// combo action
		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int speed = (Integer)combo.getSelectedItem();
				SerialPortDescriptor port = getModel().getSelectedPort();
				serialMonitor.getSerialService().setPortSpeed(port, speed);
			}
		});
		
		// binding
		getModel().addSelectedPortPropertyChangeListener(SerialPortDescriptor.SPEED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				combo.setSelectedItem(evt.getNewValue());
			}
		});
		
		return combo;
	}

	private JButton createOpenButton() {
		final JButton openCloseButton = new JButton("Open");
		openCloseButton.setActionCommand("open");
		openCloseButton.setEnabled(false);
		if(getModel().getSelectedPort() != null) {
			openButtonFollowsStatus(openCloseButton, getModel().getSelectedPort().getStatus());
		}
		
		// l'action du bouton
		openCloseButton.addActionListener(new AbstractAction() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("open")) {
					SerialPortDescriptor port = getModel().getSelectedPort();
					if(serialMonitor.getSerialService().openPort(port)) {
						getModel().addWatchedPort(port);
					}
				}
				else { // close
					SerialPortDescriptor port = getModel().getSelectedPort();
					serialMonitor.getSerialService().closePort(port);
					getModel().removeWatchedPort(port);
				}
			} 
		});
		
		getModel().addSelectedPortPropertyChangeListener(SerialPortDescriptor.STATUS, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Status status = (Status)evt.getNewValue();
				openButtonFollowsStatus(openCloseButton, status);
			}
		});
		
		return openCloseButton;
	}
	
	private void openButtonFollowsStatus(JButton openCloseButton, Status status) {
		switch(status) {
		case UNKNOWN:
		case CLOSE:
			openCloseButton.setActionCommand("open");
			openCloseButton.setText("Open");
			openCloseButton.setEnabled(true);
			break;
		case OPENING:
			openCloseButton.setActionCommand("nothing");
			openCloseButton.setText("Open");
			openCloseButton.setEnabled(false);
		case OPEN:
			openCloseButton.setActionCommand("close");
			openCloseButton.setText("Close");
			openCloseButton.setEnabled(true);
			break;
		case CLOSING:
			openCloseButton.setActionCommand("close");
			openCloseButton.setText("Close");
			openCloseButton.setEnabled(false);
			break;
		case USED:
			openCloseButton.setActionCommand("open");
			openCloseButton.setText("Open");
			openCloseButton.setEnabled(true);
			break;
		default:
			openCloseButton.setActionCommand("open");
			openCloseButton.setText("Open");
			openCloseButton.setEnabled(false);
		}
	}
	
	private JButton createWatchButton() {
		final JButton button = new JButton("Watch");
		button.setActionCommand("watch");
		button.setEnabled(false);
		if(getModel().getSelectedPort() != null) {
			watchButtonFollowsWatchList(button, getModel().getSelectedPort());
		}
		
		// button action
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("watch")) {
					getModel().addWatchedPort(getModel().getSelectedPort());
				}
				else {
					getModel().removeWatchedPort(getModel().getSelectedPort());
				}
			}
		});
		
		getModel().addSelectedPortPropertyChangeListener(SerialPortDescriptor.STATUS, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				watchButtonFollowsWatchList(button, getModel().getSelectedPort());
			}
		});
		
		getModel().addPropertyChangeListener(SerialConsoleModel.WATCHED_PORTS, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				watchButtonFollowsWatchList(button, getModel().getSelectedPort());
			}
		});
		return button;
	}

	private void watchButtonFollowsWatchList(JButton button, SerialPortDescriptor port) {
		if(port == null) {
			button.setText("Watch");
			button.setActionCommand("watch");
			button.setEnabled(false);
		}
		else {
			if(getModel().isPortWatched(port)) {
				button.setText("Unwatch");
				button.setActionCommand("unwatch");
				button.setEnabled(true);
			}
			else {
				button.setText("Watch");
				button.setActionCommand("watch");
				button.setEnabled(port.getStatus() == Status.OPEN);
			}
		}
	}
	
	private JButton createColorButton() {
		final Action colorAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SerialPortDescriptor descriptor = getModel().getSelectedPort();
				if(descriptor != null) {
					ConsoleSpecPortDescriptor specDescriptor = getModel().getSpecDescriptor(descriptor);
					specDescriptor.setColor((Color)getValue(ColorButton.ACTION_COLOR_KEY));
				}
			} 
		};
		
		getModel().addPropertyChangeListener(SerialConsoleModel.SELECTED_PORT, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				SerialPortDescriptor descriptor = (SerialPortDescriptor)evt.getNewValue();
				if(descriptor == null) {
					colorAction.putValue(ColorButton.ACTION_COLOR_KEY, Color.decode(Pref.get("defaultForeground", "#000")));
				}
				else {
					ConsoleSpecPortDescriptor specDescriptor = getModel().getSpecDescriptor(descriptor);
					colorAction.putValue(ColorButton.ACTION_COLOR_KEY, specDescriptor.getColor());
				}
			}
		});
		
		return new ColorButton(colorAction);
	}

	private JButton createFilterButton() {
		Action filterAction = new AbstractAction("filter...") {
			private FilterEditor filterFrame;

			@Override 
			public void actionPerformed(ActionEvent event) {
				SerialPortDescriptor descriptor = getModel().getSelectedPort();
				if(descriptor != null) {
					if(filterFrame == null) {
						filterFrame = new FilterEditor(SerialConsole.this);
					}
					filterFrame.setLocationRelativeTo(SerialConsole.this);
					filterFrame.setVisible(true);
				}
				
				
				ScriptEngineManager factory = new ScriptEngineManager();
				ScriptEngine engine = (ScriptEngine)factory.getEngineByName("JavaScript");
				Invocable inv = (Invocable) engine;
				try {
					engine.eval("function filter(message) { return true; }");
					boolean returnValue = (boolean)inv.invokeFunction("filter", "Hello world!");
					System.out.println(returnValue);
				}
				catch(ScriptException e) {
					System.err.println(e.getMessage());
				}
				catch(NoSuchMethodException e) {
					System.err.println(e.getMessage());
				}
			}
		};
		return new JButton(filterAction);
	}

	private JComponent buildTextPane() {
		final LogDocument logDocument = getModel().getLogDocument();
		LogPane textPane = new LogPane(logDocument);
		textPane.setEditable(false);
		textPane.getCaret().setVisible(true);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		
		// initialise les styles
		logStyle = logDocument.addStyle("log", null);
		StyleConstants.setFontFamily(logStyle, Pref.getString("defaultFontFamily", Font.MONOSPACED));
		StyleConstants.setFontSize(logStyle,   Pref.getInt("defaultFontSize", 12));
		StyleConstants.setForeground(logStyle, Color.decode(Pref.getString("defaultForeground", "#000")));
		
		systemStyle = logDocument.addStyle("red", logStyle);
		StyleConstants.setForeground(systemStyle, Color.decode(Pref.getString("defaultSystemForeground", "#aa0000")));
		
		serialMonitor.getSerialService().addSerialMessageListener(new SerialMessageListener() {
			@Override
			public void newSystemMessage(final SerialMessageEvent event) {
				StringBuilder sb = new StringBuilder();
				try {
					// s'assure que le message apparaisse sur une ligne unique.
					if(logDocument.getLength() > 0 && !"\n".equals(logDocument.getText(logDocument.getLength()-1, 1))) {
						sb.append("\n");
					}
				}
				catch(BadLocationException e) {
					// ne devrait pas arriver
					e.printStackTrace();
				}
				sb.append(event.getMessage()).append("\n");
				printText(sb.toString(), systemStyle);
			}

			@Override
			public void newSerialMessage(final SerialMessageEvent event) {
				if(getModel().isPortWatched(event.getSerialPort())) {
					SerialPortDescriptor descriptor = event.getSerialPort();
					ConsoleSpecPortDescriptor specDescriptor = getModel().getSpecDescriptor(descriptor);
					Style style = logDocument.getStyle(descriptor.getName());
					if(style == null) {
						style = logDocument.addStyle(descriptor.getName(), logStyle);
					}
					StyleConstants.setForeground(style, specDescriptor.getColor());
					
//					Invocable inv = specDescriptor.getFilter();
					boolean display = true;
					String message = event.getMessage();
//					if(inv != null) {
//						try {
							FilterMessage mes = new FilterMessage(message, style);
//							mes =  (FilterMessage)inv.invokeFunction("filter", mes);
							display = mes.display;
							message = mes.message;
							style = mes.style;
//						} catch (NoSuchMethodException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} catch (ScriptException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
					
					if(display) {
						printText(message, style);
					}
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return scrollPane;
	}
	
	/**
	 * Affiche du text dans la zone de log, le style étant specifié
	 * 
	 * @param text Le texte à afficher
	 * @param style le style à employer
	 * @throws IOException 
	 */
	private void printText(final String text, final Style style) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				getModel().getLogDocument().appendString(text, style);
			}
		});
	}


	private Component buildBottomBar() {
		Box box = new Box(BoxLayout.LINE_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		box.add(new JCheckBox("Auto scroll"));
		box.add(Box.createHorizontalStrut(5));
		
		box.add(new JTextField());
		box.add(Box.createHorizontalStrut(5));
		
		box.add(new JButton("send"));
		
		return box;
	}

	public SerialConsoleModel getModel() {
		return model;
	}

	/**
	 * Override pour que le titre de la toolbar suive celui de la fenêtre.
	 */
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		toolBar.setName(MessageFormat.format(res.getString(TOOLBAR_TITLE_KEY), title));
	}
}
