package com.example.phonecallingsample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {
    private TelephonyManager mTelephonyManager;
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
    private MyPhoneCallListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (isTelephonyEnabled()) {
            checkForPhonePermission();
            mListener = new MyPhoneCallListener();
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
            Log.d(TAG, String.valueOf(R.string.telephony_enabled));
        } else {
            Toast.makeText(this,
                    R.string.telephony_enabled,
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, getString(R.string.telephony_not_enabled));
            // Disable the call button
            disableCallButton();
        }
    }


    private boolean isTelephonyEnabled() {
        if (mTelephonyManager != null) {
            if (mTelephonyManager.getSimState() ==
                    TelephonyManager.SIM_STATE_READY) {
                return true;
            }
        }
        return false;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTelephonyEnabled()) {
            mTelephonyManager.listen(mListener,
                    PhoneStateListener.LISTEN_NONE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {
                if (permissions[0].equalsIgnoreCase
                        (Manifest.permission.CALL_PHONE)
                        && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                } else {
                    // Permission denied.
                    Log.d(TAG, getString(R.string.failure_permission));
                    Toast.makeText(this,
                            R.string.failure_permission,
                            Toast.LENGTH_LONG).show();
                    // Disable the call button
                    disableCallButton();
                }
            }
        }
    }

    private void disableCallButton() {
        Toast.makeText(this,
                R.string.phone_disabled, Toast.LENGTH_LONG).show();
        ImageButton callButton = (ImageButton) findViewById(R.id.phone_icon);
        callButton.setVisibility(View.INVISIBLE);
        if (isTelephonyEnabled()) {
            Button retryButton = (Button) findViewById(R.id.button_retry);
            retryButton.setVisibility(View.VISIBLE);
        }
    }

    private void enableCallButton() {
        ImageButton callButton = (ImageButton) findViewById(R.id.phone_icon);
        callButton.setVisibility(View.VISIBLE);
    }

    public void retryApp(View view) {
        enableCallButton();
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName());
        startActivity(intent);
    }

    private void checkForPhonePermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) !=
                PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, getString(R.string.permission_not_granted));
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
        } else {
            // Permission already granted. Enable the call button.
            enableCallButton();
        }
    }

    public void callNumber() {
        EditText editText = (EditText) findViewById(R.id.editText_main);
        // Use format with "tel:" and phone number to create phoneNumber.
        String phoneNumber = String.format("tel: %s",
                editText.getText().toString());
        // Log the concatenated phone number for dialing.
        Log.d(TAG, getString(R.string.dial_number) + phoneNumber);
        Toast.makeText(this,
                getString(R.string.dial_number) + phoneNumber,
                Toast.LENGTH_LONG).show();
        // Create the intent.
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        // Set the data for the intent as the phone number.
        callIntent.setData(Uri.parse(phoneNumber));
        // If package resolves to an app, send intent.
        if (callIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(callIntent);
        } else {
            Log.e(TAG, "Can't resolve app for ACTION_CALL Intent.");
        }
    }

    private class MyPhoneCallListener extends PhoneStateListener {
        private boolean returningFromOffHook = false;
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            String message = getString(R.string.phone_status);
            switch (state) {

                case TelephonyManager.CALL_STATE_RINGING:
                    // Incoming call is ringing (not used for outgoing call).
                    message = message +
                            getString(R.string.ringing) + incomingNumber;
                    Toast.makeText(MainActivity.this, message,
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Phone call is active -- off the hook.
                    message = message + getString(R.string.offhook);
                    Toast.makeText(MainActivity.this, message,
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    returningFromOffHook = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // Phone is idle before and after phone call.
                    // If running on version older than 19 (KitKat),
                    // restart activity when phone call ends.
                    message = message + getString(R.string.idle);
                    Toast.makeText(MainActivity.this, message,
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    if (returningFromOffHook) {
                        // No need to do anything if >= version KitKat.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            Log.i(TAG, getString(R.string.restarting_app));
                            // Restart the app.
                            PackageManager pm = getPackageManager();
                            getPackageName();
                            Intent intent = pm.getLaunchIntentForPackage(getPackageName());
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                    break;
                default:
                    message = message + "Phone off";
                    Toast.makeText(MainActivity.this, message,
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, message);
                    break;
            }
        }
    }

}