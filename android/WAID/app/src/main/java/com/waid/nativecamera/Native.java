package com.waid.nativecamera;

public class Native {

	public static void loadlibs() {

		/*
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            System.exit(-1);
        } else {

            System.loadLibrary("NativeCamera");
        }

		*/
		System.loadLibrary("NativeCamera");
	}
	
	public static native void initCamera(int width, int height);
	public static native void releaseCamera();
	public static native void renderBackground();
	public static native void surfaceChanged(int width, int height, int orientation);
}
