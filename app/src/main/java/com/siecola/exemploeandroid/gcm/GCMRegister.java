package com.siecola.exemploeandroid.gcm;

import java.io.IOException;
import java.sql.Timestamp;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public class GCMRegister {

	private static final String TAG = "GCMRegister";	
	private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";
	private static final String PROPERTY_REG_ID = "registration_id";
	static final String PROPERTY_SENDER_ID = "senderID";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	
	private IOException ioException;

    /**
     * Default lifespan (7 days) of a reservation until it is considered expired.
     */
    private static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;

	private Context context;
	private GoogleCloudMessaging gcm;
	private String regid;
	private String senderID;
	private GCMRegisterEvents gcmRegisterEvents;

	public GCMRegister(Context context) {
		this.context = context;
		this.gcmRegisterEvents = (GCMRegisterEvents)context;		
	}

	public String getRegistrationId(String senderID) {
		this.senderID = senderID;
		setSenderId(senderID);
		regid = getCurrentRegistrationId();

		if (regid.length() == 0) {
			registerBackground();
		}
		return regid;
	}
	
	public void unRegister() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					gcm.unregister();
					clearRegistrationId();
					return true;
				} catch (IOException ex) {
					ioException = ex;
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean unregistered) {
				if (unregistered == true) {
					Log.i(TAG, "Device unregistered");
					gcmRegisterEvents.gcmUnregisterFinished();
				}
				else {
					gcmRegisterEvents.gcmUnregisterFailed(ioException);					
				}
			}
		}.execute(null, null, null);
		
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's shared preferences.
	 */
	private void registerBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					regid = gcm.register(senderID);					
					
					// Save the regid - no need to register again.
					setRegistrationId(regid);
					msg = regid;
				} catch (IOException ex) {
					msg = null;
					ioException = ex;
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String registrationID) {
				if (registrationID != null) {
					Log.i(TAG, "Device registered, registration id=" + registrationID);
					gcmRegisterEvents.gcmRegisterFinished(registrationID);
				}
				else {
					gcmRegisterEvents.gcmRegisterFailed(ioException);					
				}
			}
		}.execute(null, null, null);
	}

	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration id
	 */
	private void setRegistrationId(String regId) {
		final SharedPreferences prefs = getGCMPreferences();
		int appVersion = getAppVersion(context);
		Log.v(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();		
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		long expirationTime = System.currentTimeMillis()
				+ REGISTRATION_EXPIRY_TIME_MS;

		Log.v(TAG, "Setting registration expiry time to "
				+ new Timestamp(expirationTime));
		editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
		editor.commit();
	}
	
	private void setSenderId (String senderId) {
		final SharedPreferences prefs = getGCMPreferences();
		SharedPreferences.Editor editor = prefs.edit();		
		editor.putString(PROPERTY_SENDER_ID, senderId);
		editor.commit();
	}

	private void clearRegistrationId () {
		final SharedPreferences prefs = getGCMPreferences();
		SharedPreferences.Editor editor = prefs.edit();		
		editor.remove(PROPERTY_REG_ID);
		editor.remove(PROPERTY_APP_VERSION);
		editor.remove(PROPERTY_ON_SERVER_EXPIRATION_TIME);
		editor.commit();		
	}

	/**
	 * Gets the current registration id for application on GCM service.
	 * <p>
	 * If result is empty, the registration has failed.
	 * 
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	public String getCurrentRegistrationId() {
		final SharedPreferences prefs = getGCMPreferences();
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.length() == 0) {
			Log.v(TAG, "Registration not found.");
			return "";
		}
		// check if app was updated; if so, it must clear registration id to
		// avoid a race condition if GCM sends a message
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion || isRegistrationExpired()) {
			Log.v(TAG, "App version changed or registration expired.");
			return "";
		}
		return registrationId;
	}
	
	public String getSenderId () {
		final SharedPreferences prefs = getGCMPreferences();
		String senderId = prefs.getString(PROPERTY_SENDER_ID, "");
		if (senderId.length() == 0) {
			return "";
		}
		return senderId;		
	}

	/**
	 * Checks if the registration has expired.
	 * 
	 * <p>
	 * To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to
	 * re-register after REGISTRATION_EXPIRY_TIME_MS.
	 * 
	 * @return true if the registration has expired.
	 */
	public boolean isRegistrationExpired() {
		final SharedPreferences prefs = getGCMPreferences();
		// checks if the information is not stale
		long expirationTime = prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME,
				-1);
		return System.currentTimeMillis() > expirationTime;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences() {
		return context.getSharedPreferences(context.getClass().getSimpleName(),
				Context.MODE_PRIVATE);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
}
