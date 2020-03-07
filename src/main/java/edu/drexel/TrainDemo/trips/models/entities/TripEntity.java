package edu.drexel.TrainDemo.trips.models.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "trip")
public class TripEntity {

    @Id
    private long id;
    private long routeId;
    private long calendarId;
    private String headsign;
    private boolean direction;

    protected TripEntity() {
    }

    public long getId() {
        return id;
    }
}