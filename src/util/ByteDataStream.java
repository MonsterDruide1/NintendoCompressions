package util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

import types.Vector3f;

public class ByteDataStream {
	
	private Stack<Integer> positionStore;
	private ByteBuffer buffer;

	public ByteDataStream(byte[] array) {
		this(array, ByteOrder.LITTLE_ENDIAN);
	}
	public ByteDataStream(byte[] array, ByteOrder order) {
		buffer = ByteBuffer.wrap(array).order(order);
		positionStore = new Stack<>();
	}
	
	public int size() {
		return buffer.capacity();
	}

	public void storePos() {
		positionStore.push(buffer.position());
	}
	public void loadPos() {
		buffer.position(positionStore.pop());
	}
	public int position() {
		return buffer.position();
	}
	
	public void seek(int position) {
		buffer.position(position);
	}
	public void seekForward(int offset) {
		buffer.position(buffer.position()+offset);
	}
	public void seekBackward(int offset) {
		buffer.position(buffer.position()-offset);
	}
	
	public void align(int alignment) {
		align(alignment, (byte) 0);
	}
	public void align(int alignment, byte fillValue) {
		int difference = (alignment-(position()%alignment))%alignment;
		byte[] alignmentBytes = new byte[difference];
		Arrays.fill(alignmentBytes, fillValue);
		expectBytes(alignmentBytes, "data in align"); // checks it really is empty
	}


	public byte getByte() {
		return buffer.get();
	}
	public short getShort() {
		return buffer.getShort();
	}
	public int getU24() {
		return NintendoUtil.u24IntFromByteArray(getBytes(3), buffer.order());
	}
	public int getInt() {
		return buffer.getInt();
	}
	public long getLong() {
		return buffer.getLong();
	}
	public float getHalf() {
		return ByteConversions.toHalf(getShort());
	}
	public float getFloat() {
		return buffer.getFloat();
	}
	public double getDouble() {
		return buffer.getDouble();
	}

	public byte peekByte() {
		return buffer.get(buffer.position());
	}

	public byte[] getBytes(int i) {
		if(i > buffer.remaining()) {
			throw new IllegalArgumentException("Not enough bytes left in buffer: Requested "+i+" bytes, but only "+buffer.remaining()+" are available!");
		}
		byte[] data = new byte[i];
		buffer.get(data);
		return data;
	}
	public short[] getShorts(int count) {
		short[] returns = new short[count];
		for(int i=0;i<count;i++) {
			returns[i] = getShort();
		}
		return returns;
	}
	public int[] getInts(int count) {
		int[] returns = new int[count];
		for(int i=0;i<count;i++) {
			returns[i] = getInt();
		}
		return returns;
	}
	public long[] getLongs(int count) {
		long[] returns = new long[count];
		for(int i=0;i<count;i++) {
			returns[i] = getLong();
		}
		return returns;
	}
	public float[] getFloats(int count) {
		float[] returns = new float[count];
		for(int i=0;i<count;i++) {
			returns[i] = getFloat();
		}
		return returns;
	}

	public String getString(StringType type) {
		return getString(type, StandardCharsets.UTF_8);
	}
	public String getString(StringType type, Charset encoding) {
		return switch(type) {
		case NULL_TERMINATED -> {
			buffer.mark();
			int count = 0;
			while(getByte() != 0) count++;
			buffer.reset();
			String s = getString(count, encoding);
			expectByte(0, "value after null-terminated string");
			yield s;
		}
		case BYTE_LEN_PREFIX -> {
			yield getString(getByte(), encoding);
		}
		case SHORT_LEN_PREFIX -> {
			yield getString(getShort(), encoding);
		}
		};
	}
	public String getString(int length, Charset encoding) {
		return new String(getBytes(length), encoding);
	}
	public String getString(int length) {
		return new String(getBytes(length), StandardCharsets.UTF_8);
	}

	public Vector3f readVector3F() {
		return new Vector3f(getFloat(), getFloat(), getFloat());
	}
	public int[] readVector3U() {
		return getInts(3);
	}
	
	public ByteOrder readByteOrder() {
		short byteOrderMark = getShort();
		return switch(byteOrderMark) {
		case (short) 0xFEFF -> ByteOrder.BIG_ENDIAN;
		case (short) 0xFFFE -> ByteOrder.LITTLE_ENDIAN;
		default -> throw new UnsupportedOperationException("Unknown byte order mark: "+byteOrderMark);
		};
	}
	
	
	public String debugInfo() {
		byte[] nextBytes = new byte[100];
		buffer.get(nextBytes);
		return "Offset: "+Integer.toHexString(buffer.position())+", "+
				"next 100: "+Arrays.toString(NintendoUtil.intStream(nextBytes).mapToObj(Integer::toHexString).map(s -> s.length()==1 ? "0"+s : s).toArray());
	}

	public int remainingLength() {
		return buffer.capacity()-buffer.position();
	}
	public byte[] remainingOriginal() {
		return getBytes(remainingLength());
	}
	
	public void assertMagic(String expectedMagic) {
		String magic = getString(expectedMagic.length());
		if(!magic.equals(expectedMagic)) {
			throw new UnsupportedOperationException(expectedMagic+" magic not correct: "+magic);
		}
	}
	public void assertPosition(int pos) {
		if(position() != pos) {
			throw new UnsupportedOperationException("Does not match expected position: "+position()+", expected: "+pos);
		}
	}
	public void assertEOF() {
		if(buffer.remaining() != 0) {
			throw new UnsupportedOperationException("Expected end of stream, but actually "+buffer.remaining()+" bytes remain");
		}
	}
	

	public void expectByte(int value, String message) {
		byte val = getByte();
		if(val != value) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+value+", actual: "+val);
		}
	}
	public void expectShort(int value, String message) {
		short val = getShort();
		if(val != value) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+value+", actual: "+val);
		}
	}
	public void expectInt(int value, String message) {
		int val = getInt();
		if(val != value) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+value+", actual: "+val);
		}
	}
	public void expectLong(long value, String message) {
		long val = getLong();
		if(val != value) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+value+", actual: "+val);
		}
	}
	public void expectFloat(float value, String message) {
		float val = getFloat();
		if(val != value) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+value+", actual: "+val);
		}
	}
	public void expectByteOrder(ByteOrder order) {
		ByteOrder o = readByteOrder();
		if(o != order) {
			throw new UnsupportedOperationException("Unexpected or unsupported byte order. expected: "+order+", actual: "+o);
		}
	}
	public void expectBytes(byte[] data, String message) {
		byte[] val = getBytes(data.length);
		if(!Arrays.equals(val, data)) {
			throw new UnsupportedOperationException("Unexpected "+message+". expected: "+Arrays.toString(data)+", actual: "+Arrays.toString(val));
		}
	}
	
	
	public enum StringType {
		NULL_TERMINATED,
		BYTE_LEN_PREFIX,
		SHORT_LEN_PREFIX
	}

}
