package de.arbeeco.statcord.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Config;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Date;

public class ConfigApi {
    private final JDA jda;
    public ConfigApi(JDA jda, Javalin app) {
        this.jda = jda;
        app
                .get("/guilds/{guildId}/config", this::getGuildConfig)
                .post("/guilds/{guildId}/config", this::setGuildConfig)
                .get("/guilds/{guildId}/config/{category}", this::getGuildConfigCategory);
    }

    private void getGuildConfig(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        FindIterable<Document> collection = Config.getGuildConfig(guild).find();
        JsonArray respArr = new JsonArray();
        for (Document document: collection) {
            JsonObject jsonObject = new Gson().fromJson(document.toJson(), JsonObject.class);
            jsonObject.remove("_id");
            respArr.add(jsonObject);
        }
        ctx.result(String.valueOf(respArr));
    }
    private void setGuildConfig(Context ctx) {
        ctx.status(501);
        ctx.result(String.valueOf(new JsonObject()));
    }
    private void getGuildConfigCategory(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        if (guild == null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("404", "Guild not found");
            ctx.status(404);
            ctx.result(String.valueOf(jsonObject));
            return;
        }
        JsonObject data = Config.getConfigCategory(guild, ctx.pathParam("category")).getAsJsonObject();
        ctx.result(String.valueOf(data));
    }
}
