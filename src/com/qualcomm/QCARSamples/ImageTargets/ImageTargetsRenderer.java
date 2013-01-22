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
	private float		fovy;

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
		// DebugLog.LOGD("FOV: " + fov);
		this.fov = fov;
	}

	public void setFovy(float fov) {
		// DebugLog.LOGD("FOVY: " + fovy);
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
		fb = new FrameBuffer(1196, 897);

		// Call native function to update rendering when render surface
		// parameters have changed:
		updateRendering(width, height);

		// Call QCAR function to handle render surface size changes:
		QCAR.onSurfaceChanged(width, height);

		if (!init) {
			// Create a texture out of the icon...:-)

			//
			// Texture[] textures;
			// Texture.defaultTo4bpp(true);
			// textures = new Texture[15];
			// textures[0] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.murofachada2b)));
			// textures[0].enable4bpp(true);
			// textures[0].compress();
			// TextureManager.getInstance().addTexture("murofachada2b.jpg",
			// textures[0]);
			//
			// textures[1] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.rayasescaleiras)));
			// textures[1].compress();
			// TextureManager.getInstance().addTexture("rayasescaleiras.jpg",
			// textures[1]);
			//
			// textures[2] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.suelo)));
			// textures[2].compress();
			// TextureManager.getInstance().addTexture("suelo.jpg",
			// textures[2]);
			//
			// textures[3] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.casita1)));
			// textures[3].compress();
			// TextureManager.getInstance().addTexture("casita1.jpg",
			// textures[3]);
			//
			// textures[4] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.cristalescupula)));
			// textures[4].compress();
			// TextureManager.getInstance().addTexture("cristalescupula.jpg",
			// textures[4]);
			//
			// textures[5] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.frontalescaleiras2)));
			// textures[5].compress();
			// TextureManager.getInstance().addTexture("frontalescaleiras2.jpg",
			// textures[5]);
			//
			// textures[6] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.lateralcasita)));
			// textures[6].compress();
			// TextureManager.getInstance().addTexture("lateralcasita.jpg",
			// textures[6]);
			//
			// textures[7] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.muroforaretocado)));
			// textures[7].compress();
			// TextureManager.getInstance().addTexture("muroforaretocado.jpg",
			// textures[7]);
			//
			// textures[8] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.paredeint03)));
			// textures[8].compress();
			// TextureManager.getInstance().addTexture("paredeint03.jpg",
			// textures[8]);
			//
			// textures[9] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.placa)));
			// textures[9].compress();
			// TextureManager.getInstance().addTexture("placa.jpg",
			// textures[9]);
			//
			// textures[10] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.placa3)));
			// textures[10].compress();
			// TextureManager.getInstance().addTexture("placa3.jpg",
			// textures[10]);
			//
			// textures[11] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.portaentrada1)));
			// textures[11].compress();
			// TextureManager.getInstance().addTexture("portaentrada1.jpg",
			// textures[11]);
			//
			// textures[12] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.portaentrada2)));
			// textures[12].compress();
			// TextureManager.getInstance().addTexture("portaentrada2.jpg",
			// textures[12]);
			//
			// textures[13] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.sueloentrada)));
			// textures[13].compress();
			// TextureManager.getInstance().addTexture("sueloentrada.jpg",
			// textures[13]);
			//
			// textures[14] = new
			// Texture(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.telladocasita)));
			// textures[14].compress();
			// TextureManager.getInstance().addTexture("telladocasita.jpg",
			// textures[14]);

			world = new World();
			world.setAmbientLight(50, 50, 50);
			sun = new Light(world);
			sun.setIntensity(250, 250, 250);

			Texture textureBarco = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.barcot)), 512, 512));
			TextureManager.getInstance().addTexture("barcot.jpg", textureBarco);
			//
			Texture textureVelas = new Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.velas)), 1024, 1024));
			TextureManager.getInstance().addTexture("velas.jpg", textureVelas);

			barco = Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.barco), mActivity.getResources().openRawResource(R.raw.barcomat), 2.0f));
			barco.setRotationPivot(new SimpleVector(0,0,0));
			//barco.rotateX(1.5f);
//			barco.rotateMesh();

			cube = Primitives.getCube(30);
			cube.calcTextureWrapSpherical();
			cube.setTexture("barcot.jpg");
			cube.strip();
			cube.build();

			cube.rotateY(0.7853981763f);
			cube.rotateMesh();

			// torre =
			// Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.torresola),
			// mActivity.getResources().openRawResource(R.raw.torremat),
			// 20.0f));
			// torre.rotateX(1.57f);
			// torre.setCulling(false);
			// torre.rotateMesh();

			world.addObject(cube);
			world.addObject(barco);
			world.setClippingPlanes(2f, 2500f);
			// world.addObject(torre);
			// world.addObject(barco);
			cam = world.getCamera();

			SimpleVector sv = new SimpleVector();
			sv.set(cube.getTransformedCenter());
			sv.z -= 20;
			sun.setPosition(sv);
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
		// mProjection.setDump(projectionMatrix);
		// mModelView = mModelView.invert();
		// barco.setOrigin(new SimpleVector(x, y, z));
		// cam.setPosition(-x, -y, -z);

		// cam.setYFOV(fov);
		// cam.setBack(mModelView);
		// cam.setPosition(x, y, z);

		// mModelView.rotateY(0.78f);
		cam.setFOV(fov);
		cam.setYFOV(fovy);
		// mModelView.invert();
		// cam.setBack(mModelView);
		cam.setPosition(-x, -y, -z);
		// torre.setRotationMatrix(mModelView);
		// torre.setOrigin(new SimpleVector(x, y, z));
		
		cube.setRotationMatrix(mModelView);
		//cube.setOrigin(new SimpleVector(x, y, z));
		
		//barco.rotateX(0.1f);
		//barco.setRotationMatrix(mModelView);
		//barco.setOrigin(new SimpleVector(x, y, z));
		
	}

	/** Called to draw the current frame. */
	public void onDrawFrame(GL10 gl) {

		if (!mIsActive)
			return;

		// Update render view (projection matrix and viewport) if needed:
		mActivity.updateRenderView();
		// Call our native function to render content
		renderFrame();

		setCameraMatrix(modelViewMat);

		if (showScene) {
			// fb.clear(back);
			world.renderScene(fb);
			world.draw(fb);

			// DebugLog.LOGD("Barco: " + cube.getTransformedCenter().x + ", " +
			// cube.getTransformedCenter().y + ", " +
			// cube.getTransformedCenter().z);

			// DebugLog.LOGD("Cam: " + cam.getPosition().x + ", " +
			// cam.getPosition().y + ", " + cam.getPosition().z);
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