package de.arbeeco.statcord.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
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
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Sorts.descending;

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
                user = jda.getUserById(userId.get(0));
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
                User user = jda.getUserById(ctx.queryParamAsClass("user", Long.class).getOrThrow(error -> new BadRequestResponse("Invalid User-ID")));
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
            FindIterable<Document> data = collection.find().sort(Sorts.descending("voicescore", "textscore", "id")).skip(page * limit).limit(limit);
            for (Document memberData : data) {
                Member member = guild.getMemberById(memberData.getString("id"));
                if (member == null && (boolean) Config.getConfigValue(guild, "data", "deleteonleave")) {
                    Data.deleteMemberData(guild, (String) memberData.get("id"));
                    continue;
                }
                count++;
                JsonObject jsonObject = new Gson().fromJson(memberData.toJson(), JsonObject.class);
                jsonObject.remove("_id");
                JsonElement name = jsonObject.remove("name");
                jsonObject.addProperty("name", member.getEffectiveName() + "#" + member.getUser().getDiscriminator());
                jsonObject.addProperty("pos", count + (page * limit));
                jsonObject.addProperty("avatar", member.getUser().getAvatarUrl());
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
}
