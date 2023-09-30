package de.arbeeco.statcord.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.arbeeco.statcord.messages.MessageType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class StatcordMessage {
  public static MessageCreateData getMessage(GenericInteractionCreateEvent event, String messageName) {
    MessageType messageType = new MessageType("score", "userping", "score", "textmessages", "voiceseconds");
    return fromJson(messageType.getMessage(event));
  }

  public static MessageCreateData fromJson(JsonObject messageJson) {
    JsonArray embeds = messageJson.get("embeds").getAsJsonArray();
    MessageEmbed messageEmbed = EmbedBuilder.fromData(DataObject.fromJson(embeds.get(0).toString())).build();
    String content = "";
    if (messageJson.get("content") != null) {
      content = messageJson.get("content").getAsString();
    }
    return new MessageCreateBuilder()
            .addContent(content)
            .addEmbeds(messageEmbed)
            .build();
  }
}
