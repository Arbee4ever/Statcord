package de.arbeeco.statcord.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Config;
import de.arbeeco.statcord.util.Data;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.descending;

public class DataApi {
    private final JDA jda;

    public DataApi(JDA jda, Javalin app) {
        this.jda = jda;
        app
                .get("/guilds", this::getGuilds)
                .get("/guilds/{guildId}", this::getGuildById)
                .get("/user/{userId}", this::getUser);
    }

    private void getGuilds(Context ctx) {
        JsonObject respArr = new JsonObject();

        List<Guild> allGuilds = new ArrayList<>(jda.getGuilds());
        List<String> userId = ctx.queryParams("user");
        if (userId.size() != 0) {
            User user = jda.getUserById(userId.get(0));
            if (user == null) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("404", "User not found");
                ctx.status(404);
                ctx.result(String.valueOf(jsonObject));
                return;
            }

            List<Guild> mutGuilds = new ArrayList<>(jda.getMutualGuilds(user));
            JsonArray mutGuildsArr = new JsonArray();
            for (Guild guild : mutGuilds) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", guild.getId());
                jsonObject.addProperty("name", guild.getName());
                jsonObject.addProperty("icon", guild.getIconId());
                jsonObject.addProperty("moderator", guild.getMemberById(user.getId()).hasPermission(Permission.MANAGE_SERVER));
                mutGuildsArr.add(jsonObject);
            }
            respArr.add("mutual_guilds", mutGuildsArr);
            allGuilds.removeAll(mutGuilds);
        }

        JsonArray allGuildsArr = new JsonArray();
        for (Guild guild : allGuilds) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", guild.getId());
            jsonObject.addProperty("name", guild.getName());
            jsonObject.addProperty("icon", guild.getIconId());
            allGuildsArr.add(jsonObject);
        }
        respArr.add("other_guilds", allGuildsArr);
        ctx.result(String.valueOf(respArr));
    }

    private void getGuildById(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParam("guildId"));
        JsonObject respObject = new JsonObject();
        if (guild == null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("404", "Guild not found");
            ctx.status(404);
            ctx.result(String.valueOf(jsonObject));
        } else {
            if (ctx.queryParams("user").size() != 0) {
                User user = jda.getUserById(ctx.queryParams("user").get(0));
                if (user == null) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("404", "User not found");
                    ctx.status(404);
                    ctx.result(String.valueOf(jsonObject));
                    return;
                } else if (guild.getMemberById(user.getId()) == null) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("404", "Member not found");
                    ctx.status(404);
                    ctx.result(String.valueOf(jsonObject));
                    return;
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("moderator", guild.getMemberById(user.getId()).hasPermission(Permission.MANAGE_SERVER));
                ctx.result(String.valueOf(jsonObject));
                return;
            }
            JsonObject guildData = new JsonObject();
            guildData.addProperty("name", guild.getName());
            guildData.addProperty("id", guild.getId());
            guildData.addProperty("icon", guild.getIconId());
            guildData.addProperty("banner", guild.getBannerId());
            guildData.addProperty("splash", guild.getSplashId());
            guildData.addProperty("vanity", guild.getVanityUrl());
            guildData.addProperty("description", guild.getDescription());
            guildData.addProperty("membercount", guild.getMemberCount());
            guildData.addProperty("textcount", guild.getTextChannels().size());
            guildData.addProperty("voicecount", guild.getVoiceChannels().size());
            guildData.addProperty("rolecount", guild.getRoles().size());
            respObject.add("guild", guildData);
            MongoCollection<Document> collection = Data.getGuildData(guild);
            JsonArray jsonArray = new JsonArray();
            int count = 0;
            int index = ctx.queryParams("page").size() != 0 ? Integer.parseInt(ctx.queryParams("page").get(0)) : 0;
            int limit = 50;
            for (Document memberData : collection.find().sort(descending("textscore", "voicescore")).skip(index * limit).limit(limit)) {
                count++;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pos", count + (index * limit));
                jsonObject.addProperty("id", memberData.getString("id"));
                jsonObject.addProperty("score", (memberData.getInteger("textscore") / Config.getConfigValue(guild, "conversionvalues", "msgsperpoint").getAsInt()) + (memberData.getInteger("voicescore") / Config.getConfigValue(guild, "conversionvalues", "vcsecondsperpoint").getAsInt()));
                jsonArray.add(jsonObject);
            }
            respObject.add("members", jsonArray);
            ctx.result(String.valueOf(respObject));
        }
    }

    private void getUser(Context ctx) {
        User user = jda.getUserById(ctx.pathParam("userId"));
        JsonObject jsonObject = new JsonObject();
        if (user == null) {
            jsonObject.addProperty("404", "User not found");
            ctx.status(404);
        } else {
            jsonObject.addProperty("name", user.getName() + "#" + user.getDiscriminator());
            jsonObject.addProperty("pfp", user.getAvatarId());
        }
        ctx.result(String.valueOf(jsonObject));
    }
}
