package gnu.io;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

public class FakeIdentifier extends CommPortIdentifier {

	public FakeIdentifier(String name) {
		super(name, null, PORT_SERIAL, new FakeCommDriver());
	}
	
	@Override
	public void addPortOwnershipListener(CommPortOwnershipListener arg0) {
		System.out.println("In FakeIdentifier.addPortOwnershipListener");
		super.addPortOwnershipListener(arg0);
	}
	
	@Override
	void fireOwnershipEvent(int arg0) {
		System.out.println("In FakeIdentifier.fireOwnershipEvent: " + arg0);
		super.fireOwnershipEvent(arg0);
	}
	
	@Override
	public String getCurrentOwner() {
		System.out.println("In FakeIdentifier.getCurrentOwner");
		return super.getCurrentOwner();
	}
	
	@Override
	public String getName() {
		System.out.println("In FakeIdentifier.getName");
		return super.getName();
	}
	
	@Override
	public int getPortType() {
		System.out.println("In FakeIdentifier.getPortType");
		return super.getPortType();
	}
	
	@Override
	void internalClosePort() {
		System.out.println("In FakeIdentifier.internalClosePort");
		super.internalClosePort();
	}
	
	@Override
	public synchronized boolean isCurrentlyOwned() {
		System.out.println("In FakeIdentifier.isCurrentlyOwned");
		return super.isCurrentlyOwned();
	}
	
	@Override
	public synchronized CommPort open(FileDescriptor arg0) throws UnsupportedCommOperationException {
		System.out.println("In FakeIdentifier.open");
		return super.open(arg0);
	}
	
	@Override
	public CommPort open(String arg0, int arg1) throws PortInUseException {
		System.out.println("In FakeIdentifier.open");
		return super.open(arg0, arg1);
	}
	
	@Override
	public void removePortOwnershipListener(CommPortOwnershipListener arg0) {
		System.out.println("In FakeIdentifier.removePortOwnershipListener");
		super.removePortOwnershipListener(arg0);
	}
	
	static class FakePort extends SerialPort {
		@Override
		public void close() {
			System.out.println("In FakePort.close");
			super.close();
		}
		@Override
		public void disableReceiveFraming() {
			System.out.println("In FakePort.disableReceiveFraming");
		}

		@Override
		public void disableReceiveThreshold() {
			System.out.println("In FakePort.disableReceiveThreshold");
		}

		@Override
		public void disableReceiveTimeout() {
			System.out.println("In FakePort.disableReceiveTimeout");
		}

		@Override
		public void enableReceiveFraming(int f)	throws UnsupportedCommOperationException {
			System.out.println("In FakePort.enableReceiveFraming");
		}

		@Override
		public void enableReceiveThreshold(int thresh) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.enableReceiveThreshold");
		}

