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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.SkeletonPose;
import raft.jpct.bones.SkinClip;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;

import com.qualcomm.QCAR.QCAR;
import com.threed.jpct.Animation;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Mesh;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

/** The renderer class for the ImageTargets sample. */
public class ImageTargetsRenderer implements GLSurfaceView.Renderer {
	

	
	private static final int GRANULARITY = 25;
	
	private FrameBuffer					fb						= null;
	private World						world					= null;

	private RGBColor					back					= new RGBColor(0, 0, 0, 255);

	private Object3D					cube					= null;
	private Object3D					barco					= null;
	private Object3D					torre					= null;
	private int							fps						= 0;

	private Light						sun						= null;

	private float						touchTurn				= 0;
	private float						touchTurnUp				= 0;

	private float						xpos					= -1;
	private float						ypos					= -1;

	public boolean						mIsActive				= false;

	private boolean						init					= false;

	private float						modelViewMat[];
	private float						projectionMatrix[];

	/** Reference to main activity **/
	public ImageTargets					mActivity;
	private Camera						cam;
	private float						rotate;
	private boolean						invert;
	private boolean						showScene				= false;
	private float						fov;
	private int							screenWidth;
	private int							screenHeight;
	private float						prevyfov;

	private Texture						font					= null;
	private float						fovy;
	private int							mode					= 0;
	private SimpleVector				mCameraPosition			= new SimpleVector();
	private SimpleVector				mCameraRightVector		= new SimpleVector();
	private SimpleVector				mCameraUpVector			= new SimpleVector();
	private SimpleVector				mCameraDirectionVector	= new SimpleVector();
	private Object3D					plane;

	private AnimatedGroup				gaviota;

	private final List<AnimatedGroup>	gaviotas				= new LinkedList<AnimatedGroup>();
	private long	frameTime  = System.currentTimeMillis();
	private long	aggregatedTime = 0;

	private float	animateSeconds = 0f;

	private int animation = -1;
	private float	speed = 1f;

	/** Native function for initializing the renderer. */
	public native void initRendering();

	/** Native function to update the renderer. */
	public native void updateRendering(int width, int height);

	public void setCameraPos(float x, float y, float z) {

		DebugLog.LOGD("CAM POS: " + x + ", " + y + ", " + z);
		this.mCameraPosition = new SimpleVector(x, y, z);
	}

	public void setCameraRightVector(float x, float y, float z) {
		DebugLog.LOGD("RIGHT VEC: " + x + ", " + y + ", " + z);
		this.mCameraRightVector = new SimpleVector(x, y, z);
	}

	public void setCameraUpVector(float x, float y, float z) {
		DebugLog.LOGD("UP VEC: " + x + ", " + y + ", " + z);
		this.mCameraUpVector = new SimpleVector(x, y, z);
	}

