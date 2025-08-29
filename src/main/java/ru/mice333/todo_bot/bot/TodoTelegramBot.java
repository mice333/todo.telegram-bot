package ru.mice333.todo_bot.bot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mice333.todo_bot.api.ApiRequest;
import ru.mice333.todo_bot.api.ImageGeneratorApi;
import ru.mice333.todo_bot.model.Task;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.mice333.todo_bot.services.MessageService.*;

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
                if (call.equals("deleteAll")) {
                    ApiRequest.deleteAllTasks(username);
                    deleteMessage(this, chatId, update.getCallbackQuery().getMessage().getMessageId());
                    return;
                } else if (call.contains("complete")) {
                    ApiRequest.updateCompleteStatus(Long.parseLong(call.substring(8)), username);
                    log.info("У задачи с id: {} изменён статус", call.substring(8));
                    deleteMessage(this, chatId, update.getCallbackQuery().getMessage().getMessageId());
                    return;
                } else if (call.contains("delete")) {
                    ApiRequest.deleteTaskById(Long.parseLong(call.substring(6)), username);
                    log.info("Задача с id: {} удалена", call.substring(6));
                    deleteMessage(this, chatId, update.getCallbackQuery().getMessage().getMessageId());
                    return;
                }
                Task task = ApiRequest.findTaskById(Long.parseLong(call));
                log.info("task id: {}", task.getId());
                String img = ImageGeneratorApi.getImageLink(task.getId());
                try {
                    sendMessagePhoto(this, chatId, String.format("*Название*: %s\n_Описание_: %s\n_Приоритет_: %s\n_Дата создания_: %s\n%s\n\n==========\n\n",
                            task.getTitle(),
                            task.getDescription(),
                            task.getPriority(),
                            task.getCreatedAt(),
                            task.getCompleted()
                    ), img, task.getId());
                    return;
                } catch (FileNotFoundException e) {
                    img = "https://i.pinimg.com/736x/e4/6d/87/e46d873dc5389bd3f76102c0cd9df176.jpg";
                    sendMessagePhoto(this, chatId, String.format("*Название*: %s\n_Описание_: %s\n_Приоритет_: %s\n_Дата создания_: %s\n%s\n\n==========\n\n",
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
                    sendMessage(this, chatId, "Введите название задачи");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_TITLE")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setTitle(message);
                    userStates.put(Long.parseLong(chatId), "WAITING_FOR_DESCRIPTION");

                    sendMessage(this, chatId, "Введите описание задачи");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_DESCRIPTION")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setDescription(message);
                    userStates.put(Long.parseLong(chatId), "WAITING_FOR_PRIORITY");

                    sendMessage(this, chatId, "Введите приоритет задачи");
                } else if (userStates.getOrDefault(Long.parseLong(chatId), "").equals("WAITING_FOR_PRIORITY")) {
                    Task task = userTasks.get(Long.parseLong(chatId));
                    task.setPriority(message);
                    userStates.remove(Long.parseLong(chatId));
                    ApiRequest.createTask(username, task);

                    sendMessage(this, chatId, "✅ Задача создана:\n" +
                            "Название: " + task.getTitle() + "\n" +
                            "Описание: " + task.getDescription() + "\n" +
                            "Приоритет: " + task.getPriority());
                } else if (message.contains("/all")) {
                    String[] cmd = message.split(" ");
                    if (cmd.length > 1) {
                        String status = cmd[1];
                        if (!status.equals("да") && !status.equals("нет")) {
                            sendMessage(
                                    this, chatId,
                                    "Такого статуса у задач не может быть. Попробуйте выполнить команду снова указав _\"да\"_ или _\"нет\"_");
                        } else {
                            allTasks = ApiRequest.showAllTasksFilteredByStatus(username, status.equals("да"));
                            if (allTasks == null) {
                                if (status.equals("да")) {
                                    sendMessage(this, chatId, "Может пора выполнить хотя бы одну задачу?");
                                    return;
                                }
                                sendMessage(this, chatId, "Ого, ты выполнил все задачи");
                                return;
                            }
                            sendMessage(this, chatId, normalizeListTasks(allTasks));
                        }
                    } else {
                        log.info("Пользователь {} использовал команду \"/all\"", username);
                        allTasks = ApiRequest.showAllTasks(username);
                        if (allTasks == null) {
                            sendMessage(this, chatId, "Ваш список задач пуст. Возможно пора создать первую?");
                            return;
                        }


                        sendAllTasks(this, chatId, normalizeListTasks(allTasks), allTasks);
                    }
                } else if (message.equals("/date")) {
                    log.info("Пользователь {} использовал команду \"/date\"", username);
                    allTasks = ApiRequest.showAllTasksSortedByDate(username);
                    sendMessage(this, chatId, normalizeListTasks(allTasks));
                } else if (message.equals("/priority")) {
                    log.info("Пользователь {} использовал команду \"/priority\"", username);
                    allTasks = ApiRequest.showAllTasksSortedByPriority(username);
                    sendMessage(this, chatId, normalizeListTasks(allTasks));
                } else if (message.equals("/start")) {
                    sendMessageWithGif(
                            this, chatId,
                            "Привет, это бот для создания списка задач.\nМожешь ввести команду /help - она покажет тебе все команды.",
                            "https://i.pinimg.com/originals/fc/21/16/fc2116fb21de12a62d4b36c31bbb1e6f.gif");
                } else if (message.equals("/help")) {
                    sendMessageWithGif(
                            this, chatId,
                            "/create - создание задачи\n/all [да|нет] - фильтрация задач выполнены/не выполнены\n/all - список всех задач\n/date - сортировка по дате\n/priority - сортировка по приоритету",
                            "https://i.pinimg.com/originals/e1/85/18/e18518c6d24257c6fb02e3c95a862d85.gif");
                } else {
                    sendMessageWithGif(this, chatId, "Такой команды не существует, но может я добавлю её позже :)\n\nможет..", null);
                }
            }
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
