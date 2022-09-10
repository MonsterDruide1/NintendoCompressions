package types;

import java.nio.ByteOrder;
import java.util.Arrays;

import util.ByteConversions;

public class Vector2f {
	
	public float x, y;
	
	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}
	private Vector2f(Vector2f copy) {
		x = copy.x;
		y = copy.y;
	}
	
	@Override
	public Vector2f clone() {
		return new Vector2f(this);
	}
	
	@Override
	public String toString() {
		return "x: "+x+", y: "+y;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
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
		Vector2f other = (Vector2f) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		return true;
	}
	public byte[] getData() {
		byte[] xb = ByteConversions.fromFloat(x, ByteOrder.LITTLE_ENDIAN);
		byte[] yb = ByteConversions.fromFloat(y, ByteOrder.LITTLE_ENDIAN);
		
		return ByteConversions.mergeArrays(xb, yb);
	}

	public static Vector2f fromData(byte[] data, int offset) {
		return new Vector2f(
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset, offset + 4), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 4, offset + 8), ByteOrder.LITTLE_ENDIAN));
	}

}
