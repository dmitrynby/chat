package org.example.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.data.FileUploaded;

import java.util.Set;

public class RestServerVerticle extends AbstractVerticle {

    private String fileLocation;

    @Override
    public void start() {
        fileLocation = System.getProperty("java.io.tmpdir") + "/uploads";
        HttpServer httpServer = vertx.createHttpServer();
        Router httpRouter = Router.router(vertx);
        httpRouter.route().handler(BodyHandler.create().setUploadsDirectory(fileLocation));
        httpRouter.post("/sendMessage")
                .handler(request -> {
                    vertx.eventBus().send("router", request.getBodyAsString());
                    request.response().end("ok");
                });
        httpRouter.get("/getHistory")
                .handler(request ->
                        vertx.eventBus().request("getHistory", request.getBodyAsString(), result ->
                                request.response().end(result.result().body().toString())
                        )
                );


        httpRouter.post("/file2").handler(this::fileUploadHandler);

        httpRouter.get("/file2/:id").handler(this::fileDownloadHandler);

        httpServer.exceptionHandler(this::exceptionHandler);

        httpServer.requestHandler(httpRouter).listen(8081);
    }

    private void fileDownloadHandler(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        System.out.println("Download file (id): " + id);
        vertx.eventBus().request("database.file.get", id, result -> {
            routingContext.response()
                    .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8082"); //for swagger ui
            if (result.succeeded()) {
                if (result.result().body() == null) {
                    routingContext.response().setStatusCode(404).end("File with id [" + id + "] not found");
                    return;
                }
                FileUploaded file = new FileUploaded((JsonObject) result.result().body());
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, file.getContentType())
                        .putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getFileName())
                        .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked");

                routingContext.response().setStatusCode(200).sendFile(file.getUploadedFileName());

            } else {
                routingContext.response().setStatusCode(500).end(result.cause().getMessage());
            }
        });
    }

    private void fileUploadHandler(RoutingContext routingContext) {
        Set<FileUpload> set = routingContext.fileUploads();
        routingContext.response()
                .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8082"); //for swagger ui
        if (set.size() == 0) {
            routingContext.response().setStatusCode(400).end("No file body uploaded");
        }
        FileUpload fileUploaded = set.iterator().next();
        System.out.println("Upload file: " + fileUploaded.fileName());
        JsonObject file = new JsonObject().put("fileName", fileUploaded.fileName())
                .put("contentType", fileUploaded.contentType())
                .put("uploadedFileName", fileUploaded.uploadedFileName());
        vertx.eventBus().request("database.file.save", file, result -> {
            if (result.succeeded()) {
                routingContext.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setStatusCode(200)
                        .end(new JsonObject().put("id", result.result().body()).toString());
            }
        });
    }

    private void exceptionHandler(Throwable throwable) {
        throwable.printStackTrace();
    }

}
