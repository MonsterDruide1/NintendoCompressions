package compression;

import java.util.Arrays;
import java.util.HashMap;

import util.ByteDataStream;
import util.ByteDataStream.StringType;

public class BYMLDecompress {
	
	public String[] hashKeyTable, stringTable;
	public Node rootNode;
	
	private HashMap<Integer, Object> cache; // maps offset => object

	public BYMLDecompress(byte[] dataArray) {
		ByteDataStream data = new ByteDataStream(dataArray);
		cache = new HashMap<>();
		
		data.assertMagic("YB"); //BY in little endian
		data.expectShort(3, "version");
		
		int hashKeyTableOffset = data.getInt();
		int stringTableOffset = data.getInt();
		int rootNodeOffset = data.getInt();

		data.align(4);
		if(hashKeyTableOffset != 0) {
			data.assertPosition(hashKeyTableOffset);
			hashKeyTable = (String[]) new Node(data, null, null).content;
		}
		data.align(4);
		if(stringTableOffset != 0) {
			data.assertPosition(stringTableOffset);
			stringTable = (String[]) new Node(data, hashKeyTable, null).content;
		}
		data.align(4);
		if(rootNodeOffset != 0) {
			data.assertPosition(rootNodeOffset);
			rootNode = new Node(data, hashKeyTable, stringTable);
		}
		
		data.assertEOF();
	}
	
	public class Node {
		
		public Type type;
		public Object content;
		
		public Node(ByteDataStream data, String[] hashKeyTable, String[] stringTable) {
			// main nodes only support StringTable (hash key/string table) and Array/Hash (root node)
			type = Type.getType(data.peekByte());
			content = switch(type) {
			case StringTable -> getStringTable(data);
			case Array -> getArray(data, hashKeyTable, stringTable);
			case Hash -> getHash(data, hashKeyTable, stringTable);
			default -> throw new UnsupportedOperationException("Unimplemented case: " + type);
			};
		}
		
		private String getString(int index, String[] stringTable) {
			return stringTable[index];
		}
		
		private String[] getStringTable(ByteDataStream data) {
			int startOffset = data.position();
			data.expectByte(Type.StringTable.id, "ID");
			
			int entries = data.getU24();
			int[] offsets = data.getInts(entries+1);
			
			data.assertPosition(startOffset+offsets[0]);
			
			String[] strings = new String[entries];
			for(int i=0;i<strings.length;i++) {
				strings[i] = data.getString(StringType.NULL_TERMINATED);
				data.assertPosition(startOffset+offsets[i+1]);
			}
			
			return strings;
		}
		
		private Object[] getArray(ByteDataStream data, String[] hashKeyTable, String[] stringTable) {
			data.expectByte(Type.Array.id, "ID");
			
			int entries = data.getU24();
			byte[] types = data.getBytes(entries);
			data.align(4);
			int[] values = data.getInts(entries);
			
			Object[] objects = new Object[entries];
			for(int i=0;i<entries;i++) {
				objects[i] = getData(Type.getType(types[i]), values[i], data, hashKeyTable, stringTable);
			}
			return objects;
		}
		
		private record HashEntry(int nameIndex, Type type, int value) {
			public HashEntry(ByteDataStream data) {
				this(data.getU24(), Type.getType(data.getByte()), data.getInt());
			}
		}
		
		private HashMap<String, Object> getHash(ByteDataStream data, String[] hashKeyTable, String[] stringTable) {
			data.expectByte(Type.Hash.id, "ID");
			int entriesNum = data.getU24();
			
			HashEntry[] entries = new HashEntry[entriesNum];
			for(int i=0; i<entriesNum; i++) {
				entries[i] = new HashEntry(data);
			}
			// as entries are sorted alphabetically in file, re-sort them to match file layout (pointer values)
			Arrays.sort(entries, (a, b) -> Integer.compare(a.value, b.value));
			
			HashMap<String, Object> map = new HashMap<>(entriesNum);
			for(int i=0;i<entriesNum;i++) {
				Object content = getData(entries[i].type, entries[i].value, data, hashKeyTable, stringTable);
				
				String hashKey = hashKeyTable[entries[i].nameIndex];
				if(map.containsKey(hashKey))
					throw new UnsupportedOperationException("Duplicate key in Hash: "+hashKey);
				map.put(hashKey, content);
			}
			return map;
		}
		
		private Object getData(Type type, int data, ByteDataStream file, String[] hashKeyTable, String[] stringTable) {
			Object o = switch(type) {
			case Array -> {
				if(cache.containsKey(data)) {
					yield cache.get(data);
				}
				file.assertPosition(data);
				Object hash = getArray(file, hashKeyTable, stringTable);
				cache.put(data, hash);
				yield hash;
			}
			case Binary -> throw new UnsupportedOperationException("Unimplemented case: " + type);
			case Bool -> {
				yield switch(data) {
					case 1 -> true;
					case 0 -> false;
					default -> throw new UnsupportedOperationException("Unknown value for Bool: "+data);
				};
			}
			case Double -> throw new UnsupportedOperationException("Unimplemented case: " + type);
			case Float -> Float.intBitsToFloat(data);
			case Hash -> {
				if(cache.containsKey(data)) {
					yield cache.get(data);
				}
				file.assertPosition(data);
				Object hash = getHash(file, hashKeyTable, stringTable);
				cache.put(data, hash);
				yield hash;
			}
			case Int -> data;
			case Int64 -> {
				file.assertPosition(data);
				yield file.getLong();
			}
			case NULL -> null;
			case String -> getString(data, stringTable);
			case StringTable -> throw new UnsupportedOperationException("Unimplemented case: " + type);
			case UInt -> data;
			case UInt64 -> {
				file.assertPosition(data);
				yield file.getLong();
			}
			};
			return o;
		}
		
		public enum Type {
			String(0xA0),
			Binary(0xA1),
			Array(0xC0),
			Hash(0xC1),
			StringTable(0xC2),
			Bool(0xD0),
			Int(0xD1),
			Float(0xD2),
			UInt(0xD3),
			Int64(0xD4),
			UInt64(0xD5),
			Double(0xD6),
			NULL(0xFF);
			
			public byte id;
			private Type(byte id) {
				this.id=id;
			}
			private Type(int id) {
				this((byte)id);
			}
			public static Type getType(byte id) {
		    	for(Type type : Type.values()) {
		    		if(type.id == id) {
		    			return type;
		    		}
		    	}
				throw new UnsupportedOperationException("BYML-Node-Type of id "+id+" not found");
			}
		}
		
	}
	
}
