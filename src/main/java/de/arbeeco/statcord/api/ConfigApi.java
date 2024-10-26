package de.arbeeco.statcord.api;

import com.google.gson.*;
import com.mongodb.client.FindIterable;
import de.arbeeco.statcord.util.Config;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;

import java.util.Iterator;
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
    JsonObject object = JsonParser.parseString(ctx.body()).getAsJsonObject();
    removeEmptyElements(object);
    removeEmptyElements(object);
    Document doc = Document.parse(String.valueOf(object));
    doc.forEach((name, value) -> Config.setConfigValue(guild, categoryName, name, value));
    ctx.json(String.valueOf(getConfig(ctx)));
    ctx.status(200);
  }

  public void removeEmptyElements(JsonObject jsonObject) {
    Iterator<String> keys = jsonObject.keySet().iterator();
    while (keys.hasNext()) {
      String key = keys.next();
      JsonElement value = jsonObject.get(key);
      if (value instanceof JsonObject) {
        if (value.getAsJsonObject().isEmpty()) {
          jsonObject.remove(key);
          removeEmptyElements(jsonObject);
          return;
        } else {
          removeEmptyElements((JsonObject) value);
        }
      } else if (value instanceof JsonArray) {
        for (JsonElement element : ((JsonArray) value).asList()) {
          if (element.getAsJsonObject().has("color") && element.getAsJsonObject().has("type") && element.getAsJsonObject().size() == 2) {
            ((JsonArray) value).remove(element);
            return;
          }
          removeEmptyElements((JsonObject) element);
        }
      } else if (value.getAsString().isEmpty()) {
        jsonObject.remove(key);
        removeEmptyElements(jsonObject);
        return;
      } else if (key.equals("color")) {
        if (value.getAsString().startsWith("#")) {
          jsonObject.addProperty(key, Long.parseLong(value.getAsString().substring(1), 16));
        } else if (value.getAsString().isEmpty() || !(value.getAsString().length() <= 16777215)) {
          jsonObject.remove(key);
          removeEmptyElements(jsonObject);
          return;
        }
      } else if (value == null || value.toString().isEmpty()) {
        jsonObject.remove(key);
        removeEmptyElements(jsonObject);
        return;
      }
    }
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
