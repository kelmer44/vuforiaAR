/*==============================================================================
 Copyright (c) 2010-2012 QUALCOMM Austria Research Center GmbH.
 All Rights Reserved.
 Qualcomm Confidential and Proprietary

 @file
 ImageTargets.cpp

 @brief
 Sample for ImageTargets

 ==============================================================================*/

#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <math.h>

#ifdef USE_OPENGL_ES_1_1
#include <GLES/gl.h>
#include <GLES/glext.h>
#else
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#endif

#include <QCAR/QCAR.h>
#include <QCAR/CameraDevice.h>
#include <QCAR/Renderer.h>
#include <QCAR/VideoBackgroundConfig.h>
#include <QCAR/Trackable.h>
#include <QCAR/TrackableResult.h>
#include <QCAR/Tool.h>
#include <QCAR/Tracker.h>
#include <QCAR/TrackerManager.h>
#include <QCAR/ImageTracker.h>
#include <QCAR/CameraCalibration.h>
#include <QCAR/UpdateCallback.h>
#include <QCAR/DataSet.h>

#include "SampleUtils.h"
#include "Texture.h"
#include "CubeShaders.h"
#include "Teapot.h"

#ifdef __cplusplus
extern "C" {
#endif

// Textures:
int textureCount = 0;
Texture** textures = 0;

// OpenGL ES 2.0 specific:
#ifdef USE_OPENGL_ES_2_0
unsigned int shaderProgramID = 0;
GLint vertexHandle = 0;
GLint normalHandle = 0;
GLint textureCoordHandle = 0;
GLint mvpMatrixHandle = 0;
GLint texSampler2DHandle = 0;
#endif

// Screen dimensions:
unsigned int screenWidth = 0;
unsigned int screenHeight = 0;

// Indicates whether screen is in portrait (true) or landscape (false) mode
bool isActivityInPortraitMode = false;

// The projection matrix used for rendering virtual objects:
QCAR::Matrix44F projectionMatrix;

// Constants:
static const float kObjectScale = 3.f;

QCAR::DataSet* dataSetStonesAndChips = 0;
QCAR::DataSet* dataSetTarmac = 0;

bool switchDataSetAsap = false;

// Object to receive update callbacks from QCAR SDK
class ImageTargets_UpdateCallback: public QCAR::UpdateCallback {
	virtual void QCAR_onUpdate(QCAR::State& /*state*/) {
		if (switchDataSetAsap) {
			switchDataSetAsap = false;

			// Get the image tracker:
			QCAR::TrackerManager& trackerManager =
					QCAR::TrackerManager::getInstance();
			QCAR::ImageTracker* imageTracker =
					static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(
							QCAR::Tracker::IMAGE_TRACKER));
			if (imageTracker == 0 || dataSetStonesAndChips == 0
					|| dataSetTarmac == 0
					|| imageTracker->getActiveDataSet() == 0) {
				LOG("Failed to switch data set.");
				return;
			}

			if (imageTracker->getActiveDataSet() == dataSetStonesAndChips) {
				imageTracker->deactivateDataSet(dataSetStonesAndChips);
				imageTracker->activateDataSet(dataSetTarmac);
			} else {
				imageTracker->deactivateDataSet(dataSetTarmac);
				imageTracker->activateDataSet(dataSetStonesAndChips);
			}
		}
	}
};

ImageTargets_UpdateCallback updateCallback;

JNIEXPORT int JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_getOpenGlEsVersionNative(JNIEnv *, jobject)
{
#ifdef USE_OPENGL_ES_1_1        
	return 1;
#else
	return 2;
#endif
}

