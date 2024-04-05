package de.arbeeco.statcord.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.arbeeco.statcord.messages.GenericMessage;
import de.arbeeco.statcord.messages.MessageType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StatcordMessage {
  public static MessageCreateData getMessage(GenericInteractionCreateEvent event, String messageName) throws IOException {
    String[] variables = new Gson().fromJson(new Gson().fromJson(Files.readString(Path.of("./messages/" + messageName + ".json")), JsonObject.class).getAsJsonObject().get("variables"), String[].class);
    GenericMessage messageType = new GenericMessage(messageName, MessageType.DEFAULT, variables);
    return fromJson(messageType.getMessage(event));
  }

  public static MessageCreateData fromJson(JsonObject messageJson) {
    JsonArray embeds = messageJson.get("embeds").getAsJsonArray();
    List<MessageEmbed> messageEmbeds = new ArrayList<>();
    for (JsonElement embed : embeds) {
      messageEmbeds.add(EmbedBuilder.fromData(DataObject.fromJson(embed.toString())).build());
    }
    String content = "";
    if (messageJson.has("content")) {
      content = messageJson.get("content").getAsString();
    }
    return new MessageCreateBuilder()
            .addContent(content)
            .addEmbeds(messageEmbeds)
            .build();
  }
}
