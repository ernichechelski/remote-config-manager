package com.ernichechelski.remoteconfigmanager.utils;

import com.google.gson.GsonBuilder;

public class ParameterContainer<T> {

    PropertyContainer<T> defaultValue;

    public ParameterContainer(T defaultValue) {
        this.defaultValue = new PropertyContainer<T>(defaultValue);
    }

    private static class PropertyContainer<T> {
        String value;

        public PropertyContainer(T value) {
            this.value = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(value);
        }
    }
}