		@Override
		public void enableReceiveTimeout(int time) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.enableReceiveTimeout");
		}

		@Override
		public int getInputBufferSize() {
			System.out.println("In FakePort.getInputBufferSize");
			return 0;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			System.out.println("In FakePort.getInputStream");
			return new ByteArrayInputStream("Hello world\n".getBytes());
		}

		@Override
		public int getOutputBufferSize() {
			System.out.println("In FakePort.getOutputBufferSize");
			return 0;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			System.out.println("In FakePort.getOutputStream");
			return null;
		}

		@Override
		public int getReceiveFramingByte() {
			System.out.println("In FakePort.getReceiveFramingByte");
			return 0;
		}

		@Override
		public int getReceiveThreshold() {
			System.out.println("In FakePort.getReceiveThreshold");
			return 0;
		}

		@Override
		public int getReceiveTimeout() {
			System.out.println("In FakePort.getReceiveTimeout");
			return 0;
		}

		@Override
		public boolean isReceiveFramingEnabled() {
			System.out.println("In FakePort.isReceiveFramingEnabled");
			return false;
		}

		@Override
		public boolean isReceiveThresholdEnabled() {
			System.out.println("In FakePort.isReceiveThresholdEnabled");
			return false;
		}

		@Override
		public boolean isReceiveTimeoutEnabled() {
			System.out.println("In FakePort.isReceiveTimeoutEnabled");
			return false;
		}

		@Override
		public void setInputBufferSize(int size) {
			System.out.println("In FakePort.setInputBufferSize");
			
		}

		@Override
		public void setOutputBufferSize(int size) {
			System.out.println("In FakePort.setOutputBufferSize: " + size);
			
		}

		@Override
		public void addEventListener(SerialPortEventListener lsnr) throws TooManyListenersException {
			System.out.println("In FakePort.addEventListener");
			
		}

		@Override
		public int getBaudBase() throws UnsupportedCommOperationException, IOException {
			System.out.println("In FakePort.getBaudBase");
			return 0;
		}

		@Override
		public int getBaudRate() {
			System.out.println("In FakePort.getBaudRate");
			return 0;
		}

		@Override
		public boolean getCallOutHangup() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.getCallOutHangup");
			return false;
		}

		@Override
		public int getDataBits() {
			System.out.println("In FakePort.getDataBits");
			return 0;
		}

		@Override
		public int getDivisor() throws UnsupportedCommOperationException, IOException {
			System.out.println("In FakePort.getDivisor");
			return 0;
		}

		@Override
		public byte getEndOfInputChar() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.getEndOfInputChar");
			return 0;
		}

		@Override
		public int getFlowControlMode() {
			System.out.println("In FakePort.getFlowControlMode");
			return 0;
		}

		@Override
		public boolean getLowLatency() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.getLowLatency");
			return false;
		}

		@Override
		public int getParity() {
			System.out.println("In FakePort.getParity");
			return 0;
		}

		@Override
		public byte getParityErrorChar() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.getParityErrorChar");
			return 0;
		}

		@Override
		public int getStopBits() {
			System.out.println("In FakePort.getStopBits");
			return 0;
		}

		@Override
		public String getUARTType() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.getUARTType");
			return null;
		}

		@Override
		public boolean isCD() {
			System.out.println("In FakePort.isCD");
			return false;
		}

		@Override
		public boolean isCTS() {
			System.out.println("In FakePort.isCTS");
			return false;
		}

		@Override
		public boolean isDSR() {
			System.out.println("In FakePort.isDSR");
			return false;
		}

		@Override
		public boolean isDTR() {
			System.out.println("In FakePort.isDTR");
			return false;
		}

		@Override
		public boolean isRI() {
			System.out.println("In FakePort.isRI");
			return false;
		}

		@Override
		public boolean isRTS() {
			System.out.println("In FakePort.isRTS");
			return false;
		}

		@Override
		public void notifyOnBreakInterrupt(boolean enable) {
			System.out.println("In FakePort.notifyOnBreakInterrupt");
			
		}

		@Override
		public void notifyOnCTS(boolean enable) {
			System.out.println("In FakePort.notifyOnCTS");
			
		}

		@Override
		public void notifyOnCarrierDetect(boolean enable) {
			System.out.println("In FakePort.notifyOnCarrierDetect");
		}

		@Override
		public void notifyOnDSR(boolean enable) {
			System.out.println("In FakePort.notifyOnDSR");
		}

		@Override
		public void notifyOnDataAvailable(boolean enable) {
			System.out.println("In FakePort.notifyOnDataAvailable");
		}

		@Override
		public void notifyOnFramingError(boolean enable) {
			System.out.println("In FakePort.notifyOnFramingError");
		}

		@Override
		public void notifyOnOutputEmpty(boolean enable) {
			System.out.println("In FakePort.notifyOnOutputEmpty");
		}

		@Override
		public void notifyOnOverrunError(boolean enable) {
			System.out.println("In FakePort.notifyOnOverrunError");
		}

		@Override
		public void notifyOnParityError(boolean enable) {
			System.out.println("In FakePort.notifyOnParityError");
		}

		@Override
		public void notifyOnRingIndicator(boolean enable) {
			System.out.println("In FakePort.notifyOnRingIndicator");
		}

		@Override
		public void removeEventListener() {
			System.out.println("In FakePort.removeEventListener");
		}

		@Override
		public void sendBreak(int duration) {
			System.out.println("In FakePort.sendBreak");
		}

		@Override
		public boolean setBaudBase(int BaudBase) throws UnsupportedCommOperationException, IOException {
			System.out.println("In FakePort.setBaudBase");
			return false;
		}

		@Override
		public boolean setCallOutHangup(boolean NoHup) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setCallOutHangup");
			return false;
		}

		@Override
		public void setDTR(boolean state) {
			System.out.println("In FakePort.setDTR");
		}

		@Override
		public boolean setDivisor(int Divisor) throws UnsupportedCommOperationException, IOException {
			System.out.println("In FakePort.setDivisor");
			return false;
		}

		@Override
		public boolean setEndOfInputChar(byte b) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setEndOfInputChar");
			return false;
		}

		@Override
		public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setFlowControlMode");
			
		}

		@Override
		public boolean setLowLatency() throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setLowLatency");
			return false;
		}

		@Override
		public boolean setParityErrorChar(byte b)
				throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setParityErrorChar");
			return false;
		}

		@Override
		public void setRTS(boolean state) {
			System.out.println("In FakePort.setRTS");
		}

		@Override
		public void setSerialPortParams(int b, int d, int s, int p)	throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setSerialPortParams: " + b + ", " + d + ", " + s + ", " + p);
			
		}

		@Override
		public boolean setUARTType(String type, boolean test) throws UnsupportedCommOperationException {
			System.out.println("In FakePort.setUARTType");
			return false;
		}
		
	}

	static class FakeCommDriver implements CommDriver {

		@Override
		public CommPort getCommPort(String portName, int portType) {
			System.out.println("In FakeCommDriver.getCommPort");
			return new FakePort();
		}

		@Override
		public void initialize() {
			System.out.println("In FakeCommDriver.initialize");
		}
		
	}
}
