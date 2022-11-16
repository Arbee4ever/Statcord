package de.arbeeco.statcord.api;

import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Date;

public class Api {
    public static DataApi dataApi;
    public static ConfigApi configApi;

    public Api(JDA jda) {
        Javalin app = Javalin.create()
                .get("/", ctx -> ctx.result("Hello World"))
                .start(8080);

        app.exception(Exception.class, (exception, ctx) -> {
            JsonObject errorResp = new JsonObject();
            errorResp.addProperty("error", "An Error occurred and the Dev is already informed.");
            jda.retrieveUserById(391979592883372042L).queue(user ->
                    user.openPrivateChannel()
                            .flatMap(privateChannel -> {
                                File file = new File("test.log");
                                PrintStream ps = null;
                                try {
                                    ps = new PrintStream(file);
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                                exception.printStackTrace(ps);
                                return privateChannel.sendMessage("Path: " + ctx.fullUrl() + "\nError: ```" + exception.getMessage() + "```").addFiles(FileUpload.fromData(file));
                            })
                            .queue());
            ctx.result(String.valueOf(errorResp));
        });
        app.before(ctx -> {
            ctx.contentType("application/json");
            ctx.header("Access-Control-Allow-Origin", "*");
        });

        dataApi = new DataApi(jda, app);
        configApi = new ConfigApi(jda, app);

        StatcordBot.logger.info("API started: " + Date.from(Instant.now()));
    }
}
