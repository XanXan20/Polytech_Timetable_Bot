package ru.krasilnikov.tgbots.polytech_timetamble_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.config.BotConfig;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.excel.XLSXFileReader;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.User;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.UserRepository;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final String lastFilePath = "/home/sasalomka/TimeTableFiles/last_file.txt";
    XLSXFileReader excelFileReader;
    @Autowired
    private UserRepository userRepository;
    final static String VERSION = "0.1.5";
    final static String SPECIAL_THANKS = "Отдельная благодарность: \n" +
            "@FTP0N1Y";
    final static String VERSION_TXT = "Данные об обновлениях:\n" +
            "\tТекущая версия бота: " + VERSION + "\n" +
            "Нововведения каждой версии:\n" +
            "\t0.1.5: логи архивируются при достижении некоторого объема" +
            "\t0.1.4: добавление логов в бота\n" +
            "\t0.1.3: изменение способа хранения файлов, небольшой рефакторинг кода, расширение команд админа\n" +
            "\t0.1.2: добавление фидбека /feedback\n" +
            "\t0.1.1: добавление регулярного выражения для симпатичного вывода\n" +
            "\t0.1: добавлена подписка на уведомления и собственно уведомления\n" +
            "\t0.0.5: добавлены: загрузка файла, чтение файла, разделение на юзеров и админов, возможность узнать свое расписание вручную\n" +
            "\t0.0.4: добавлены команды /changegroup и /mygroup\n" +
            "\t0.0.3: добавлена поддержка СУБД MySql\n" +
            "\t0.0.2: добавлено выпадающее меню с доступными командами.\n" +
            "\t0.0.1: добавлены 2 команды: /start и /help.\n\n" +
            "Отдельные благодарности: ";
    final static String HELP_TXT = """
            Внимание! Если бот вдруг не работает как надо, то возможно вас еще нет в базе. Чтобы зарегистрироваться, отправьте команду /start

            Доступные команды:
            \t/help - список доступных команд
            \t/versions - информация о версиях бота
            \t/changegroup [номер_группы] (напр. /changegroup 71)- подписаться на уведомления по расписанию для вашей группы
            \t/mygroup - узнать свою текущую группу
            \t/myrole - узнать свою роль
            \t/timetable - узнать расписание своей группы
            \t/feedback [ваши любые пожелания или опыт использования] - Опишите опыт использования ботом.\t/thanks

            Бот хостится на моем компе, поэтому по очевидным причинам иногда будет недоступен, но если он будет уж очень всем полезен и нужен, то так и быть, раскошелюсь на сервер, только скажите

            При обнаружении багов, или если есть какие-либо пожелания, то пишите в /feedback или мне в тг:
            @Sasalomka""";

    final BotConfig config;
    public TelegramBot(BotConfig config){
        this.config = config;

        try{
            excelFileReader = new XLSXFileReader(new java.io.File(checkLastFile()));
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken() {
        return config.getToken();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText() && !update.getMessage().hasDocument()){

            String[] messageText = update.getMessage().getText().split(" ");
            long chatId = update.getMessage().getChatId();

            switch (messageText[0]){
                case "/start":
                    registerUser(update.getMessage());

                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    helpCommandReceived(chatId);
                    break;
                case "/versions":
                    versionCommandReceived(chatId);
                    break;
                case "/thanks":
                    thanksCommandReceiver(chatId);
                    break;
                case "/changegroup":
                    if(messageText.length == 2) {
                        String stringGroupId = messageText[1];

                        changeGroupCommandReceiver(update.getMessage(), stringGroupId);
                    }
                    else{
                        sendMessage(update.getMessage().getChatId(), "Используйте шаблон /changegroup [номер группы]");
                    }
                    break;
                case "/mygroup":
                    myGroupCommandReceiver(update.getMessage().getChatId());
                    break;
                case "/getUpd":
                    sendMessage(update.getMessage().getChatId(), "ID этого сообщения: " + update.getMessage().getMessageId().toString());
                    break;

                case "/myrole":
                    myRoleCommandReceiver(update.getMessage().getChatId());
                    break;

                case "/timetable":
                    timetableCommandReceiver(chatId);
                    break;
                case "/notice":
                    if(getRole(update.getMessage().getChatId()) == 2) {
                        noticeCommandReceiver(chatId);
                    } else{
                        sendMessage(update.getMessage().getChatId(), "Недостаточно прав");
                    }
                    break;
                case "/autonotice":
                    autoNoticeCommandReceiver(chatId);
                    break;
                case "/customnotice":

                    if(getRole(update.getMessage().getChatId()) == 2) {
                        boolean isOnlySubs = false;
                        ArrayList<String> message = new ArrayList<>();
                        for (int i = 2; i < messageText.length; i++) {
                            message.add(messageText[i]);
                        }

                        if(messageText[1].equalsIgnoreCase("y"))
                            isOnlySubs = true;

                        customNoticeCommandReceiver(message, isOnlySubs, chatId);
                    } else{
                        sendMessage(update.getMessage().getChatId(), "Недостаточно прав");
                    }
                    break;
                case "/feedback":
                    if(messageText.length == 1)
                        break;

                    ArrayList<String> feedback = new ArrayList<>();
                    for (int i = 1; i < messageText.length; i++) {
                        feedback.add(messageText[i]);
                    }
                    feedbackCommandReceiver(update.getMessage(), feedback);
                    break;

                case "/personalnotice":
                    if(getRole(update.getMessage().getChatId()) == 2) {
                        ArrayList<String> message = new ArrayList<>();
                        for (int i = 1; i < messageText.length; i++) {
                            message.add(messageText[i]);
                        }
                        personalNoticeCommandReceiver(messageText[1], message);
                    }else{
                        sendMessage(update.getMessage().getChatId(), "Недостаточно прав");
                    }
                    break;
                default:
                    sendMessage(chatId, "Простите, эта команда неправильна, или не поддерживается.");
            }
        }
        else if(update.getMessage().hasDocument() && getRole(update.getMessage().getChatId()) == 2){
            uploadCommandReceiver(update.getMessage());
        }
    }
    private void registerUser(Message msg) {

        if(userRepository.findById(msg.getChatId()).isEmpty()){

            Long chatId = msg.getChatId();
            Chat chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            logsUpdate(new Date() + "\tUserId: " + chatId + " was registered");
        }
    }
    private void startCommandReceived(long chatId, String name){
        String answer = "Привет, " + name + ", будем знакомы!\n" +
                "Скоро ты сможешь смотреть расписание в нашем ЗАМЕЧАТЕЛЬНОМ КОЛЛЕДЖЕ\n\n" +
                "P.S. ОБЯЗАТЕЛЬНО отправь /help, чтобы узнать больше о боте";

        log.info("User " + name + " started work with bot");

        sendMessage(chatId, answer);
    }
    private void helpCommandReceived(long chatId){
        sendMessage(chatId, HELP_TXT);
        logsUpdate(new Date() + "\tUser: " + chatId + " HELP COMMAND");
    }
    private void versionCommandReceived(long chatId){
        sendMessage(chatId, VERSION_TXT);
        logsUpdate(new Date() + "\tUser: " + chatId + " VERSIONS COMMAND");
    }
    private void myGroupCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();

        if(user.getGroupId() == 0){
            sendMessage(chatId, "Вы еще не выбрали свою группуб используйте /changegroup [номер группы] для выбора своей группы");
        }
        else {
            sendMessage(chatId, "Номер вашей группы: " + user.getGroupId());
        }
        logsUpdate(new Date() + "\tUser: " + chatId + " MYGROUP COMMAND");
    }
    private void changeGroupCommandReceiver(Message msg, String stringGroupId){

        int intGroupId = 0;
        try {
            intGroupId = Integer.parseInt(stringGroupId);
        }catch (Exception e){
            log.error("Group Id not changed: exception");
            sendMessage(msg.getChatId(), "Ошибка в прочтении номера группы, возможно вы использовали буквы, а не числа, попробуйте еще раз.");
            return;
        }

        ArrayList<Integer> groupList = excelFileReader.getGroupIdList();

        for(int i : groupList){
            if(intGroupId == i){
                Optional<User> optionalUser = userRepository.findById(msg.getChatId());

                User user = optionalUser.get();
                int oldGroupId = user.getGroupId();
                user.setGroupId(intGroupId);

                userRepository.save(user);

                sendMessage(msg.getChatId(), "Ваша группа успешно изменена.");
                logsUpdate(new Date() + "\tUser: " + msg.getChatId() + " CHANGE GROUP FROM: " + oldGroupId + " TO: " + stringGroupId);
                return;
            }
        }
        sendMessage(msg.getChatId(), "Такой группы не найдено, проверьте корректность введенных данных");
    }
    private void myRoleCommandReceiver(long chatId){

        int role = getRole(chatId);

        String text = "Ваша роль: ";

        switch (role) {
            case 0 -> sendMessage(chatId, text + "бедолага");
            case 1 -> sendMessage(chatId, text + "работяга");
            case 2 -> sendMessage(chatId, text + "гигачад");
        }
        logsUpdate(new Date() + "\tUser: " + chatId + " MYROLE COMMAND");
    }
    private void timetableCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();
        int userGroup = user.getGroupId();

        String answer = findGroupTimetable(userGroup);

        sendMessage(chatId, answer);

        logsUpdate(new Date() + "\tUser: " + chatId + " TIMETABLE COMMAND");
    }
    private void autoNoticeCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();
        user.setNotice(!user.isNotice());

        if(user.isNotice()){
            sendMessage(chatId, "Вы подписались на уведомления");
        }else{
            sendMessage(chatId, "Вы отписались от уведомлений");
        }

        userRepository.save(user);

        logsUpdate(new Date() + "\tUser: " + chatId + " AUTONOTICE COMMAND; SUNSCRIBER = " + user.isNotice());
    }
    private void thanksCommandReceiver(long chatId){
        sendMessage(chatId, SPECIAL_THANKS);
        logsUpdate(new Date() + "\tUser: " + chatId + " THANKS COMMAND");
    }
    private void uploadCommandReceiver(Message message){
        Document document = message.getDocument();

        GetFile getFile = new GetFile();
        getFile.setFileId(document.getFileId());
        String filePath = "/home/sasalomka/TimeTableFiles/";
        try{
            File file = execute(getFile);

            String[] docName = document.getFileName().split("\\.");
            if(docName.length == 2 && docName[1].equals("xlsx"))
                downloadFile(file, new java.io.File(filePath + document.getFileName()));
            else {
                sendMessage(message.getChatId(), "Формат вашего файла: ." + docName[1] + "\nПока что бот принимает только файлы .xlsx");
                return;
            }

        }catch (TelegramApiException e){
            System.out.println(e.getMessage());
        }

        sendMessage(message.getChatId(), "Файл принят, начало обработки...");
        try {
            java.io.File file = new java.io.File(filePath + document.getFileName());

            excelFileReader = new XLSXFileReader(file); //Чтение excel-файла. Все должно происходить в конструкторе класса, а мы можем просто использовать все паблик функции для работы с файлом

            sendMessage(message.getChatId(), "Файл обработан. Рассылка: /notice");
            setLastFile(filePath + document.getFileName());
        }catch (IOException e){sendMessage(message.getChatId(), "При обработке файла произошла ошибка: " + e.getMessage());}

        logsUpdate(new Date() + "\tUser: " + message.getChatId() + " NEW TIMETABLE UPLOAD");
    }
    private void noticeCommandReceiver(long chatId){
        ArrayList<Integer> list = excelFileReader.getGroupIdList();
        Iterable<User> users = userRepository.findAll();

        for (Integer i:
             list) {

            String timetable = findGroupTimetable(i);

            for (User user : users) {
                if (user.getGroupId() == i && user.isNotice()) {
                    sendMessage(user.getChatId(), "Ваше расписание на ближайший учебный день:\n\n" + timetable +
                            "\nP.S. Учитывая немного неодинаковое заполнение расписания администрацией и очень раннюю стадию разработки бота, " +
                            "очень советую перепровверять свое расписание ;)");
                }
            }
        }

        logsUpdate(new Date() + "\tUser: " + chatId + " NOTICE COMMAND");
    }
    private void customNoticeCommandReceiver(ArrayList<String> messageList, boolean isOnlySub, long chatId){
        Iterable<User> users = userRepository.findAll();
        String message = "";

        for(String str : messageList){
            message += str + " ";
        }
        message = message.trim();
        if(isOnlySub) {
            for (User user :
                    users) {
                if (user.isNotice())
                    sendMessage(user.getChatId(), message);
            }
        }
        else{
            for (User user :
                    users) {
                sendMessage(user.getChatId(), message);
            }
        }

        logsUpdate(new Date() + "\tUser: " + chatId + " CUSTOMNOTICE COMMAND;\n\tNOTICE TEXT: " + message);
    }
    private void feedbackCommandReceiver(Message message, ArrayList<String> feedback){
        String feedbackFilePath = "/home/sasalomka/TimeTableFiles/feedback.txt";
        try (FileWriter writer = new FileWriter(feedbackFilePath, true)){
            Date date = new Date();
            writer.write("Username: " + message.getChat().getUserName() + " ID: " + message.getChatId() + " Date: " + date + "--> ");
            for(String str : feedback){
                writer.write(str + " ");
            }
            writer.write("\n");
            writer.flush();
        }catch (IOException e){
            System.out.println(e.getMessage());
        }

        sendMessage(message.getChatId(), "Спасибо за фидбек, еще можно написать мне в тг (подробности в /help)");

        logsUpdate(new Date() + "\tUser: " + message.getChatId() + " FEEDBACK COMMAND");
    }
    private void personalNoticeCommandReceiver(String chatId, ArrayList<String> messageList){
        long l = Long.parseLong(chatId);
        String message = "";

        for(String str : messageList){
            message += str + " ";
        }
        message = message.trim();

        sendMessage(l, message);

        logsUpdate(new Date() + "\tUser: " + chatId + " SEND PERSONAL NOTICE TO USER: " + l + "\n\tMESSAGE TEXT: " + message);
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        }catch(TelegramApiException e){
            log.error("Error occured: " + e.getMessage());
        }
    }
    private int getRole(long chatId){
        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();

        return user.getRole();
    }
    private String findGroupTimetable(int groupId){

        Map<Integer, String> groupTimetable = excelFileReader.getGroupTimetable(groupId);
        String answer = "";

        for (int i = 1; i < 15; i++) {

            if(groupTimetable.get(i) == null){
                continue;
            }

            String[] lesionName = groupTimetable.get(i).split(",");

            answer += i + " - ";
            for(String str : lesionName){
                str = str.trim();
                answer += str + " | ";
            }
            answer += "\n";
        }
        return answer;
    }
    private String checkLastFile(){
        String path = "";
        try(FileReader reader = new FileReader(lastFilePath)){
            int buf;
            while((buf = reader.read()) != -1){
                path += (char)buf;
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        return path;
    }
    private void setLastFile(String fileName){
        try(FileWriter writer = new FileWriter(lastFilePath)) {
            writer.write(fileName);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
    private void logsUpdate(String log){
        try(FileWriter writer = new FileWriter("/home/sasalomka/TimeTableFiles/logs.txt", true);){

            writer.write(log + "\n");

            java.io.File file = new java.io.File("/home/sasalomka/TimeTableFiles/logs.txt");

            long fileInBytes = file.length();
            long fileInKb = fileInBytes/1024;
            long fileInMb = fileInKb/1024;

            if(fileInMb > 100){
                System.out.println("Должна начаться архивация файла");
                try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("/home/sasalomka/TimeTableFiles/zip_logs/" + new Date() + ".zip"));
                    FileInputStream fis = new FileInputStream("/home/sasalomka/TimeTableFiles/logs.txt")){

                    ZipEntry zipEntry = new ZipEntry(new Date().toString());
                    zos.putNextEntry(zipEntry);

                    byte[] buf = new byte[fis.available()];
                    fis.read(buf);

                    zos.write(buf);
                    zos.closeEntry();

                    file.delete();
                }
            }

        }catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
