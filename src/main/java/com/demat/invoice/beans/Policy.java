package com.demat.invoice.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public class Policy {
    private String id;
    private String name;
    private String description;
    private String type;
    private String logic;
    private String decisionStrategy;

    private Config config;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Config getConfig() {
        return config;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public String getDecisionStrategy() {
        return decisionStrategy;
    }

    public void setDecisionStrategy(String decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }
}
