package ru.krasilnikov.tgbots.polytech_timetamble_bot.excel;

import java.util.ArrayList;
import java.util.Map;

public interface ExcelFileReader{
     Map<Integer, String> getGroupTimetable(int groupId);
     ArrayList<Integer> getGroupIdList();
}
