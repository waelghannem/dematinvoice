package com.demat.invoice.beans;

import java.util.Map;

public class Permission {
    private String id;
    private String name;
    private String type;
    private String logic;
    private String decisionStrategy;
    private Map<String, Object> config;

    // Getter and Setter methods

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
