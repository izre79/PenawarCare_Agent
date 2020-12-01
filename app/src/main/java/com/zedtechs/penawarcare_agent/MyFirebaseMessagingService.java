package com.zedtechs.penawarcare_agent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    static final String CHANNEL_ID = "penawarcare";
    SharedPreferences sharedpreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Class created");
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        //Log.d(TAG, "Refreshed token: " + token);
        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        sharedpreferences.edit().putString("FCM_TOKEN_ID", token).apply();
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        //sendRegistrationToServer(token);
    }

    /*public  String getToken(Context context) {
       //sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);;
        //return sharedpreferences.getString("FCM_TOKEN_ID", "empty");
    }
*/
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");

            sendNotification(title,message);
        }
        // Check if message contains a notification payload.
        else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(),remoteMessage.getNotification().getBody());
        }

    }

    public void sendNotification(String title, String desc){

        NotificationManager mNotificationManager;

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, HomePageActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.pharmacy_icon)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setContentText(desc)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(desc))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // === Removed some obsoletes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = CHANNEL_ID;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

}
