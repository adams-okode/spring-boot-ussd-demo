package com.decoded.ussd.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Menu {
    @JsonProperty("id")
    private String id;

    @JsonProperty("menu_level")
    private String menuLevel;

    @JsonProperty("text")
    private String text;

    @JsonProperty("menu_options")
    private List<MenuOption> menuOptions;

    @JsonProperty("action")
    private String action;

    @JsonProperty("max_selections")
    private Integer maxSelections;
}
