package de.arbeeco.statcord.util;

import com.google.gson.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import de.arbeeco.statcord.StatcordBot;
import io.javalin.http.NotFoundResponse;
import jakarta.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class Config {
    public static void newGuildConfig(Guild guild) {
        MongoCollection<Document> collection = StatcordBot.configsDB.getCollection(guild.getId());
        try {
            JsonArray obj = (JsonArray) JsonParser.parseReader(new FileReader("configdoc.json"));
            for (JsonElement config : obj) {
                Document document = Document.parse(config.toString());
                collection.insertOne(document);
            }
            JsonObject auth = new JsonObject();
            auth.addProperty("id", "auth");
            auth.addProperty("token", UUID.randomUUID().toString());
            collection.insertOne(Document.parse(auth.toString()));
        } catch (FileNotFoundException e) {
            StatcordBot.logger.error(e.toString());
        }
    }

    public static MongoCollection<Document> getGuildConfig(Guild guild) {
        MongoCollection<Document> collection = StatcordBot.configsDB.getCollection(guild.getId());
        if (collection.countDocuments() == 0) {
            newGuildConfig(guild);
        }
        return collection;
    }

    public static Map getConfigCategory(Guild guild, String categoryName) {
        Document document = getGuildConfig(guild).find(eq("id", categoryName)).first();
        if (document == null) {
            throw new NotFoundResponse("Category not found");
        }
        Map<String, Object> map = new Gson().fromJson(document.toJson(), Map.class);
        return map;
    }

    @NotNull
    public static Object getConfigValue(Guild guild, String configCategory, String valueName) {
        Object value = getConfigCategory(guild, configCategory).get(valueName);
        if (value == null) return null;
        if (value.getClass() == Double.class) {
            value = ((Double) value).intValue();
        }
        return value;
    }

    public static void setConfigValue(Guild guild, String categoryName, String valueName, Object input) {
        Object old = getConfigValue(guild, categoryName, valueName);
        if (old == null) return;
        if (input.getClass() != old.getClass()) return;
        update(guild, categoryName, Updates.set(valueName, input));
    }

    public static UpdateResult update(Guild guild, String categoryName, Bson updates) {
        MongoCollection<Document> collection = StatcordBot.configsDB.getCollection(guild.getId());
        if (collection.countDocuments() == 0) {
            newGuildConfig(guild);
        }
        return collection.updateOne(eq("id", categoryName), updates);
    }
}
