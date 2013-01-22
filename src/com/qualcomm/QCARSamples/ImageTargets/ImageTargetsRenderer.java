/*==============================================================================
            Copyright (c) 2010-2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary

@file
    ImageTargetsRenderer.java

@brief
    Sample for ImageTargets

==============================================================================*/

package com.qualcomm.QCARSamples.ImageTargets;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.qualcomm.QCAR.QCAR;
import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

/** The renderer class for the ImageTargets sample. */
public class ImageTargetsRenderer implements GLSurfaceView.Renderer {
	private FrameBuffer	fb			= null;
	private World		world		= null;

	private RGBColor	back		= new RGBColor(0, 0, 0, 2);

	private Object3D	cube		= null;
	private Object3D[]	barco		= null;
	private Object3D[]  torre 		= null;
	private int			fps			= 0;

	private Light		sun			= null;

	private float		touchTurn	= 0;
	private float		touchTurnUp	= 0;

	private float		xpos		= -1;
	private float		ypos		= -1;

	public boolean		mIsActive	= false;

	private boolean		init		= false;

	private float		modelViewMat[];

	/** Reference to main activity **/
	public ImageTargets	mActivity;
	private Camera		cam;
	private float		rotate;
	private boolean		invert;
	private boolean		showScene	= false;

	/** Native function for initializing the renderer. */
	public native void initRendering();

	/** Native function to update the renderer. */
	public native void updateRendering(int width, int height);

	public void updateModelviewMatrix(float mat[]) {

		modelViewMat = mat;
	}

	public void isTracking(boolean is) {
		showScene = is;
	}

	public ImageTargetsRenderer(ImageTargets activity) {

		this.mActivity = activity;
		modelViewMat = new float[16];
	}

	/** Called when the surface is created or recreated. */
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
		if (fb != null) {
			fb.dispose();
		}
		fb = new FrameBuffer(width, height);

