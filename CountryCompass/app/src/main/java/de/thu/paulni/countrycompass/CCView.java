package de.thu.paulni.countrycompass;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Locale;

/**
 * This class handles everything UI-related. Its methods are called from both the controller
 * and the model. All methods that start with `display...` access one or multiple UI elements and
 * modify one of their key attributes
 * @see MainActivity
 * @see CCModel
 */
public class CCView {
    private final MainActivity controller;
    private final TextView countryName;

    public CCView(MainActivity mainActivity) {
        this.controller = mainActivity;
        countryName = controller.findViewById(R.id.countryName);
    }

    public void displayCountry(String name) {
        countryName.setText(name);
    }

    /**
     * Rotate the compass image and its surrounding border by the given angle.
     * @param z : angle to rotate around
     */
    public void displayCompassRotation(float z) {
        ImageView compassImg = controller.findViewById(R.id.compass_image);
        View compassBorder = controller.findViewById(R.id.compass_border);
        compassImg.setRotation(z);
        compassBorder.setRotation(z);
    }

    public float getCompassImgRotation() {
        ImageView compassImg = controller.findViewById(R.id.compass_image);
        return compassImg.getRotation();
    }

    public void displayFeedback(String hint) {
        TextView hintText = controller.findViewById(R.id.feedbackText);
        hintText.setText(hint);
    }

    /**
     * Select one of the ImageViews displaying a quarter and set its foreground color to the given
     * color.
     * @param idx : the index of the quarter (1 - 4, indexed like cartesian quarters)
     * @param color : the color used for highlighting
     */
    public void highlightQuarter(int idx, int color) {
        ImageView quarter;
        switch(idx) {
            case 1:
                quarter = controller.findViewById(R.id.quarter1);
                break;
            case 2:
                quarter = controller.findViewById(R.id.quarter2);
                break;
            case 3:
                quarter = controller.findViewById(R.id.quarter3);
                break;
            case 4:
                quarter = controller.findViewById(R.id.quarter4);
                break;
            default:
                throw new IllegalArgumentException("Index is not in the range of 1..5 !");
        }
        Drawable quarterDrawable = quarter.getForeground();
        quarterDrawable = DrawableCompat.wrap(quarterDrawable);
        DrawableCompat.setTint(quarterDrawable, color);
        DrawableCompat.setTintMode(quarterDrawable, PorterDuff.Mode.MULTIPLY);
        quarter.setForeground(quarterDrawable);
    }

    /**
     * Highlight the entire circle
     * @param color : color used for highlighting
     */
    public void highlightCircle(int color) {
        for(int i=1; i<5; i++) {
            highlightQuarter(i, color);
        }
    }

    public void displayScore(int score) {
        TextView scoreText = controller.findViewById(R.id.scoreText);
        scoreText.setText(String.format(Locale.getDefault(),"Points: %d", score));
    }
}
