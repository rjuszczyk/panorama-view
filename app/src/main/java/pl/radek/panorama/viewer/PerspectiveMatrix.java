package pl.radek.panorama.viewer;

/**
 * Created by radoslaw.juszczyk on 2015-03-18.
 */
public class PerspectiveMatrix {
	/**
	 * Define a projection matrix in terms of a field of view angle, an
	 * aspect ratio, and z clip planes
	 *
	 * @param m      the float array that holds the perspective matrix
	 * @param offset the offset into float array m where the perspective
	 *               matrix data is written
	 * @param fovy   field of view in y direction, in degrees
	 * @param aspect width to height aspect ratio of the viewport
	 * @param zNear
	 * @param zFar
	 */
	public static void perspectiveM(float[] m, int offset,
									float fovy, float aspect, float zNear, float zFar) {
		float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
		float rangeReciprocal = 1.0f / (zNear - zFar);

		m[offset + 0] = f / aspect;
		m[offset + 1] = 0.0f;
		m[offset + 2] = 0.0f;
		m[offset + 3] = 0.0f;

		m[offset + 4] = 0.0f;
		m[offset + 5] = f;
		m[offset + 6] = 0.0f;
		m[offset + 7] = 0.0f;

		m[offset + 8] = 0.0f;
		m[offset + 9] = 0.0f;
		m[offset + 10] = (zFar + zNear) * rangeReciprocal;
		m[offset + 11] = -1.0f;

		m[offset + 12] = 0.0f;
		m[offset + 13] = 0.0f;
		m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
		m[offset + 15] = 0.0f;
	}
}
