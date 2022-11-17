package de.arbeeco.statcord.util;

import com.google.gson.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.FileNotFoundException;
import java.io.FileReader;

import static com.mongodb.client.model.Filters.eq;

public class Config {
    static ConnectionString connectionString = new ConnectionString(Data.config.get("connection_string").getAsString());
    static MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
            .build();
    static MongoClient mongoClient = MongoClients.create(settings);
    public static MongoDatabase database = mongoClient.getDatabase("Configs");
    public static void newGuildConfig(Guild guild) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        JsonObject jsonObject = new JsonObject();
        try {
            Object obj = JsonParser.parseReader(new FileReader("configdoc.json"));

            jsonObject = (JsonObject) obj;
        } catch (FileNotFoundException e) {
            System.out.print(e);
        }
        Document document = Document.parse(jsonObject.toString());
        collection.insertOne(document);
    }

    public static MongoCollection<Document> getGuildConfig(Guild guild) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        if (collection.countDocuments() == 0) {
            newGuildConfig(guild);
        }
        return collection;
    }

    public static JsonObject getConfigCategory(Guild guild, String categoryName) {
        Document document = getGuildConfig(guild).find(eq("id", categoryName)).first();
        JsonObject data = new JsonObject();
        if (document == null) {
            data.addProperty("404", "Category not found");
            return data;
        }
        data = new Gson().fromJson(document.toJson(), JsonObject.class);
        data.remove("_id");
        data.remove("name");
        data.remove("id");
        return data;
    }

    public static JsonElement getConfigValue(Guild guild, String categoryName, String valueName) {
        return getConfigCategory(guild, categoryName).get(valueName).getAsJsonObject().get("value");
    }

    public static void setValue(Guild guild, String valueName, String input) {
        update(guild, Updates.set(valueName, input));
    }

    public static UpdateResult update(Guild guild, Bson updates) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        if (collection.countDocuments() == 0) {
            newGuildConfig(guild);
        }
        return collection.updateOne(eq("id", "config"), updates);
    }
}
