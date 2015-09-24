package ch.tarnet.common;

import java.io.IOException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class ApplicationPreferencesFactory implements PreferencesFactory {

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
			System.err.println("impossible de lire le fichier de config par default, qui devrait être inclu dans le jar parmi les .class.");
			System.err.println("Quelque chose de grave s'est donc produit avec le build, à réinstaller d'urgence.");
			System.err.println("Le programme va maintenant se terminé, pas la peine de tenter le diable.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(666);
		}
	}
}
