package com.example.screentimeusabilitydemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AppOpsManager;
import android.app.UiModeManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    TextView wifiData;
    private Handler wifiHandler;
    private Intent overlay;
    private boolean isOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SeekBar brightnessBar = (SeekBar) findViewById(R.id.brightnessBar);
        SeekBar volumeBar = (SeekBar) findViewById(R.id.volumeBar);
        SeekBar animationBar = (SeekBar) findViewById(R.id.animiationBar);
        Switch darkModeSW = (Switch) findViewById(R.id.darkModeSW);
        Switch colorModeSW = (Switch) findViewById(R.id.colorModeSW);

        wifiData = (TextView) findViewById(R.id.wifiData);
        overlay = new Intent(this, ForegroundService.class);
        isOverlay = false;

        brightnessBar.setKeyProgressIncrement(1);
        volumeBar.setKeyProgressIncrement(1);
        animationBar.setKeyProgressIncrement(1);

        brightnessBar.setProgress(getCurrentBrightness(MainActivity.this));
        volumeBar.setProgress(getCurrentVolume());

        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse(
                    "package:" + getPackageName()
            )));
        }

        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("Brightness", String.format("%d", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("Brightness", String.format("%d", seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("Brightness", String.format("Final %d", seekBar.getProgress()));
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.System.canWrite(getApplicationContext())) {
                        Handler handler = new Handler();
                        handler.post(() -> {
                           changeBrightness(MainActivity.this,
                                   Math.round(seekBar.getProgress()));
                           toast("Current Brightness: "
                                   + getCurrentBrightness(MainActivity.this));
                        });
                    } else {
                        getSystemWritingPermission();
                    }
                }
            }
        });

        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("Volume", String.format("%d", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("Volume", String.format("%d", seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("Volume", String.format("Final %d", seekBar.getProgress()));
                changeVolume((float) (seekBar.getProgress() / 100.0));
                toast("Volume change to " + seekBar.getProgress());
            }
        });

        darkModeSW.setOnCheckedChangeListener((compoundButton, b) -> {
            UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
//            uiModeManager.enableCarMode(0);
            if (b) {
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                toast("Night mode enabled");
            } else {
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
                toast("Night mode disabled");
            }
        });

        colorModeSW.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                Settings.Secure.putString(getContentResolver(),
                        "accessibility_display_daltonizer_enabled", "1");
                Settings.Secure.putString(getContentResolver(),
                        "accessibility_display_daltonizer", "0");
                toast("Grayscale enabled");
            } else {
                Settings.Secure.putString(getContentResolver(),
                        "accessibility_display_daltonizer_enabled", "0");
                Settings.Secure.putString(getContentResolver(),
                        "accessibility_display_daltonizer", "-1");
                toast("Grayscale disabled");
            }
        });

        animationBar.setProgress(getCurrentAnimationScale(this));
        animationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("Animation scale", String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("Animation scale", String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("Animation scale", String.valueOf(seekBar.getProgress()));
                changeAnimationScale(MainActivity.this, seekBar.getProgress());
            }
        });

        // Start wifi usage monitoring
        wifiHandler = new Handler();
        startCheckingWifiUsage();

        // start overlay
        checkOverlayPermission();

        Button blurringWindowbtn = (Button) findViewById(R.id.blurringWindowbtn);
        blurringWindowbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOverlay();
            }
        });
        Button exitBlurring = (Button) findViewById(R.id.exitBlurringBtn);
        exitBlurring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopOverlay();
            }
        });

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Button vibrateBtn = (Button) findViewById(R.id.vibrateBtn);
        vibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    vibrator.vibrate(500);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("APP", "destoryed");
        stopCheckingWifiUsage();

    }

    // check for permission again when user grants it from
    // the device settings, and start the service
//    @Override
//    protected void onResume() {
//        super.onResume();
//        startOverlay();
//    }


    Runnable checkWifi = new Runnable() {
        @Override
        public void run() {
            try {
                NetworkStatsManager networkStatsManager =
                        (NetworkStatsManager) getSystemService(
                                Context.NETWORK_STATS_SERVICE);
                NetworkStats.Bucket bucket = networkStatsManager.querySummaryForDevice(
                        ConnectivityManager.TYPE_WIFI, "",
                        System.currentTimeMillis() - 100, System.currentTimeMillis());
                wifiData.setText(String.format("Bytes: %d", bucket.getRxBytes()));
                Log.d("Receive", String.format("%d", bucket.getRxBytes()));

            }
            catch (RemoteException re) {
                Log.d("networkStatsManager", "Remote exception");
            }
            wifiHandler.postDelayed(checkWifi, 10000);
        }
    };

    private void startCheckingWifiUsage() {
//        new Thread(checkWifi).start();
        checkWifi.run();
    }

    private void stopCheckingWifiUsage() {
        wifiHandler.removeCallbacks(checkWifi);
    }

    // ask for writing permission
    private void getSystemWritingPermission() {
        if (!Settings.System.canWrite(MainActivity.this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Permission Request");
            builder.setMessage("System write permission is required to adjust the brightness");

            builder.setNegativeButton(android.R.string.no,
                    (dialog, which) -> toast("Permission denied"));
            builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse(
                    "package:" + getPackageName()
                ));
                startActivity(intent);
            });
            builder.setCancelable(false);
            builder.show();
        }
    }

    private void toast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private int getCurrentBrightness(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 100);
    }

    private void changeBrightness(Context context, int val) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int brightnessMode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, val);
    }

    private int getCurrentVolume() {
        AudioManager audioManager =(AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void changeVolume(float volume) {
        AudioManager audioManager =(AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (!audioManager.isVolumeFixed()) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    Math.round(volume * maxVolume), AudioManager.FLAG_SHOW_UI);
        }
    }

    private int getCurrentAnimationScale(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void changeAnimationScale(Context context, int scale) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, scale);
        toast("Change animation scale to " + scale);
    }

    public void checkOverlayPermission(){

        if (!Settings.canDrawOverlays(this)) {
            // send user to the device settings
            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(myIntent);
        }
    }

    public void startOverlay(){
        // check if the user has already granted
        // the Draw over other apps permission
        if(Settings.canDrawOverlays(this)) {
            // start the service based on the android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlay);
            } else {
                startService(overlay);
            }
            isOverlay = true;
        }
    }

    public void stopOverlay() {
//        if (isOverlay) {
//            isOverlay = false;
            stopService(overlay);
//        }
    }
}