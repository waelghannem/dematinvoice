package com.demat.invoice.beans;

public class PolicyResponse {
    String id;
    String name;
    Role[] config;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role[] getConfig() {
        return config;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConfig(Role[] config) {
        this.config = config;
    }
}
