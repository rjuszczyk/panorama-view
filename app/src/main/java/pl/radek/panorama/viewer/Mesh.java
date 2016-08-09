package pl.radek.panorama.viewer;

import android.content.res.Resources;
import android.os.SystemClock;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import mesh.data.MeshData;
import pl.radek.panorama.R;

/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */

public class Mesh {
	/**
	 * Used for debug logs.
	 */
	private static final String TAG = "Mesh";
	public static HashMap<Integer, Mesh> meshes = new HashMap<Integer, Mesh>();
	private final int mBytesPerFloat = 4;
	/**
	 * Size of the position data in elements.
	 */
	private final int mPositionDataSize = 3;
	/**
	 * Size of the normal data in elements.
	 */
	private final int mNormalDataSize = 3;
	private int mPositionHandle;
	private int mNormalHandle;
	private int tex_resourceID;
	private FloatBuffer positions;
	private FloatBuffer normals;
	private FloatBuffer uvs;
	private float[] color;
	private int trianglesCount;
	private int mTextureDataHandle;

	public Mesh(int resourceID, Resources resources, float[] colorData, int tex_resourceID) {
		Log.i("debug", "openning mesh rid=" + resourceID);
		// Load the texture
		//mTextureDataHandle = TextureHelper.loadTexture(resources, R.raw.tex1);
		this.tex_resourceID = tex_resourceID;


		ArrayList<vec3f> temp_vertices = new ArrayList<vec3f>();
		ArrayList<vec2f> temp_uvs = new ArrayList<vec2f>();
		ArrayList<vec3f> temp_normals = new ArrayList<vec3f>();

		ArrayList<vec3f> out_vertices = new ArrayList<vec3f>();
		ArrayList<vec2f> out_uvs = new ArrayList<vec2f>();
		ArrayList<vec3f> out_normals = new ArrayList<vec3f>();

		ArrayList<Integer> vertexIndices = new ArrayList<Integer>();
		ArrayList<Integer> uvIndices = new ArrayList<Integer>();
		ArrayList<Integer> normalIndices = new ArrayList<Integer>();
		try {
			InputStream is = resources.openRawResource(resourceID);

			InputStreamReader converter = new InputStreamReader(is);
			BufferedReader in = new BufferedReader(converter);
			long time = SystemClock.uptimeMillis();
			String line;


			while ((line = in.readLine()) != null) {

				if (line.startsWith("v ")) {
					String[] elements = line.split(" +");

					float x = Float.parseFloat(elements[1]);
					float y = Float.parseFloat(elements[2]);
					float z = Float.parseFloat(elements[3]);
					temp_vertices.add(new vec3f(x, y, z));
				}

				if (line.startsWith("vt ")) {
					String[] elements = line.split(" +");

					float x = Float.parseFloat(elements[1]);
					float y = Float.parseFloat(elements[2]);
					temp_uvs.add(new vec2f(x, y));
				}
				if (line.startsWith("vn ")) {
					String[] elements = line.split(" +");

					float x = Float.parseFloat(elements[1]);
					float y = Float.parseFloat(elements[2]);
					float z = Float.parseFloat(elements[3]);

					temp_normals.add(new vec3f(x, y, z));
				}
				if (line.startsWith("f")) {

					int vertexIndex;
					int uvIndex;
					int normalIndex;

					String[] elements = line.split(" +");

					for (int i = 0; i < 3; i++) {
						String triple = elements[i + 1];
						String[] splited = triple.split("/");
						vertexIndex = Integer.parseInt(splited[0]);
						uvIndex = Integer.parseInt(splited[1]);
						normalIndex = Integer.parseInt(splited[2]);

						vertexIndices.add(vertexIndex);
						uvIndices.add(uvIndex);
						normalIndices.add(normalIndex);
					}
				}
			}

			in.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found exception: " + e.toString());
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.toString());
		} catch (Exception e) {
			Log.e(TAG, "nieznany wyjatek");
		}
		float[] positionsData = new float[vertexIndices.size() * 3];
		float[] normalsData = new float[vertexIndices.size() * 3];
		float[] uvsData = new float[uvIndices.size() * 2];


		for (int i = 0; i < vertexIndices.size(); i++) {
			int vertexIndex = vertexIndices.get(i);
			int uvIndex = uvIndices.get(i);
			int normalIndex = normalIndices.get(i);

			vec3f vertex = temp_vertices.get(vertexIndex - 1);
			vec2f uv = temp_uvs.get(uvIndex - 1);
			vec3f normal = temp_normals.get(normalIndex - 1);


			positionsData[i * 3] = vertex.x;
			positionsData[i * 3 + 1] = vertex.y;
			positionsData[i * 3 + 2] = vertex.z;

			normalsData[i * 3] = normal.x;
			normalsData[i * 3 + 1] = normal.y;
			normalsData[i * 3 + 2] = normal.z;

			uvsData[i * 2] = uv.u;
			uvsData[i * 2 + 1] = 1.0f - uv.v;

			//out_vertices.add(vertex);
			//out_uvs.add(uv);
			//out_normals.add(normal);
		}

