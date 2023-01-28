package de.thu.paulni.countrycompass;

public class Country {
    private final String name;
    private final GeoPoint location;

    /**
     * Represents a country
     * @param name : the name of the country
     * @param location : the geographical center of the area of the country
     */
    public Country(String name, GeoPoint location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public GeoPoint getLocation() {
        return location;
    }
}
