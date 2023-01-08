package de.thu.paulni.countrycompass;

import android.hardware.SensorEvent;
import android.location.Location;
import android.util.Log;

import java.util.Random;
import java.util.Vector;

public class CCModel {
    private CCView view;
    private MainActivity controller;
    private Random random;

    private final static double threshold = 10.0;

    public CCModel(MainActivity mainActivity, CCView view) {
        this.controller = mainActivity;
        this.view = view;
        random = new Random();
    }

    public Country chooseCountry() {
        Country[] countries = CountryGeolocationDatabase.getCountries();
        int index = random.nextInt(countries.length);
        return countries[index];
    }

    public void rotateCompass(SensorEvent event) {
        float[] vals = event.values.clone();
        float z = -vals[0];
        view.displayCompassRotation(z);
    }

    public boolean process(double compassRotation, GeoPoint targetPoint) {
        // idea:
        // 1. calculate the distance vector from current location to target location (internet)
        // 2. find out if this vector is parallel or just slightly off from the cardinal direction of
        //    the compass
        MercatorPoint here = new MercatorPoint(new GeoPoint(51.165691, 10.451526));
        MercatorPoint target = new MercatorPoint(targetPoint);

        double offset = 0;
        double yDiff = target.getY() - here.getY();
        double xDiff = target.getX() - here.getX();
        MercatorPoint top = new MercatorPoint(0,-1);
        MercatorPoint projectedTarget = new MercatorPoint(xDiff, yDiff);
        double length1 = top.distanceFromOrigin();
        double length2 = projectedTarget.distanceFromOrigin();
        double cos = top.scalarWithPoint(projectedTarget) / (length1 * length2);
        double cardinalDirection = Math.toDegrees(Math.acos(cos));
        if(xDiff < 0) {
            offset = (180-cardinalDirection) * 2;
            if(yDiff < 0)
                offset = 360 - 2 * cardinalDirection;
        }
        cardinalDirection += offset;

        //Log.d("LOC", here.toString());
        //Log.d("LOC", target.toString());
        Log.d("LOC", "Angle: " + cardinalDirection + ", Compass Rotation: " + compassRotation);

        if(Math.abs(cardinalDirection - compassRotation) <= threshold) {
            // user is right
            view.displayAnswer(true);
            return true;
        } else {
            // user is wrong
            view.displayAnswer(false);
            processHint(cardinalDirection, compassRotation);
            return false;
        }
    }

    private void processHint(double cardinalDirection, double compassRotation) {

    }
}
