package com.demat.invoice.beans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Config {
    private String roles;

    public List<Role> getRoles() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<Role> roleList = objectMapper.readValue(this.roles, new TypeReference<List<Role>>() {});

            // Now, roleList contains the list of Role objects
            return roleList;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
}
