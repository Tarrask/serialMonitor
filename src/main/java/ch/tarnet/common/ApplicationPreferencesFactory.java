package ch.tarnet.common;

import java.io.IOException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationPreferencesFactory implements PreferencesFactory {

	private Logger logger = LoggerFactory.getLogger(ApplicationPreferencesFactory.class);
	private static ApplicationPreferences root;
	
	public ApplicationPreferencesFactory() {
		createRoot();
	}

	@Override
	public Preferences systemRoot() {
		if(root == null) {
			createRoot();
		}
		return root;
	}

	@Override
	public Preferences userRoot() {
		return systemRoot();
	}

	private void createRoot() {
		try {
			root = new ApplicationPreferences();
		}
		catch(IOException e) {
			logger.error("impossible de lire le fichier de config par default, qui devrait être inclu dans le jar parmi les .class.");
			logger.error("Quelque chose de grave s'est donc produit avec le build, à réinstaller d'urgence.");
			logger.error("Le programme va maintenant se terminer, pas la peine de tenter le diable.");
			logger.error(e.getMessage());
			e.printStackTrace();
			System.exit(666);
		}
	}
}
