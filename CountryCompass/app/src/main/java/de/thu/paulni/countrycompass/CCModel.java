package de.thu.paulni.countrycompass;

import android.graphics.Color;
import android.hardware.SensorEvent;

import java.util.Random;

/**
 * This class handles everything logic-related. It contains all relevant calculations and
 * determinations. It also holds constants that are relevant for said calculations.
 */
public class CCModel {
    /**
     * A class that specifies the thresholds for predetermined difficulty levels.
     */
    public static class Difficulty {
        public final static int EASY = 15;
        public final static int MEDIUM = 10;
        public final static int HARD = 5;
        public final static int EXTREME = 2;
    }
    // The view of the MVC pattern. Used to make calls to the UI after operations.
    private final CCView view;
    // The controller of the MVC pattern. Used to call Activity-specific methods.
    private final MainActivity controller;
    private final Random random;

    /**
     * This threshold describes the maximum difference between the angle the user submitted and the
     * angle that points to the given country, relative to the user's position.
     */
    private static int threshold = Difficulty.MEDIUM;

    // The user's score
    private int points = 0;
    // addend to the score for guessing right
    private final static int reward = 5;
    // subtractive to the score for guessing wrong, but very close
    private final static int tiny_loss = -1;
    // subtractive to the score for guessing slightly wrong
    private final static int small_loss = -2;
    // subtractive to the score for guessing pretty wrong
    private final static int medium_loss = -5;
    // subtractive to the score for guessing really wrong
    private final static int big_loss = -15;


    public CCModel(MainActivity mainActivity, CCView view) {
        this.controller = mainActivity;
        this.view = view;
        random = new Random();
    }

    /**
     * Update the threshold
     * @param _threshold : the new threshold
     */
    public static void setThreshold(int _threshold) {
        threshold = _threshold;
    }

    /**
     * Retrieve the threshold
     * @return the current threshold
     */
    public static int getThreshold() {
        return threshold;
    }

    /**
     * @return a random country object from the geolocation database
     * @see CountryGeolocationDatabase
     */
    public Country chooseCountry() {
        int index = random.nextInt(CountryGeolocationDatabase.countries.length);
        return CountryGeolocationDatabase.countries[index];
    }

    /**
     * get the z rotation of the orientation sensor and tell view to rotate the compass image
     * accordingly.
     * @param event : the sensor event holding the current orientation of the phone.
     */
    public void rotateCompass(SensorEvent event) {
        float[] vals = event.values.clone();
        float z = -vals[0];
        view.displayCompassRotation(z);
    }

    /**
     * This method implements the core functionality of the game. The given spherical coordinates
     * (latitude and longitude) are first mapped to coordinates on the Mercator projection.
     * Then, the direction to the target point is calculated and compared to the direction given by
     * the user. Depending on whether or not the user's guess was close enough, a UI feedback is
     * fired and the score is adapted.
     * @see <a href="https://en.wikipedia.org/wiki/Mercator_projection">Mercator Projection Wiki</a>
     * @param compassRotation : the user's guess of the direction that leads to targetPoint
     * @param userPoint : the global location of the user
     * @param targetPoint : the global location of the target point
     * @return whether the user guessed right or wrong
     */
    public boolean process(double compassRotation, GeoPoint userPoint, GeoPoint targetPoint) {
        MercatorPoint here = new MercatorPoint(userPoint);
        MercatorPoint target = new MercatorPoint(targetPoint);

        double offset = 0;
        double yDiff = target.getY() - here.getY();
        double xDiff = target.getX() - here.getX();
        // Calculate the angle pointing to the target point (0° is North, 180° is South)
        // SHORTENED: this is basically the scalar product of the projected point with a normalized
        // vector pointing northwards divided by the length of the projected point vector
        double cos = (-1 * yDiff) / Math.sqrt(xDiff*xDiff + yDiff*yDiff);
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
            // show hint
            processHint(targetAngle);
        } else {
            // show success feedback
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

    /**
     * Determines the quarter of the circle that the given angle lies within and tells the view
     * to highlight it, to give a hint to the user.
     * @param targetAngle : the angle that the highlighted quarter should contain
     */
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

    /**
     * Determines the textual feedback for the user, which depends on how far off the user's guess
     * was.
     * @param angleDifference : the difference between the user's guess and the calculated angle
     * @return the difference in points to be made to the user's score
     */
    private int processFeedback(double angleDifference) {
        String feedback;
        int pointDifference;
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

    /**
     * @param ang1 : first angle
     * @param ang2 : second angle
     * @return the difference between the first and the second angle
     */
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
