package de.arbeeco.statcord.commands.slash;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import de.arbeeco.statcord.util.UserDoc;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class AddCommand {
  public AddCommand(SlashCommandInteractionEvent event) {
    if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
      User user = event.getOption("user").getAsUser();
      if (!user.isBot()) {
        MongoCollection<Document> collection = Data.getGuildData(event.getGuild());
        if (collection.find(eq("id", user.getId())).first() == null) {
          collection.insertOne(new UserDoc(user));
        }
        int textScoreOld = Data.getTextMessages(user, event.getGuild());
        int voiceScoreOld = Data.getVoiceSeconds(user, event.getGuild());
        int addTextScore = 0;
        int addVoiceScore = 0;
        if (event.getOption("textmessages") != null) {
          addTextScore = event.getOption("textmessages").getAsInt();
        }
        if (event.getOption("voiceseconds") != null) {
          addVoiceScore = event.getOption("voiceseconds").getAsInt();
        }
        Data.addTextMessages(user, event.getGuild(), addTextScore, false);
        Data.addVoiceSeconds(user, event.getGuild(), addVoiceScore);
        event.replyEmbeds(new StatcordEmbed().setDescription(user.getAsMention())
                .addField("Messages", textScoreOld + " + " + addTextScore + " = " + Data.getTextMessages(user, event.getGuild()), false)
                .addField("Seconds in Voicechat", voiceScoreOld + " + " + addVoiceScore + " = " + Data.getVoiceSeconds(user, event.getGuild()), false)
                .build()).setEphemeral(true).queue();
        return;
      }
      event.replyEmbeds(new StatcordEmbed().setDescription("User is a Bot and has no Score.")
                      .build())
              .setEphemeral(true)
              .queue();
      return;
    }
    event.replyEmbeds(new StatcordEmbed().setDescription("Sorry, but you need the Administrator Permission to add Score.")
                    .build())
            .setEphemeral(true)
            .queue();
  }
}
