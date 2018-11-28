package jep.com.testingapp;

public class Plant {

    private String id;
    private String author;
    private String name;
    private double lat;
    private double lon;
    private String description;

    public Plant(String id, String author, String name, double lat, double lon, String description) {
        this.id = id;
        this.author = author;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Plant{" +
                "id='" + id + '\'' +
                ", author='" + author + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", description='" + description + '\'' +
                '}';
    }
}
