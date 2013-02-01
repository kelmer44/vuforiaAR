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

	private static final int			GRANULARITY		= 25;

	private QCARFrameHandler			mARHandler		= null;
	private FrameBuffer					fb				= null;
	private World						world			= null;

	private Object3D					cube			= null;
	private Object3D					barco			= null;
	private Object3D					torre			= null;
	private int							fps				= 0;

	private Light						sun				= null;

	private float						touchTurn		= 0;
	private float						touchTurnUp		= 0;

	private float						xpos			= -1;
	private float						ypos			= -1;

	public boolean						mIsActive		= false;

	private boolean						init			= false;

	/** Reference to main activity **/
	public ImageTargets					mActivity;
	private Camera						cam;
	private float						rotate;
	private boolean						invert;
	private boolean						showScene		= false;
	private int							screenWidth;
	private int							screenHeight;

	private Texture						font			= null;

	private int							mode			= 0;
	private Object3D					plane;

	private AnimatedGroup				gaviota;

	private final List<AnimatedGroup>	gaviotas		= new LinkedList<AnimatedGroup>();
	private long						frameTime		= System.currentTimeMillis();
	private long						aggregatedTime	= 0;

	private float						animateSeconds	= 0f;

	private int							animation		= -1;
	private float						speed			= 1f;

	private Object3D					dummy;

	public ImageTargetsRenderer(ImageTargets activity) {

		this.mActivity = activity;
		mARHandler = new QCARFrameHandler();
		init();
	}

	private void init() {
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

		plane.setTexture("gaviota.jpg");
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
		world.addObject(torre);
		dummy = torre.createDummyObj();
		try {
			gaviota = BonesIO.loadGroup(mActivity.getResources().openRawResource(R.raw.gaviota));
			// if (MESH_ANIM_ALLOWED)
			createMeshKeyFrames();
			gaviota.getRoot().scale(2.0f);
			gaviota.getRoot().rotateY((float) Math.PI);
			gaviota.getRoot().rotateX((float) (-Math.PI / 2));
			gaviota.getRoot().rotateZ((float) Math.PI);
			gaviota.setSkeletonPose(new SkeletonPose(gaviota.get(0).getSkeleton()));
			for (Animated3D a : gaviota) {
				a.setTexture("gaviota.jpg");
				a.rotateMesh();
				a.translate(10.f, 20.0f, 0.0f);
			}
			animation = 1;

			gaviota.getRoot().setRotationPivot(dummy.getCenter());

			gaviota.getRoot().rotateY((float) Math.PI / 2);
			// addGaviota();

			gaviota.addToWorld(world);
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

		dummy.rotateY(0.001f * animateSeconds);
		gaviota.getRoot().rotateY(0.001f * animateSeconds);

		// Update render view (projection matrix and viewport) if needed:
		mActivity.updateRenderView();
		mARHandler.update();

		updateCamera();

		if (mARHandler.isTracking()) {
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

	public void updateRendering(int mScreenWidth, int mScreenHeight) {
		mARHandler.updateRendering(mScreenWidth, mScreenHeight);
	}
}