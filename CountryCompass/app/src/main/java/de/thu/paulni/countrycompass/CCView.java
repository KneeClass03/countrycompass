package de.thu.paulni.countrycompass;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Locale;

public class CCView {
    private MainActivity controller;
    private TextView countryName;

    public CCView(MainActivity mainActivity) {
        this.controller = mainActivity;
        countryName = controller.findViewById(R.id.countryName);
    }

    public void displayCountry(String name) {
        countryName.setText(name);
    }

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

    public void displayAnswer(boolean directionIsCorrect) {
        int color = directionIsCorrect ? Color.GREEN : Color.RED;

        FrameLayout fl = controller.findViewById(R.id.frameLayout);
        fl.setBackgroundColor(color);
    }

    public void displayFeedback(String hint) {
        TextView hintText = controller.findViewById(R.id.feedbackText);
        hintText.setText(hint);
    }

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
        //the color is a direct color int and not a color resource
        DrawableCompat.setTint(quarterDrawable, color);
        DrawableCompat.setTintMode(quarterDrawable, PorterDuff.Mode.MULTIPLY);
        quarter.setForeground(quarterDrawable);
    }

    public void highlightCircle(int color) {
        for(int i=1; i<5; i++) {
            highlightQuarter(i, color);
        }
    }

    public void displayScore(int score) {
        TextView scoreText = controller.findViewById(R.id.scoreText);
        scoreText.setText(String.format(Locale.getDefault(),"Points: %d", score));
    }

    public void clearHint() {
        TextView hintText = controller.findViewById(R.id.feedbackText);
        hintText.setText("");
    }
}
