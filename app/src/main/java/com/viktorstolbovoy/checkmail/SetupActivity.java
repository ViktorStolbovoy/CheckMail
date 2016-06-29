package com.viktorstolbovoy.checkmail;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


public class SetupActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "CheckMailSetup";
    public static final String EMAIL_SETTING = "email";
    public static final String COLOR_SETTING = "color";
    public static final String SERVER_SETTING = "server";
    public static final String LOGIN_SETTING = "login";
    public static final String PWD_SETTING = "password";

    public static final String VIBRATE_SETTING = "vibrate";
    public static final String N_AREA_SETTING = "narea";

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if(extras == null) {
            //There should be extras
            finish();
            return;
        }

        setContentView(R.layout.activity_setup);

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME,  MODE_PRIVATE);
        final EditText emailField = (EditText) findViewById(R.id.edit_emailList);
        final RadioGroup rbGrp = (RadioGroup) findViewById(R.id.radioGroupColor);

        String prevLogin = settings.getString(LOGIN_SETTING, "");
        String prevEmail = "";
        if (prevLogin.equalsIgnoreCase(extras.getString(LOGIN_SETTING))) {
            prevEmail = settings.getString(EMAIL_SETTING, "");
        }

        final CheckBox cbxVibrate = (CheckBox) findViewById(R.id.cbxVibrate);
        final CheckBox cbxShowInToolbar = (CheckBox) findViewById(R.id.cbxShowInToolbar);

        cbxVibrate.setChecked(settings.getBoolean(VIBRATE_SETTING, false));
        cbxShowInToolbar.setChecked(settings.getBoolean(N_AREA_SETTING, true));


        emailField.setText(prevEmail);

        emailField.setError(null);

        switch(settings.getInt(COLOR_SETTING, 0)){
            case 0xFF00FFFF:
                rbGrp.check(R.id.radioButtonCyan);
                break;
            case 0xFFFF7F7F:
                rbGrp.check(R.id.radioButtonPink);
                break;
            case 0xFF7FFF7F:
                rbGrp.check(R.id.radioButtonSalad);
                break;
            case 0xFFFFFF00:
                rbGrp.check(R.id.radioButtonYellow);
                break;
            default:
                rbGrp.check(R.id.radioButtonNone);
                break;
        }

        Button saveButton = (Button) findViewById(R.id.buttonSaveSettings);
        final Context ctx = this;
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = emailField.getText().toString();
                try {
                    new InternetAddress(email);
                    emailField.setError(null);
                }
                catch(AddressException err) {
                    emailField.setError(getString(R.string.error_invalid_email));
                    emailField.requestFocus();
                    return;
                }

                int color = 0xFFFFFFFF;
                switch(rbGrp.getCheckedRadioButtonId()) {
                    case R.id.radioButtonCyan:
                        color = 0xFF00FFFF;
                        break;
                    case R.id.radioButtonPink:
                        color = 0xFFFF7F7F;
                        break;
                    case R.id.radioButtonSalad:
                        color = 0xFF7FFF7F;
                        break;
                    case R.id.radioButtonYellow:
                        color = 0xFFFFFF00;
                        break;
                    case R.id.radioButtonNone:
                        color = 0;
                        break;
                }


                // We need an Editor object to make preference changes.
                // All objects are from android.context.Context
                SharedPreferences.Editor editor = settings.edit();
                for (String key :  new String[]{SERVER_SETTING, LOGIN_SETTING, PWD_SETTING}) {
                    editor.putString(key, extras.getString(key));
                }

                editor.putInt(COLOR_SETTING, color);

                editor.putString(EMAIL_SETTING, email);

                Boolean shouldVibrate = cbxVibrate.isChecked();
                Boolean shouldShowInToolbar = cbxShowInToolbar.isChecked();

                editor.putBoolean(VIBRATE_SETTING, shouldVibrate);
                editor.putBoolean(N_AREA_SETTING, shouldShowInToolbar);

                // Commit the edits!
                editor.commit();

                if (color == 0 && !shouldShowInToolbar && !shouldVibrate) {
                    CancelSchedule(ctx);
                    AlarmReceiver.cancelNotification((NotificationManager) ctx.getSystemService( ctx.NOTIFICATION_SERVICE ));
                }
                else {
                    SetSchedule(ctx);
                }

                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.buttonCancelSettings);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public final static int ALARM_REQUEST_CODE = 234423;

    final static void SetSchedule(Context ctx) {
        AlarmManager manager = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);
        int interval = 60000;

        Intent alarmIntent = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx.getApplicationContext(), ALARM_REQUEST_CODE, alarmIntent, 0);

        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, interval, pendingIntent);
        Toast.makeText(ctx, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    public final static void CancelSchedule(Context ctx) {
        Intent alarmIntent = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx.getApplicationContext(), ALARM_REQUEST_CODE, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
