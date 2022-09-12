package compression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import types.Vector3f;
import util.ByteDataStream;
import util.NintendoUtil;

public class KCLDecompress {
	
	public static int COMPRESSION_LEVEL = 0; //0 = none, 1 = search SAME (base) vertex, 2 = search all equal vertices
	public static boolean PARSE_POLYGON_OCTREE = false;

	public Model[] models;
	public ModelOctree modelOctree;

	public KCLDecompress(byte[] dataArray) {
		ByteDataStream data = new ByteDataStream(dataArray);
		
		data.expectBytes(new byte[] { 2, 2, 0, 0 }, "KCL version magic");
		int offsetToOctree = data.getInt();
		int offsetToModelList = data.getInt();
		int modelCount = data.getInt();
		Vector3f minValues = data.readVector3F();
		Vector3f maxValues = data.readVector3F();
		int[] coordShift = data.readVector3U();
		int prismCount = data.getInt(); // not quite, but really close to it...?
		
		data.assertPosition(offsetToOctree);
		modelOctree = new ModelOctree(data);

		data.assertPosition(offsetToModelList);
		
		int[] offsets = data.getInts(modelCount);
		models = new Model[modelCount];
		for (int i = 0; i < modelCount; i++) {
			int offset = offsets[i];
			data.seek(offset); // TODO fix PolygonOctree and add assertEOF
			models[i] = new Model(data);
			if(COMPRESSION_LEVEL > 0) {
				models[i].mergeDuplicateVerticies();
				models[i].removeUnusedVerticies();
			}
		}
	}
	
	public class ModelOctree {
		
		public class ModelOctreeNode {
			
			public Flags flags;
			
			// max one of the following is set, depending on the flag
			public ModelOctree subtree;
			public int modelIndex;
			
			public ModelOctreeNode(int key, ByteDataStream data, int startOfNode) {
				flags = Flags.get(key & 0b11000000_00000000_00000000_00000000);
				int value = key & 0b00111111_11111111_11111111_11111111;
				
				switch (flags) {
				case Divide: {
					data.assertPosition(startOfNode+value*4);
					subtree = new ModelOctree(data);
					break;
				}
				case Values: {
					modelIndex = value;
					break;
				}
				case NoData: {
					break;
				}
				default:
					throw new UnsupportedOperationException("Unexpected value: " + flags);
				}
			}
			
			public enum Flags {
				Divide(0b00000000_00000000_00000000_00000000),
				Values(0b10000000_00000000_00000000_00000000),
				NoData(0b11000000_00000000_00000000_00000000);

				private int key;

				private Flags(int key) {
					this.key = key;
				}

				public static Flags get(int key) {
					for (Flags flag : values()) {
						if (key == flag.key) {
							return flag;
						}
					}
					throw new UnsupportedOperationException("Unexpected value: " + key);
				}
			}
		}
		
		public ModelOctreeNode[] nodes;
		
		public ModelOctree(ByteDataStream data) {
			int startOfNode = data.position();
			int[] keys = data.getInts(8);
			
			nodes = new ModelOctreeNode[8];
			for(int i=0; i<8; i++) {
				nodes[i] = new ModelOctreeNode(keys[i], data, startOfNode);
			}
		}
	}

	public static class Model {

		public Triangle[] triangles;
		public Vector3f[] vertexs;

