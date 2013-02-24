package com.qualcomm.QCARSamples.ImageTargets;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.qualcomm.QCAR.QCAR;

public class ImageTargets extends Activity implements OnClickListener {
	// Focus mode constants:
	private static final int		FOCUS_MODE_NORMAL				= 0;
	private static final int		FOCUS_MODE_CONTINUOUS_AUTO		= 1;

	// Application status constants:
	private static final int		APPSTATUS_UNINITED				= -1;
	private static final int		APPSTATUS_INIT_APP				= 0;
	private static final int		APPSTATUS_INIT_QCAR				= 1;
	private static final int		APPSTATUS_INIT_TRACKER			= 2;
	private static final int		APPSTATUS_INIT_APP_AR			= 3;
	private static final int		APPSTATUS_LOAD_TRACKER			= 4;
	private static final int		APPSTATUS_INITED				= 5;
	private static final int		APPSTATUS_CAMERA_STOPPED		= 6;
	private static final int		APPSTATUS_CAMERA_RUNNING		= 7;

	// The current application status:
	private int						mAppStatus						= APPSTATUS_UNINITED;

	// The async tasks to initialize the QCAR SDK:
	private InitQCARTask			mInitQCARTask;
	private LoadTrackerTask			mLoadTrackerTask;

	// An object used for synchronizing QCAR initialization, dataset loading and
	// the Android onDestroy() life cycle event. If the application is destroyed
	// while a data set is still being loaded, then we wait for the loading
	// operation to finish before shutting down QCAR:
	private Object					mShutdownLock					= new Object();

	// Name of the native dynamic libraries to load:
	private static final String		NATIVE_LIB_SAMPLE				= "ImageTargets";
	private static final String		NATIVE_LIB_QCAR					= "QCAR";
	// Constants for Hiding/Showing Loading dialog
	static final int				HIDE_LOADING_DIALOG				= 0;
	static final int				SHOW_LOADING_DIALOG				= 1;

	private View					mLoadingDialogContainer;

	// QCAR initialization flags:
	private int						mQCARFlags						= 0;

	private QCARSampleGLView			mGlView;
	private ImageTargetsRenderer	mRenderer						= null;

	// Display size of the device:
	private int						mScreenWidth					= 0;
	private int						mScreenHeight					= 0;

	// Constant representing invalid screen orientation to trigger a query:
	private static final int		INVALID_SCREEN_ROTATION			= -1;

	// Last detected screen rotation:
	private int						mLastScreenRotation				= INVALID_SCREEN_ROTATION;

	boolean							mIsStonesAndChipsDataSetActive	= false;
	private RelativeLayout			mUILayout;

	// Contextual Menu Options for Camera Flash - Autofocus
	private boolean					mFlash							= false;
	private boolean					mContAutofocus					= false;

	private Handler					loadingDialogHandler			= new LoadingDialogHandler(this);
	private int						camWidth;
	private int						camHeight;
	private ImageView	splashImage;

	/** Static initializer block to load native libraries on start-up. */
	static {
		loadLibrary(NATIVE_LIB_QCAR);
		loadLibrary(NATIVE_LIB_SAMPLE);
	}

	public void setSize(float w, float h) {
		this.camWidth = (int) Math.ceil(w);
		this.camHeight = (int) Math.ceil(h);
		DebugLog.LOGD("w:" + this.camWidth + ", h: " + this.camHeight);
	}

	/**
	 * Creates a handler to update the status of the Loading Dialog from an UI
	 * Thread
	 */
	static class LoadingDialogHandler extends Handler {
		private final WeakReference<ImageTargets>	mImageTargets;

		LoadingDialogHandler(ImageTargets imageTargets) {
			mImageTargets = new WeakReference<ImageTargets>(imageTargets);
		}

		public void handleMessage(Message msg) {
			ImageTargets imageTargets = mImageTargets.get();
			if (imageTargets == null) {
				return;
			}

			if (msg.what == SHOW_LOADING_DIALOG) {
				imageTargets.mLoadingDialogContainer.setVisibility(View.VISIBLE);
				imageTargets.splashImage.setVisibility(View.VISIBLE);

			} else if (msg.what == HIDE_LOADING_DIALOG) {
				imageTargets.mLoadingDialogContainer.setVisibility(View.GONE);
				imageTargets.splashImage.setVisibility(View.GONE);
			}
		}
	}

