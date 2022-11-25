package de.arbeeco.statcord.api;

import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
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

import static io.javalin.apibuilder.ApiBuilder.*;

public class Api {
    public static DataApi dataApi;
    public static ConfigApi configApi;
    private static JDA jda;

    public Api(JDA jda) {
        this.jda = jda;
        dataApi = new DataApi(jda);
        configApi = new ConfigApi(jda);
        Javalin app = Javalin.create(config -> {
                    config.plugins.enableCors(cors -> {
                        cors.add(CorsPluginConfig::anyHost);
                    });
                    config.accessManager(((handler, context, set) -> {
                        Role userRole = getUserRole(context);
                        RouteRole routeRole = (RouteRole) set.toArray()[0];
                        if (userRole.ordinal() >= Role.valueOf(routeRole.toString()).ordinal()) {
                            handler.handle(context);
                        } else {
                            context.status(401).result("Unauthorized");
                        }
                    }));
                })
                .routes(() -> {
                    get(ctx -> ctx.result("Hello World"), Role.EVERYONE);
                    path("guilds", () -> {
                        get(dataApi::getGuilds, Role.EVERYONE);
                        path("{guildId}", () -> {
                            before(this::beforeGuildId);
                            get(dataApi::getGuildById, Role.GUILD_ADMIN);
                            get("config", configApi::getGuildConfig, Role.GUILD_MODERATOR);
                            patch("{category}", configApi::setGuildConfig, Role.GUILD_MODERATOR);
                            get("{category}", configApi::getGuildConfigCategory, Role.GUILD_MODERATOR);
                        });
                    });
                    path("user", () -> {
                        get("{userId}", dataApi::getUser, Role.EVERYONE);
                    });
                })
                .before("/guilds/{guildId}/*", this::beforeGuildId)
                .before("/guilds/{guildId}", this::beforeGuildId)
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
            ctx.json(String.valueOf(errorResp));
        });

        StatcordBot.logger.info("API started: " + Date.from(Instant.now()));
    }

    Role getUserRole(Context ctx) {
        return Role.GUILD_ADMIN;
    }

    enum Role implements RouteRole {
        EVERYONE, GUILD_USER, GUILD_MODERATOR, GUILD_ADMIN
    }

    public static boolean isAuthorized(Context ctx) {/*
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        if (Objects.equals(ctx.header("Authorization"), jda.getToken()) || Objects.equals(ctx.header("Authorization"), Config.getConfigValue(guild, "auth", "token")))
            return true;
        ctx.status(401);
        JsonObject error = new JsonObject();
        error.addProperty("message", "401: Unauthorized");
        ctx.json(error.toString());
        return false;*/
        return true;
    }

    private void beforeGuildId(Context ctx) {
        if (ctx.pathParam("guildId").isBlank() || !ctx.pathParam("guildId").matches("[0-9]+")) throw new BadRequestResponse("Please provide a valid Guild-ID");
        if (ctx.pathParamAsClass("guildId", String.class).hasValue()) {
            Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
            if (guild == null) {
                throw new NotFoundResponse("Guild not found");
            }
        }
    }
}
