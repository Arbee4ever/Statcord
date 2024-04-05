package de.arbeeco.statcord.commands.context;

import de.arbeeco.statcord.Statcord;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import de.arbeeco.statcord.util.StatcordMessage;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;

import java.io.IOException;

import static com.mongodb.client.model.Filters.eq;

public class ContextScoreCommand {
  public ContextScoreCommand(UserContextInteractionEvent event) throws IOException {
    User user = event.getTargetMember().getUser();
    if (user.isBot()) {
      event.replyEmbeds(new StatcordEmbed()
                      .setDescription("User is a bot and has no score.")
                      .build())
              .setEphemeral(true)
              .queue();
      return;
    } else if (Statcord.guildsDB.getCollection(event.getGuild().getId()).find(eq("id", user.getId())).first() == null) {
      event.replyEmbeds(new StatcordEmbed()
                      .setDescription("User has not yet been active on this Server and has no score.")
                      .build())
              .setEphemeral(true)
              .queue();
      return;
    }
    int txtscore = Data.getTextScore(user, event.getGuild());
    int vcscore = Data.getVoiceScore(user, event.getGuild());
    int txtmessages = Data.getTextMessages(user, event.getGuild());
    int vcseconds = Data.getVoiceSeconds(user, event.getGuild());
    String hours = "";
    String minutes = "0m";
    String seconds = "";
    String timeString;
    if (vcseconds / 3600 != 0) {
      hours = vcseconds / 3600 + "h:";
    }
    if ((vcseconds % 3600) / 60 != 0) {
      minutes = (vcseconds % 3600) / 60 + "m";
    }
    if (vcseconds % 60 != 0) {
      seconds = ":" + vcseconds % 60 + "s";
    }

    timeString = String.join("", hours, minutes, seconds);
    event.reply(StatcordMessage.getMessage(event, "rankup")).queue();
        /*event.replyEmbeds(new StatcordEmbed()
                        .setDescription(user.getAsMention() + "'s current Score is: **" + (txtscore + vcscore) + "** (**" + txtmessages + "** text, **" + timeString + "** VC).")
                        .build())
                .setEphemeral(true)
                .queue();*/
  }
}
