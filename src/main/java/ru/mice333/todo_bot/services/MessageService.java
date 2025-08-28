package ru.mice333.todo_bot.services;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mice333.todo_bot.model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {


    public static void sendMessagePhoto(TelegramLongPollingBot bot,String chatId, String text, String img, Long taskId) throws IOException {
        InputStream stream = new URL(img).openStream();
        SendPhoto sp = new SendPhoto();
        sp.setChatId(chatId);
        sp.setCaption(text);
        sp.setParseMode("Markdown");
        sp.setPhoto(new InputFile(stream, img));
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row1.add(new InlineKeyboardButton("✅Выполнено", null, "complete" + taskId,
                null,null,
                null,null,null,null));
        row2.add(new InlineKeyboardButton("❌Удалить", null, "delete" + taskId,
                null,null,
                null,null,null,null));
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        sp.setReplyMarkup(keyboardMarkup);
        try {
            bot.execute(sp);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public static void sendAllTasks(TelegramLongPollingBot bot,String chatId, String text, List<Task> allTasks) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        int counter = 0;
        while (allTasks.size() > counter) {
            if (counter >= 2 && counter <= 4) {
                row2.add(new InlineKeyboardButton(allTasks.get(counter).getTitle(), null,
                        allTasks.get(counter).getId().toString(),null,null,
                        null,null,null,null));
            }
            if (counter < 2 && counter >= 0) { // 0,1
                row1.add(new InlineKeyboardButton(allTasks.get(counter).getTitle(), null,
                        allTasks.get(counter).getId().toString(),null,null,
                        null,null,null,null));
            }
            counter++;
        }
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);
        sm.enableMarkdown(true);
        sm.setReplyMarkup(keyboardMarkup);
        try {
            bot.execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessageWithGif(TelegramLongPollingBot bot,String chatId, String text, String gif) {
        if (gif == null) {
            gif = "https://i.pinimg.com/originals/70/84/c6/7084c682f10716fcaf0469b550a92b6a.gif";
        }
        SendVideo sv = new SendVideo();
        sv.setChatId(chatId);
        sv.setVideo(new InputFile(gif));
        sv.setCaption(text);
        try {
            bot.execute(sv);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(TelegramLongPollingBot bot,String chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);

        sm.enableMarkdown(true);
        try {
            bot.execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
