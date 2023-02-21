package ru.krasilnikov.tgbots.polytech_timetamble_bot.service;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SiteCommunication {
    public static String downloadFile() throws IOException{
        Document site = Jsoup.connect("https://polytech-rzn.ru/?page_id=14410").get();
        Element element = site.select("div.ramka:nth-child(7) > p:nth-child(1) > a:nth-child(8)").first();
        String url = element.attr("href");

        String[] urlArray = url.split("/");
        String fileName = urlArray[urlArray.length-1];

        FileUtils.copyURLToFile(new URL(url), new File("C:\\Users\\Sasalomka\\Documents\\GitHub\\Polytech_Timetable_Bot\\data\\timetables\\" + fileName));
        System.out.println("Загружен файл по ссылке: " + url);
        return "C:\\Users\\Sasalomka\\Documents\\GitHub\\Polytech_Timetable_Bot\\data\\timetables\\" + fileName;
    }
}