		if (!init) {
			// Create a texture out of the icon...:-)
			Texture textureBarco = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.barcot)), 512, 512));
			TextureManager.getInstance().addTexture("barcot", textureBarco);

			Texture textureVelas = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.velas)), 1024, 1024));
			TextureManager.getInstance().addTexture("velas", textureVelas);

			world = new World();
			world.setAmbientLight(20, 20, 20);
			sun = new Light(world);
			sun.setIntensity(250, 250, 250);
			cube = Primitives.getCube(10);
			cube.calcTextureWrapSpherical();
			cube.strip();
			cube.build();
			

			
			barco = Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.barco), mActivity.getResources().openRawResource(R.raw.barcomat), 1.0f);
			torre = Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.torresola), mActivity.getResources().openRawResource(R.raw.torremat), 10.0f);
			
			
			for(int i=0;i<barco.length;i++){
				barco[i].setCulling(Object3D.CULLING_DISABLED);
				barco[i].build();
			}	
			
			for(int i=0;i<torre.length;i++){
				torre[i].setCulling(Object3D.CULLING_DISABLED);
				torre[i].build();
				
			}
			
			
			barco[3].setTexture("barcot");
			barco[4].setTexture("barcot");
			barco[5].setTexture("barcot");
			barco[6].setTexture("barcot");

			for (int i = 7; i < 19; i++) {
				barco[i].setTexture("velas");
			}
			// world.addObject(cube);
			//world.addObjects(barco);
			world.addObjects(torre);
			cam = world.getCamera();
			cam.moveCamera(Camera.CAMERA_MOVEOUT, 10);
			cam.lookAt(cube.getTransformedCenter());

			SimpleVector sv = new SimpleVector();
			sv.set(barco[0].getTransformedCenter());
			sv.y -= 10;
			sv.z -= 10;
			sun.setPosition(sv);
			MemoryHelper.compact();

			init = true;
		}
		// Call native function to update rendering when render surface
		// parameters have changed:
		updateRendering(width, height);

		// Call QCAR function to handle render surface size changes:
		QCAR.onSurfaceChanged(width, height);

	}

	/** The native render function. */
	public native void renderFrame();

	public boolean setCameraMatrix(float[] matrix) {

		if (cam != null) {

			com.threed.jpct.Matrix _cameraMatrix = new com.threed.jpct.Matrix();
			;
			SimpleVector _cameraPosition = new SimpleVector();

			_cameraPosition.set(matrix[12], matrix[13], matrix[14]);
			matrix[12] = matrix[13] = matrix[14] = 0;

			_cameraMatrix.setDump(matrix);
			// _cameraMatrix = _cameraMatrix.invert();

			cam.setBack(_cameraMatrix);
			cam.setPosition(_cameraPosition);

			return true;

		} else {
			return false;
		}

	}

	/** Called to draw the current frame. */
	public void onDrawFrame(GL10 gl) {

		if (!mIsActive)
			return;

		// Update render view (projection matrix and viewport) if needed:
		mActivity.updateRenderView();
		// Call our native function to render content
		renderFrame();

		// StringBuffer str = new StringBuffer("MATRIX:\n");
		// for (int i = 0; i < 4; i++) {
		// for (int j = 0; j < 4; j++) {
		// str.append("" + modelViewMat[4 * i + j] + " ");
		// }
		// str.append("\n");
		// }
		// DebugLog.LOGD(str.toString());

		// com.threed.jpct.Matrix mResult = new com.threed.jpct.Matrix();
		// mResult.setDump(modelViewMat); // modelviewMatrix i get from Qcar

		// Esto foi o que engadiu Roi
		com.threed.jpct.Matrix _cameraMatrix = new com.threed.jpct.Matrix();
		SimpleVector _cameraPosition = new SimpleVector();

		_cameraPosition.set(modelViewMat[3], modelViewMat[7], modelViewMat[11]); // Collo a translación
		modelViewMat[3] = modelViewMat[7] = modelViewMat[11] = 0; // Borro a translación da matriz

		_cameraMatrix.setDump(modelViewMat);
		if (invert) {
			_cameraMatrix = _cameraMatrix.invert();
		}

		cam.setBack(_cameraMatrix); // Aplico a matriz de rotacións
		cam.setPosition(_cameraPosition); // Aplico o vector de posición

				
		
		// cam.setBack(mResult);
		// setCameraMatrix(modelViewMat);
		// cube.setRotationMatrix(mResult);
		for (int i = 0; i < barco.length; i++) {
			barco[i].setRotationPivot(barco[3].getCenter());
			barco[i].rotateAxis(new SimpleVector(1.0, 0.0, 0), (float) 1.570);
		}

		// fb.clear(back);
		if(showScene){
			world.renderScene(fb);
			world.draw(fb);
			fb.display();
		}

		for (int i = 0; i < barco.length; i++) {
			barco[i].setRotationPivot(barco[3].getCenter());
			barco[i].rotateAxis(new SimpleVector(1.0, 0.0, 0), (float) -1.570);
		}

	}

	public float getXpos() {
		return xpos;
	}

	public void setXpos(float xpos) {
		this.xpos = xpos;
	}

	public float getTouchTurn() {
		return touchTurn;
	}

	public void setTouchTurn(float touchTurn) {
		this.touchTurn = touchTurn;
	}

	public float getTouchTurnUp() {
		return touchTurnUp;
	}

	public void setTouchTurnUp(float touchTurnUp) {
		this.touchTurnUp = touchTurnUp;
	}

	public float getYpos() {
		return ypos;
	}

	public void setYpos(float ypos) {
		this.ypos = ypos;
	}

	public void touch() {
//		invert = !invert;
//
//		for(int i=0;i<barco.length;i++){
//			barco[i].setVisibility(invert);
//		}
//		for(int i=0;i<torre.length;i++){
//			torre[i].setVisibility(!invert);
//		}
	}
}
