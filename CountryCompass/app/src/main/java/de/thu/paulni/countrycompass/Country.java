package de.thu.paulni.countrycompass;

public class Country {
    private final String name;
    private final GeoPoint location;

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
