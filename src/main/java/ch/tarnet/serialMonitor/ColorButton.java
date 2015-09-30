package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;

import ch.tarnet.common.Pref;

public class ColorButton extends JButton {
	
	public static final String ACTION_COLOR_KEY = "Color";
	
	private BufferedImage iconImage;
	private Icon icon;
	
	public ColorButton(Action action) {
		iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		icon = new ImageIcon(iconImage);
		setAction(action);
		super.setModel(new ColorButtomModel());
	}
	
	@Override
	public void setAction(Action a) {
		a.putValue(Action.LARGE_ICON_KEY, icon);
		a.putValue(Action.SMALL_ICON, icon);
		super.setAction(a);
	}
	
	@Override
	protected void actionPropertyChanged(Action action, String propertyName) {
		super.actionPropertyChanged(action, propertyName);
		if(propertyName == ACTION_COLOR_KEY) {
			updateColorIcon(action);
		}
	}
	
	@Override
	protected void configurePropertiesFromAction(Action action) {
		super.configurePropertiesFromAction(action);
		updateColorIcon(action);
	}
	
	private void updateColorIcon(Action action) {
		Object obj = action.getValue(ACTION_COLOR_KEY);
		Color color = null;
		if(obj instanceof Color) {
			color = (Color)obj;
		}
		else {
			color = Color.decode(Pref.get("defaultForeground", "#000"));
		}
		Graphics2D g = (Graphics2D)iconImage.getGraphics();
		g.setColor(color);
		g.fillRect(0, 0, 16, 16);

		repaint();
	}
	
	private class ColorButtomModel extends DefaultButtonModel {
		@Override
		protected void fireActionPerformed(ActionEvent e) {
			Color newColor = JColorChooser.showDialog(ColorButton.this, "Pick a color", Color.black);
			ColorButton.this.getAction().putValue(ACTION_COLOR_KEY, newColor);
			
			super.fireActionPerformed(e);
		}
	}
}
