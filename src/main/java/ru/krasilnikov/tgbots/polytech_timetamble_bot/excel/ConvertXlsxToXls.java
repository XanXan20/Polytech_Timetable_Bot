package ru.krasilnikov.tgbots.polytech_timetamble_bot.excel;

import com.spire.xls.ExcelVersion;
import com.spire.xls.Workbook;

public class ConvertXlsxToXls {
    public static void convert(String path){
        Workbook wb = new Workbook();

        wb.loadFromFile(path);
        wb.saveToFile("C:\\Users\\Sasalomka\\Documents\\GitHub\\Polytech_Timetable_Bot\\data\\actualTimetable.xls", ExcelVersion.Version2016);
    }
}
