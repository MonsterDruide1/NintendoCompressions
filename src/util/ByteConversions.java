package util;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;

public class ByteConversions {
	
	public static byte[] fromShort(short s, ByteOrder order) {
		if(order.equals(ByteOrder.BIG_ENDIAN)) {
			return new byte[] {(byte)(s >> 8), (byte)s};
		} else {
			return new byte[] {(byte)s, (byte)(s >> 8)};
		}
	}
	
	public static byte[] fromInt(int i, ByteOrder order) {
		if(order.equals(ByteOrder.BIG_ENDIAN)) {
		    return new byte[] {
		            (byte)(i >> 24),
		            (byte)(i >> 16),
		            (byte)(i >> 8),
		            (byte)i};
		} else {
		    return new byte[] {
		            (byte)i,
		            (byte)(i >> 8),
		            (byte)(i >> 16),
		            (byte)(i >> 24)};
		}
	}
	
	public static int toInt(byte[] data, ByteOrder order) {
		if(order.equals(ByteOrder.BIG_ENDIAN)) {
		    return
		            ((data[0] & 0xff) << 24) |
		            ((data[1] & 0xff) << 16) |
		            ((data[2] & 0xff) << 8) |
		            (data[3] & 0xff);
		} else {
		    return
		            ((int)data[0] & 0xff) |
		            (((int)data[1] & 0xff) << 8) |
		            (((int)data[2] & 0xff) << 16) |
		            (((int)data[3] & 0xff) << 24);
		}
	}
	
	public static byte[] fromLong(long l, ByteOrder order) {
		if(order.equals(ByteOrder.BIG_ENDIAN)) {
			return new byte[] {
				       (byte) (l >> 56),
				       (byte) (l >> 48),
				       (byte) (l >> 40),
				       (byte) (l >> 32),
				       (byte) (l >> 24),
				       (byte) (l >> 16),
				       (byte) (l >> 8),
				       (byte) l};
		} else {
			return new byte[] {
				       (byte) l,
				       (byte) (l >> 8),
				       (byte) (l >> 16),
				       (byte) (l >> 24),
				       (byte) (l >> 32),
				       (byte) (l >> 40),
				       (byte) (l >> 48),
				       (byte) (l >> 56)};
		}
	}

	public static byte[] fromFloat(float f, ByteOrder order) {
		return fromInt(Float.floatToIntBits(f), order);
	}
	
	public static float toFloat(byte[] data, ByteOrder order) {
		return Float.intBitsToFloat(toInt(data, order));
	}
	
	public static byte[] fromDouble(double d, ByteOrder order) {
		return fromLong(Double.doubleToLongBits(d), order);
	}
	
	public static byte[] fromBooleans(boolean... bools) {
		BitSet bits = new BitSet(bools.length);
	    for (int i = 0; i < bools.length; i++) {
	        if (bools[i]) {
	            bits.set(i);
	        }
	    }

	    byte[] bytes = bits.toByteArray();
	    if (bytes.length * 8 >= bools.length) {
	        return bytes;
	    } else {
	        return Arrays.copyOf(bytes, bools.length / 8 + (bools.length % 8 == 0 ? 0 : 1));
	    }
	}

	public static String nullterminatedString(byte[] data, int start) {
		int end = data.length;
		for(int i=start; i<data.length; i++) {
			if(data[i] == '\0') {
				end = i;
				break;
			}
		}
		return new String(Arrays.copyOfRange(data, start, end));
	}
	
	public static byte[] mergeArrays(byte[]... arrays) {
		int size = 0;
		for(int i=0;i<arrays.length;i++)
			size += arrays[i].length;
		
		byte[] array = new byte[size];
		size = 0;
		for(int i=0;i<arrays.length;i++) {
			System.arraycopy(arrays[i], 0, array, size, arrays[i].length);
			size += arrays[i].length;
		}
		
		return array;
	}
	
	// ignores the higher 16 bits
	public static float toHalf( int hbits )
	{
	    int mant = hbits & 0x03ff;            // 10 bits mantissa
	    int exp =  hbits & 0x7c00;            // 5 bits exponent
	    if( exp == 0x7c00 )                   // NaN/Inf
	        exp = 0x3fc00;                    // -> NaN/Inf
	    else if( exp != 0 )                   // normalized value
	    {
	        exp += 0x1c000;                   // exp - 15 + 127
	        if( mant == 0 && exp > 0x1c400 )  // smooth transition
	            return Float.intBitsToFloat( ( hbits & 0x8000 ) << 16
	                                            | exp << 13 | 0x3ff );
	    }
	    else if( mant != 0 )                  // && exp==0 -> subnormal
	    {
	        exp = 0x1c400;                    // make it normal
	        do {
	            mant <<= 1;                   // mantissa * 2
	            exp -= 0x400;                 // decrease exp by 1
	        } while( ( mant & 0x400 ) == 0 ); // while not normal
	        mant &= 0x3ff;                    // discard subnormal bit
	    }                                     // else +/-0 -> +/-0
	    return Float.intBitsToFloat(          // combine all parts
	        ( hbits & 0x8000 ) << 16          // sign  << ( 31 - 15 )
	        | ( exp | mant ) << 13 );         // value << ( 23 - 10 )
	}

}
