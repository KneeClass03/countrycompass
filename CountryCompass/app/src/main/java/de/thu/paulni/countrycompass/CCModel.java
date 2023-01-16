package de.thu.paulni.countrycompass;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import java.util.Random;

public class CCModel {

    public class Difficulty {
        public final static int EASY = 15;
        public final static int MEDIUM = 10;
        public final static int HARD = 5;
        public final static int EXTREME = 2;
    }

    private final CCView view;
    private MainActivity controller;
    private final Random random;

    private static int threshold = Difficulty.MEDIUM;

    private int points = 0;
    private final static int reward = 5;
    private final static int tiny_loss = -1;
    private final static int small_loss = -2;
    private final static int medium_loss = -5;
    private final static int big_loss = -15;


    public CCModel(MainActivity mainActivity, CCView view) {
        this.controller = mainActivity;
        this.view = view;
        random = new Random();
    }

    public static void setThreshold(int _threshold) {
        threshold = _threshold;
    }

    public static int getThreshold() {
        return threshold;
    }

    public Country chooseCountry() {
        int index = random.nextInt(CountryGeolocationDatabase.countries.length);
        return CountryGeolocationDatabase.countries[index];
    }

    public void rotateCompass(SensorEvent event) {
        float[] vals = event.values.clone();
        float z = -vals[0];
        view.displayCompassRotation(z);
    }

    public boolean process(double compassRotation, GeoPoint userPoint, GeoPoint targetPoint) {
        // idea:
        // 1. calculate the distance vector from current location to target location (internet)
        // 2. find out if this vector is parallel or just slightly off from the cardinal direction of
        //    the compass
        MercatorPoint here = new MercatorPoint(userPoint);
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
        double difference = calculateAngleDifference(targetAngle, compassRotation);
        boolean found = difference <= threshold;
        if(!found) {
            processHint(targetAngle);
        } else {
            view.highlightCircle(Color.GREEN);
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
                controller.runOnUiThread(() -> view.highlightCircle(Color.GRAY));
            });
            thread.start();
        }
        points += processFeedback(difference);
        view.displayScore(points);
        return found;
    }

    private void processHint(double targetAngle) {
        int color = Color.YELLOW;
        int quarter = 0;
        if(targetAngle >= 0 && targetAngle < 90) {
            quarter = 1;
        } else if(targetAngle >= 90 && targetAngle < 180) {
            quarter = 4;
        } else if(targetAngle >= 180 && targetAngle < 270) {
            quarter = 3;
        } else if(targetAngle >= 270 && targetAngle < 360) {
            quarter = 2;
        }
        view.highlightQuarter(quarter, color);
    }

    private int processFeedback(double angleDifference) {
        String feedback = "";
        int pointDifference = 0;
        if(angleDifference > 90) {
            feedback = "very far off (> 90°)";
            pointDifference = big_loss;
        } else if(angleDifference > 45) {
            feedback = "quite off (> 45°)";
            pointDifference = medium_loss;
        } else if(angleDifference > 20) {
            feedback = "slightly off (> 20°)";
            pointDifference = small_loss;
        } else if(angleDifference > threshold) {
            feedback = "very close (< 20°)";
            pointDifference = tiny_loss;
        } else {
            feedback = "Great job!";
            pointDifference = reward;
        }

        view.displayFeedback(feedback);
        return pointDifference;
    }

    private double calculateAngleDifference(double ang1, double ang2) {
        double difference = Math.abs(ang1 - ang2);
        if(difference > 360.0 - difference) {
            difference = 360.0 - difference;
        }
        return difference;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
