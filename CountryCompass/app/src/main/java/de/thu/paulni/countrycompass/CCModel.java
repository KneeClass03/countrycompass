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
        double targetAngle = Math.toDegrees(Math.acos(cos));
        if(xDiff < 0) {
            offset = (180-targetAngle) * 2;
            if(yDiff < 0)
                offset = 360 - 2 * targetAngle;
        }
        targetAngle += offset;

        //Log.d("LOC", here.toString());
        //Log.d("LOC", target.toString());
        Log.d("LOC", "Angle: " + targetAngle + ", Compass Rotation: " + compassRotation);
        double difference = calculateAngleDifference(targetAngle, compassRotation);
        if(difference <= threshold) {
            // user is right
            view.displayAnswer(true);
            view.clearHint();
            return true;
        } else {
            // user is wrong
            view.displayAnswer(false);
            processHint(difference);
            return false;
        }
        // a = 350, b = 5
        // a - b = 345
    }

    private void processHint(double angleDifference) {
        String hint = "";
        if(angleDifference > 90) {
            hint = "very far off (> 90°)";
        } else if(angleDifference > 45) {
            hint = "quite off (> 45°)";
        } else if(angleDifference > 20) {
            hint = "slightly off (> 20°)";
        } else {
            hint = "very close (< 20°)";
        }

        view.displayHint(hint);
    }

    private CardinalDirection determineClosestCD(double angle) {
        if(angle < 45 || angle >= 315)
            return CardinalDirection.NORTH;
        else if(angle >= 45 && angle < 135)
            return CardinalDirection.EAST;
        else if(angle >= 135 && angle < 215)
            return CardinalDirection.SOUTH;
        else
            return CardinalDirection.WEST;
    }

    private double calculateAngleDifference(double ang1, double ang2) {
        double difference = Math.abs(ang1 - ang2);
        if(difference > 360.0 - difference) {
            difference = 360.0 - difference;
        }
        return difference;
    }
}
