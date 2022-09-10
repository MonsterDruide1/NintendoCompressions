package types;

import java.nio.ByteOrder;
import java.util.Arrays;

import util.ByteConversions;

public class Vector3f {

	public float x, y, z;

	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	private Vector3f(Vector3f copy) {
		x = copy.x;
		y = copy.y;
		z = copy.z;
	}

	@Override
	public Vector3f clone() {
		return new Vector3f(this);
	}
	
	@Override
	public String toString() {
		return "x: "+x+", y: "+y+", z: "+z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		Vector3f other = (Vector3f) obj;
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

		return ByteConversions.mergeArrays(xb, yb, zb);
	}

	public static Vector3f fromData(byte[] data, int offset) {
		return new Vector3f(
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset, offset + 4), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 4, offset + 8), ByteOrder.LITTLE_ENDIAN),
				ByteConversions.toFloat(Arrays.copyOfRange(data, offset + 8, offset + 12), ByteOrder.LITTLE_ENDIAN));
	}
	
	public Vector3f crossProduct(Vector3f other) {
		return new Vector3f(
				this.y * other.z - this.z * other.y,
				this.z * other.x - this.x * other.z,
				this.x * other.y - this.y * other.x
		);
	}
	public float dotProduct(Vector3f other) {
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}
	public Vector3f multiply(float factor) {
		return new Vector3f(
				this.x * factor,
				this.y * factor,
				this.z * factor
		);
	}
	public Vector3f add(Vector3f other) {
		return new Vector3f(
				this.x + other.x,
				this.y + other.y,
				this.z + other.z
		);
	}

}
