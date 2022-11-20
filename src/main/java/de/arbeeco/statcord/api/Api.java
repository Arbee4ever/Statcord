package de.arbeeco.statcord.api;

import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Config;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class Api {
    public static DataApi dataApi;
    public static ConfigApi configApi;
    private static JDA jda;

    public Api(JDA jda) {
        this.jda = jda;
        Javalin app = Javalin.create(config -> {
                    config.plugins.enableCors(cors -> {
                        cors.add(it -> {
                            it.anyHost();
                        });
                    });
                })
                .get("/", ctx -> ctx.result("Hello World"))
                .start();

        app.exception(Exception.class, (exception, ctx) -> {
            JsonObject errorResp = new JsonObject();
            errorResp.addProperty("error", "An Error occurred and the Dev is already informed.");
            File file = new File("ERROR.log");
            jda.retrieveUserById(391979592883372042L).queue(user ->
                    user.openPrivateChannel()
                            .flatMap(privateChannel -> {
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
            file.delete();
            ctx.result(String.valueOf(errorResp));
        });
        app.before(ctx -> {
            ctx.contentType("application/json");
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Headers", "*");
        });

        dataApi = new DataApi(jda, app);
        configApi = new ConfigApi(jda, app);

        StatcordBot.logger.info("API started: " + Date.from(Instant.now()));
    }

    public static boolean isAuthorized(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        if (Objects.equals(ctx.header("Authorization"), jda.getToken()) || Objects.equals(ctx.header("Authorization"), Config.getConfigValue(guild, "auth", "token").getAsString()))
            return true;
        ctx.status(401);
        JsonObject error = new JsonObject();
        error.addProperty("message", "401: Unauthorized");
        ctx.result(error.toString());
        return false;
    }
}
