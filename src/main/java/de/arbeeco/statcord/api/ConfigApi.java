package de.arbeeco.statcord.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mongodb.client.FindIterable;
import de.arbeeco.statcord.util.Config;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;

import java.util.Map;

public class ConfigApi {
    private final JDA jda;

    public ConfigApi(JDA jda) {
        this.jda = jda;
    }

    public void getGuildConfig(Context ctx) {
        ctx.json(String.valueOf(getConfig(ctx)));
    }

    public void setGuildConfig(Context ctx) {
        try {
            JsonParser.parseString(ctx.body());
        } catch (JsonSyntaxException e) {
            throw new BadRequestResponse("Invalid JSON String");
        }
        Guild guild = jda.getGuildById(ctx.pathParamAsClass("guildId", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid Guild-ID")));
        String categoryName = ctx.pathParam("category");
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        body.forEach((name, value) -> {
            Config.setConfigValue(guild, categoryName, name, value);
        });
        ctx.json(String.valueOf(getConfig(ctx)));
        ctx.status(200);
    }

    public void getGuildConfigCategory(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParamAsClass("guildId", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid Guild-ID")));
        Map data = Config.getConfigCategory(guild, ctx.pathParam("category"));
        String respObj = new Gson().toJson(String.valueOf(data));
        ctx.json(respObj);
        ctx.status(200);
    }

    private JsonObject getConfig(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParamAsClass("guildId", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid Guild-ID")));
        FindIterable<Document> collection = Config.getGuildConfig(guild).find();
        JsonObject respObj = new JsonObject();
        for (Document document : collection) {
            document.remove("_id");
            String id = document.remove("id").toString();
            respObj.add(id, new Gson().fromJson(document.toJson(), JsonObject.class));
        }
        return respObj;
    }
}
