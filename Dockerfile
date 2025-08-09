FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/todo-bot.jar todo-bot.jar

CMD ["java", "-jar", "todo-bot.jar"]
