package ru.mice333.todo_bot.model;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Message {

    public static SendPhoto sendMessagePhoto(String chatId, String text, String img, Long taskId) throws IOException {
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
        row1.add(new InlineKeyboardButton("✅Выполнено", null, "isComplete" + taskId,
                null,null,
                null,null,null,null));
        row2.add(new InlineKeyboardButton("❌Удалить", null, "delete" + taskId,
                null,null,
                null,null,null,null));
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        sp.setReplyMarkup(keyboardMarkup);

        return sp;
    }
}
