package com.siecola.exemploeandroid.gcm;

import java.util.Set;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.siecola.exemploeandroid.MainActivity;
import com.siecola.exemploeandroid.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	private Context context;
	private NotificationManager mNotificationManager;
	public static final int NOTIFICATION_ID = 1;

	@Override
	public void onReceive(Context context, Intent intent) {

		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		this.context = context;
		String messageType = gcm.getMessageType(intent);

		if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
			sendNotificationError ("Send error: " + intent.getExtras().toString());
		} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
				.equals(messageType)) {
			sendNotificationError ("Deleted messages on server: " + intent.getExtras().toString());
		} else {
			Bundle extras = intent.getExtras();
			
//			String key = extras.keySet().iterator().next();		
//			sendNotification (key, extras.getString(key));
			Set<String> keySet = extras.keySet();
			for (String key : keySet) {
				if (!key.equals("from")) {
					sendNotification (key, extras.getString(key));
					break;
				}
			}
		}

		setResultCode(Activity.RESULT_OK);
	}

	// Put the GCM message into a notification and post it.
	private void sendNotification(String key, String payload) {
		mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra("messageRX", "Key: " + key + "\r\nPayload:\r\n" + payload);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder mBuilder = new Notification.Builder(
				context)
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.ic_stat_gcm)
				.setContentTitle("Siecola Exempolo GCM")
				.setStyle(new Notification.BigTextStyle().bigText(key))
				.setContentText(key);

		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
	
	private void sendNotificationError (String errorMessage) {
		Notification.Builder mBuilder = new Notification.Builder(
				context)
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.ic_stat_gcm)
				.setContentTitle("Siecola Exempolo GCM")
				.setStyle(new Notification.BigTextStyle().bigText("Error"))
				.setContentText("Error");

		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());		
	}
}
