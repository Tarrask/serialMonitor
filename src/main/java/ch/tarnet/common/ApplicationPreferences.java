package ch.tarnet.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationPreferences extends AbstractPreferences {

	private final Logger logger = LoggerFactory.getLogger(ApplicationPreferences.class.getName());
	
	/**
	 * Les 3 fichiers de preferences, celui par defaut, se trouvant avec le code, celui global
	 * � l'application, se trouvant généralement dans le répertoire de l'application et celui
	 * de l'utilisateur, qui réside en principe dans le dossier de l'utilisateur
	 */
	private Properties defaultProperties, applicationProperties, userProperties;
	/**
	 * Une référence aux fichiers de preferences utilisé pour les preferences de l'application
	 * ainsi que celle de l'utilisateur.
	 */
	private File applicationFile, userFile;
	
	/**
	 * Construit un ApplicationPreference, charge les 3 fichiers de configuration.
	 * 
	 * Le fichier des preferences par defaut doit se trouver dans le classpath, par default, il
	 * se trouve à la racine du classpath et se nomme default.config. Le nom et l'emplacement du
	 * fichier peut être modifié grace à la propriété system java.util.prefs.PreferencesFactory.defaultFile.
	 * Il est localisé au moyen de ClassLoader.getSystemResource.
	 * 
	 * Le fichier des preferences de l'application se trouve par défaut à la racine de l'application
	 * et se nomme application.config. L'emplacement et le nom du fichier peuvent être modifié
	 * en utilisant la preference applicationConfigFile qui doit se trouver dans le fichier de config
	 * par defaut.
	 * 
	 * Le fichier utilisateur se trouve par défaut dans le dossier utilisateur et se nomme .unnamedJavaApp.config
	 * L'emplacement et le nom peuvent être modifié grace à la preference userConfigFile, qui peut se
	 * trouvé dans le fichier par defaut ou dans celui de l'application, la preference de l'application
	 * prenant le dessus. Dans le chemin d'accès au fichier, la chaine "%user.dir%" sera remplacé par la
	 * propriété system user.home (System.getProperty("user.home");
	 * @throws IOException
	 */
	public ApplicationPreferences() throws IOException {
		super(null, "");
		
		// on charge le fichier de properties par default, se trouvant dans le jar de l'application
		String path = System.getProperty("java.util.prefs.PreferencesFactory.defaultFile", "default.config");
		defaultProperties = new Properties();
		defaultProperties.load(ClassLoader.getSystemResourceAsStream(path));
		
		// on charge le fichier de properties de l'application, modifiant les param�tres pour tous
		// les utilisateurs du system.
		path = defaultProperties.getProperty("applicationConfigFile", "application.config");
		applicationFile = new File(path);
		applicationProperties = new Properties();
		// si le fichier existe on le lit, sinon il y aura juste pas de preferences
		if(applicationFile.exists()) {
			try {
				applicationProperties.load(new FileInputStream(applicationFile));
			}
			catch(IOException e) {
				logger.warn("Unable to read application preference file at: {}", path);			
			}
		}
		
		// on charge le fichier de properties de l'utilisateur, normalement dans le r�pertoire utilisateur
		// A nouveau, pas grave si le fichier n'existe pas.
		path = applicationProperties.getProperty("userConfigFile", 
					defaultProperties.getProperty("userConfigFile", "%user.home%/.unnamedJavaApp.config"));
		String userHome = System.getProperty("user.home").replaceAll("\\\\", "/");
		path = path.replaceFirst("%user\\.dir%", userHome);
		logger.debug("user config: {}", path);
		userFile = new File(path);
		userProperties = new Properties();
		if(userFile.exists()) {
			try {
				userProperties.load(new FileInputStream(userFile));
			}
			catch(IOException e) {
				logger.info("Unable to read user preference file at: " + path);
			}
		}
	}

	@Override
	protected void putSpi(String key, String value) {
		userProperties.setProperty(key, value);
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getSpi(String key) {
		String val = userProperties.getProperty(key);
		if(val == null) {
			val = applicationProperties.getProperty(key);
		}
		if(val == null) {
			val = defaultProperties.getProperty(key);
		}
		
		return val;
	}

	@Override
	protected void removeSpi(String key) {
		userProperties.remove(key);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return (String[])defaultProperties.keySet().toArray();
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		// TODO Auto-generated method stub
		System.out.println("in ApplicationPreferences.childSpi: " + name);
		//throw new UnsupportedOperationException();
		return this;
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	
}
