package ch.tarnet.serialMonitor;

public class TestApplication extends Launcher {
	
	public TestApplication() {
		super(new FakePortWatcher());
	}

	public static void main(String[] args) {
		new TestApplication().run();
	}
}


