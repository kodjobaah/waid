package com.waid.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.os.Build;
import android.util.Log;

public class HttpConnectionHelper {

	private String TAG = HttpConnectionHelper.class.getName();
	private static final String COOKIES_HEADER = "Set-Cookie";
	private static final int CONNECTION_TIMEOUT = 10000;
	private static final int DATARETRIEVAL_TIMEOUT = 10000;
	private HttpURLConnection urlConnection = null;
	
	public ConnectionResult connect(String urlVal) {
		
	
		disableConnectionReuseIfNecessary();
		ConnectionResult connectionResult = null;
		try {
			URL url = new URL(urlVal);
			urlConnection = (HttpURLConnection) url.openConnection();

			urlConnection.setRequestProperty("Accept-Encoding", "identity");
			urlConnection.setDoOutput(true);
			urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
			urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			String line = reader.readLine();
			String result = line;
			while ((line = reader.readLine()) != null) {
				result += line;
			}
			int statusCode = urlConnection.getResponseCode();
			Log.i(TAG, "status code[" + statusCode + "]");
			connectionResult = new ConnectionResult(result, statusCode);
		} catch (java.io.FileNotFoundException fne) {
			int statusCode = 0;
			try {
				statusCode = urlConnection.getResponseCode();
				connectionResult = new ConnectionResult(fne.getMessage(), statusCode);
				Log.i(TAG, "status code[" + statusCode + "]");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.e(TAG,"HttpConnectionHelper could not connecct");
			e.printStackTrace();
		}
		return connectionResult;
	}
	
	public ConnectionResult connectOther(String urlVal) {
		
		
		disableConnectionReuseIfNecessary();
		ConnectionResult connectionResult = null;
		try {
			URL url = new URL(urlVal);
			urlConnection = (HttpURLConnection) url.openConnection();

			urlConnection.setRequestProperty("Accept-Encoding", "identity");
			urlConnection.setDoOutput(false);
			urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
			urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			String line = reader.readLine();
			String result = line;
			while ((line = reader.readLine()) != null) {
				result += line;
			}
			int statusCode = urlConnection.getResponseCode();
			Log.i(TAG,"status code["+statusCode+"]");
			connectionResult = new ConnectionResult(result, statusCode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connectionResult;
	}
	
	
	public SessionParser getPlaySession() {
		Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
		List<String> cookiesHeader = headerFields
				.get(COOKIES_HEADER);

		String cookie = null;
		for (int i = 0; i < cookiesHeader.size(); i++) {
			String val = cookiesHeader.get(i);

			if (val.contains("PLAY_SESSION")) {
				cookie = val;
			}
		}
		
		SessionParser sessionParser = null;
		if (cookie != null) 
			sessionParser = new SessionParser(cookie);
			
		return sessionParser;
		
	}

	public void closeConnection() {

		if (urlConnection != null) {
			this.urlConnection.disconnect();
		}
	}

	public HttpURLConnection getConnection() {
		return urlConnection;
	}

	
	/*
	private void enableHttpResponseCache() {
		try {
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			File httpCacheDir = new File(getCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
		}
	}
*/
	private void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}
}
