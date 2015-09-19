package ch.tarnet.serialMonitor;

import gnu.io.CommPortIdentifier;
import gnu.io.FakeIdentifier;

import java.util.Enumeration;
import java.util.Vector;

public class FakePortWatcher extends PortWatcher {
	protected Enumeration<CommPortIdentifier> getPortIdentifiers()  {
		Vector<CommPortIdentifier> v = new Vector<CommPortIdentifier>();
		Enumeration<CommPortIdentifier> e = unsafeCast(CommPortIdentifier.getPortIdentifiers());
		while(e.hasMoreElements()) {
			v.add(e.nextElement());
		}
		v.add(new FakeIdentifier("COM91"));
		return v.elements();
	}
}
