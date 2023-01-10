package de.thu.paulni.countrycompass;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.List;

// TODO:
// 1. Add small arrow above compass
// 2. Show hint to the user when wrong
// 3. Use user's location
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    CCModel model;
    CCView view;
    Country selectedCountry;
    SensorManager sman;
    Sensor lightSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        view = new CCView(this);
        model = new CCModel(this, view);

        sman = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sman.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() == 0) { finish(); return; }
        lightSensor = sensors.get(0);

        handleCountrySelection();

        // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        Log.d("REQ", "Granted");
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // feature requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        Log.d("REQ", "Denied");
                    }
                });

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            //performAction(...);
            Log.d("REQ", "Got the permission already!");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                //showInContextUI(...);
                Log.d("REQ", "Need to show rationale!");
            } else {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_COARSE_LOCATION);
                Log.d("REQ", "Got the permission without rationale!");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sman.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sman.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        model.rotateCompass(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void handleCountrySelection() {
        selectedCountry = model.chooseCountry();
        view.displayCountry(selectedCountry.getName());
    }

    public void onDirectionSelected(View button) {
        if(selectedCountry == null) return;
        double compassRotation = -view.getCompassImgRotation();
        boolean countryFound = model.process(compassRotation, selectedCountry.getLocation());
        if(countryFound)
            handleCountrySelection();
    }
}