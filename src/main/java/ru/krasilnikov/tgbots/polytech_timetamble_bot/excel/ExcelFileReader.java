package ru.krasilnikov.tgbots.polytech_timetamble_bot.excel;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public abstract class ExcelFileReader{

     XSSFSheet sheet;
     File file;
     Map<Integer, Integer> groupIdToGroupColumn;
     ArrayList<Integer> groupIdList;
     abstract public Map<Integer, String> getGroupTimetable(int groupId);
     abstract public ArrayList<Integer> getGroupIdList();

     public File getFile() {
          return file;
     }
}
