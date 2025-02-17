package com.zuehlke.securesoftwaredevelopment.domain;

public class Food {
    int id;
    String name;

    public Food(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "Food { id: " + this.id + " name: " + this.name + " }";
    }
}
