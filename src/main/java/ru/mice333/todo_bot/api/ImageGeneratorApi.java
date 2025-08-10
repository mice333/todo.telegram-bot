package ru.mice333.todo_bot.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import ru.mice333.todo_bot.model.Task;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
public class ImageGeneratorApi {

    private static String BASE_URL = "http://localhost:8082/api/";

    public static void createImage(Task task) throws URISyntaxException, IOException, InterruptedException {
        String IMAGE_API = BASE_URL + "image/new";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(IMAGE_API);
        String json = String.format( "{" +
                "\"id\":%d," +
                "\"title\":\"%s\"," +
                "\"description\":\"%s\"," +
                "\"priority\":%d," +
                "\"createdAt\":\"%s\""+
                "}", task.getId(), task.getTitle(), task.getDescription(), Integer.parseInt(task.getPriority()), task.getCreatedAt());
        log.info(task.toString());
        post.addHeader("Content-Type", "application/json");
        StringEntity body = new StringEntity(json, "UTF-8");
        post.setEntity(body);

        client.execute(post);
    }

    public static String getImageLink(Long id) throws IOException {
        log.info("id: {}}", id);
        String IMAGE_API = BASE_URL + "image/" + id;
        log.info(IMAGE_API);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(IMAGE_API);
        HttpResponse response = client.execute(get);

        return EntityUtils.toString(response.getEntity());
    }

}