	public void setCameraDirectionVector(float x, float y, float z) {
		DebugLog.LOGD("DIR VEC: " + x + ", " + y + ", " + z);
		this.mCameraDirectionVector = new SimpleVector(x, y, z);
	}

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
		DebugLog.LOGD("FOV: " + fov);
		this.fov = fov;
	}

	public void setFovy(float fov) {
		DebugLog.LOGD("FOVY: " + fovy);
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
			// Texture textureBarco = new
			// Texture(BitmapHelper.rescale(BitmapHelper.convert(mActivity.getResources().getDrawable(R.drawable.barcot)),
			// 512, 512));
			// TextureManager.getInstance().addTexture("barcot.jpg",
			// textureBarco);
			//

			world = new World();
			world.setClippingPlanes(2f, 2500f);
			world.setAmbientLight(50, 50, 50);

			loadObjects();

			sun = new Light(world);
			sun.setIntensity(250, 250, 250);

			cube = Primitives.getCube(30);
			cube.calcTextureWrapSpherical();
			// cube.setTexture("velas.jpg");
			cube.strip();
			cube.build();

			plane = Primitives.getPlane(3, 70);
			// plane.rotateX((float) 1.53);
			plane.setCulling(false);
			plane.setSpecularLighting(true);
			plane.calcTextureWrap();
			
			plane.setTexture("pedrascontodo.jpg");
			plane.strip();
			plane.build();
			// barco.rotateX(1.5f);
			cube.rotateY(0.7853981763f);
			// cube.rotateMesh();
			// world.addObject(cube);
			// world.addObject(cube);
			world.addObject(plane);
			cam = world.getCamera();

			SimpleVector sv = new SimpleVector();
			sv.set(torre.getOrigin());
			sv.z += 100;
			sv.y -= 30;

			Object3D sphere = Primitives.getSphere(5f);
			sphere.calcTextureWrapSpherical();
			sphere.setAdditionalColor(new RGBColor(255, 0, 0));
			sphere.strip();
			sphere.build();

			sphere.setOrigin(sv);

			world.addObject(sphere);
			sun.setPosition(sv);
			sun.setAttenuation(400f);
			MemoryHelper.compact();

			init = true;
		}
	}

	private void loadObjects() {

		Texture.defaultTo4bpp(true);
		AssetManager mngr = mActivity.getAssets();

		try {
			InputStream is2 = mngr.open("torrebasemat.mtl");
			Scanner input;
			input = new Scanner(is2);
			while (input.hasNext()) {

				String s = input.nextLine();
				if (s.startsWith("map_Kd")) {
					String filename = s.substring(6, s.length());
					filename = filename.trim();
					if (filename.startsWith("-s")) {

						filename = filename.substring(filename.lastIndexOf(' ') + 1, filename.length());
					}

					System.out.println(filename);
					Texture t = new Texture(mngr.open(filename));
					t.compress();
					if (!TextureManager.getInstance().containsTexture(filename))
						TextureManager.getInstance().addTexture(filename, t);
				}
			}

			input.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Texture texture = new Texture(mActivity.getResources().getDrawable(R.drawable.gaviota));
		texture.keepPixelData(true);
		TextureManager.getInstance().addTexture("gaviota.jpg", texture);

		// torre =
		// Object3D.mergeAll(Loader.loadOBJ(mActivity.getResources().openRawResource(R.raw.torrebase),
		// mActivity.getResources().openRawResource(R.raw.torrebasemat),
		// 20.0f));
		torre = Loader.loadSerializedObject(mActivity.getResources().openRawResource(R.raw.torre));
		torre.scale(2.0f);
		torre.compile();
		torre.rotateX(-(float) Math.PI / 2);
		// torreSingle.rotateMesh();
		torre.build();
		torre.setRotationPivot(SimpleVector.ORIGIN);
		torre.translate(new SimpleVector(0.0f, +20.f, 0.0f));
		Object3D dummy = torre.createDummyObj();
		world.addObject(torre);
		try {
			gaviota = BonesIO.loadGroup(mActivity.getResources().openRawResource(R.raw.gaviota));
			// if (MESH_ANIM_ALLOWED)
			createMeshKeyFrames();
			gaviota.addToWorld(world);
			gaviota.getRoot().scale(10.0f);
			gaviota.getRoot().rotateY((float) Math.PI);
			gaviota.getRoot().rotateX((float) (-Math.PI / 2));
			gaviota.getRoot().rotateZ((float) Math.PI);
			gaviota.setSkeletonPose(new SkeletonPose(gaviota.get(0).getSkeleton()));
			
			for (Animated3D a : gaviota) {
				a.setTexture("gaviota.jpg");
				a.rotateMesh();
				
			}
			gaviota.getRoot().addParent(dummy);
			gaviota.getRoot().translate(10.f, 0.0f, 0.0f);
			animation = 1;
			// gaviota.getRoot().rotateY((float) Math.PI/2);
			// addGaviota();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	
	private void createMeshKeyFrames() {
		Config.maxAnimationSubSequences = gaviota.getSkinClipSequence().getSize() + 1; // +1
																						// for
																						// whole
																						// sequence

		int keyframeCount = 0;
		final float deltaTime = 0.2f; // max time between frames

		for (SkinClip clip : gaviota.getSkinClipSequence()) {
			float clipTime = clip.getTime();
			int frames = (int) Math.ceil(clipTime / deltaTime) + 1;
			keyframeCount += frames;
		}

		Animation[] animations = new Animation[gaviota.getSize()];
		for (int i = 0; i < gaviota.getSize(); i++) {
			animations[i] = new Animation(keyframeCount);
			animations[i].setClampingMode(Animation.USE_CLAMPING);
		}
		// System.out.println("------------ keyframeCount: " + keyframeCount +
		// ", mesh size: " + masterNinja.getSize());
		int count = 0;

		int sequence = 0;
		for (SkinClip clip : gaviota.getSkinClipSequence()) {
			float clipTime = clip.getTime();
			int frames = (int) Math.ceil(clipTime / deltaTime) + 1;
			float dIndex = 1f / (frames - 1);

			for (int i = 0; i < gaviota.getSize(); i++) {
				animations[i].createSubSequence(clip.getName());
			}
			// System.out.println(sequence + ": " + clip.getName() +
			// ", frames: " + frames);
			for (int i = 0; i < frames; i++) {
				gaviota.animateSkin(dIndex * i, sequence + 1);

				for (int j = 0; j < gaviota.getSize(); j++) {
					Mesh keyframe = gaviota.get(j).getMesh().cloneMesh(true);
					keyframe.strip();
					animations[j].addKeyFrame(keyframe);
					count++;
					// System.out.println("added " + (i + 1) + " of " + sequence
					// + " to " + j + " total: " + count);
				}
			}
			sequence++;
		}
		for (int i = 0; i < gaviota.getSize(); i++) {
			gaviota.get(i).setAnimationSequence(animations[i]);
		}
		gaviota.get(0).getSkeletonPose().setToBindPose();
		gaviota.get(0).getSkeletonPose().updateTransforms();
		gaviota.applySkeletonPose();
		gaviota.applyAnimation();

		Logger.log("created mesh keyframes, " + keyframeCount + "x" + gaviota.getSize());
	}

	/** The native render function. */
	public native void renderFrame();

	public void setCameraMatrix(float[] modelViewMatrixFromVuforia) {

		float x = modelViewMatrixFromVuforia[12];
		float y = modelViewMatrixFromVuforia[13];
		float z = modelViewMatrixFromVuforia[14];

		// modelViewMatrixFromVuforia[12] = modelViewMatrixFromVuforia[13] =
		// modelViewMatrixFromVuforia[14] = 0;
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
		/*
		 * if (mode == 1) { mModelView.invert(); } else if (mode == 2) {
		 * mModelView.rotateAxis(new SimpleVector(1, 0, 0), (float) Math.PI); }
		 */
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
		// cam.setBack(mModelView);
		cam.setOrientation(mCameraDirectionVector, mCameraUpVector);
		cam.setPosition(mCameraPosition);

		// cam.setPosition(x, y, z);
		// cube.setOrigin(new SimpleVector(x, y, z));

		// torre.setRotationMatrix(mModelView);
		// torre.setOrigin(new SimpleVector(x, y, z));
		// cube.setRotationMatrix(mModelView);
		// cube.setOrigin(new SimpleVector(x, y, z));

	}

	/** Called to draw the current frame. */
	public void onDrawFrame(GL10 gl) {

		if (!mIsActive)
			return;

		long now = System.currentTimeMillis();
		aggregatedTime += (now - frameTime);
		frameTime = now;

		if (aggregatedTime > 1000) {
			aggregatedTime = 0;
		}

		while (aggregatedTime > GRANULARITY) {
			aggregatedTime -= GRANULARITY;
			animateSeconds += GRANULARITY * 0.001f * speed;
		}

		if (animation > 0 && gaviota.getSkinClipSequence().getSize() >= animation) {
			float clipTime = gaviota.getSkinClipSequence().getClip(animation-1).getTime();
			if (animateSeconds > clipTime) {
				animateSeconds = 0;
			}
			float index = animateSeconds / clipTime;
			gaviota.animateSkin(index, animation);

		} else {
			animateSeconds = 0f;
		}

		// Update render view (projection matrix and viewport) if needed:
		// mActivity.updateRenderView();
		// Call our native function to render content
		renderFrame();

		setCameraMatrix(modelViewMat);

		if (showScene) {
			if (mode == 1) {
				torre.rotateX(-0.01f);
			}
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
		switch (mode) {
		case 0:
			mode = 1;
			break;
		case 1:
			mode = 2;
			break;
		case 2:
			mode = 0;
			break;

		}
		//
		// for(int i=0;i<barco.length;i++){
		// barco[i].setVisibility(invert);
		// }
		// for(int i=0;i<torre.length;i++){
		// torre[i].setVisibility(!invert);
		// }
	}
}