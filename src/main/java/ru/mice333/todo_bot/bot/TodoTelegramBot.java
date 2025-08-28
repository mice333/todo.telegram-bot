package ru.mice333.todo_bot.bot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mice333.todo_bot.api.ApiRequest;
import ru.mice333.todo_bot.api.ImageGeneratorApi;
import ru.mice333.todo_bot.model.Task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TodoTelegramBot extends TelegramLongPollingBot {

    @Autowired
    private BotConfig botConfig;

    private final Map<Long, Task> userTasks = new HashMap<>();
    private final Map<Long, String> userStates = new HashMap<>();

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        log.info("update используется");
        if (update.hasMessage() || update.hasCallbackQuery()) {
            String call = "";
            String username;
            String chatId;

            if (update.hasCallbackQuery()) {
                call = update.getCallbackQuery().getData();
                username = update.getCallbackQuery().getFrom().getUserName();
                chatId = update.getCallbackQuery().getMessage().getChatId().toString();
                if (call.contains("complete")) {
                    ApiRequest.updateCompleteStatus(Long.parseLong(call.substring(8)), username);
                    log.info("У задачи с id: {} изменён статус", call.substring(8));
                    DeleteMessage deleteMessage = new DeleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                    try {
                        execute(deleteMessage);
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                    return;
                }

                if (call.contains("delete")) {
                    ApiRequest.deleteTaskById(Long.parseLong(call.substring(6)), username);
                    log.info("Задача с id: {} удалена", call.substring(6));
                    DeleteMessage deleteMessage = new DeleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                    try {
                        execute(deleteMessage);
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                    return;
                }
                Task task = ApiRequest.findTaskById(Long.parseLong(call));
                log.info("task id: {}", task.getId());
                String img = ImageGeneratorApi.getImageLink(task.getId());
                try {
                    sendMessagePhoto(chatId, String.format("*Название*: %s\n_Описание_: %s\n_Приоритет_: %s\n_Дата создания_: %s\n%s\n\n==========\n\n",
                            task.getTitle(),
                            task.getDescription(),
                            task.getPriority(),
                            task.getCreatedAt(),
                            task.getCompleted()
                    ), img, task.getId());
                    return;
                } catch (FileNotFoundException e) {
                    img = "https://i.pinimg.com/736x/e4/6d/87/e46d873dc5389bd3f76102c0cd9df176.jpg";
                    sendMessagePhoto(chatId, String.format("*Название*: %s\n_Описание_: %s\n_Приоритет_: %s\n_Дата создания_: %s\n%s\n\n==========\n\n",
                            task.getTitle(),
                            task.getDescription(),
                            task.getPriority(),
                            task.getCreatedAt(),
                            task.getCompleted()
                    ), img, task.getId());
                }

                return;
            }

            if (update.getMessage().hasText()) {
                String message = update.getMessage().getText().trim();
                chatId = update.getMessage().getChatId().toString();
                username = update.getMessage().getChat().getUserName();
                List<Task> allTasks;
                if (message.equals("/create")) {
                    userTasks.put(Long.parseLong(chatId), new Task());
                    userStates.put(Long.parseLong(chatId), "WAITING_FOR_TITLE");
                    sendMessage(chatId, "Введите название задачи");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_TITLE")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setTitle(message);
                    userStates.put(Long.parseLong(chatId), "WAITING_FOR_DESCRIPTION");
                    sendMessage(chatId, "Введите описание задачи");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_DESCRIPTION")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setDescription(message);
                    userStates.put(Long.parseLong(chatId), "WAITING_FOR_PRIORITY");
                    sendMessage(chatId, "Введите приоритет задачи (1-3)");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_PRIORITY")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setPriority(message);
                    userStates.remove(Long.parseLong(chatId));
                    ApiRequest.createTask(username, task);
                    sendMessage(chatId, "✅ Задача создана:\n" +
                            "Название: " + task.getTitle() + "\n" +
                            "Описание: " + task.getDescription() + "\n" +
                            "Приоритет: " + task.getPriority());
                } else if (message.contains("/all")) {
                    String[] cmd = message.split(" ");
                    if (cmd.length > 1) {
                        String status = cmd[1];
                        if (!status.equals("да") && !status.equals("нет")) {
                            sendMessage(chatId, "Такого статуса у задач не может быть. Попробуйте выполнить команду снова указав _\"да\"_ или _\"нет\"_");
                        } else {
                            allTasks = ApiRequest.showAllTasksFilteredByStatus(username, status.equals("да"));
                            if (allTasks == null) {
                                if (status.equals("да")) {
                                    sendMessage(chatId, "Может пора выполнить хотя бы одну задачу?");
                                    return;
                                }
                                sendMessage(chatId, "Ого, ты выполнил все задачи");
                                return;
                            }
                            sendMessage(chatId, normalizeListTasks(allTasks));
                        }
                    } else {
                        log.info("Пользователь {} использовал команду \"/all\"", username);
                        allTasks = ApiRequest.showAllTasks(username);
                        if (allTasks == null) {
                            sendMessage(chatId, "Ваш список задач пуст. Возможно пора создать первую?");
                            return;
                        }


                        sendAllTasks(chatId, normalizeListTasks(allTasks), allTasks);
                    }
                } else if (message.equals("/date")) {
                    log.info("Пользователь {} использовал команду \"/date\"", username);
                    allTasks = ApiRequest.showAllTasksSortedByDate(username);
                    sendMessage(chatId, normalizeListTasks(allTasks));
                } else if (message.equals("/priority")) {
                    log.info("Пользователь {} использовал команду \"/priority\"", username);
                    allTasks = ApiRequest.showAllTasksSortedByPriority(username);
                    sendMessage(chatId, normalizeListTasks(allTasks));
                } else if (message.equals("/start")) {
                    sendMessageWithGif(
                            chatId,
                            "Привет, это бот для создания списка задач.\nМожешь ввести команду /help - она покажет тебе все команды.",
                            "https://i.pinimg.com/originals/fc/21/16/fc2116fb21de12a62d4b36c31bbb1e6f.gif");
                } else if (message.equals("/help")) {
                    sendMessageWithGif(
                            chatId,
                            "/create - создание задачи\n/all [да|нет] - фильтрация задач выполнены/не выполнены\n/all - список всех задач\n/date - сортировка по дате\n/priority - сортировка по приоритету",
                            "https://i.pinimg.com/originals/e1/85/18/e18518c6d24257c6fb02e3c95a862d85.gif");
                } else {
                    sendMessageWithGif(chatId, "Такой команды не существует, но может я добавлю её позже :)\n\nможет..", null);
                }
            }
        }

    }

    public void sendMessagePhoto(String chatId, String text, String img, Long taskId) throws IOException {
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
            execute(sp);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendAllTasks(String chatId, String text, List<Task> allTasks) {
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
            execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageWithGif(String chatId, String text, String gif) {
        if (gif == null) {
            gif = "https://i.pinimg.com/originals/70/84/c6/7084c682f10716fcaf0469b550a92b6a.gif";
        }
        SendVideo sv = new SendVideo();
        sv.setChatId(chatId);
        sv.setVideo(new InputFile(gif));
        sv.setCaption(text);
        try {
            execute(sv);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);

        sm.enableMarkdown(true);
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String normalizeListTasks(List<Task> tasks) {
        StringBuilder response = new StringBuilder();
        for (Task task : tasks) {
            response.append(String.format("*Название*: %s\n_Описание_: %s\n_Приоритет_: %s\n_Дата создания_: %s\n%s\n\n==========\n\n",
                    task.getTitle(),
                    task.getDescription(),
                    task.getPriority(),
                    task.getCreatedAt(),
                    task.getCompleted()
            ));
        }
        return response.toString();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }
}
