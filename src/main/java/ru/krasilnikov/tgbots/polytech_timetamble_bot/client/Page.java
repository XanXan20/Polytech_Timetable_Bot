package ru.krasilnikov.tgbots.polytech_timetamble_bot.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Page {

    private int id;

    private String username;

    private String email;

    private String phone;

    private String website;
}
