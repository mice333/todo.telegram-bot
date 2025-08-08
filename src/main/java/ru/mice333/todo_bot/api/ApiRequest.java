package ru.mice333.todo_bot.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import ru.mice333.todo_bot.model.Task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApiRequest {

    static String BASE_URL = "http://localhost:8080/api/";

    public static void createTask(String username, Task task) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/create?username=" + username;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(TODO_API);
        String json = String.format( "{" +
                "\"title\":\"%s\"," +
                "\"description\":\"%s\"," +
                "\"priority\":%d" +
                "}", task.getTitle(), task.getDescription(), Integer.parseInt(task.getPriority()));
        log.info(task.toString());
        post.addHeader("Content-Type", "application/json");
        StringEntity body = new StringEntity(json, "UTF-8");
        post.setEntity(body);

        client.execute(post);
    }

    public static List<Task> showAllTasks(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks?username=" + username;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(TODO_API)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.statusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksFilteredByStatus(String username,boolean status) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/filter/status?completed=" + status;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(TODO_API)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.statusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksSortedByPriority(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = "http://localhost:8080/tasks";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(TODO_API)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.statusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksSortedByDate(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/filter/date";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(new URI(TODO_API)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.statusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }


    private static List<Task> displayActivity(JsonArray events) {
        List<Task> allTasks = new ArrayList<>();
        for (JsonElement elements : events) {
            JsonObject event = elements.getAsJsonObject();
            System.out.println(event);
            String action;
            String title = event.get("title").getAsString();
            log.info("title: {}", title);
            String description = event.get("description").getAsString();
            log.info("description: {}", description);
            String priority = event.get("priority").getAsString();
            log.info("priority: {}", priority);
            String createdAt = event.get("createdAt").getAsString();
            log.info("createdAt: {}", createdAt);
            String user = event.get("user").getAsJsonObject().get("username").getAsString();
            String completed = event.get("completed").getAsBoolean() ? "✅" : "❌";
            Task task = new Task(title, description, priority, createdAt, user, completed);
            allTasks.add(task);
        }
        return allTasks;
    }

}
