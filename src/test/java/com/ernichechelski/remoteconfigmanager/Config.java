package com.ernichechelski.remoteconfigmanager;

import com.ernichechelski.remoteconfigmanager.utils.ParameterContainer;

/**
 * Define all parameters here. To introduce special structures you can define static classes here.
 */
public class Config {

    public ParameterContainer<String> text = new ParameterContainer("Remote config projectID");

    public ParameterContainer<Theme> theme = new ParameterContainer(new Theme());

    public static class Theme {
        public String colorHex = "#92003B";
        public Integer size = 54;
    }
}