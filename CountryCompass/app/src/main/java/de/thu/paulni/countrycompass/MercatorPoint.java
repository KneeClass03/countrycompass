package de.thu.paulni.countrycompass;

import androidx.annotation.NonNull;

public class MercatorPoint {
    private final static double R = 6371.0;
    private final double x;
    private final double y;

    public MercatorPoint(GeoPoint gp) {
        double mapHeight = 100;//R*2;
        double mapWidth = 200;//(mapHeight * Math.PI);

        x = (gp.getLon()+180) * (mapWidth/360);

        double latRad = Math.toRadians(gp.getLat());
        double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
        y = (mapHeight/2)-(mapWidth*mercN/(2*Math.PI));
    }

    public MercatorPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double distanceFromOrigin() {
        return Math.sqrt(x*x + y*y);
    }

    public double scalarWithPoint(MercatorPoint mp) {
        return x * mp.getX() + y * mp.getY();
    }

    @NonNull
    @Override
    public String toString() {
        return "MercatorPoint{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