		public Model(ByteDataStream data) {
			int startPos = data.position();
			
			int offsetSection1 = data.getInt();
			int offsetSection2 = data.getInt();
			int offsetSection3 = data.getInt();
			int offsetSection4 = data.getInt();
			data.expectFloat(40, "thickness");
			Vector3f minCoordinate = data.readVector3F();
			int[] mask = data.readVector3U();
			int[] shift = data.readVector3U();
			data.expectFloat(0, "sphere radius");
			
			data.assertPosition(startPos+offsetSection1);

			int verticesCount = (offsetSection2 - offsetSection1) / 0x0C;
			Vector3f[] vertices = new Vector3f[verticesCount];
			for (int i = 0; i < verticesCount; i++) {
				vertices[i] = data.readVector3F();
			}
			
			data.assertPosition(startPos+offsetSection2);

			int normalCount = (offsetSection3 - offsetSection2) / 0x0C;
			Vector3f[] normals = new Vector3f[normalCount];
			for (int i = 0; i < normalCount; i++) {
				normals[i] = data.readVector3F();
			}
			
			data.assertPosition(startPos+offsetSection3);

			int triCount = (offsetSection4 - offsetSection3) / 0x14;

			triangles = new Triangle[triCount];
			vertexs = new Vector3f[triCount*3];
			for (int i = 0; i < triCount; i++) {
				NinTriangle tri = new NinTriangle(data);
				triangles[i] = new Triangle(tri, vertices, normals);
				vertexs[i*3] = triangles[i].vertex1;
				vertexs[i*3+1] = triangles[i].vertex2;
				vertexs[i*3+2] = triangles[i].vertex3;
				triangles[i].vIndex1 = i*3;
				triangles[i].vIndex2 = i*3+1;
				triangles[i].vIndex3 = i*3+2;
			}
			
			// TODO finish and enable this
			if(PARSE_POLYGON_OCTREE) {
				int spatialNodeCount = ((~(int)mask[0] >> (int)shift[0]) + 1)
		                 * ((~(int)mask[1] >> (int)shift[0]) + 1)
		                 * ((~(int)mask[2] >> (int)shift[0]) + 1);
				
				PolygonOctree[] octrees = new PolygonOctree[spatialNodeCount];
				int[] octreeOffsets = data.getInts(spatialNodeCount);
				for(int i=0; i<spatialNodeCount; i++) {
					octrees[i] = new PolygonOctree(octreeOffsets[i], data, startPos+offsetSection4);
				}
			}
		}

		public Vector3f[] getVerticies() {
			return vertexs;
		}
		
		private void removeUnusedVerticies() {
			if(vertexs == null) {
				return;
			}
			ArrayList<Vector3f> verticies = new ArrayList<>(Arrays.asList(vertexs));
			for(int i=0;i<verticies.size();i++) {
				boolean used = false;
				for(int j=0;j<triangles.length;j++) {
					if(NintendoUtil.contains(triangles[j].getVerticies(),i)) {
						used = true;
						break;
					}
				}
				
				if(!used) {
					prepareRemoveVertex(i);
					verticies.remove(i);
					i--;
				}
			}
			vertexs = verticies.toArray(Vector3f[]::new);
		}
		
		private void mergeDuplicateVerticies() {
			if(vertexs == null) {
				return;
			}
			ArrayList<Vector3f> verticies = new ArrayList<>(Arrays.asList(vertexs));
			for(int i=0;i<verticies.size();i++) {
				Vector3f v = verticies.get(i);
				int firstIndex;
				if(COMPRESSION_LEVEL == 1)
					firstIndex = verticies.indexOf(verticies.get(i));
				else if(COMPRESSION_LEVEL == 2)
					firstIndex = verticies.stream().filter(v1 -> v.equals(v1)).mapToInt(v1 -> verticies.indexOf(v1)).findFirst().orElse(i);
				else //unknown compression - assert none
					firstIndex = i;
				
				if(firstIndex != i) {
					prepareReplaceVertex(i, firstIndex);
					prepareRemoveVertex(i);
					verticies.remove(i);
					i--;
				}
			}
			vertexs = verticies.toArray(Vector3f[]::new);
		}
		
		private void prepareRemoveVertex(int index) {
			Arrays.stream(triangles).forEach(face -> face.prepareRemoveVertex(index));
		}
		
		private void prepareReplaceVertex(int index, int newIndex) {
			Arrays.stream(triangles).forEach(face -> face.prepareReplaceVertex(index, newIndex));
		}