//This is the first being called
JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setActivityPortraitMode(JNIEnv *, jobject, jboolean isPortrait)
{
	isActivityInPortraitMode = isPortrait;
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_switchDatasetAsap(JNIEnv *, jobject)
{
	switchDataSetAsap = true;
}

//Segundo que se llama
JNIEXPORT int JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initTracker(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initTracker");

	// Initialize the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* tracker = trackerManager.initTracker(QCAR::Tracker::IMAGE_TRACKER);
	if (tracker == NULL)
	{
		LOG("Failed to initialize ImageTracker.");
		return 0;
	}

	LOG("Successfully initialized ImageTracker.");
	return 1;
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitTracker(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitTracker");

	// Deinit the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	trackerManager.deinitTracker(QCAR::Tracker::IMAGE_TRACKER);
}

//Cuarto que se llama
JNIEXPORT int JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_loadTrackerData(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_loadTrackerData");

	// Get the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
			trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
	if (imageTracker == NULL)
	{
		LOG("Failed to load tracking data set because the ImageTracker has not"
				" been initialized.");
		return 0;
	}

	// Create the data sets:
	dataSetStonesAndChips = imageTracker->createDataSet();
	if (dataSetStonesAndChips == 0)
	{
		LOG("Failed to create a new tracking data.");
		return 0;
	}

	// Load the data sets:
	if (!dataSetStonesAndChips->load("StonesAndChips.xml", QCAR::DataSet::STORAGE_APPRESOURCE))
	{
		LOG("Failed to load data set.");
		return 0;
	}

	// Activate the data set:
	if (!imageTracker->activateDataSet(dataSetStonesAndChips))
	{
		LOG("Failed to activate data set.");
		return 0;
	}

	LOG("Successfully loaded and activated data set.");
	return 1;
}

JNIEXPORT int JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_destroyTrackerData(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_destroyTrackerData");

	// Get the image tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
			trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER));
	if (imageTracker == NULL)
	{
		LOG("Failed to destroy the tracking data set because the ImageTracker has not"
				" been initialized.");
		return 0;
	}

	if (dataSetStonesAndChips != 0)
	{
		if (imageTracker->getActiveDataSet() == dataSetStonesAndChips &&
				!imageTracker->deactivateDataSet(dataSetStonesAndChips))
		{
			LOG("Failed to destroy the tracking data set StonesAndChips because the data set "
					"could not be deactivated.");
			return 0;
		}

		if (!imageTracker->destroyDataSet(dataSetStonesAndChips))
		{
			LOG("Failed to destroy the tracking data set StonesAndChips.");
			return 0;
		}

		LOG("Successfully destroyed the data set StonesAndChips.");
		dataSetStonesAndChips = 0;
	}

	if (dataSetTarmac != 0)
	{
		if (imageTracker->getActiveDataSet() == dataSetTarmac &&
				!imageTracker->deactivateDataSet(dataSetTarmac))
		{
			LOG("Failed to destroy the tracking data set Tarmac because the data set "
					"could not be deactivated.");
			return 0;
		}

		if (!imageTracker->destroyDataSet(dataSetTarmac))
		{
			LOG("Failed to destroy the tracking data set Tarmac.");
			return 0;
		}

		LOG("Successfully destroyed the data set Tarmac.");
		dataSetTarmac = 0;
	}

	return 1;
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_onQCARInitializedNative(JNIEnv *, jobject)
{
	// Register the update callback where we handle the data set swap:
	QCAR::registerCallback(&updateCallback);

	// Comment in to enable tracking of up to 2 targets simultaneously and
	// split the work over multiple frames:
	// QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_renderFrame(JNIEnv *env, jobject obj)
{
	//LOG("Java_com_qualcomm_QCARSamples_ImageTargets_GLRenderer_renderFrame");
	const QCAR::CameraCalibration& cameraCalibration = QCAR::CameraDevice::getInstance().getCameraCalibration();
	QCAR::Vec2F size = cameraCalibration.getSize();
	QCAR::Vec2F focalLength = cameraCalibration.getFocalLength();
	float fovRadians = 2 * atan(0.5f * size.data[1] / focalLength.data[1]);
	float fovDegrees = fovRadians * 180.0f / M_PI;

		// Passing the Modelview matrix up to Java
	jclass activityClass = env->GetObjectClass(obj);
	jmethodID method = env->GetMethodID(activityClass, "updateModelviewMatrix", "([F)V");
	jmethodID projMatMethod = env->GetMethodID(activityClass, "updateProjMatrix", "([F)V");
	jmethodID patternRecognizedMethod = env->GetMethodID(activityClass, "isTracking", "(Z)V");
	jmethodID fovMethod = env->GetMethodID(activityClass, "setFov", "(F)V");
	env->CallVoidMethod(obj, fovMethod, fovRadians);

	//LOG("SIZE: w: %f, h: %f, fov: %f", size.data[0], size.data[1], fovRadians);
	jfloatArray modelviewArray = env->NewFloatArray(16);
	jfloatArray projectionArray = env->NewFloatArray(16);

	// Clear color and depth buffer
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	// Get the state from QCAR and mark the beginning of a rendering section
	QCAR::State state = QCAR::Renderer::getInstance().begin();

	// Explicitly render the Video Background
	QCAR::Renderer::getInstance().drawVideoBackground();

	//glEnable(GL_DEPTH_TEST);

	// We must detect if background reflection is active and adjust the culling direction.
	// If the reflection is active, this means the post matrix has been reflected as well,
	// therefore standard counter clockwise face culling will result in "inside out" models.
	//glEnable(GL_CULL_FACE);
	//glCullFace(GL_BACK);
	/*if(QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON)
	glFrontFace(GL_CW);//Front camera
	else
	glFrontFace(GL_CCW);//Back camera*/

	if(state.getNumTrackableResults()==0){
		env->CallVoidMethod(obj, patternRecognizedMethod, false);
	}
	// Did we find any trackables this frame?
	for(int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
	{
		// Get the trackable:
		const QCAR::TrackableResult* result = state.getTrackableResult(tIdx);
		const QCAR::Trackable& trackable = result->getTrackable();
		QCAR::Matrix44F modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(result->getPose());


/*



#ifdef USE_OPENGL_ES_1_1
		// Load projection matrix:
		glMatrixMode(GL_PROJECTION);
		glLoadMatrixf(projectionMatrix.data);

		// Load model view matrix:
		glMatrixMode(GL_MODELVIEW);
		glLoadMatrixf(modelViewMatrix.data);
		glTranslatef(0.f, 0.f, kObjectScale);
		glScalef(kObjectScale, kObjectScale, kObjectScale);

#else
		//En la 2.0 lo hacen todo v�a shaders, as� que calculan la matriz modelviewProjection
		QCAR::Matrix44F modelViewProjection;

		SampleUtils::translatePoseMatrix(0.0f, 0.0f, kObjectScale, &modelViewMatrix.data[0]);
		SampleUtils::scalePoseMatrix(kObjectScale, kObjectScale, kObjectScale, &modelViewMatrix.data[0]);
		SampleUtils::multiplyMatrix(&projectionMatrix.data[0], &modelViewMatrix.data[0] , &modelViewProjection.data[0]);

		SampleUtils::checkGlError("ImageTargets renderFrame");
#endif*/
		//env->CallVoidMethod(obj, patternRecognizedMethod, true);
		SampleUtils::rotatePoseMatrix(180.0f, 1.0f, 0, 0, &modelViewMatrix.data[0]);

		SampleUtils::rotatePoseMatrix(180.0f, 1.0f, 0, 0, &projectionMatrix.data[0]);
		// Passing the ModelView matrix up to Java (cont.)
		env->SetFloatArrayRegion(modelviewArray, 0, 16, modelViewMatrix.data);
		env->SetFloatArrayRegion(projectionArray, 0, 16, projectionMatrix.data);
		env->CallVoidMethod(obj, method, modelviewArray);
		env->CallVoidMethod(obj, projMatMethod, projectionArray);
	}
	
	env->DeleteLocalRef(modelviewArray);


	QCAR::Renderer::getInstance().end();
}

void configureVideoBackground() {
	// Get the default video mode:
	QCAR::CameraDevice& cameraDevice = QCAR::CameraDevice::getInstance();
	QCAR::VideoMode videoMode = cameraDevice.getVideoMode(
			QCAR::CameraDevice::MODE_DEFAULT);

	// Configure the video background
	QCAR::VideoBackgroundConfig config;
	config.mEnabled = true;
	config.mSynchronous = true;
	config.mPosition.data[0] = 0.0f;
	config.mPosition.data[1] = 0.0f;

	if (isActivityInPortraitMode) {
		//LOG("configureVideoBackground PORTRAIT");
		config.mSize.data[0] = videoMode.mHeight
				* (screenHeight / (float) videoMode.mWidth);
		config.mSize.data[1] = screenHeight;

		if (config.mSize.data[0] < screenWidth) {
			LOG(
					"Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
			config.mSize.data[0] = screenWidth;
			config.mSize.data[1] = screenWidth
					* (videoMode.mWidth / (float) videoMode.mHeight);
		}
	} else {
		//LOG("configureVideoBackground LANDSCAPE");
		config.mSize.data[0] = screenWidth;
		config.mSize.data[1] = videoMode.mHeight
				* (screenWidth / (float) videoMode.mWidth);

		if (config.mSize.data[1] < screenHeight) {
			LOG(
					"Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
			config.mSize.data[0] = screenHeight
					* (videoMode.mWidth / (float) videoMode.mHeight);
			config.mSize.data[1] = screenHeight;
		}
	}

	LOG(
			"Configure Video Background : Video (%d,%d), Screen (%d,%d), mSize (%d,%d)", videoMode.mWidth, videoMode.mHeight, screenWidth, screenHeight, config.mSize.data[0], config.mSize.data[1]);

	// Set the config:
	QCAR::Renderer::getInstance().setVideoBackgroundConfig(config);
}

//Tercero que se llama
JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative(JNIEnv* env, jobject obj, jint width, jint height)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative");

	// Store screen dimensions
	screenWidth = width;
	screenHeight = height;

	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_initApplicationNative finished");
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitApplicationNative( JNIEnv* env, jobject obj)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_deinitApplicationNative");

	// Release texture resources
	if (textures != 0)
	{
		for (int i = 0; i < textureCount; ++i)
		{
			delete textures[i];
			textures[i] = NULL;
		}

		delete[]textures;
		textures = NULL;

		textureCount = 0;
	}
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_startCamera(JNIEnv *env, jobject obj)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_startCamera");

	// Select the camera to open, set this to QCAR::CameraDevice::CAMERA_FRONT
	// to activate the front camera instead.
	QCAR::CameraDevice::CAMERA camera = QCAR::CameraDevice::CAMERA_DEFAULT;

	// Initialize the camera:
	if (!QCAR::CameraDevice::getInstance().init(camera))
	return;

	// Configure the video background
	configureVideoBackground();

	// Select the default mode:
	if (!QCAR::CameraDevice::getInstance().selectVideoMode(
					QCAR::CameraDevice::MODE_DEFAULT))
	return;
	QCAR::VideoBackgroundConfig config = QCAR::Renderer::getInstance().getVideoBackgroundConfig();
	jclass activityClass = env->GetObjectClass(obj);
	jmethodID sceensizeMethod = env->GetMethodID(activityClass, "setSize", "(FF)V");

	env->CallVoidMethod(obj, sceensizeMethod, config.mSize.data[0], config.mSize.data[1]);


	// Start the camera:
	if (!QCAR::CameraDevice::getInstance().start())
	return;

	// Uncomment to enable flash
	//if(QCAR::CameraDevice::getInstance().setFlashTorchMode(true))
	//	LOG("IMAGE TARGETS : enabled torch");

	// Uncomment to enable infinity focus mode, or any other supported focus mode
	// See CameraDevice.h for supported focus modes
	//if(QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_INFINITY))
	//	LOG("IMAGE TARGETS : enabled infinity focus");

	// Start the tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
	if(imageTracker != 0)
	imageTracker->start();
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_stopCamera(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_stopCamera");

	// Stop the tracker:
	QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
	QCAR::Tracker* imageTracker = trackerManager.getTracker(QCAR::Tracker::IMAGE_TRACKER);
	if(imageTracker != 0)
	imageTracker->stop();

	QCAR::CameraDevice::getInstance().stop();
	QCAR::CameraDevice::getInstance().deinit();
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setProjectionMatrix(JNIEnv *, jobject)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setProjectionMatrix");

	// Cache the projection matrix:
	const QCAR::CameraCalibration& cameraCalibration = QCAR::CameraDevice::getInstance().getCameraCalibration();
	projectionMatrix = QCAR::Tool::getProjectionGL(cameraCalibration, 2.0f, 2500.0f);
}

// ----------------------------------------------------------------------------
// Activates Camera Flash
// ----------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_activateFlash(
		JNIEnv*, jobject, jboolean flash) {
	return QCAR::CameraDevice::getInstance().setFlashTorchMode(
			(flash == JNI_TRUE)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_autofocus(
		JNIEnv*, jobject) {
	return QCAR::CameraDevice::getInstance().setFocusMode(
			QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargets_setFocusMode(
		JNIEnv*, jobject, jint mode) {
	int qcarFocusMode;

	switch ((int) mode) {
	case 0:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_NORMAL;
		break;

	case 1:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO;
		break;

	case 2:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_INFINITY;
		break;

	case 3:
		qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_MACRO;
		break;

	default:
		return JNI_FALSE;
	}

	return QCAR::CameraDevice::getInstance().setFocusMode(qcarFocusMode) ?
			JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_initRendering( JNIEnv* env, jobject obj)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_initRendering");

	// Define clear color
	glClearColor(0.0f, 0.0f, 0.0f, QCAR::requiresAlpha() ? 0.0f : 1.0f);

}

JNIEXPORT void JNICALL Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_updateRendering( JNIEnv* env, jobject obj, jint width, jint height)
{
	LOG("Java_com_qualcomm_QCARSamples_ImageTargets_ImageTargetsRenderer_updateRendering");

	// Update screen dimensions
	screenWidth = width;
	screenHeight = height;

	// Reconfigure the video background
	configureVideoBackground();
}

#ifdef __cplusplus
}
#endif
