package com.example.vehicle_maintence_scheduler.model;

public class Depot {
    private int ID;
    private int MechanicHours;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getMechanicHours() {
        return MechanicHours;
    }

    public void setMechanicHours(int mechanicHours) {
        MechanicHours = mechanicHours;
    }
}