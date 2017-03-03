package com.viktorstolbovoy.checkmail;

/**
 * Created by Main User on 6/21/2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.app.Notification;
import android.app.NotificationManager;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;

import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class AlarmReceiver extends BroadcastReceiver {
    private static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences settings = ctx.getSharedPreferences(SetupActivity.PREFS_NAME,  Context.MODE_PRIVATE);
        NotificationManager nm = (NotificationManager) ctx.getSystemService( ctx.NOTIFICATION_SERVICE );
        if (isNetworkAvailable(ctx)) {
            CheckMailTask task = new CheckMailTask(settings, nm, ctx);
            task.execute();
        }
    }



    final static int LED_NOTIFICATION_ID = 0;
    public static final void cancelNotification(NotificationManager nm) {
        nm.cancel(LED_NOTIFICATION_ID);
    }

    class CheckMailTask extends AsyncTask<Void, Void, Boolean> {

        private InternetAddress mEmail;
        private final String mLogin;
        private final String mPassword;
        private final String mServer;
        private final int mColor;
        private final Boolean mShouldVibrate;
        private final Boolean mShouldShowInToolbar;
        private final NotificationManager mNotificationManager;
        private final Context mCtx;
        private final SharedPreferences mSettings;
        private Date mLastDate;
        private final String LAST_RECEIVED = "lastRcvd";

        CheckMailTask(SharedPreferences settings, NotificationManager nm, Context ctx) {
            mCtx = ctx;
            mLogin = settings.getString(SetupActivity.LOGIN_SETTING, "");
            mPassword = settings.getString(SetupActivity.PWD_SETTING, "");
            mServer = settings.getString(SetupActivity.SERVER_SETTING, "");
            mShouldVibrate = settings.getBoolean(SetupActivity.VIBRATE_SETTING, false);
            mShouldShowInToolbar = settings.getBoolean(SetupActivity.N_AREA_SETTING, true);
            mLastDate = new Date(settings.getLong(LAST_RECEIVED, 0));
            mSettings = settings;

            try {
                mEmail = new InternetAddress(settings.getString(SetupActivity.EMAIL_SETTING, ""));
            }
            catch (AddressException err){
                mEmail = null;
            }

            mColor =  settings.getInt(SetupActivity.COLOR_SETTING, 0);
            mNotificationManager = nm;
        }

        @Override
        protected Boolean doInBackground(Void... p) {
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");
            Session session = Session.getDefaultInstance(props, null);
            Store store = null;
            try {
                store = session.getStore("imaps");
                store.connect(mServer, mLogin, mPassword);
                Folder inbox = store.getFolder("inbox");
                inbox.open(Folder.READ_ONLY);

                // search for all "unseen" messages
                int unread = 0;
                int total = inbox.getMessageCount();
                final int CHECK_DEPTH = 20;
                final int last = total > CHECK_DEPTH ? total - CHECK_DEPTH : 1;
                for (int i = total; i >= last; i--) {
                    try {
                        Message msg = inbox.getMessage(i);


                        for (Address address : msg.getFrom()) {
                            if (address.equals(mEmail)) {
                                if (!msg.isSet(Flags.Flag.SEEN)) {
                                    //Going from the end, so the last email should come first
                                    Date receivedDate = msg.getReceivedDate();
                                    if (receivedDate == null)
                                        receivedDate = new Date(); //Sorry, you need to actually read the email in this case

                                    if (mLastDate == null || mLastDate.before(receivedDate)) {
                                        mLastDate = receivedDate;
                                        SharedPreferences.Editor editor = mSettings.edit();
                                        editor.putLong(LAST_RECEIVED, mLastDate.getTime());
                                        setNotitication();
                                    }
                                    //else - Notification already there, it might be removed by user, so do nothing.
                                    inbox.close(false);
                                    store.close();
                                    return true;
                                } else {
                                    //The last email is read
                                    cancelNotification();
                                    inbox.close(false);
                                    store.close();
                                    return false;
                                }

                            }
                        }
                    } catch (Exception me) {
                        // This is an expunged message, ignore it.
                        continue;
                    }
                }

                inbox.close(false);
                store.close();

                //No unread emails - remove notification
                cancelNotification();
                return false;
            } catch (Exception e)  {
                //Do nothing
                return false;
            }
        }

        private void setNotitication() {
            Notification.Builder builder = new Notification.Builder(mCtx);
            if (mShouldShowInToolbar) {
                builder.setContentTitle("New message");
                builder.setSmallIcon(R.drawable.ic_notification);
            }
            if (mShouldVibrate) {
                builder.setVibrate(new long[]{500, 500});
            }

            Notification notification = builder.build();
            if (mColor != 0) {
                notification.ledARGB = mColor;
                notification.flags = Notification.FLAG_SHOW_LIGHTS;
                notification.ledOnMS = 70;
                notification.ledOffMS = 3000;
            }


            try {
                mNotificationManager.notify(LED_NOTIFICATION_ID, notification);
            }
            catch (Exception e) {
            }
        }

        public void cancelNotification() {
            AlarmReceiver.cancelNotification(mNotificationManager);
        }

    }
}
