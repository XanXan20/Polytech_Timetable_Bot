package ru.krasilnikov.tgbots.polytech_timetamble_bot.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XLSXFileReader {

    private XSSFSheet sheet;
    private XSSFRow row;
    private Map<Integer, Integer> groupMap;
    private ArrayList<Integer> groupIdList;

    public XLSXFileReader(File filePath) throws IOException{

        FileInputStream file = new FileInputStream(filePath);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        sheet = workbook.getSheet("Лист1");

        row = sheet.getRow(3);

        groupMap = readGroupInfo(row);

        workbook.close();
    }

    public Map<Integer, String> getGroupTimetable(int groupId){

        Map<Integer, String> groupTimetable = new HashMap<>();

        int groupColumn = groupMap.get(groupId);

        int i = 4;
        int lastRow = 18;
        boolean isMonday = false;
        XSSFRow startReadingRow = sheet.getRow(i);

        Iterator<Cell> cellIterator = startReadingRow.cellIterator();
        while(cellIterator.hasNext()){
            Cell cell = cellIterator.next();

            if(cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains("Классный час")){
                i++;
                lastRow+=2;
                isMonday = true;
                break;
            }
        }

        for (int lesionId = 1; i < lastRow; i++, lesionId++) {

            if(isMonday && i == 13)
                i++;

            XSSFRow row = sheet.getRow(i);
            XSSFCell cell = row.getCell(groupColumn);

            String lesionName = cell.getStringCellValue();

            if(lesionName.equals("") || lesionName.contains("Классный час"))
                continue;


            switch (lesionId) {
                case 1 -> groupTimetable.put(1, lesionName);
                case 2 -> groupTimetable.put(1, lesionName);
                case 3 -> groupTimetable.put(2, lesionName);
                case 4 -> groupTimetable.put(2, lesionName);
                case 5 -> groupTimetable.put(3, lesionName);
                case 6 -> groupTimetable.put(3, lesionName);
                case 7 -> groupTimetable.put(4, lesionName);
                case 8 -> groupTimetable.put(4, lesionName);
                case 9 -> groupTimetable.put(5, lesionName);
                case 10 -> groupTimetable.put(5, lesionName);
                case 11 -> groupTimetable.put(6, lesionName);
                case 12 -> groupTimetable.put(6, lesionName);
                case 13 -> groupTimetable.put(7, lesionName);
                case 14 -> groupTimetable.put(7, lesionName);
            }

        }

        return groupTimetable;
    }

    private Map<Integer, Integer> readGroupInfo(XSSFRow row){

        Map<Integer, Integer> groupMap = new HashMap<>();
        ArrayList<Integer> groupList = new ArrayList<>();
        Iterator<Cell> cellIterator = row.cellIterator();

        while(cellIterator.hasNext()){
            Cell cell = cellIterator.next();

            int cellColumn = cell.getColumnIndex();
            int cellValue = (int)cell.getNumericCellValue();

            if(cellValue == 0.0){
                continue;
            }

            groupList.add(cellValue);
            groupMap.put(cellValue, cellColumn);
        }

        this.groupIdList = groupList;
        return groupMap;
    }


    public XSSFSheet getSheet() {
        return sheet;
    }

    public void setSheet(XSSFSheet sheet) {
        this.sheet = sheet;
    }

    public XSSFRow getRow() {
        return row;
    }

    public void setRow(XSSFRow row) {
        this.row = row;
    }

    public Map<Integer, Integer> getGroupMap() {
        return groupMap;
    }

    public void setGroupMap(Map<Integer, Integer> groupMap) {
        this.groupMap = groupMap;
    }

    public ArrayList<Integer> getGroupIdList() {
        return groupIdList;
    }

    public void setGroupIdList(ArrayList<Integer> groupIdList) {
        this.groupIdList = groupIdList;
    }
}
