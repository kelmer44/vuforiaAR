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

	private RGBColor	back		= new RGBColor(0, 0, 0, 255);

	private Object3D	cube		= null;
	private Object3D	barco		= null;
	private Object3D	torre		= null;
	private int			fps			= 0;

	private Light		sun			= null;

	private float		touchTurn	= 0;
	private float		touchTurnUp	= 0;

	private float		xpos		= -1;
	private float		ypos		= -1;

	public boolean		mIsActive	= false;

	private boolean		init		= false;

	private float		modelViewMat[];
	private float		projectionMatrix[];

	/** Reference to main activity **/
	public ImageTargets	mActivity;
	private Camera		cam;
	private float		rotate;
	private boolean		invert;
	private boolean		showScene	= false;
	private float		fov;
	private int			screenWidth;
	private int			screenHeight;
	private float		prevyfov;

	private Texture		font		= null;
	private float	fovy;

	/** Native function for initializing the renderer. */
	public native void initRendering();

	/** Native function to update the renderer. */
	public native void updateRendering(int width, int height);

	public void updateModelviewMatrix(float mat[]) {

		modelViewMat = mat;
	}

	public void updateProjMatrix(float mat[]) {

		projectionMatrix = mat;
	}

	public void isTracking(boolean is) {
		showScene = is;
	}

	public void setFov(float fov) {
		this.fov = fov;
	}

	public void setFovy(float fov) {
		this.fovy = fov;
	}
	
	
	public ImageTargetsRenderer(ImageTargets activity) {

		this.mActivity = activity;
		modelViewMat = new float[16];
		projectionMatrix = new float[16];
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
		// fb = new FrameBuffer(gl, width,height);
		fb = new FrameBuffer(1196, 720);
		
		// Call native function to update rendering when render surface
		// parameters have changed:
		updateRendering(width, height);

		// Call QCAR function to handle render surface size changes:
		QCAR.onSurfaceChanged(width, height);

		if (!init) {
			// Create a texture out of the icon...:-)
			Texture textureBarco = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.barcot)), 512, 512));
			TextureManager.getInstance().addTexture("barcot.jpg", textureBarco);

			Texture textureVelas = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.velas)), 1024, 1024));
			TextureManager.getInstance().addTexture("velas.jpg", textureVelas);

			world = new World();
			world.setAmbientLight(200, 200, 200);
			sun = new Light(world);
			sun.setIntensity(250, 250, 250);
			
			Light sun2 = new Light(world);
			sun2.setIntensity(250, 250, 250);
			Light sun3 = new Light(world);
			sun3.setIntensity(250, 250, 250);
			
			
			 cube = Primitives.getCube(30);
			 cube.calcTextureWrapSpherical();
			 cube.setTexture("barcot.jpg");
			 cube.strip();
			 cube.build();
			 
			font = new Texture(mActivity.getResources().openRawResource(R.raw.numbers));
			font.setMipmap(false);

			barco = Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.barco), mActivity.getResources().openRawResource(R.raw.barcomat), 2.0f));
			
			// barco.setTransparency(-1);
			// barco =
			// Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.vance),
			// mActivity.getResources().openRawResource(R.raw.vancemat), 1.0f));
//			torre = Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.torresola), mActivity.getResources().openRawResource(R.raw.torremat), 10.0f));

			// barco.rotateX(1.5f);
//			cube.rotateY(0.7853981763f);
			world.addObject(cube);
			world.setClippingPlanes(2f, 2500f);
			barco.setOrigin(new SimpleVector(0, 0, 0));
			// world.addObjects(torre);io8 
			cam = world.getCamera();
			//cam.moveCamera(Camera.CAMERA_MOVEOUT, -3);
//			cam.moveCamera(Camera.CAMERA_MOVEOUT, 10);
//			cam.lookAt(cube.getTransformedCenter());

			SimpleVector sv = new SimpleVector();
			sv.set(cube.getTransformedCenter());
			sv.z -= 20;
			sun.setPosition(sv);	
			SimpleVector sv2 = new SimpleVector();
			sv2.set(cube.getTransformedCenter());
			sv2.x -= 20;
			sun2.setPosition(sv2);
			SimpleVector sv3 = new SimpleVector();
			sv3.set(cube.getTransformedCenter());
			sv3.y -= 20;
			sun3.setPosition(sv3);
			MemoryHelper.compact();

			init = true;
		}
	}

	/** The native render function. */
	public native void renderFrame();

	public void setCameraMatrix(float[] modelViewMatrixFromVuforia) {

		float x = modelViewMatrixFromVuforia[12];
		float y = modelViewMatrixFromVuforia[13];
		float z = modelViewMatrixFromVuforia[14];

		modelViewMatrixFromVuforia[12] = modelViewMatrixFromVuforia[13] = modelViewMatrixFromVuforia[14] = 0;
		// StringBuffer str = new StringBuffer("MATRIX:\n");
		// for (int i = 0; i < 4; i++) {
		// for (int j = 0; j < 4; j++) {
		// str.append("" + modelViewMatrixFromVuforia[4 * i + j] + " ");
		// }
		// str.append("\n");
		// }
		// DebugLog.LOGD(str.toString());
		//
		// DebugLog.LOGD("Translate: " + x + ", " + y + "," + z);

		com.threed.jpct.Matrix mModelView = new com.threed.jpct.Matrix();

		mModelView.setDump(modelViewMatrixFromVuforia);
		//mProjection.setDump(projectionMatrix);
		//mModelView = mModelView.invert();
		// barco.setOrigin(new SimpleVector(x, y, z));
		//cam.setPosition(-x, -y, -z);
		
		//cam.setYFOV(fov);
		//cam.setBack(mModelView);
		//cam.setPosition(x, y, z);
		
		//mModelView.rotateY(0.78f);
		cam.setFOV(fov);
		cam.setYFOV(fovy);
		cube.setRotationMatrix(mModelView);
		cube.setOrigin(new SimpleVector(x, y, z));
		//torre.setRotationMatrix(mModelView);
		//torre.setOrigin(new SimpleVector(x, y , z));

	}

	/** Called to draw the current frame. */
	public void onDrawFrame(GL10 gl) {

		if (!mIsActive)
			return;



		// Update render view (projection matrix and viewport) if needed:
		//mActivity.updateRenderView();
		// Call our native function to render content
		renderFrame();

		cam.setFOV(fov);
		setCameraMatrix(modelViewMat);
		
		if (showScene) {
			//fb.clear(back);
				world.renderScene(fb);
				world.draw(fb);
				
				DebugLog.LOGD("Barco: " + cube.getTransformedCenter().x + ", " + cube.getTransformedCenter().y + ", " + cube.getTransformedCenter().z);

				DebugLog.LOGD("Cam: " + cam.getPosition().x + ", " + cam.getPosition().y + ", " + cam.getPosition().z);
			fb.display();
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
		invert = !invert;
		//
		// for(int i=0;i<barco.length;i++){
		// barco[i].setVisibility(invert);
		// }
		// for(int i=0;i<torre.length;i++){
		// torre[i].setVisibility(!invert);
		// }
	}
}