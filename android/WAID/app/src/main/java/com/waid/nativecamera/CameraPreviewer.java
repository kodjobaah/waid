package com.waid.nativecamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.LinearLayout;

import com.waid.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.ListItemAccessor;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.NativeCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import java.util.List;

//import org.opencv.android.NativeCameraView.OpenCvSizeAccessor;
//import org.opencv.videoio.VideoCapture;


public class CameraPreviewer extends Activity {

	private static final String TAG = "CameraPreviwer";
	GLSurfaceView mView;

	/*

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
					System.exit(-1);
		} else {
			System.loadLibrary("NativeCamera");
		}
	}
	*/

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
		} else {
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

		Native.loadlibs();
		VideoCapture mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
		//VideoCapture mCamera = new VideoCapture();
		java.util.List<Size> sizes = mCamera.getSupportedPreviewSizes();
		mCamera.release();
		
		mView = new GLSurfaceView(getApplication()) {
			@Override
			public void onPause() {
				super.onPause();
				Native.releaseCamera();
			}
			
		};
		
		
		Size  size = calculateCameraFrameSize(sizes,new NativeCameraView.OpenCvSizeAccessor());
		mView.setRenderer(new CameraRenderer(this, size));
		mView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		//setContentView(mView);
		setContentView(R.layout.test);
		LinearLayout surface = (LinearLayout)findViewById(R.id.kodjo);
		surface.addView(mView);
	}
	
	protected Size calculateCameraFrameSize(List<Size> supportedSizes, ListItemAccessor accessor) {
		
		int calcWidth = Integer.MAX_VALUE;
		int calcHeight = Integer.MAX_VALUE;
		
		Display display = getWindowManager().getDefaultDisplay();
		
		int maxAllowedWidth = 1024;
		int maxAllowedHeight = 1024;
		
		for(Object size: supportedSizes){
			int width = accessor.getWidth(size);
			int height = accessor.getHeight(size);
			
			if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
				if (width <= calcWidth 
						&& width >= (maxAllowedWidth /2)
						&& (display.getWidth()% width == 0 || display.getHeight()%height ==0)){
					calcWidth= (int)width;
					calcHeight = (int)height;
				}
			}
			
		}
		return new Size(calcWidth, calcHeight);
	}
	
	@Override
	protected void onPause() {
        super.onPause();
        mView.onPause();
        
    }
 
    @Override
	protected void onResume() {
        super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        mView.onResume();
         
    }


	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
					//                   mOpenCvCameraView.enableFpsMeter();
//                    mOpenCvCameraView.enableView();
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
		}
	};

}