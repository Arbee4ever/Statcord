package de.arbeeco.statcord.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.descending;
import static spark.Spark.get;
import static spark.Spark.port;

public class StatcordApi {
    public StatcordApi(JDA jda) throws LoginException {
        port(80);

        get("/guilds", ((request, response) -> {
            List<Guild> mutGuilds;
            List<Guild> allGuilds = new ArrayList<>(jda.getGuilds());
            JsonObject respArr = new JsonObject();
            if (request.queryParams("user") != null) {
                mutGuilds = new ArrayList<>(jda.getMutualGuilds(jda.getUserById(request.queryParams("user"))));
                JsonArray mutGuildsArr = new JsonArray();
                for (Guild guild: mutGuilds) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("id", guild.getId());
                    jsonObject.addProperty("name", guild.getName());
                    jsonObject.addProperty("icon", guild.getIconUrl());
                    mutGuildsArr.add(jsonObject);
                }
                respArr.add("mutual_guilds", mutGuildsArr);
                allGuilds.removeAll(mutGuilds);
            }

            JsonArray allGuildsArr = new JsonArray();
            for (Guild guild: allGuilds) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", guild.getId());
                jsonObject.addProperty("name", guild.getName());
                jsonObject.addProperty("icon", guild.getIconUrl());
                allGuildsArr.add(jsonObject);
            }
            respArr.add("other_guilds", allGuildsArr);
            return respArr;
        }));

        get("/guilds/:guildId", ((request, response) -> {
            MongoCollection<Document> collection = Data.getGuildData(jda.getGuildById(request.params(":guildId")));
            JsonArray jsonArray = new JsonArray();
            int count = 0;
            for (Document memberData : collection.find().sort(descending("textscore", "voicescore"))) {
                count++;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pos", count);
                jsonObject.addProperty("id", memberData.getString("id"));
                jsonObject.addProperty("score", memberData.getInteger("textscore") + memberData.getInteger("voicescore") / 10);
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }));

        get("/user/:userId", ((request, response) -> {
            User user = jda.getUserById(request.params(":userId"));
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", user.getName() + "#" + user.getDiscriminator());
            jsonObject.addProperty("pfp", user.getAvatarUrl());
            return jsonObject;
        }));
    }
}
