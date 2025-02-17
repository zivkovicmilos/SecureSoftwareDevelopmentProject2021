package com.zuehlke.securesoftwaredevelopment.domain;

public class CustomerUpdate {
    private int id;
    private String username;
    private String password;

    public CustomerUpdate(int id, String username, String password) {
        this.username = username;
        this.password = password;
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return "Customer { id: " + this.id + " username: " + this.username + " }";
    }
}
