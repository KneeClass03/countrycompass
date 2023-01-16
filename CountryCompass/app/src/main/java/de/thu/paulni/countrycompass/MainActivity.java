package de.thu.paulni.countrycompass;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

// TODO:
// 1. Add small arrow above compass
// 2. Show hint to the user when wrong
// 3. Use user's location
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private CCModel model;
    private CCView view;
    private Country selectedCountry;
    private SensorManager sman;
    private Sensor orientationSensor;
    private GeoPoint userLocation;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MenuItem selectedDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        //Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        view = new CCView(this);
        model = new CCModel(this, view);

        sman = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sman.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() == 0) { finish(); return; }
        orientationSensor = sensors.get(0);

        // Get the location permission
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        checkLocationPermission();
                        Log.d("REQ", "Granted");
                    } else {
                        Log.d("REQ", "Denied");
                    }
                });
        checkLocationPermission();

        handleCountrySelection();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sman.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if(prefs.contains("COUNTRY")) {
            //Log.d("COU", "Found in preferences");
            selectedCountry = CountryGeolocationDatabase.getCountry(prefs.getString("COUNTRY", "Germany"));
            view.displayCountry(selectedCountry.getName());
        }
        model.setPoints(prefs.getInt("POINTS", 0));
        view.displayScore(model.getPoints());
        CCModel.setThreshold(prefs.getInt("THRESHOLD", CCModel.Difficulty.MEDIUM));
    }

    @Override
    protected void onPause() {
        super.onPause();

        sman.unregisterListener(this);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("COUNTRY", selectedCountry.getName());
        editor.putInt("POINTS", model.getPoints());
        if(selectedCountry != null)
            editor.putInt("DIFF", selectedDifficulty.getItemId());
        editor.putInt("THRESHOLD", CCModel.getThreshold());
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cc_menu, menu);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        selectedDifficulty = menu.findItem(prefs.getInt("DIFF", R.id.difficulty_medium));
        selectedDifficulty.setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(!selectedDifficulty.equals(item)) {
            model.setPoints(0);
            view.displayScore(model.getPoints());
            selectedDifficulty.setChecked(false);
            selectedDifficulty = item;
            selectedDifficulty.setChecked(true);
            switch (selectedDifficulty.getItemId()) {
                case R.id.difficulty_easy:
                    CCModel.setThreshold(CCModel.Difficulty.EASY);
                    break;
                case R.id.difficulty_medium:
                    CCModel.setThreshold(CCModel.Difficulty.MEDIUM);
                    break;
                case R.id.difficulty_hard:
                    CCModel.setThreshold(CCModel.Difficulty.HARD);
                    break;
                case R.id.difficulty_extreme:
                    CCModel.setThreshold(CCModel.Difficulty.EXTREME);
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
        return true;
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            //Log.d("REQ", "Got the permission already!");
            FusedLocationProviderClient flpClient = LocationServices.getFusedLocationProviderClient(this);
            flpClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            Log.d("LOC", userLocation.toString());
                        } else {
                            Log.d("LOC", "location is null!");
                            userLocation = new GeoPoint(48.401082, 9.987608);
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                                    "Unable to receive user location. Using the location of Ulm, Germany.",
                                    Toast.LENGTH_LONG).show());
                        }
                    });
        } else {
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION);

        }
    }

    public void onDirectionSelected(View button) {
        if(selectedCountry == null) return;
        double compassRotation = -view.getCompassImgRotation();
        boolean countryFound = model.process(compassRotation, userLocation, selectedCountry.getLocation());
        if(countryFound)
            handleCountrySelection();
    }

    private void uncheckMenuItems(MenuItem... items) {
        for (MenuItem item : items) {
            if(item.isChecked())
                item.setChecked(false);
        }
    }
}