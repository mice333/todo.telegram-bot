package ru.mice333.todo_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.mice333.todo_bot.bot.TodoTelegramBot;

@SpringBootApplication
public class TodoBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoBotApplication.class, args);
	}

}
