package ch.tarnet.common;

import java.util.prefs.Preferences;

public class Pref {

	static private Preferences pref = Preferences.systemRoot();
	
	static public String get(String key) {
		return get(key, null);
	}
	
	static public String get(String key, String def) {
		return pref.get(key, def);
	}
	
	static public String getString(String key, String def) {
		return get(key, def);
	}
	
	static public boolean getBoolean(String key, boolean def) {
		return pref.getBoolean(key, def);
	}
	
	static public byte[] getByteArray(String key, byte[] def) {
		return pref.getByteArray(key, def);
	}
	
	static public double getDouble(String key, double def) {
		return pref.getDouble(key, def);
	}
	
	static public float getFloat(String key, float def) {
		return pref.getFloat(key, def);
	}
	
	static public int getInt(String key, int def) {
		return pref.getInt(key, def);
	}

	static public long getLong(String key, long def) {
		return pref.getLong(key, def);
	}
}
