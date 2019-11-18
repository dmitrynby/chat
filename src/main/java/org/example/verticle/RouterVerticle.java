package org.example.verticle;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.JacksonCodec;
import org.example.data.Data;

public class RouterVerticle extends AbstractVerticle {
    @Override
    public void start() {
        vertx.eventBus().consumer("router", this::router);
    }

    private void router(Message<String> message) {
        if (message.body() != null && !message.body().isEmpty()) {
            System.out.println("Router message: " + message.body());
            JsonObject data = new JsonObject(message.body());
            //Data data = Json.decodeValue(message.body(), Data.class);
            System.out.println(data);
            vertx.eventBus().send("/token/" + data.getString("address"), message.body());

            // Сохраняем сообщение в БД
            vertx.eventBus().send("database.save", message.body());
        }
    }
}
