package ru.krasilnikov.tgbots.polytech_timetamble_bot.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

public class WebTest {

    FirefoxProfile profile;
    FirefoxOptions options;

    public WebTest(){
        options = new FirefoxOptions();
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", "/home/sasalomka/TimeTableFiles");

    }

    public String checkSite(String lastFileName) {

        WebDriver driver = new FirefoxDriver(options);

        driver.get("http://polytech-rzn.ru/?page_id=14410");

        WebElement downloadRef = driver.findElement(By.xpath("/html/body/div/div[2]/main/article/div/div[2]/div[3]/p/a[2]"));
        String downloadLink = downloadRef.getAttribute("href");

        String[] splitLink = downloadLink.split("/");
        String[] splitLastFile = lastFileName.split("/");

        if(!splitLink[6].equals(splitLastFile[4])){
            downloadRef.click();
        }

        driver.quit();
        return splitLink[6];
    }
}
