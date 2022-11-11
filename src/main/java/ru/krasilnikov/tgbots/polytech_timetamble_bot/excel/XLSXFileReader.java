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

public class XLSXFileReader extends ExcelFileReader{

    private final XSSFSheet sheet;
    private File file;
    private final Map<Integer, Integer> groupIdToGroupColumn;
    private final ArrayList<Integer> groupIdList;

    public XLSXFileReader(File filePath) throws IOException{
        this.file = filePath;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
        sheet = workbook.getSheet("Лист1");

        XSSFRow row = sheet.getRow(3);

        groupIdToGroupColumn = new HashMap<>();
        groupIdList = new ArrayList<>();
        Iterator<Cell> cellIterator = row.cellIterator();

        while(cellIterator.hasNext()){
            Cell cell = cellIterator.next();

            int cellColumn = cell.getColumnIndex();
            int cellValue = (int)cell.getNumericCellValue();

            if(cellValue == 0.0){
                continue;
            }

            groupIdList.add(cellValue);
            groupIdToGroupColumn.put(cellValue, cellColumn);
        }

        //this.groupIdList = groupList;

        workbook.close();
    }

    public Map<Integer, String> getGroupTimetable(int groupId){

        Map<Integer, String> groupTimetable = new HashMap<>();

        int groupColumn = groupIdToGroupColumn.get(groupId);

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
                case 1, 2-> groupTimetable.put(1, lesionName);
                case 3, 4 -> groupTimetable.put(2, lesionName);
                case 5, 6 -> groupTimetable.put(3, lesionName);
                case 7, 8 -> groupTimetable.put(4, lesionName);
                case 9, 10 -> groupTimetable.put(5, lesionName);
                case 11, 12 -> groupTimetable.put(6, lesionName);
                case 13, 14 -> groupTimetable.put(7, lesionName);
            }
        }
        return groupTimetable;
    }

    public ArrayList<Integer> getGroupIdList() {
        return groupIdList;
    }
    public File getFile(){
        return this.file;
    }

}
