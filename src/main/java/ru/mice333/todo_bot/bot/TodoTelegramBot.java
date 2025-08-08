package ru.mice333.todo_bot.bot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mice333.todo_bot.api.ApiRequest;
import ru.mice333.todo_bot.model.Task;

import java.util.List;

@Slf4j
@Component
public class TodoTelegramBot extends TelegramLongPollingBot {

    @Autowired
    private BotConfig botConfig;

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        log.info("update используется");
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText().trim();
            String chatId = update.getMessage().getChatId().toString();
            String username = update.getMessage().getChat().getUserName();
            String[] cmd = message.split(" ");
            List<Task> allTasks;
            if (cmd.length == 1) {
                switch (cmd[0]) {
                    case "/all":
                        log.info("Пользователь {} использовал команду \"/all\"", username);
                        allTasks = ApiRequest.showAllTasks(username);
                        if (allTasks == null) {
                            sendMessage(chatId, "Ваш список задач пуст. Возможно пора создать первую?");
                            break;
                        }
                        sendMessage(chatId, normalizeListTasks(allTasks));
                        break;
                    case "/date":
                        log.info("Пользователь {} использовал команду \"/date\"", username);
                        allTasks = ApiRequest.showAllTasksSortedByDate(username);
                        sendMessage(chatId, normalizeListTasks(allTasks));
                        break;
                    case "/priority":
                        log.info("Пользователь {} использовал команду \"/priority\"", username);
                        allTasks = ApiRequest.showAllTasksSortedByPriority(username);
                        sendMessage(chatId, normalizeListTasks(allTasks));
                        break;
                    default:
                        sendMessageWithGif(chatId, "Такой команды не существует, но может я добавлю её позже :)\n\nможет..");
                }
            }
            if (cmd.length > 1) {
                switch (cmd[0]) {
                    case "/all":
                        String status = cmd[1];
                        if (!status.equals("да") && !status.equals("нет")) {
                            sendMessage(chatId, "Такого статуса у задач не может быть. Попробуйте выполнить команду снова указав _\"да\"_ или _\"нет\"_");
                        } else {
                            allTasks = ApiRequest.showAllTasksFilteredByStatus(username, status.equals("да"));
                            if (allTasks == null) {
                                if (status.equals("да")) {
                                    sendMessage(chatId, "Может пора выполнить хотя бы одну задачу?");
                                    break;
                                }
                                sendMessage(chatId, "Ого, ты выполнил все задачи");
                                break;
                            }
                            sendMessage(chatId, normalizeListTasks(allTasks));
                        }
                        break;
                    case "/create":
                        Task task = new Task();
                        task.setTitle(cmd[1]);
                        task.setDescription(cmd[2]);
                        task.setPriority(cmd[3]);
                        log.info("title: {}, desc: {}", cmd[1], cmd[2]);
                        ApiRequest.createTask(update.getMessage().getChat().getUserName(), task);
                }
            }
        }
    }

    public void sendMessageWithGif(String chatId, String text) {
        SendVideo sv = new SendVideo();
        sv.setChatId(chatId);
        sv.setVideo(new InputFile("https://i.pinimg.com/originals/70/84/c6/7084c682f10716fcaf0469b550a92b6a.gif"));
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
    public void sendMessageV2(String chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);
        sm.enableMarkdownV2(true);
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
