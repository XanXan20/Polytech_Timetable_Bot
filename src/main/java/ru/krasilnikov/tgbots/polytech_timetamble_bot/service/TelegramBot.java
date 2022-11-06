package ru.krasilnikov.tgbots.polytech_timetamble_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.config.BotConfig;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.User;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.UserRepository;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private String filePath = "/home/sasalomka/TimeTableFiles/stable.xlsx";
    ExcelFileReader excelFileReader;

    @Autowired
    private UserRepository userRepository;

    final static String VERSION = "0.1.1";
    final static String VERSION_TXT = "Данные об обновлениях:\n" +
            "\tТекущая версия бота: " + VERSION + "\n" +
            "Нововведения каждой версии:\n" +
            "\t0.1.1: добавление регулярного выражения для симпатичного вывода" +
            "\t0.1: добавлена подписка на уведомления и собственно уведомления" +
            "\t0.0.5: добавлены: загрузка файла, чтение файла, разделение на юзеров и админов, возможность узнать свое расписание вручную\n" +
            "\t0.0.4: добавлены команды /changegroup и /mygroup\n" +
            "\t0.0.3: добавлена поддержка СУБД MySql\n" +
            "\t0.0.2: добавлено выпадающее меню с доступными командами.\n" +
            "\t0.0.1: добавлены 2 команды: /start и /help.";
    final static String HELP_TXT = "Доступные команды:\n" +
            "\t/help - список доступных команд\n" +
            "\t/version - информация о версиях бота\n" +
            "\t/changegroup номер_группы - подписаться на уведомления по расписанию для вашей группы\n" +
            "\t/mygroup - узнать свою текущую группу\n" +
            "\t/myrole - узнать свою роль\n" +
            "\t/timetable - узнать расписание своей группы" +
            "\t/notice[admin] - оповестить о расписании" +
            "\t/customnotice сообщение_без_пробелов[admin] - Совершить рассылку всем пользователям\n\n" +
            "При обнаружении багов, или если есть какие-либо пожелания, то пишите мне в тг:\n@Sasalomka";

    final BotConfig config;


    public TelegramBot(BotConfig config){
        this.config = config;

        try{
            excelFileReader = new ExcelFileReader(new java.io.File(filePath));
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

    /**
     *
     * Обработка апдейтов
     *
     * @param update Update received
     */
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
                case "/version":
                    versionCommandReceived(chatId);
                    break;
                case "/changegroup":
                    if(messageText.length == 2) {
                        String stringGroupId = messageText[1];

                        chooseGroupCommandReceiver(update.getMessage(), stringGroupId);
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
                        noticeCommandReceiver();
                    } else{
                        sendMessage(update.getMessage().getChatId(), "Недостаточно прав");
                    }
                    break;
                case "/autonotice":
                    autoNoticeCommandReceiver(chatId);
                    break;
                case "/customnotice":
                    if(getRole(update.getMessage().getChatId()) == 2) {
                        customNoticeCommandReceiver(messageText[1]);
                    } else{
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

    /**
     *
     * Регистрация юзера, если он вызвал команду /start впервые
     *
     * @param msg
     */
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
            log.info("User saved: " + user);
        }
    }

    /**
     *
     * Обработка команды /start
     *
     * @param chatId
     * @param name
     */
    private void startCommandReceived(long chatId, String name){
        String answer = "Привет, " + name + ", будем знакомы!\n" +
                "Скоро ты сможешь смотреть расписание в нашем ЗАМЕЧАТЕЛЬНОМ КОЛЛЕДЖЕ\n\n" +
                "P.S. ОБЯЗАТЕЛЬНО отправь /help, чтобы узнать больше о боте";

        log.info("User " + name + " started work with bot");

        sendMessage(chatId, answer);
    }


    /**
     *
     * Обработка команды /help
     *
     * @param chatId
     */
    private void helpCommandReceived(long chatId){
        sendMessage(chatId, HELP_TXT);
        log.info("ChatID: " + chatId + " called /help");
    }

    /**
     *
     * Обработка команды /version
     *
     * @param chatId
     */
    private void versionCommandReceived(long chatId){
        sendMessage(chatId, VERSION_TXT);
        log.info("ChatID: " + chatId + " called /version");
    }

    /**
     *
     * Функция отвечает за изменение/выбор группы юзера
     *
     * @param msg
     * @param stringGroupId
     */
    private void chooseGroupCommandReceiver(Message msg, String stringGroupId){

        int intGroupId = -1;
        try {
            intGroupId = Integer.parseInt(stringGroupId);
        }catch (Exception e){
            log.error("Group Id not changed: exception");
            sendMessage(msg.getChatId(), "Ошибка в прочтении номера группы, возможно вы использовали буквы, а не числа, попробуйте еще раз.");
            return;
        }

        Optional<User> optionalUser = userRepository.findById(msg.getChatId());

        User user = optionalUser.get();

        user.setGroupId(intGroupId);

        userRepository.save(user);

        sendMessage(msg.getChatId(), "Ваша группа успешно изменена.");

        log.info("ChatID: " + msg.getChatId() + " called /chooseGroup");
    }

    private void myGroupCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();

        sendMessage(chatId, "Номер вашей группы: " + user.getGroupId());

        log.info("ChatID: " + chatId + " called /myGroup");
    }

    private void myRoleCommandReceiver(long chatId){

        int role = getRole(chatId);

        String text = "Ваша роль: ";

        switch (role){
            case 0:
                sendMessage(chatId, text + "student");
                break;
            case 1:
                sendMessage(chatId, text + "teacher");
                break;
            case 2:
                sendMessage(chatId, text + "admin");
                break;
        }
        log.info("ChatID: " + chatId + " called /myRole");
    }

    private void uploadCommandReceiver(Message message){
        Document document = message.getDocument();

        GetFile getFile = new GetFile();
        getFile.setFileId(document.getFileId());
        try{
            File file = execute(getFile);
            downloadFile(file, new java.io.File(filePath));
        }catch (TelegramApiException e){}

        sendMessage(message.getChatId(), "Файл принят, начало обработки...");
        try {
            java.io.File file = new java.io.File(filePath);
            excelFileReader = new ExcelFileReader(file); //Чтение excel-файла. Все должно происходить в конструкторе класса, а мы можем просто использовать все паблик функции для работы с файлом

            sendMessage(message.getChatId(), "Файл обработан");
        }catch (IOException e){sendMessage(message.getChatId(), "При обработке файла произошла ошибка: " + e.getMessage());}

        log.info("ChatID: " + message.getChatId() + " called /upload");
    }

    private void timetableCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();
        int userGroup = user.getGroupId();
        
        String answer = findGroupTimetable(userGroup);

        sendMessage(chatId, answer);
    }

    private void noticeCommandReceiver(){
        ArrayList<Integer> list = excelFileReader.getGroupIdList();
        Iterable<User> users = userRepository.findAll();

        for (Integer i:
             list) {

            String timetable = findGroupTimetable(i);

            Iterator<User> iterator = users.iterator();

            while(iterator.hasNext()){
                User user = iterator.next();
                if(user.getGroupId() == i && user.isNotice()){
                    sendMessage(user.getChatId(), "Ваше расписание на ближайший учебный день:\n" + timetable);
                }
            }
        }
    }
    private void customNoticeCommandReceiver(String message){
        Iterable<User> users = userRepository.findAll();

        for (User user:
             users) {
            sendMessage(user.getChatId(), message);
        }
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
    }

    /**
     *
     * Функция отвечает за отпраку сообщения юзеру
     *
     * @param chatId
     * @param textToSend
     */
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


}
