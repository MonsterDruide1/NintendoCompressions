package compression;

import java.nio.ByteOrder;

import util.ByteDataStream;

public class YAZ0Decompress {
	//http://wiki.tockdom.com/wiki/YAZ0_(File_Format)
	
	private byte[] compressedData;
	private int size;
	
	public int alignment; // used in SARCDecompress for alignment of main segment
	
	public YAZ0Decompress(byte[] dataArray) {
		ByteDataStream data = new ByteDataStream(dataArray, ByteOrder.BIG_ENDIAN);
		data.assertMagic("Yaz0");
		size = data.getInt();
		alignment = data.getInt();
		if(alignment != 0x80 && alignment != 0x1000 && alignment != 0x2000 && alignment != 0x4000)
			throw new UnsupportedOperationException("Unknown data alignment value: "+alignment);
		data.expectInt(0, "unknown");
		compressedData = data.remainingOriginal();
	}
	 
	
	public byte[] decompressed() {
		byte[] dest = new byte[size];
		byte[] src = compressedData;
		int srcIndex = 0;
		int destIndex = 0;
		int groupHead = 0;
		int groupHeadLength = 0;
		
		while(srcIndex < src.length && destIndex < dest.length) {
			if(groupHeadLength == 0) { //start new data group
				groupHead = src[srcIndex++] & 0xFF;
				groupHeadLength = 8;
			}
			groupHeadLength--;
			
			if((groupHead & 0x80) != 0) { //copy byte directly
				dest[destIndex++] = src[srcIndex++];
			}
			else { //run length encoding
				int b1 = src[srcIndex++] & 0xFF;
				int b2 = src[srcIndex++] & 0xFF;
				
				int copySrc = destIndex - ((b1 & 0x0f) << 8 | b2) - 1; //source position
				
				int n = b1 >> 4; //number of bytes to copy
				
				if(n == 0) {
					n = (src[srcIndex++] & 0xFF) + 0x12; //read third byte
				} else {
					n += 2; //just add 2
				}
				
				while(n-- > 0) {
					dest[destIndex++] = dest[copySrc++];
				}
			}
			
			groupHead <<= 1;
		}
		
		return dest;
	}
	
}
