package de.arbeeco.statcord.api;

import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Config;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.security.RouteRole;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Api {
    public static DataApi dataApi;
    public static ConfigApi configApi;
    JDA jda;

    public Api(JDA jda) {
        this.jda = jda;
        dataApi = new DataApi(jda);
        configApi = new ConfigApi(jda);
        Javalin app = Javalin.create(config -> {
                    config.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
                    config.accessManager((handler, context, routeRoles) -> {
                        if (routeRoles.size() == 0) {
                            handler.handle(context);
                            return;
                        }
                        Set<Permissions> userRoles = getUserRole(context);
                        if (userRoles.containsAll(routeRoles)) {
                            handler.handle(context);
                        } else {
                            context.status(401).result("Unauthorized");
                        }
                    });
                })
                .routes(() -> {
                    get(ctx -> ctx.result("Hello World"));
                    path("guilds", () -> {
                        get(dataApi::getGuilds);
                        path("{guildId}", () -> {
                            get(dataApi::getGuildById);
                            get("config", configApi::getGuildConfig, Permissions.GUILD_CONFIG);
                            patch("{category}", configApi::setGuildConfig, Permissions.GUILD_CONFIG);
                            get("{category}", configApi::getGuildConfigCategory, Permissions.GUILD_CONFIG);
                        });
                    });
                    path("logs", () -> {
                        get(dataApi::getLogFiles, Permissions.ADMINISTRATOR);
                        delete(dataApi::deleteLogFiles, Permissions.ADMINISTRATOR);
                        path("{filename}", () -> {
                            get(dataApi::getLogFile, Permissions.ADMINISTRATOR);
                            delete(dataApi::deleteLogFile, Permissions.ADMINISTRATOR);
                        });
                    });
                })
                .start();

        app.exception(Exception.class, (exception, ctx) -> {
            JsonObject errorResp = new JsonObject();
            errorResp.addProperty("error", "An Error occurred and the Dev is already informed.");
            File file = new File("ERROR.log");
            jda.retrieveUserById(391979592883372042L).queue(user ->
                    user.openPrivateChannel()
                            .flatMap(privateChannel -> {
                                PrintStream ps;
                                try {
                                    ps = new PrintStream(file);
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                                exception.printStackTrace(ps);
                                return privateChannel.sendMessage("Path: " + ctx.fullUrl() + "\nError: ```" + exception.getMessage() + "```").addFiles(FileUpload.fromData(file));
                            })
                            .queue());
            if (!file.delete()) StatcordBot.logger.error("Logfile not deleted!");
            ctx.json(String.valueOf(errorResp));
        });

        StatcordBot.logger.info("API started: " + Date.from(Instant.now()));
    }

    Set<Permissions> getUserRole(Context ctx) {
        Set<Permissions> userRoles = new HashSet<>();
        if (Objects.equals(ctx.header("Authorization"), jda.getToken())) {
            userRoles.add(Permissions.ADMINISTRATOR);
        }
        if (!ctx.pathParamMap().containsKey("guildId"))
            return userRoles;
        Long guildId = ctx.pathParamAsClass("guildId", Long.class).getOrDefault(0L);
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new NotFoundResponse("Guild not found");
        }
        if (Objects.equals(ctx.header("Authorization"), jda.getToken()) || Objects.equals(ctx.header("Authorization"), Config.getConfigValue(guild, "auth", "token"))) {
            userRoles.add(Permissions.GUILD_CONFIG);
            return userRoles;
        }
        return userRoles;
    }

    enum Permissions implements RouteRole {
        GUILD_CONFIG,
        ADMINISTRATOR
    }
}
