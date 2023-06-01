package de.arbeeco.statcord.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;

import java.time.OffsetDateTime;
import java.util.List;

public class StatcordMessage {
  public static MessageCreateData createMessage(GenericInteractionCreateEvent event, String messageName) {
    Object string = Config.getConfigValue(event.getGuild(), "messages", messageName);
    JsonObject jsonObject = new Gson().toJsonTree(string).getAsJsonObject();
    MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
    JsonArray embeds = jsonObject.get("embeds") == null ? null : jsonObject.get("embeds").getAsJsonArray();
    String content = "";
    if (jsonObject.get("content") != null) {
      content = jsonObject.get("content").getAsString();
      messageCreateBuilder.addContent(content);
    }
    if (embeds != null) {
      for (JsonElement embed : embeds) {
        JDAImpl jda = (JDAImpl)event.getJDA();
        EntityBuilder entityBuilder = jda.getEntityBuilder();
        MessageEmbed messageEmbed = entityBuilder.createMessageEmbed(DataObject.fromJson(embed.toString()));
        messageCreateBuilder.addEmbeds(messageEmbed);
      }
    }
    return messageCreateBuilder.build();
  }
}