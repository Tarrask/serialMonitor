package ch.tarnet.serialMonitor;

public class TestApplication extends Launcher {
	
	public TestApplication() {
		super(new FakePortWatcher());
	}

	public static void main(String[] args) {
		System.setProperty("java.util.prefs.PreferencesFactory.defaultFile", "test.config");
		new TestApplication().run();
	}
}


