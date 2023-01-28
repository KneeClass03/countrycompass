package de.thu.paulni.countrycompass;

import androidx.annotation.NonNull;

/**
 * Represents a point on the Mercator projection of the globe.
 * @see <a href="https://en.wikipedia.org/wiki/Mercator_projection">Mercator Projection Wiki</a>
 */
public class MercatorPoint {
    private final double x;
    private final double y;

    /**
     * Creates a new MercatorPoint by projecting the given latitude and longitude onto a Mercator map.
     * @param gp : the geographical point which this point is based on
     */
    public MercatorPoint(GeoPoint gp) {
        // Width and height of the map that the global coordinates of gp are projected onto.
        // Though these are not arbitrary, they seemed to fit as a good approximation for this use case.
        double mapHeight = 100;
        double mapWidth = 200;

        x = (gp.getLon()+180) * (mapWidth/360);

        double latRad = Math.toRadians(gp.getLat());
        double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
        y = (mapHeight/2)-(mapWidth*mercN/(2*Math.PI));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
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
