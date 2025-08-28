package ru.mice333.todo_bot.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import ru.mice333.todo_bot.model.Task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ApiRequest {

    private static String BASE_URL = "http://api:8080/";

    public static void createTask(String username, Task task) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/create?username=" + username;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(TODO_API);
        String json = String.format( "{" +
                "\"title\":\"%s\"," +
                "\"description\":\"%s\"," +
                "\"priority\":\"%s\"" +
                "}", task.getTitle(), task.getDescription(), task.getPriority());
        log.info(task.toString());
        post.addHeader("Content-Type", "application/json");
        StringEntity body = new StringEntity(json, "UTF-8");
        post.setEntity(body);

        log.info("{}", Thread.currentThread().getName());

        HttpResponse httpResponse = client.execute(post);
        task.setId(Long.parseLong(EntityUtils.toString(httpResponse.getEntity())));
        ImageGeneratorApi.createImageAsync(task);

    }

    public static List<Task> showAllTasks(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks?username=" + username;
        log.info("{}", BASE_URL);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(TODO_API);

        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksFilteredByStatus(String username,boolean status) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/filter/status?username="+ username +"&completed=" + status;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(TODO_API);

        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.getEntity().toString()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksSortedByPriority(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/sort/priority?username=" + username;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(TODO_API);

        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.getEntity().toString()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static List<Task> showAllTasksSortedByDate(String username) throws URISyntaxException, IOException, InterruptedException {
        String TODO_API = BASE_URL + "tasks/sort/date?username=" + username;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(TODO_API);

        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 404) {
            System.out.println("tasks not found");
            return null;
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            JsonArray jsonArray = JsonParser.parseString(response.getEntity().toString()).getAsJsonArray();
            System.out.println(jsonArray);
            return displayActivity(jsonArray);
        }

        client.close();
        return null;
    }

    public static Task findTaskById(Long id) throws IOException {
        String TODO_API = BASE_URL + "tasks/task/" + id;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(TODO_API);
        HttpResponse response = client.execute(get);
        String res = EntityUtils.toString(response.getEntity());
        log.info("{}", res);
        JsonElement element = JsonParser.parseString(res);
        JsonObject jsonObject = element.getAsJsonObject();
        Task task = new Task();
        task.setId(jsonObject.get("id").getAsLong());
        task.setTitle(jsonObject.get("title").getAsString());
        task.setDescription(jsonObject.get("description").getAsString());
        task.setPriority(jsonObject.get("priority").getAsString());
        task.setCreatedAt(jsonObject.get("createdAt").getAsString());
        task.setCompleted(jsonObject.get("completed").getAsBoolean() ? "✅" : "❌");
        return task;
    }

    public static void deleteTaskById(Long id, String username) throws IOException {
        String TODO_API = BASE_URL + "tasks/task/" + id + "?username=" + username;
        CloseableHttpClient client = HttpClients.createDefault();
        HttpDelete delete = new HttpDelete(TODO_API);
        client.execute(delete);
    }


    private static List<Task> displayActivity(JsonArray events) {
        List<Task> allTasks = new ArrayList<>();
        for (JsonElement elements : events) {
            JsonObject event = elements.getAsJsonObject();
            System.out.println(event);
            Long id = event.get("id").getAsLong();
            log.info("id: {}", id);
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
            Task task = new Task(id, title, description, priority, createdAt, user, completed);
            allTasks.add(task);
        }
        return allTasks;
    }

}
