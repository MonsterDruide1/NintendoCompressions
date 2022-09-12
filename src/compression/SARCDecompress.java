package compression;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import util.ByteDataStream;
import util.ByteDataStream.StringType;

public class SARCDecompress {
	
	public HashMap<String, byte[]> files;
	
	// compatibility
	public SARCDecompress(byte[] compressed) {
		this(new YAZ0Decompress(compressed));
	}
	public SARCDecompress(YAZ0Decompress yaz0) {
		this(yaz0.decompressed(), yaz0.alignment);
	}

	// most common alignment: 4096
	// pass -1 for auto-detecting alignment
	public SARCDecompress(byte[] dataArray, int alignment) {
		ByteDataStream data = new ByteDataStream(dataArray);
		
		data.assertMagic("SARC");
		data.expectShort(0x14, "SARC header size");
		data.expectByteOrder(ByteOrder.BIG_ENDIAN);
		data.expectInt(dataArray.length, "mismatch in file size");
		
		int dataOffset = data.getInt();
		
		data.expectShort(0x0100, "version number");
		data.expectShort(0, "data in reserved area");
		
		SFAT sfat = new SFAT(data);
		
		SFNT sfnt = new SFNT(data, sfat);

		if(alignment == -1) {
			// ignore checks and just take value stored in file
			data.seek(dataOffset+sfat.nodes[0].nodeFileDataStart);
		} else {
			data.align(alignment);
			data.assertPosition(dataOffset+sfat.nodes[0].nodeFileDataStart);
		}
		
		for(int i=0; i<sfat.nodes.length; i++) {
			sfat.nodes[i].setFileName(sfnt.filenames[i]);
			
			//data.align(128);
			//FIXME
			//data.assertPosition(dataOffset + sfat.nodes[i].nodeFileDataStart);
			data.seek(dataOffset + sfat.nodes[i].nodeFileDataStart);
			sfat.nodes[i].extractContent(data);
		}
		
		data.assertEOF();
		
		
		files = new HashMap<>();
		for(SFAT.Node node : sfat.nodes) {
			if(files.containsKey(node.fileName)) {
				System.err.println("Duplicate filename in SARC: "+node.fileName);
			}
			files.put(node.fileName, node.content);
		}
	}
	
	public class SFAT {
		
		public Header header;
		public Node[] nodes;
		
		public SFAT(ByteDataStream data) {
			header = new Header(data);
			nodes = new Node[header.nodeCount];
			
			for(int i=0; i<header.nodeCount; i++) {
				nodes[i] = new Node(data);
			}
		}
		
		public class Header {
			public short nodeCount;
			
			public Header(ByteDataStream data) {
				data.assertMagic("SFAT");
				data.expectShort(0xC, "SFAT header size");
				
				nodeCount = data.getShort();

				data.expectInt(0x0000_0065, "SFAT header hash key");
			}
		}
		
		public class Node {
			private int fileNameHash, nameTableOffset, nodeFileDataStart, nodeFileDataEnd;
			public String fileName;
			public byte[] content;
			
			public Node(ByteDataStream data){
				fileNameHash = data.getInt();
				int fileAttributes = data.getInt();
				if((fileAttributes & 0x0100_0000) == 0x0100_0000) {
					nameTableOffset = (fileAttributes & 0xFFFF)*4;
				} else {
					throw new UnsupportedOperationException("invalid fileAttributes: "+fileAttributes);
				}
				nodeFileDataStart = data.getInt();
				nodeFileDataEnd = data.getInt();
			}
			
			public void setFileName(String fileName) {
				if(fileNameHash != fileNameHashCode(fileName, 101)) {
					throw new UnsupportedOperationException("FileNameHash doesn't match: should be: "+fileNameHash+" ; is: "+fileNameHashCode(fileName, 101)+" for "+fileName);
				}
				this.fileName = fileName;
			}
			
			public int fileNameHashCode(String fileName, int key) {
				int result = 0;
				for(byte c : fileName.getBytes(StandardCharsets.UTF_8)) {
					result = c + result*key;
				}
				return result;
			}
			
			public void extractContent(ByteDataStream data) {
				content = data.getBytes(nodeFileDataEnd-nodeFileDataStart);
			}
			
		}
	}
	
	public class SFNT {
		
		public Header header;
		public String[] filenames;
		
		public SFNT(ByteDataStream data, SFAT sfat) {
			header = new Header(data);
			
			int startOffset = data.position();
			filenames = new String[sfat.nodes.length];
			for(int i=0; i<filenames.length; i++) {
				data.align(4);
				data.assertPosition(startOffset + sfat.nodes[i].nameTableOffset);
				
				filenames[i] = data.getString(StringType.NULL_TERMINATED);
			}
		}

		public class Header {
			public short nodeCount;
			
			public Header(ByteDataStream data) {
				data.assertMagic("SFNT");
				data.expectShort(0x8, "SFNT header size");
				data.expectShort(0, "SFNT reserved");
			}
		}
		
	}
	
	
}
