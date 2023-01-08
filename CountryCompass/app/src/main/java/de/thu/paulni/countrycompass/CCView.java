package de.thu.paulni.countrycompass;

import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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
        compassImg.setRotation(z);
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
}
