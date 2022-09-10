package types;

import java.nio.ByteOrder;
import java.util.Arrays;

import util.ByteConversions;

public class Quatf {

	public float x, y, z, w;

	public Quatf(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	private Quatf(Quatf copy) {
		x = copy.x;
		y = copy.y;
		z = copy.z;
		w = copy.w;
	}

	@Override
	public Quatf clone() {
		return new Quatf(this);
	}
	
	@Override
	public String toString() {
		return "x: "+x+", y: "+y+", z: "+z+", w: "+w;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(w);
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		result = prime * result + Float.floatToIntBits(z);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Quatf other = (Quatf) obj;
		if (Float.floatToIntBits(w) != Float.floatToIntBits(other.w))
			return false;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
			return false;
		return true;
	}

	public byte[] getData() {
		byte[] xb = ByteConversions.fromFloat(x, ByteOrder.LITTLE_ENDIAN);
		byte[] yb = ByteConversions.fromFloat(y, ByteOrder.LITTLE_ENDIAN);
		byte[] zb = ByteConversions.fromFloat(z, ByteOrder.LITTLE_ENDIAN);
		byte[] wb = ByteConversions.fromFloat(w, ByteOrder.LITTLE_ENDIAN);

		return ByteConversions.mergeArrays(xb, yb, zb, wb);
	}

	public static Quatf fromData(byte[] data, int offset) {
		return new Quatf(
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset, offset + 4), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 4, offset + 8), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 8, offset + 12), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 12, offset + 16), ByteOrder.LITTLE_ENDIAN));
	}

}