		public HashMap<Short, ArrayList<Triangle>> getFacesByCollisionFlag() {
			HashMap<Short, ArrayList<Triangle>> map = new HashMap<Short, ArrayList<Triangle>>();
			Arrays.stream(triangles).forEach((face) -> {
				ArrayList<Triangle> matchingColor = map.get(face.collisionFlags);
				if (matchingColor == null) {
					matchingColor = new ArrayList<Triangle>();
				}
				matchingColor.add(face);
				map.put(face.collisionFlags, matchingColor);
			});

			return map;
		}

		public static class NinTriangle {

			public float length;
			public int positionIndex;
			public int directionIndex;
			public int normalAIndex;
			public int normalBIndex;
			public int normalCIndex;
			public short collisionFlags;
			public int globalTriIndex;

			public NinTriangle(ByteDataStream data) {
				length = data.getFloat();
				positionIndex = data.getShort() & 0xffff;
				directionIndex = data.getShort() & 0xffff;
				normalAIndex = data.getShort() & 0xffff;
				normalBIndex = data.getShort() & 0xffff;
				normalCIndex = data.getShort() & 0xffff;
				collisionFlags = data.getShort();
				globalTriIndex = data.getInt();
			}

		}

		public static class Triangle {
			
			private final float EPSILON = 0.00000011921f;

			public NinTriangle ninTriangle;
			public Vector3f vertex1, vertex2, vertex3;
			public int vIndex1, vIndex2, vIndex3;
			public short collisionFlags;
			public int globalTriIndex;

			public Triangle(NinTriangle tri, Vector3f[] vertices, Vector3f[] normals) {
				ninTriangle = tri;
				collisionFlags = tri.collisionFlags;
				globalTriIndex = tri.globalTriIndex;
				
				Vector3f crossA = normals[tri.normalAIndex].crossProduct(normals[tri.directionIndex]);
				Vector3f crossB = normals[tri.normalBIndex].crossProduct(normals[tri.directionIndex]);
				float dotA = crossA.dotProduct(normals[tri.normalCIndex]);
				float dotB = crossB.dotProduct(normals[tri.normalCIndex]);
				float factorA = tri.length / (dotA >= 0 ? Math.max(dotA, EPSILON) : Math.min(dotA, EPSILON));
				float factorB = tri.length / (dotB >= 0 ? Math.max(dotB, EPSILON) : Math.min(dotB, EPSILON));
				vertex1 = vertices[tri.positionIndex];
				vertex2 = vertices[tri.positionIndex].add(crossB.multiply(factorB));
				vertex3 = vertices[tri.positionIndex].add(crossA.multiply(factorA));
			}
			
			public Integer[] getVerticies() {
				return new Integer[] {vIndex1, vIndex2, vIndex3};
			}

			public void prepareRemoveVertex(int index) {
				if(vIndex1 > index) vIndex1--;
				if(vIndex2 > index) vIndex2--;
				if(vIndex3 > index) vIndex3--;
			}

			public void prepareReplaceVertex(int index, int newIndex) {
				if(vIndex1 == index) vIndex1 = newIndex;
				if(vIndex2 == index) vIndex2 = newIndex;
				if(vIndex3 == index) vIndex3 = newIndex;
			}


		}
		
		public class PolygonOctree {
			
			// one of the two contains values, other one is null
			public short[] triangleIndices;
			public PolygonOctree[] children;
			
			public PolygonOctree(int key, ByteDataStream data, int baseOffset) {
				int offset = baseOffset + (key & 0b00111111_11111111_11111111_11111111);
				if((key >>> 31) == 1) {
					// TODO find right layout and assertPosition instead
					data.seek(offset+2);
					
					List<Short> indices = new ArrayList<Short>();
					short index;
					while((index = data.getShort()) != ((short)0xffff)) {
						indices.add(index);
					}
					
					triangleIndices = new short[indices.size()];
					for(int i=0; i<triangleIndices.length; i++) {
						triangleIndices[i] = indices.get(i);
					}
				}
				else {
					// TODO find right layout and assertPosition instead
					data.seek(offset);
					
					int[] keys = data.getInts(8);

					children = new PolygonOctree[8];
					for(int i=0; i<8; i++) {
						children[i] = new PolygonOctree(keys[i], data, offset);
					}
				}
			}
		}

	}

}
