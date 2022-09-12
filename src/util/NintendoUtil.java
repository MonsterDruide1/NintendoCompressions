package util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class NintendoUtil {

	public static String[] readFile(File file) throws IOException {
		final LinkedList<String> list = new LinkedList<>();
		final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
		String line;
		while ((line = in.readLine()) != null) {
			list.add(line);
		}
		in.close();
		return list.toArray(new String[0]);
	}
	
	public static byte[] readFileToByte(File file) throws IOException {
		return Files.readAllBytes(file.toPath());
	}

	public static void writeToFile(File file, String string) throws FileNotFoundException {
		if(string == null)
			throw new NullPointerException("string to write was null!");
		PrintWriter pw = new PrintWriter(file);
		pw.print(string);
		pw.flush();
		pw.close();
	}

	public static void writeToFile(File file, byte[] array) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
		    stream.write(array);
		}
	}

	public static int intFromByteArray(byte[] bytes) {
	     return intFromByteArray(bytes, ByteOrder.BIG_ENDIAN);
	}
	public static int intFromByteArray(byte[] bytes, ByteOrder order) {
	     return ByteBuffer.wrap(bytes).order(order).getInt();
	}
	public static long longFromByteArray(byte[] bytes) {
	     return longFromByteArray(bytes, ByteOrder.BIG_ENDIAN);
	}
	public static long longFromByteArray(byte[] bytes, ByteOrder order) {
	     return ByteBuffer.wrap(bytes).order(order).getLong();
	}
	public static short shortFromByteArray(byte[] bytes) {
	     return shortFromByteArray(bytes, ByteOrder.BIG_ENDIAN);
	}
	public static short shortFromByteArray(byte[] bytes, ByteOrder order) {
	     return ByteBuffer.wrap(bytes).order(order).getShort();
	}
	public static float floatFromByteArray(byte[] bytes) {
	     return floatFromByteArray(bytes, ByteOrder.BIG_ENDIAN);
	}
	public static float floatFromByteArray(byte[] bytes, ByteOrder order) {
	     return ByteBuffer.wrap(bytes).order(order).getFloat();
	}
	public static double doubleFromByteArray(byte[] bytes) {
	     return doubleFromByteArray(bytes, ByteOrder.BIG_ENDIAN);
	}
	public static double doubleFromByteArray(byte[] bytes, ByteOrder order) {
	     return ByteBuffer.wrap(bytes).order(order).getDouble();
	}
	public static int u24IntFromByteArray(byte[] input, ByteOrder order) {
	    if (input.length == 3) {
	    	if(order == ByteOrder.LITTLE_ENDIAN) {
		        return (input[2] & 0xFF) << 16 | (input[1] & 0xFF) <<8 | (input[0] & 0xFF);
	    	} else {
		        return (input[0] & 0xFF) << 16 | (input[1] & 0xFF) <<8 | (input[2] & 0xFF);
	    	}
	    } else {
	    	throw new UnsupportedOperationException("input length is wrong: "+input.length);
	    }
	}
	public static byte[] u24IntToByteArray(int input, ByteOrder order) {
    	if(order == ByteOrder.LITTLE_ENDIAN) {
	        return new byte[] {(byte) (input & 0xff), (byte) ((input >> 8) & 0xff), (byte) ((input >> 16) & 0xff)};
    	} else {
	        return new byte[] {(byte) ((input >> 16) & 0xff), (byte) ((input >> 8) & 0xff), (byte) (input & 0xff)};
    	}
	}
	
	public static byte[] toPrimitiveByte(Byte[] bytes) {
		byte[] prim = new byte[bytes.length];
		for(int i=0;i<bytes.length;i++) {
			prim[i] = bytes[i];
		}
		return prim;
	}
	
	public static float[] toFloatArray(double[] arr) {
		float[] array = new float[arr.length];
		for(int i=0;i<arr.length;i++)
			array[i] = (float) arr[i];
		return array;
	}

	/*public static float[] eulerToQuaternion(float[] eulerRot) {
		double yaw = Math.toRadians(eulerRot[0]);
		double pitch = Math.toRadians(eulerRot[1]);
		double roll = Math.toRadians(eulerRot[2]);
		double q[] = new double[4];
        
        // Apply Euler angle transformations
        // Derivation from www.euclideanspace.com
		double c1 = Math.cos(yaw/2.0);
		double s1 = Math.sin(yaw/2.0);
		double c2 = Math.cos(pitch/2.0);
		double s2 = Math.sin(pitch/2.0);
		double c3 = Math.cos(roll/2.0);
		double s3 = Math.sin(roll/2.0);
		double c1c2 = c1*c2;
		double s1s2 = s1*s2;
        
        // Compute quaternion from components
        q[0] = c1c2*c3 - s1s2*s3;
        q[1] = c1c2*s3 + s1s2*c3;
        q[2] = s1*c2*c3 + c1*s2*s3;
        q[3] = c1*s2*c3 - s1*c2*s3;
        return toFloatArray(q);
	}*/
	
	public static float[] eulerToQuaternion(float[] eulerRot) {
		if(eulerRot == null) return null;
		
		//FIXME not sure if this is working properly now...
		/*double heading = Math.toRadians(eulerRot[1]);
		double attitude = Math.toRadians(eulerRot[0]);
		double bank = Math.toRadians(eulerRot[2]);*/
		double heading = Math.toRadians(eulerRot[1]); //Rotate around Z-Axis
		double attitude = Math.toRadians(0); //Rotate around X-Axis
		double bank = Math.toRadians(0); //Rotate around Y-Axis
		
	    // Assuming the angles are in radians.
	    double c1 = Math.cos(heading/2);
	    double s1 = Math.sin(heading/2);
	    double c2 = Math.cos(attitude/2);
	    double s2 = Math.sin(attitude/2);
	    double c3 = Math.cos(bank/2);
	    double s3 = Math.sin(bank/2);
	    double c1c2 = c1*c2;
	    double s1s2 = s1*s2;
	    
	    double w =c1c2*c3 - s1s2*s3;
	  	double x =c1c2*s3 + s1s2*c3;
		double y =s1*c2*c3 + c1*s2*s3;
		double z =c1*s2*c3 - s1*c2*s3;
		return toFloatArray(new double[] {x,y,z,w});
	  }

	public static IntStream intStream(byte[] bytes) {
		return IntStream.range(0, bytes.length).map(i -> bytes[i]&0xff);
	}

	public static short switchBytes(short value) {
		return Short.reverseBytes(value);
	}

	public static Float[][] floatToBoxedFloat(float[][] data) {
		Float[][] returns = new Float[data.length][];
		for(int i=0;i<data.length;i++) {
			returns[i] = floatToBoxedFloat(data[i]);
		}
		return returns;
	}

	public static Float[] floatToBoxedFloat(float[] data) {
		Float[] returns = new Float[data.length];
		for(int i=0;i<data.length;i++) {
			returns[i] = data[i];
		}
		return returns;
	}
	
	public static BufferedImage invertImage(BufferedImage image) {
		BufferedImage imageNew = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		for(int i=0;i<image.getHeight();i++)
			for(int j=0;j<image.getWidth();j++)
				imageNew.setRGB(j, i, image.getRGB(j, i)^0x00ffffff);
		return imageNew;
	}
	
}
