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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import raft.jpct.bones.Animated3D;
import raft.jpct.bones.AnimatedGroup;
import raft.jpct.bones.BonesIO;
import raft.jpct.bones.SkinClip;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;

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

	private static final int			GRANULARITY		= 25;

	private static final float			PLANE_WIDTH 		= 130f;
	private static final float 			PLANE_HEIGHT		= 100f;
	
	private QCARFrameHandler			mARHandler		= null;
	private FrameBuffer					fb				= null;
	private World						world			= null;

	private Object3D					cube			= null;
	private Object3D					barco			= null;
	private Object3D					torre			= null;

	private Light						sun				= null;

	private float						touchTurn		= 0;
	private float						touchTurnUp		= 0;

	private float						xpos			= -1;
	private float						ypos			= -1;

	public boolean						mIsActive		= false;

	/** Reference to main activity **/
	public ImageTargets					mActivity;
	private Camera						cam;


	private int							mode			= 2;
	private Object3D					plane;

	private AnimatedGroup				gaviota;

	private final List<AnimatedGroup>	gaviotas		= new LinkedList<AnimatedGroup>();
	private long						frameTime		= System.currentTimeMillis();
	private long						aggregatedTime	= 0;

	private float						animateSeconds	= 0f;

	private int							animation		= -1;
	private float						speed			= 2f;

	private Object3D					dummy;

	private float	rotation;

	private MediaPlayer	mMediaPlayer;

	private float	sinMovement;

	public ImageTargetsRenderer(ImageTargets activity) {

		this.mActivity = activity;
		mARHandler = new QCARFrameHandler();
		

		mMediaPlayer = MediaPlayer.create(mActivity.getApplicationContext(), R.raw.seagull);
		try {
			mMediaPlayer.prepare();
		} catch (IllegalStateException e) {
			DebugLog.LOGE("Could not load sound");
		} catch (IOException e) {
			DebugLog.LOGE("Could not load sound");
		}

		
		initScene();
		
	}

	private void initScene() {
		world = new World();
		world.setClippingPlanes(2f, 2500f);
		world.setAmbientLight(50, 50, 50);

		loadObjects();

		sun = new Light(world);
		sun.setIntensity(250, 250, 250);

		cam = world.getCamera();

		//Centramos la luz en la torre
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
		//world.addObject(sphere);
		
		sun.setPosition(sv);
		sun.setAttenuation(500f);
		MemoryHelper.compact();
	}

	/** Called when the surface is created or recreated. */
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		DebugLog.LOGD("GLRenderer::onSurfaceCreated");

		mARHandler.onSurfaceCreated(gl, config);
	}

	/** Called when the surface changed size. */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		DebugLog.LOGD("GLRenderer::onSurfaceChanged");
		if (fb != null) {
			fb.dispose();
		}
		// fb = new FrameBuffer(gl, width,height);
		fb = new FrameBuffer(1196, 897);

		mARHandler.onSurfaceChanged(gl, width, height);
	}

	private void loadObjects() {

		
		torre = loadTorre();
		world.addObject(torre);
		
		dummy = torre.createDummyObj();
		loadGaviotas();
		
		
		Object3D root = gaviota.getRoot();
		dummy.addChild(root);
		dummy.translate(0, 20f, 0);
		root.translate(30f, 0, 100);
		
		//root.setOrigin(torre.getTransformedCenter());
		//root.translate(20f, 0, 0f);
		//Objetos debug
		cube = Primitives.getCube(10);
		cube.calcTextureWrapSpherical();
		cube.strip();
		cube.build();
		cube.rotateY(0.7853981763f);

		cube.setOrigin(torre.getTransformedCenter());
		
		//world.addObject(cube);
		Texture grass = new Texture(mActivity.getResources().getDrawable(R.drawable.grass01));
		TextureManager.getInstance().addTexture("herba.jpg", grass);
		
		grass.setClamping(false);
		
		plane = createPlane(PLANE_WIDTH, PLANE_HEIGHT);
		//plane = Primitives.getPlane(3, 60);
		plane.setCulling(false);
		plane.rotateX((float) Math.PI);
		plane.setSpecularLighting(true);
		plane.setTexture("pedrasdiante.jpg");
		plane.strip();
		plane.build();
		
		
		world.addObject(plane);
		
	}

	private static Object3D createPlane(float planeWidth, float planeHeight) {
		Object3D plane = new Object3D(2);
		float repeat = 4.0f;
		plane.addTriangle(new SimpleVector(-planeWidth,planeHeight,0), 0f, 0f, new SimpleVector(planeWidth,planeHeight,0),repeat, 0f, new SimpleVector(-planeWidth,-planeHeight,0), 0f, repeat);
		plane.addTriangle(new SimpleVector(planeWidth,planeHeight,0), repeat, 0f, new SimpleVector(planeWidth,-planeHeight,0), repeat, repeat, new SimpleVector(-planeWidth,-planeHeight,0), 0, repeat);
		return plane;
		
	}

	private void loadGaviotas() {
		Texture texture = new Texture(mActivity.getResources().getDrawable(R.drawable.gaviota));
		texture.keepPixelData(true);
		TextureManager.getInstance().addTexture("gaviota.jpg", texture);

		dummy = torre.createDummyObj();
		try {
			gaviota = BonesIO.loadGroup(mActivity.getResources().openRawResource(R.raw.gaviota));
			// if (MESH_ANIM_ALLOWED)
			createMeshKeyFrames();
			gaviota.addToWorld(world);
			//gaviota.setSkeletonPose(new SkeletonPose(gaviota.get(0).getSkeleton()));
			for (Animated3D a : gaviota) {
				a.setTexture("gaviota.jpg");
				//a.rotateX(-(float) Math.PI/2);
				a.scale(1.0f);
				///a.rotateMesh();
				a.discardMeshData();
				a.build();
			}
			
			animation = 1;
			Object3D root = gaviota.getRoot();
			
		//	root.scale(10.0f);
		//	gaviota.getRoot().rotateY((float) (Math.PI));
		//	gaviota.getRoot().rotateX(-(float) (Math.PI/2));
			root.rotateX(-1.57f);
			// addGaviota();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private Object3D loadTorre() {
		Texture.defaultTo4bpp(true);
		MtlTextureLoader.loadTexturesFromAssets("torrebasemat.mtl", mActivity.getAssets());
		
			Object3D torreSingle = Loader.loadSerializedObject(mActivity.getResources().openRawResource(R.raw.torre));
			//Para que se vea
			torreSingle.scale(2.0f);
			torreSingle.rotateX(-(float) Math.PI / 2);
			//torreSingle.rotateMesh();
			//Rotamos la torre para que salga derecha
			torreSingle.build();			
			torreSingle.setRotationPivot(SimpleVector.ORIGIN);
			torreSingle.translate(new SimpleVector(0.0f, +20.f, 0.0f));
		
		return torreSingle;
	}

	private void createMeshKeyFrames() {
		
		Config.maxAnimationSubSequences = gaviota.getSkinClipSequence().getSize() + 1; // +1 for whole sequence

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

	public void updateCamera() {

		cam.setFOV(mARHandler.getFov());
		cam.setYFOV(mARHandler.getFovy());
		cam.setOrientation(mARHandler.getCameraDirectionVector(), mARHandler.getCameraUpVector());
		cam.setPosition(mARHandler.getCameraPosition());

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
			float clipTime = gaviota.getSkinClipSequence().getClip(animation - 1).getTime();
			if (animateSeconds > clipTime) {
				animateSeconds = 0;
			}
			float index = animateSeconds / clipTime;
			gaviota.animateSkin(index, animation);

		} else {
			animateSeconds = 0f;
		}

		//dummy.rotateY(0.001f * animateSeconds);
		//gaviota.getRoot().rotateY(0.001f * animateSeconds);

		// Update render view (projection matrix and viewport) if needed:
		mActivity.updateRenderView();
		mARHandler.update();

		updateCamera();

		if (mARHandler.isTracking()) {
			
			
			mMediaPlayer.start();
			
			if (mode == 1) {
				plane.translate(0,0,-0.1f);
				DebugLog.LOGD("plane pos: " + plane.getTransformedCenter());
				//gaviota.getRoot().rotateX(-0.01f);
			}
			if(mode == 0){
				plane.translate(0,0,+0.1f);
				DebugLog.LOGD("plane pos: " + plane.getTransformedCenter());
			}
			sinMovement +=0.1f;
			gaviota.getRoot().translate(0, 0, (float) (0.5*Math.sin(sinMovement)));
			dummy.rotateZ(0.05f);
			world.renderScene(fb);
			world.draw(fb);
			fb.display();
		}
		else {
			if(mMediaPlayer.isPlaying())
				mMediaPlayer.pause();
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
			mode = 3;
			break;
		case 3:
			mode = 0;
			break;


		}
	}

	public void updateRendering(int mScreenWidth, int mScreenHeight) {
		mARHandler.updateRendering(mScreenWidth, mScreenHeight);
	}
}