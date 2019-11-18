# Тестовый чат

## Подготовка
Запускаем базу данных
```bash
docker container run --detach --publish 27017:27017 mongo
```

## Запуск приложения
```bash
gradle run
```

## Сборка приложения
```bash
gradle clean fatJar
```
```bash
java -jar build/libs/chat-fat-1.0-SNAPSHOT.jar
```

## Использование
### Клиент
[http://localhost:8082/](http://localhost:8082/)

### История сообщений
[http://localhost:8081/getHistory](http://localhost:8081/getHistory)

### Загрузка файлов версия 1
[http://localhost:8082/swagger/](http://localhost:8082/swagger/)

### Загрузка файлов версия через eventbus
[http://localhost:8082/swagger/index2.html](http://localhost:8082/swagger/index2.html)