package ru.krasilnikov.tgbots.polytech_timetamble_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.client.Page;

@SpringBootApplication
public class PolytechTimetambleBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolytechTimetambleBotApplication.class, args);
    }

}