		for(int i =0; i < positionsData.length; i+=3) {
			Log.d("posx", ""+positionsData[i]);
		}

		Log.i("debug", "loaded:\nnormals=" + normalsData.length + "\nvertex=" + positionsData.length);

		this.positions = ByteBuffer.allocateDirect(positionsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.positions.put(positionsData).position(0);

		this.normals = ByteBuffer.allocateDirect(normalsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.normals.put(normalsData).position(0);

		this.uvs = ByteBuffer.allocateDirect(uvsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.uvs.put(uvsData).position(0);

		StringBuilder builder = new StringBuilder();

		builder.append("positionsData = new float[]{");
		for(int i = 0; i < positionsData.length; i++) {
			if(i!=positionsData.length-1) {
				builder.append(String.valueOf(positionsData[i])+"f,");
			} else {
				builder.append(String.valueOf(positionsData[i])+"f};\n");
			}
		}

		builder.append("normalsData = new float[]{");
		for(int i = 0; i < normalsData.length; i++) {
			if(i!=normalsData.length-1) {
				builder.append(String.valueOf(normalsData[i])+"f,");
			} else {
				builder.append(String.valueOf(normalsData[i])+"f};\n");
			}
		}

		builder.append("uvsData = new float[]{");
		for(int i = 0; i < uvsData.length; i++) {
			if(i!=uvsData.length-1) {
				builder.append(String.valueOf(uvsData[i])+"f,");
			} else {
				builder.append(String.valueOf(uvsData[i])+"f};\n");
			}
		}
		String t =  builder.toString();
		Log.d("data",t);



		trianglesCount = (int) positionsData.length / mPositionDataSize;
		color = colorData;
	}

	public Mesh(float[] positionsData, float[] normalsData, float[] uvsData, float[] colorData, int tex_resourceID) {

		this.positions = ByteBuffer.allocateDirect(positionsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.positions.put(positionsData).position(0);

		this.normals = ByteBuffer.allocateDirect(normalsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.normals.put(normalsData).position(0);

		this.uvs = ByteBuffer.allocateDirect(uvsData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.uvs.put(uvsData).position(0);
		trianglesCount = (int) positionsData.length / mPositionDataSize;
		color = colorData;
		this.tex_resourceID = tex_resourceID;
	}

	public static Mesh getMesh(int resourceID, Resources resources, int tex_resourceID) {
		if (meshes.containsKey(resourceID))
			return meshes.get(resourceID);

		Mesh mesh = new Mesh(resourceID, resources, new float[]{1.0f, 0.0f, 0.0f, 1.0f}, tex_resourceID);
		meshes.put(resourceID, mesh);
		return mesh;
	}

	public static Mesh getMeshSerialized(int serializedResourceID, Resources resources, int tex_resourceID) {
		if (meshes.containsKey(serializedResourceID) && meshes.get(serializedResourceID).tex_resourceID == tex_resourceID) {
			meshes.get(serializedResourceID);
		}

		float[] positionsData = null;
		float[] normalsData = null;
		float[] uvsData = null;
		try {
			InputStream is = resources.openRawResource(R.raw.sphere);

			ObjectInputStream ois = new ObjectInputStream(is);

			MeshData meshData = (MeshData) ois.readObject();

			ois.close();

			positionsData = meshData.positions;
			normalsData = meshData.normals;
			uvsData = meshData.uvs;
		} catch (Exception e) {
			Log.e("serialized", "serialization gone wrong");
			e.printStackTrace();
		}
		Mesh mesh = new Mesh(positionsData, normalsData, uvsData, new float[]{1.0f, 0.0f, 0.0f, 1.0f}, tex_resourceID);
		meshes.put(serializedResourceID, mesh);
		return mesh;
	}

	public FloatBuffer getPositionsBuffer() {
		return positions;
	}

	public FloatBuffer getNormalsBuffer() {
		return normals;
	}

	public FloatBuffer getUVsBuffer() {
		return uvs;
	}

	public int getSize() {
		return trianglesCount;
	}

	public float[] getColor() {
		return color;
	}

	public int getTextureDataHandle(ImageDrawer imageDrawer) {
		return imageDrawer.getTextureHandler(this.tex_resourceID);
	}

	private class vec3f {
		public float x;
		public float y;
		public float z;

		public vec3f() {
		}

		public vec3f(float _x, float _y, float _z) {
			x = _x;
			y = _y;
			z = _z;
		}
	}

	private class vec2f {
		public float u;
		public float v;

		public vec2f() {
		}

		public vec2f(float _u, float _v) {
			u = _u;
			v = _v;
		}
	}
}