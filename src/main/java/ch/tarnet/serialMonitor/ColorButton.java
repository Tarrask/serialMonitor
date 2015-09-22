package ch.tarnet.serialMonitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import ch.tarnet.common.Pref;

public class ColorButton extends JButton {
	
	private BufferedImage iconImage;
	private Icon icon;
	
	public ColorButton(Action action) {
		iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		icon = new ImageIcon(iconImage);
		action.putValue(Action.LARGE_ICON_KEY, icon);
		action.putValue(Action.SMALL_ICON, icon);
		super.setAction(action);
	}
	
	@Override
	protected void actionPropertyChanged(Action action, String propertyName) {
		super.actionPropertyChanged(action, propertyName);
		if(propertyName == "Color") {
			updateColorIcon(action);
		}
	}
	
	@Override
	protected void configurePropertiesFromAction(Action action) {
		super.configurePropertiesFromAction(action);
		updateColorIcon(action);
	}
	
	private void updateColorIcon(Action action) {
		Object obj = action.getValue("Color");
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
	}
}
