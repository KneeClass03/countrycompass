package de.thu.paulni.countrycompass;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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

        view = new CCView(this);
        model = new CCModel(this, view);

        sman = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sman.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() == 0) { finish(); return; }
        lightSensor = sensors.get(0);

        handleCountrySelection();
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