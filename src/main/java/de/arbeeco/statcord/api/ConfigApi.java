package de.arbeeco.statcord.api;

import com.google.gson.*;
import com.mongodb.client.FindIterable;
import de.arbeeco.statcord.util.Config;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;

import java.lang.reflect.MalformedParametersException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.arbeeco.statcord.api.Api.isAuthorized;

public class ConfigApi {
    private final JDA jda;

    public ConfigApi(JDA jda, Javalin app) {
        this.jda = jda;
        app
                .get("/guilds/{guildId}/config", this::getGuildConfig)
                .patch("/guilds/{guildId}/{category}", this::setGuildConfig)
                .get("/guilds/{guildId}/{category}", this::getGuildConfigCategory);
    }

    private void getGuildConfig(Context ctx) {
        if (!isAuthorized(ctx)) return;
        ctx.result(getConfig(ctx));
    }

    private void setGuildConfig(Context ctx) {
        if (!isAuthorized(ctx)) return;
        try {
            JsonParser.parseString(ctx.body());
        } catch (JsonSyntaxException e) {
            throw new BadRequestResponse("Invalid JSON String");
        }
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        String categoryName = ctx.pathParam("category");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        body.forEach((name, value) -> {
            Config.setConfigValue(guild, categoryName, name, value);
        });
        ctx.result(getConfig(ctx));
        ctx.status(200);
    }

    private void getGuildConfigCategory(Context ctx) {
        if (!isAuthorized(ctx)) return;
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        Map data = Config.getConfigCategory(guild, ctx.pathParam("category"));
        ctx.result(String.valueOf(data));
        ctx.status(200);
    }

    private String getConfig(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        FindIterable<Document> collection = Config.getGuildConfig(guild).find();
        JsonObject respObj = new JsonObject();
        for (Document document : collection) {
            document.remove("_id");
            String id = document.remove("id").toString();
            respObj.add(id, new Gson().fromJson(document.toJson(), JsonObject.class));
        }
        return String.valueOf(respObj);
    }
}
