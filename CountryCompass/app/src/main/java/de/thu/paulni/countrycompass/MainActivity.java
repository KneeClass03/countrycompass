package de.thu.paulni.countrycompass;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

/**
 * This activity is created on app launch. It initializes all necessary objects and acts as the
 * controller of the MVC pattern. It mostly handles events and makes calls to the model / view
 * corresponding to the fired event.
 * Additionally, it holds all logic concerning the permission to fetch the user's location.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // The model part of MVC. Handles all the in-game logic.
    private CCModel model;
    // The view part of MVC. Handles all the UI/UX.
    private CCView view;
    // The currently selected country, whose cardinal direction the user has to guess.
    private Country selectedCountry;
    // The currently selected difficulty. Determines how precise the user has to be when guessing the cardinal direction.
    private MenuItem selectedDifficulty;
    // SensorManager and sensor for the compass
    private SensorManager sman;
    private Sensor orientationSensor;
    // The user's current location.
    private GeoPoint userLocation;
    // Can open a dialog that requests the user to allow a certain permission.
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        //Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // using a simple MVC pattern to avoid mixing app logic and app UI
        view = new CCView(this);
        model = new CCModel(this, view);

        // get the orientation sensor
        sman = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sman.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() == 0) { finish(); return; }
        orientationSensor = sensors.get(0);

        // Get the location permission
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        checkLocationPermission();
                    }
                });
        checkLocationPermission();

        handleCountrySelection();

        Button selectionBtn = findViewById(R.id.choiceButton);
        selectionBtn.setOnClickListener(this::onDirectionSelected);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sman.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME);

        // Retrieve any saved in-game state from preferences (country, score, difficulty)
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if(prefs.contains("COUNTRY")) {
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

        // Save the current in-game state to preferences
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("COUNTRY", selectedCountry.getName());
        editor.putInt("POINTS", model.getPoints());
        if(selectedDifficulty != null)
            editor.putInt("DIFF", selectedDifficulty.getItemId());
        editor.putInt("THRESHOLD", CCModel.getThreshold());
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cc_menu, menu);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        // The difficulty is retrieved here, not in onResume(), to spare a reference to the menu
        selectedDifficulty = menu.findItem(prefs.getInt("DIFF", R.id.difficulty_medium));
        selectedDifficulty.setChecked(true);
        return true;
    }

    /**
     * The only functionality of the menu is selecting a difficulty level. Therefore, on menu item
     * selection the threshold that specifies the maximum difference between the guessed direction
     * and the target direction is adapted.
     * @param item : the selected menu item
     * @return whether the selection lead to any errors or not
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(!selectedDifficulty.equals(item)) {
            // to avoid cheating, one should not be able to change difficulties while keeping their score
            model.setPoints(0);
            view.displayScore(model.getPoints());
            selectedDifficulty.setChecked(false);
            selectedDifficulty = item;
            selectedDifficulty.setChecked(true);
            // Set the threshold depending on which difficulty was selected
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

    /**
     * Select a random country and display it on the UI.
     */
    public void handleCountrySelection() {
        selectedCountry = model.chooseCountry();
        view.displayCountry(selectedCountry.getName());
    }

    /**
     * Handle the request to access the user's location.
     */
    private void checkLocationPermission() {
        // check if the application already has permission to access user location
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // Fetch user location using Google Play Services (works with MicroG as well)
            FusedLocationProviderClient flpClient = LocationServices.getFusedLocationProviderClient(this);
            flpClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Save the user's current location
                            userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        } else {
                            // Use a default location
                            userLocation = new GeoPoint(48.401082, 9.987608);
                            // Tell the user that the location request failed
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                                    "Unable to receive user location. Using the location of Ulm, Germany.",
                                    Toast.LENGTH_LONG).show());
                        }
                    });
        } else {
            // Request location permission
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION);

        }
    }

    /**
     * This method is called when the user has guessed a cardinal direction. This direction is then
     * processed by the model and if the user guessed right, another country is selected at random.
     * @param button : The button on the bottom of the UI.
     */
    public void onDirectionSelected(View button) {
        if(selectedCountry == null) return;
        double compassRotation = -view.getCompassImgRotation();
        boolean countryFound = model.process(compassRotation, userLocation, selectedCountry.getLocation());
        if(countryFound)
            handleCountrySelection();
    }
}