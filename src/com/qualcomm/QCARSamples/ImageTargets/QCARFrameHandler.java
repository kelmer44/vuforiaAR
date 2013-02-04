package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.qualcomm.QCAR.QCAR;
import com.threed.jpct.SimpleVector;

public class QCARFrameHandler {

	private SimpleVector	mCameraPosition			= new SimpleVector();
	private SimpleVector	mCameraRightVector		= new SimpleVector();
	private SimpleVector	mCameraUpVector			= new SimpleVector();
	private SimpleVector	mCameraDirectionVector	= new SimpleVector();

	private float			modelViewMat[];
	private float			projectionMatrix[];

	private boolean			isTracking;

	private float			fov;
	private float			fovy;

	public QCARFrameHandler() {
		super();
	}

	/** Native function for initializing the renderer. */
	public native void initRendering();

	/** Native function to update the renderer. */
	public native void updateRendering(int width, int height);

	/** The native render function. */
	public native void renderFrame();

	public void setCameraPos(float x, float y, float z) {

		// DebugLog.LOGD("CAM POS: " + x + ", " + y + ", " + z);
		this.mCameraPosition = new SimpleVector(x, y, z);
	}

	public void setCameraRightVector(float x, float y, float z) {
		// DebugLog.LOGD("RIGHT VEC: " + x + ", " + y + ", " + z);
		this.mCameraRightVector = new SimpleVector(x, y, z);
	}

	public void setCameraUpVector(float x, float y, float z) {
		// DebugLog.LOGD("UP VEC: " + x + ", " + y + ", " + z);
		this.mCameraUpVector = new SimpleVector(x, y, z);
	}

	public void setCameraDirectionVector(float x, float y, float z) {
		// DebugLog.LOGD("DIR VEC: " + x + ", " + y + ", " + z);
		this.mCameraDirectionVector = new SimpleVector(x, y, z);
	}

	public void updateModelviewMatrix(float mat[]) {

		modelViewMat = mat;
	}

	public void updateProjMatrix(float mat[]) {

		projectionMatrix = mat;
	}

	public void isTracking(boolean is) {
		isTracking = is;
	}

	public void setFov(float fov) {
		// DebugLog.LOGD("FOV: " + fov);
		this.fov = fov;
	}

	public void setFovy(float fov) {
		// DebugLog.LOGD("FOVY: " + fovy);
		this.fovy = fov;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		DebugLog.LOGD("GLRenderer::onSurfaceCreated");

		// Call native function to initialize rendering:
		initRendering();

		// Call QCAR function to (re)initialize rendering after first use
		// or after OpenGL ES context was lost (e.g. after onPause/onResume):
		QCAR.onSurfaceCreated();
	}

	/** Called when the surface changed size. */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		DebugLog.LOGD("GLRenderer::onSurfaceChanged");

		// Call native function to update rendering when render surface
		// parameters have changed:
		updateRendering(width, height);

		// Call QCAR function to handle render surface size changes:
		QCAR.onSurfaceChanged(width, height);
	}

	public void update() {
		// Call our native function to render content
		renderFrame();
	}

	public SimpleVector getCameraPosition() {
		return mCameraPosition;
	}

	public SimpleVector getCameraRightVector() {
		return mCameraRightVector;
	}

	public SimpleVector getCameraUpVector() {
		return mCameraUpVector;
	}

	public SimpleVector getCameraDirectionVector() {
		return mCameraDirectionVector;
	}

	public boolean isTracking() {
		return isTracking;
	}

	public float getFov() {
		return fov;
	}

	public float getFovy() {
		return fovy;
	}

	public float[] getModelViewMat() {
		return modelViewMat;
	}

	public float[] getProjectionMatrix() {
		return projectionMatrix;
	}
	
}
