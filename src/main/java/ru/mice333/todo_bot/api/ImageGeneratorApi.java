package ru.mice333.todo_bot.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.mice333.todo_bot.model.Task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ImageGeneratorApi {

    private static String BASE_URL = "http://imagetextadder:8082/";

    private static StringRedisTemplate redisTemplate;

    public ImageGeneratorApi(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Async("threadPoolTaskExecutor")
    public static CompletableFuture<Void> createImageAsync(Task task) throws URISyntaxException, IOException, InterruptedException {
        log.info("Создание изображения");
        return CompletableFuture.runAsync(() -> {
            long time = System.currentTimeMillis();
            String IMAGE_API = BASE_URL + "image/new";
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(IMAGE_API);
            String json = String.format( "{" +
                    "\"id\":%d," +
                    "\"title\":\"%s\"," +
                    "\"description\":\"%s\"," +
                    "\"priority\":\"%s\"," +
                    "\"createdAt\":\"%s\""+
                    "}", task.getId(), task.getTitle(), task.getDescription(), task.getPriority(), task.getCreatedAt());
            log.info(task.toString());
            post.addHeader("Content-Type", "application/json");
            StringEntity body = new StringEntity(json, "UTF-8");
            post.setEntity(body);
            try {
                client.execute(post);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.info("{}", Thread.currentThread().getName());
            log.info("Создание изображения закончено за: {}ms", System.currentTimeMillis() - time);
        });
    }

    public static String getImageLink(Long id) throws IOException {
        long time = System.currentTimeMillis();
        log.info("Проверка изображения в Redis: {}", redisTemplate.opsForValue().get(String.valueOf(id)));
        if (redisTemplate.opsForValue().get(String.valueOf(id)) != null) {
            log.info("Изображение взято из Redis");
            log.info("{}", System.currentTimeMillis() - time);
            return redisTemplate.opsForValue().get(String.valueOf(id));
        }
        log.info("id: {}", id);
        String IMAGE_API = BASE_URL + "image/" + id;
        log.info(IMAGE_API);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(IMAGE_API);
        HttpResponse response = client.execute(get);
        log.info("Изображение взято с API");
        log.info("{}", System.currentTimeMillis() - time);
        return EntityUtils.toString(response.getEntity());
    }

}
