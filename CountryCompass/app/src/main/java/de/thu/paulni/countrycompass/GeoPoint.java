package de.thu.paulni.countrycompass;

import androidx.annotation.NonNull;

/**
 * Represents a point in geo coordinates (latitude, longitude) with accuracy in km
 */
public class GeoPoint {

    private final double lat;
    private final double lon;
    private final double accuracy;

    /**
     * New geo point without accuracy
     * @param lat : latitude
     * @param lon : longitude
     */
    public GeoPoint(double lat, double lon){
        this(lat, lon, -1d);
    }

    /**
     * New geo point with specified accuracy
     * @param accuracy  accuracy in km
     */
    public GeoPoint(double lat, double lon, double accuracy){
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy < 0 ? -1d : accuracy;
    }

    public double getLat(){
        return this.lat;
    }

    public double getLon(){
        return this.lon;
    }

    @NonNull
    @Override
    public String toString(){
        return "lat = " + this.lat + "; lon = " + this.lon + (this.accuracy < 0 ? "" : ("; accuracy = " + this.accuracy));
    }


}
