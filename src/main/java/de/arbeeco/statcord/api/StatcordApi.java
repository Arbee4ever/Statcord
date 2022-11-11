package de.arbeeco.statcord.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Config;
import de.arbeeco.statcord.util.Data;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.descending;

public class StatcordApi {
    private final JDA jda;
    public StatcordApi(JDA jda) {
        this.jda = jda;
        var app = Javalin.create()
                .get("/", ctx -> ctx.result("Hello World"))
                .get("/guilds", this::getGuilds)
                .get("/guilds/{guildId}", this::getGuildById)
                .get("/guilds/{guildId}/config", this::getGuildConfig)
                .post("/guilds/{guildId}/config", this::setGuildConfig)
                .get("/guilds/{guildId}/config/{category}", this::getGuildConfigCategory)
                .get("/user/{userId}", this::getUser)
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
                System.out.print(ctx.queryParams("user").size());
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
            for (Document memberData : collection.find().sort(descending("textscore", "voicescore"))) {
                count++;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pos", count);
                jsonObject.addProperty("id", memberData.getString("id"));
                jsonObject.addProperty("score", (memberData.getInteger("textscore")/ Config.getConfigValue(guild, "conversionvalues", "msgsperpoint").getAsInt()) + (memberData.getInteger("voicescore")/Config.getConfigValue(guild, "conversionvalues", "vcsecondsperpoint").getAsInt()));
                jsonArray.add(jsonObject);
            }
            respObject.add("members", jsonArray);
            ctx.result(String.valueOf(respObject));
        }
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