	/** An async task to initialize QCAR asynchronously. */
	private class InitQCARTask extends AsyncTask<Void, Integer, Boolean> {
		// Initialize with invalid value:
		private int	mProgressValue	= -1;

		protected Boolean doInBackground(Void... params) {
			// Prevent the onDestroy() method to overlap with initialization:
			synchronized (mShutdownLock) {
				QCAR.setInitParameters(ImageTargets.this, mQCARFlags);

				do {
					// QCAR.init() blocks until an initialization step is
					// complete, then it proceeds to the next step and reports
					// progress in percents (0 ... 100%).
					// If QCAR.init() returns -1, it indicates an error.
					// Initialization is done when progress has reached 100%.
					mProgressValue = QCAR.init();

					// Publish the progress value:
					publishProgress(mProgressValue);

					// We check whether the task has been canceled in the
					// meantime (by calling AsyncTask.cancel(true)).
					// and bail out if it has, thus stopping this thread.
					// This is necessary as the AsyncTask will run to completion
					// regardless of the status of the component that
					// started is.
				} while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);

				return (mProgressValue > 0);
			}
		}

		protected void onProgressUpdate(Integer... values) {
			// Do something with the progress value "values[0]", e.g. update
			// splash screen, progress bar, etc.
		}

		protected void onPostExecute(Boolean result) {
			// Done initializing QCAR, proceed to next application
			// initialization status:
			if (result) {
				DebugLog.LOGD("InitQCARTask::onPostExecute: QCAR " + "initialization successful");

				updateApplicationStatus(APPSTATUS_INIT_TRACKER);
			} else {
				// Create dialog box for display error:
				AlertDialog dialogError = new AlertDialog.Builder(ImageTargets.this).create();

				dialogError.setButton(DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						System.exit(1);
					}
				});

				String logMessage;

				// NOTE: Check if initialization failed because the device is
				// not supported. At this point the user should be informed
				// with a message.
				if (mProgressValue == QCAR.INIT_DEVICE_NOT_SUPPORTED) {
					logMessage = "Failed to initialize QCAR because this " + "device is not supported.";
				} else {
					logMessage = "Failed to initialize QCAR.";
				}

				// Log error:
				DebugLog.LOGE("InitQCARTask::onPostExecute: " + logMessage + " Exiting.");

