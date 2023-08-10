package de.arbeeco.statcord.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Config;
import de.arbeeco.statcord.util.Data;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DataApi {
    private final JDA jda;

    public DataApi(JDA jda) {
        this.jda = jda;
    }

    public void getGuilds(Context ctx) {
        JsonObject respArr = new JsonObject();

        List<Guild> allGuilds = new ArrayList<>(jda.getGuilds());
        List<String> userId = ctx.queryParams("user");
        if (userId.size() != 0) {
            User user = null;
            if (!Objects.equals(userId.get(0), "")) {
                user = jda.retrieveUserById(userId.get(0)).complete();
            }
            if (user != null) {
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
        ctx.json(String.valueOf(respArr));
    }

    public void getGuildById(Context ctx) {
        Guild guild = jda.getGuildById(ctx.pathParamAsClass("guildId", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid Guild-ID")));
        JsonObject respObject = new JsonObject();
        if (guild == null) {
            ctx.status(404).result("Guild not found");
        } else {
            if (ctx.queryParams("user").size() != 0) {
                User user = jda.retrieveUserById(ctx.queryParamAsClass("user", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid User-ID"))).complete();
                if (user == null) {
                    ctx.status(404).result("User not found");
                    return;
                } else if (guild.getMemberById(user.getId()) == null) {
                    ctx.status(404).result("Member not found");
                    return;
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("moderator", guild.getMemberById(user.getId()).hasPermission(Permission.MANAGE_SERVER));
                ctx.json(String.valueOf(jsonObject));
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
            JsonObject values = new Gson().toJsonTree(Config.getConfigCategory(guild, "values")).getAsJsonObject();
            values.remove("_id");
            values.remove("id");
            guildData.add("values", values);
            respObject.add("guild", guildData);
            MongoCollection<Document> collection = Data.getGuildData(guild);
            JsonArray jsonArray = new JsonArray();
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int limit = 100;
            int count = 0;
            AggregateIterable<Document> data = collection.aggregate(Arrays.asList(
                    new Document("$set",
                            new Document("_sum",
                                    new Document("$sum", Arrays.asList("$voiceseconds", "$textmessages"))
                            )
                    ),
                    new Document("$sort",
                            new Document("_sum", -1L).append("id", -1L)
                    ),
                    new Document("$skip", page * limit),
                    new Document("$limit", limit)));
            for (Document memberData : data) {
                User user = StatcordBot.shardManager.retrieveUserById(memberData.getString("id")).complete();
                Member member = guild.getMemberById(memberData.getString("id"));
                if (member == null && (boolean) Config.getConfigValue(guild, "data", "deleteonleave")) {
                    Data.deleteMemberData(guild, (String) memberData.get("id"));
                    continue;
                }
                count++;
                JsonObject jsonObject = new Gson().fromJson(memberData.toJson(), JsonObject.class);
                jsonObject.remove("_id");
                jsonObject.remove("_sum");
                jsonObject.remove("name");
                if (member != null) {
                    jsonObject.addProperty("name", member.getEffectiveName() + "#" + member.getUser().getDiscriminator());
                    jsonObject.addProperty("avatar", member.getEffectiveAvatarUrl());
                } else if (user != null) {
                    jsonObject.addProperty("name", user.getName() + "#" + user.getDiscriminator());
                    jsonObject.addProperty("avatar", user.getAvatarUrl());
                }
                jsonObject.addProperty("pos", count + (page * limit));
                int msgs = jsonObject.remove("textmessages").getAsInt();
                int vcseconds = jsonObject.remove("voiceseconds").getAsInt();
                jsonObject.addProperty("textmessages", msgs);
                jsonObject.addProperty("voiceseconds", vcseconds);
                jsonArray.add(jsonObject);
            }
            respObject.add("members", jsonArray);
            ctx.json(String.valueOf(respObject));
        }
    }

    public void getLogFiles(Context ctx) {
        Stream logFiles = Stream.of(new File("./logs").listFiles())
                .filter(file -> !file.isDirectory())
                .sorted(Comparator.reverseOrder())
                .map(File::getName);
        ctx.json(logFiles.toArray());
        ctx.status(200);
    }

    public void deleteLogFiles(Context ctx) throws IOException {
        JsonArray files = new Gson().fromJson(ctx.body(), JsonArray.class);
        JsonArray deletedFiles = new JsonArray();
        for (JsonElement file : files) {
            String filename = file.getAsString();
            Path logFilePath = Paths.get("./logs/" + filename);
            if (Files.deleteIfExists(logFilePath)) {
                deletedFiles.add(file);
            }
        }
        if (!deletedFiles.isEmpty()) {
            ctx.json(deletedFiles.toString());
            ctx.status(202);
        } else {
            ctx.status(404);
        }
    }

    public void getLogFile(Context ctx) throws IOException {
        String filename = ctx.pathParam("{filename}");
        Path logFilePath = Paths.get("./logs/" + filename);
        if (Files.exists(logFilePath)) {
            String logContent = Files.readString(logFilePath);
            ctx.result(logContent);
            ctx.status(200);
        } else {
            ctx.status(404);
        }
    }

    public void deleteLogFile(Context ctx) throws IOException {
        String filename = ctx.pathParam("{filename}");
        Path logFilePath = Paths.get("./logs/" + filename);
        if (Files.deleteIfExists(logFilePath)) {
            ctx.status(204);
        } else {
            ctx.status(404);
        }
    }
}
