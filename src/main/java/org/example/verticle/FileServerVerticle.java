package org.example.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.GridFsUploadOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoGridFsClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class FileServerVerticle extends AbstractVerticle {

    private MongoClient client;
    private MongoGridFsClient gridFsClient;


    @Override
    public void start() {
        client = MongoClient.createShared(vertx, new JsonObject()
                .put("db_name", "my_DB"));
        client.createDefaultGridFsBucketService(event -> {
            if (event.succeeded()) {
                gridFsClient = event.result();
            } else {
                event.cause().printStackTrace();
            }
        });

        HttpServer httpServer = vertx.createHttpServer();
        Router httpRouter = Router.router(vertx);

        httpRouter.post("/file1").handler(this::fileUploadHandler);

        httpRouter.get("/file1/:id").handler(this::fileDownloadHandler);

        httpServer.exceptionHandler(this::exceptionHandler);

        httpServer.requestHandler(httpRouter).listen(8083);
    }

    private void fileDownloadHandler(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        System.out.println("Download file (id): " + id);
        final JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", id));
        client.findOne("fs.files", query, new JsonObject(), result -> {
            if (result.succeeded()) {
                routingContext.response()
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8082"); //for swagger ui
                if (result.result() == null) {
                    routingContext.response().setStatusCode(404).end("File with id [" + id + "] not found");
                    return;
                }
                JsonObject metaData = result.result().getJsonObject("metadata");
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, metaData.getString("contentType"))
                        .putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + metaData.getString("fileName"))
                        .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");

                gridFsClient.downloadById(routingContext.response(), id, handler -> routingContext.response().end());

            } else {
                serveError(routingContext, result);
            }
        });
    }

    private void fileUploadHandler(RoutingContext routingContext) {
        routingContext.request().setExpectMultipart(true);

        routingContext.request().uploadHandler(fileUpload -> {

            fileUpload.exceptionHandler(handler -> {
                handler.printStackTrace();
                routingContext.response().setStatusCode(500).end("File not uploaded");
            });

            GridFsUploadOptions options = new GridFsUploadOptions();
            options.setMetadata(new JsonObject()
                    .put("fileName", fileUpload.filename())
                    .put("contentType", fileUpload.contentType())
            );

            gridFsClient.uploadByFileNameWithOptions(fileUpload, fileUpload.filename(), options, event -> {
                if (event.succeeded()) {
                    System.out.println("MongoDB save: " + event.result());
                    routingContext.response()
                            .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8082") //for swagger ui
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .setStatusCode(200)
                            .end(new JsonObject().put("id", event.result()).toString());
                } else {
                    System.out.println("ERROR MongoDB: " + event.cause());
                    serveError(routingContext, event);
                }
            });

        });
    }

    private <T> void serveError(RoutingContext routingContext, AsyncResult<T> result) {
        if (result.failed()) {
            routingContext.response().setStatusCode(500).end(result.cause().getMessage());
        }

    }

    private void exceptionHandler(Throwable throwable) {
        System.out.println("ERROR: " + throwable.getMessage());
        throwable.printStackTrace();
    }

}