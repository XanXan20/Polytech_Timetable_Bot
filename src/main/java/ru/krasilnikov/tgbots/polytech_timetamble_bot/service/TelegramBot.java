package ru.krasilnikov.tgbots.polytech_timetamble_bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.config.BotConfig;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.User;
import ru.krasilnikov.tgbots.polytech_timetamble_bot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final static String VERSION = "0.0.2";
    final static String VERSION_TXT = "Данные об обновлениях:\n" +
            "\tТекущая версия бота: " + VERSION + "\n" +
            "Предыдущие версии:\n" +
            "\t0.0.2: добавлено выпадающее меню с доступными командами.\n" +
            "\t0.0.1: добавлены 2 команды: /start и /help.";
    final static String HELP_TXT = "Доступные команды:\n" +
            "/help - список доступных команд\n" +
            "/version - информация о версиях бота";

    final BotConfig config;

    public TelegramBot(BotConfig config){
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        //listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        //listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        //listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/version", "get version of this bot"));

        try{
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }catch(TelegramApiException e){
            log.error("Error setting bot's command list: " + e.getMessage());
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
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText){
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
                default:
                    sendMessage(chatId, "Простите, этоа команда неправильна, или неподдерживается.");
            }
        }
    }

    private void registerUser(Message msg) {

        if(userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

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

    private void startCommandReceived(long chatId, String name){
        String answer = "Привет, " + name + ", будем знакомы!\n" +
                "Скоро ты сможешь смотреть расписание в нашем ЗАМЕЧАТЕЛЬНОМ КОЛЛЕДЖЕ";

        log.info("User " + name + " started work with bot");

        sendMessage(chatId, answer);
    }

    private void helpCommandReceived(long chatId){
        sendMessage(chatId, HELP_TXT);
    }

    private void versionCommandReceived(long chatId){
        sendMessage(chatId, VERSION_TXT);
    }

    private void sendMessage(long chatId, String texTToSend){
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(texTToSend);

        try {
            execute(message);
        }catch(TelegramApiException e){
            log.error("Error occured: " + e.getMessage());
        }
    }

}
