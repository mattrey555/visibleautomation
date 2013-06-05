package com.androidApp.Intercept;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.androidApp.EventRecorder.EventRecorder;
import com.androidApp.Test.ViewInterceptor;
import com.androidApp.Utility.Constants;

import android.util.Log;
import android.view.KeyEvent;

/**
 * listen for up/down messages from our custom IME.
 * @author matthew
 * Copyright (c) 2013 Matthew Reynolds.  All Rights Reserved.
 */
public class IMEMessageListener implements Runnable {
	public static final String 	TAG = "IMEMessageListener";
	public static final int 	IME_BROADCAST_PORT = 49152;
	public static final int 	CONNECT_TIMEOUT = 10000;
	public static final int 	READ_TIMEOUT = 1000;
	public static final int 	MAXMSG = 1024;
	public static final String 	HIDE_IME = "hide_ime";
	public static final String 	SHOW_IME = "show_ime";
	protected static boolean 	sfTerminate;					// terminate loop flag.
	protected boolean 			mfKeyboardVisible = false;		// flag for IME visibility to others may ask
	protected ViewInterceptor	mViewInterceptor;				// maintains currently focused view.
	protected EventRecorder		mEventRecorder;					// to record IME hide/show events.
	
	public IMEMessageListener(ViewInterceptor viewInterceptor, EventRecorder eventRecorder) {
		sfTerminate = false;
		mViewInterceptor = viewInterceptor;
		mEventRecorder = eventRecorder;
	}
	
	public boolean isKeyboardVisible() {
		return mfKeyboardVisible;
	}
	
	/** 
	 * terminate this thread (called when the activity monitor stack is empty
	 */
	public static void terminate() {
		sfTerminate = true;
	}
	
	/**
	 * background thread to read the socket and see if the IME transmitted show or hide IME messages. 
	 */
	public void run() {
		try {
			byte[] buffer = new byte[MAXMSG];
			Socket socket = new Socket(InetAddress.getLocalHost(), IME_BROADCAST_PORT);
			SocketAddress sockAddr = new InetSocketAddress(InetAddress.getLocalHost(), IME_BROADCAST_PORT);
			socket.setSoTimeout(READ_TIMEOUT);
			InputStream is = socket.getInputStream();
			while (!sfTerminate) {
				boolean fSuccessfulRead = true;
				int numBytes = 0;
				try {
					numBytes = is.read(buffer);
				} catch (Exception ex) {
					fSuccessfulRead = false;
				}
				if (fSuccessfulRead) {
					String msg = new String(buffer);
					msg = msg.substring(0, numBytes);
					mfKeyboardVisible = msg.equals(SHOW_IME);
					if (mfKeyboardVisible) {
						mEventRecorder.writeRecord(Constants.EventTags.SHOW_IME, mViewInterceptor.getFocusedView(), "IME displayed");
					} else {
		                if (mViewInterceptor.getLastKeyAction() == KeyEvent.KEYCODE_BACK) {
		                    mEventRecorder.writeRecord(Constants.EventTags.HIDE_IME_BACK_KEY, mViewInterceptor.getFocusedView(), "IME hidden by back key pressed");
		                    mViewInterceptor.setLastKeyAction(-1);
		                } else {
		                    mEventRecorder.writeRecord(Constants.EventTags.HIDE_IME, mViewInterceptor.getFocusedView(), "IME hidden");
		                }
					}
				}
					
			}
			
		} catch (Exception ex) {
			Log.e(TAG, "an error occurred while trying to connect to the SoftKeyboard TCP listener " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}