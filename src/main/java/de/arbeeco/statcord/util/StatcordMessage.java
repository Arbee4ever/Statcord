package de.arbeeco.statcord.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.entities.EntityBuilder;

public class StatcordMessage {
    public static MessageCreateData fromJson(EntityBuilder entityBuilder, String json) {
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        JsonArray embeds = jsonObject.get("embeds").getAsJsonArray();
        MessageEmbed messageEmbed = entityBuilder.createMessageEmbed(DataObject.fromJson(embeds.get(0).toString()));
        String content = "";
        if (jsonObject.get("content") != null) {
            content = jsonObject.get("content").getAsString();
        }
        MessageCreateData messageCreateData = new MessageCreateBuilder()
                .addContent(content)
                .addEmbeds(messageEmbed)
                .build();
        return messageCreateData;
    }
}
