package ch.tarnet.common;

import static org.junit.Assert.*;

import java.util.prefs.Preferences;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestApplicationPreferences {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("java.util.prefs.PreferencesFactory", "ch.tarnet.common.ApplicationPreferencesFactory");
		System.setProperty("java.util.prefs.PreferencesFactory.defaultFile", "test.config");
		Pref.loadPreferences();
	}

	@Test
	public void testGetSpi() {
		Preferences pref = Preferences.systemRoot();
		assertTrue("Preference system doesn't load the ApplicationPreferences implementation", pref instanceof ApplicationPreferences);
		ApplicationPreferences appPref = (ApplicationPreferences)pref;
		assertEquals("Hello world!", appPref.getSpi("testStringPreference1"));
		assertEquals("Hello world! ", appPref.getSpi("testStringPreference2"));
		assertEquals("42", appPref.getSpi("testIntPreference1"));
		assertEquals("-5", appPref.getSpi("testIntPreference2"));
	}
	
	@Test
	public void testGet() {
		assertEquals("Hello world!", Pref.get("testStringPreference1"));
		assertEquals("Hello world! ", Pref.get("testStringPreference2"));
		assertEquals("42", Pref.get("testIntPreference1"));
		assertEquals("-5", Pref.get("testIntPreference2"));
	}
	
	@Test
	public void testGetInt() {
		assertEquals(42, Pref.getInt("testIntPreference1", -1));
		assertEquals(-5, Pref.getInt("testIntPreference2", -1));
		assertEquals(-1, Pref.getInt("testStringPreference1", -1));
	}
}