				// Show dialog box with error message:
				dialogError.setMessage(logMessage);
				dialogError.show();
			}
		}
	}

	/** An async task to load the tracker data asynchronously. */
	private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean> {
		protected Boolean doInBackground(Void... params) {
			// Prevent the onDestroy() method to overlap:
			synchronized (mShutdownLock) {
				// Load the tracker data set:
				return (loadTrackerData() > 0);
			}
		}

		protected void onPostExecute(Boolean result) {
			DebugLog.LOGD("LoadTrackerTask::onPostExecute: execution " + (result ? "successful" : "failed"));

			if (result) {
				// The stones and chips data set is now active:
				mIsStonesAndChipsDataSetActive = true;

				// Done loading the tracker, update application status:
				updateApplicationStatus(APPSTATUS_INITED);
			} else {
				// Create dialog box for display error:
				AlertDialog dialogError = new AlertDialog.Builder(ImageTargets.this).create();

				dialogError.setButton(DialogInterface.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Exiting application:
						System.exit(1);
					}
				});

				// Show dialog box with error message:
				dialogError.setMessage("Failed to load tracker data.");
				dialogError.show();
			}
		}
	}

	/** Stores screen dimensions */
	private void storeScreenDimensions() {
		// Query display dimensions:
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
	}

	/**
	 * Called when the activity first starts or the user navigates back to an
	 * activity.
	 */
	protected void onCreate(Bundle savedInstanceState) {
		DebugLog.LOGD("ImageTargets::onCreate");
		super.onCreate(savedInstanceState);


		LayoutInflater inflater = LayoutInflater.from(this);
		mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay, null, false);


		mUILayout.setVisibility(View.VISIBLE);
		mUILayout.setBackgroundColor(Color.BLACK);

		// Gets a reference to the loading dialog
		mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);
		
		//((ProgressBar) mLoadingDialogContainer).animate();
		splashImage = (ImageView) mUILayout.findViewById(R.id.imageView1);
		
		
		// Shows the loading indicator at start
		loadingDialogHandler.sendEmptyMessage(SHOW_LOADING_DIALOG);

		// Adds the inflated layout to the view
		addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		// Query the QCAR initialization flags:
		mQCARFlags = getInitializationFlags();

		// Update the application status to start initializing application:
		updateApplicationStatus(APPSTATUS_INIT_APP);
	}

	/** Configure QCAR with the desired version of OpenGL ES. */
	private int getInitializationFlags() {
		int flags = 0;

		// Query the native code:
		if (getOpenGlEsVersionNative() == 1) {
			flags = QCAR.GL_11;
		} else {
			flags = QCAR.GL_20;
		}

		return flags;
	}

	/**
	 * Native method for querying the OpenGL ES version. Returns 1 for OpenGl ES
	 * 1.1, returns 2 for OpenGl ES 2.0.
	 */
	public native int getOpenGlEsVersionNative();

	/** Native tracker initialization and deinitialization. */
	public native int initTracker();

	public native void deinitTracker();

	/** Native functions to load and destroy tracking data. */
	public native int loadTrackerData();

	public native void destroyTrackerData();

	/** Native sample initialization. */
	public native void onQCARInitializedNative();

	/** Native methods for starting and stopping the camera. */
	private native void startCamera();

	private native void stopCamera();

	/**
	 * Native method for setting / updating the projection matrix for AR content
	 * rendering
	 */
	private native void setProjectionMatrix();

	@Override
	protected void onResume() {
		DebugLog.LOGD("ImageTargets::onResume");
		super.onResume();
		// QCAR-specific resume operation
		QCAR.onResume();

		// We may start the camera only if the QCAR SDK has already been
		// initialized
		if (mAppStatus == APPSTATUS_CAMERA_STOPPED) {
			updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
		}

		// Resume the GL view:
		if (mGlView != null) {
			mGlView.setVisibility(View.VISIBLE);
			mGlView.onResume();
		}
	}

	private void updateActivityOrientation() {
		Configuration config = getResources().getConfiguration();

		boolean isPortrait = false;

		switch (config.orientation) {
		case Configuration.ORIENTATION_PORTRAIT:
			isPortrait = true;
			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			isPortrait = false;
			break;
		case Configuration.ORIENTATION_UNDEFINED:
		default:
			break;
		}

		DebugLog.LOGI("Activity is in " + (isPortrait ? "PORTRAIT" : "LANDSCAPE"));
		setActivityPortraitMode(isPortrait);
	}

	/**
	 * Updates projection matrix and viewport after a screen rotation change was
	 * detected.
	 */
	public void updateRenderView() {
		int currentScreenRotation = getWindowManager().getDefaultDisplay().getRotation();
		if (currentScreenRotation != mLastScreenRotation) {
			// Set projection matrix if there is already a valid one:
			if (QCAR.isInitialized() && (mAppStatus == APPSTATUS_CAMERA_RUNNING)) {
				DebugLog.LOGD("ImageTargets::updateRenderView");

				// Query display dimensions:
				storeScreenDimensions();

				// Update viewport via renderer:
				mRenderer.updateRendering(mScreenWidth, mScreenHeight);

				// Update projection matrix:
				setProjectionMatrix();

				// Cache last rotation used for setting projection matrix:
				mLastScreenRotation = currentScreenRotation;
			}
		}
	}

	/** Callback for configuration changes the activity handles itself */
	public void onConfigurationChanged(Configuration config) {
		DebugLog.LOGD("ImageTargets::onConfigurationChanged");
		super.onConfigurationChanged(config);

		updateActivityOrientation();

		storeScreenDimensions();

		// Invalidate screen rotation to trigger query upon next render call:
		mLastScreenRotation = INVALID_SCREEN_ROTATION;
	}

	@Override
	protected void onPause() {

		DebugLog.LOGD("ImageTargets::onPause");
		super.onPause();

		if (mGlView != null) {
			mGlView.setVisibility(View.INVISIBLE);
			mGlView.onPause();
		}

		if (mAppStatus == APPSTATUS_CAMERA_RUNNING) {
			updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
		}

		// QCAR-specific pause operation
		QCAR.onPause();
	}

	/** Native function to deinitialize the application. */
	private native void deinitApplicationNative();

	/** The final call you receive before your activity is destroyed. */
	protected void onDestroy() {
		DebugLog.LOGD("ImageTargets::onDestroy");
		super.onDestroy();

		// Cancel potentially running tasks
		if (mInitQCARTask != null && mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED) {
			mInitQCARTask.cancel(true);
			mInitQCARTask = null;
		}

		if (mLoadTrackerTask != null && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED) {
			mLoadTrackerTask.cancel(true);
			mLoadTrackerTask = null;
		}

		// Ensure that all asynchronous operations to initialize QCAR
		// and loading the tracker datasets do not overlap:
		synchronized (mShutdownLock) {

			// Do application deinitialization in native code:
			deinitApplicationNative();

			// Destroy the tracking data set:
			destroyTrackerData();

			// Deinit the tracker:
			deinitTracker();

			// Deinitialize QCAR SDK:
			QCAR.deinit();
		}

		System.gc();
	}

	/**
	 * NOTE: this method is synchronized because of a potential concurrent
	 * access by ImageTargets::onResume() and InitQCARTask::onPostExecute().
	 */
	private synchronized void updateApplicationStatus(int appStatus) {
		// Exit if there is no change in status:
		if (mAppStatus == appStatus)
			return;

		// Store new status value:
		mAppStatus = appStatus;

		// Execute application state-specific actions:
		switch (mAppStatus) {
		case APPSTATUS_INIT_APP:
			// Initialize application elements that do not rely on QCAR
			// initialization:
			initApplication();

			// Proceed to next application initialization status:
			updateApplicationStatus(APPSTATUS_INIT_QCAR);
			break;
		case APPSTATUS_INIT_QCAR:
			// Initialize QCAR SDK asynchronously to avoid blocking the
			// main (UI) thread.
			//
			// NOTE: This task instance must be created and invoked on the
			// UI thread and it can be executed only once!
			try {
				mInitQCARTask = new InitQCARTask();
				mInitQCARTask.execute();
			} catch (Exception e) {
				DebugLog.LOGE("Initializing QCAR SDK failed");
			}
			break;
		case APPSTATUS_INIT_TRACKER:
			// Initialize the ImageTracker:
			if (initTracker() > 0) {
				updateApplicationStatus(APPSTATUS_INIT_APP_AR);
			}
			break;

		case APPSTATUS_INIT_APP_AR:
			// Initialize Augmented Reality-specific application elements
			// that may rely on the fact that the QCAR SDK has been
			// already initialized:
			initApplicationAR();

			// Proceed to next application initialization status:
			updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
			break;
		case APPSTATUS_LOAD_TRACKER:
			// Load the tracking data set:
			//
			// NOTE: This task instance must be created and invoked on the
			// UI thread and it can be executed only once!
			try {
				mLoadTrackerTask = new LoadTrackerTask();
				mLoadTrackerTask.execute();
			} catch (Exception e) {
				DebugLog.LOGE("Loading tracking data set failed");
			}
			break;

		case APPSTATUS_INITED:
			// Hint to the virtual machine that it would be a good time to
			// run the garbage collector:
			//
			// NOTE: This is only a hint. There is no guarantee that the
			// garbage collector will actually be run.
			System.gc();
			// Native post initialization:
			onQCARInitializedNative();

			// Activate the renderer:
			mRenderer.mIsActive = true;

			// Now add the GL surface view. It is important
			// that the OpenGL ES surface view gets added
			// BEFORE the camera is started and video
			// background is configured.
			addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			// Sets the UILayout to be drawn in front of the camera
			mUILayout.bringToFront();
			// Start the camera:
			updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);

			break;

		case APPSTATUS_CAMERA_STOPPED:
			// Call the native function to stop the camera:
			stopCamera();
			break;

		case APPSTATUS_CAMERA_RUNNING:
			// Call the native function to start the camera:
			startCamera();

			// Hides the Loading Dialog
			// loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);
			// Hides the Loading Dialog
			loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);

			// Sets the layout background to transparent
			mUILayout.setBackgroundColor(Color.TRANSPARENT);

			
			
			// Set continuous auto-focus if supported by the device,
			// otherwise default back to regular auto-focus mode.
			// This will be activated by a tap to the screen in this
			// application.
			if (!setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO)) {
				mContAutofocus = false;
				setFocusMode(FOCUS_MODE_NORMAL);
			} else {
				mContAutofocus = true;
			}
			break;
		default:
			throw new RuntimeException("Invalid application state");
		}
	}

	/** Tells native code whether we are in portait or landscape mode */
	private native void setActivityPortraitMode(boolean isPortrait);

	/** Initialize application GUI elements that are not related to AR. */
	private void initApplication() {
		// Set the screen orientation:
		// NOTE: Use SCREEN_ORIENTATION_LANDSCAPE or SCREEN_ORIENTATION_PORTRAIT
		// to lock the screen orientation for this activity.
		// int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

		// This is necessary for enabling AutoRotation in the Augmented View
		if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
			// NOTE: We use reflection here to see if the current platform
			// supports the full sensor mode (available only on Gingerbread
			// and above.
			try {
				// SCREEN_ORIENTATION_FULL_SENSOR is required to allow all
				// 4 screen rotations if API level >= 9:
				Field fullSensorField = ActivityInfo.class.getField("SCREEN_ORIENTATION_FULL_SENSOR");
				screenOrientation = fullSensorField.getInt(null);
			} catch (NoSuchFieldException e) {
				// App is running on API level < 9, do nothing.
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Apply screen orientation
		setRequestedOrientation(screenOrientation);

		updateActivityOrientation();

		// Query display dimensions:
		storeScreenDimensions();

		// As long as this window is visible to the user, keep the device's
		// screen turned on and bright:
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/** Native function to initialize the application. */
	private native void initApplicationNative(int width, int height);

	/** Initializes AR application components. */
	private void initApplicationAR() {

		// mGlView = new QCARSampleGLView(getApplication());
		//
		// mRenderer = new ImageTargetsRenderer(this);
		// mGlView.setRenderer(mRenderer);
		// setContentView(mGlView);

		
		
		// Do application initialization in native code (e.g. registering
		// callbacks, etc.):
		initApplicationNative(mScreenWidth, mScreenHeight);

		// Create OpenGL ES view:
		int depthSize = 16;
		int stencilSize = 0;
		boolean translucent = QCAR.requiresAlpha();

		 mGlView = new QCARSampleGLView(this);
		//mGlView = new GLSurfaceView(this.getApplication());
		 mGlView.init(mQCARFlags, translucent, depthSize, stencilSize);
		//mGlView.setEGLContextClientVersion(2);
		//mGlView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

		mRenderer = new ImageTargetsRenderer(this);
		mGlView.setRenderer(mRenderer);
		// setContentView(mGlView);

	}

	private native boolean autofocus();

	private native boolean setFocusMode(int mode);

	/** Activates the Flash */
	private native boolean activateFlash(boolean flash);

	/** A helper for loading native libraries stored in "libs/armeabi*". */
	public static boolean loadLibrary(String nLibName) {
		try {
			System.loadLibrary(nLibName);
			DebugLog.LOGI("Native library lib" + nLibName + ".so loaded");
			return true;
		} catch (UnsatisfiedLinkError ulee) {
			DebugLog.LOGE("The library lib" + nLibName + ".so could not be loaded");
		} catch (SecurityException se) {
			DebugLog.LOGE("The library lib" + nLibName + ".so was not allowed to be loaded");
		}

		return false;
	}

	public boolean onTouchEvent(MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			/*
			 * mRenderer.setXpos(me.getX()); mRenderer.setYpos(me.getY());
			 */

			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_UP) {
			/*
			 * mRenderer.setXpos(-1); mRenderer.setYpos(-1);
			 * mRenderer.setTouchTurn(0); mRenderer.setTouchTurnUp(0);
			 */
			mRenderer.touch();
			return true;
		}

		/*
		 * if (me.getAction() == MotionEvent.ACTION_MOVE) { float xd = me.getX()
		 * - mRenderer.getXpos(); float yd = me.getY() - mRenderer.getYpos();
		 * 
		 * mRenderer.setXpos(me.getX()); mRenderer.setYpos(me.getY());
		 * mRenderer.setTouchTurn(xd / -100f); mRenderer.setTouchTurnUp(yd /
		 * -100f); return true; }
		 * 
		 * try { Thread.sleep(15); } catch (Exception e) { // No need for
		 * this... }
		 */

		return super.onTouchEvent(me);
	}

	@Override
	public void onClick(View v) {
	
		
	}

	public void removeSplash() {
		// TODO Auto-generated method stub
		splashImage.setVisibility(View.GONE);
	}

}