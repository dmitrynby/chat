package org.example.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.GridFsUploadOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoGridFsClient;
import org.example.data.FileUploaded;

public class MongoDbVerticle extends AbstractVerticle {
    private MongoClient client;
    private MongoGridFsClient gridFsClient;
    private String fileLocation;
    @Override
    public void start() {
        fileLocation = System.getProperty("java.io.tmpdir") + "uploads";
        client = MongoClient.createShared(vertx, new JsonObject()
                .put("db_name", "my_DB"));
        vertx.eventBus().consumer("database.save", this::saveDb);
        vertx.eventBus().consumer("database.file.save", this::saveFilesDb);
        vertx.eventBus().consumer("database.file.get", this::getFileDb);
        vertx.eventBus().consumer("getHistory", this::getHistory);
        client.createDefaultGridFsBucketService(event -> {
            if (event.succeeded()) {
                gridFsClient = event.result();
            } else {
                event.cause().printStackTrace();
            }
        });
    }

    private void getHistory(Message<String> message) {
        client.find("message", new JsonObject(),
                result -> message.reply(Json.encode(result.result()))
        );
    }
    private void saveDb(Message<String> message) {
        client.insert("message", new JsonObject(message.body()), this::handler);
    }

    private void handler(AsyncResult<String> stringAsyncResult) {
        if (stringAsyncResult.succeeded()) {
            System.out.println("MongoDB save: " + stringAsyncResult.result());

        } else {
            System.out.println("ERROR MongoDB: " + stringAsyncResult.cause());
        }
    }

    private void saveFilesDb(Message<JsonObject> message) {
        GridFsUploadOptions options = new GridFsUploadOptions();
        options.setMetadata(new JsonObject()
                .put("fileName", message.body().getString("fileName"))
                .put("contentType", message.body().getString("contentType"))
        );
        gridFsClient.uploadFileWithOptions(message.body().getString("uploadedFileName"), options, event -> {
            handler(event);
            if (event.succeeded()) {
                message.reply(event.result());
            }
        });
    }

    private void getFileDb(Message<String> message) {
        final String id = message.body();
        System.out.println("Load from db (id): " + id);
        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", id));
        client.findOne("fs.files", query, new JsonObject(), result -> {
            if (result.succeeded()) {
                if (result.result() == null) {
                    message.reply(null);
                    return;
                }
                JsonObject metaData = result.result().getJsonObject("metadata");
                String tempFile = fileLocation + "/" + id;
                gridFsClient.downloadFileByID(id ,tempFile, handler -> {
                    FileUploaded file = new FileUploaded(metaData.getString("fileName"), metaData.getString("contentType"),
                            handler.result(), tempFile );
                    message.reply(file.toJson());
                });
            } else {
                message.fail(500, result.cause().getMessage());
            }
        });
    }
}
