package de.arbeeco.statcord.commands.slash;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import de.arbeeco.statcord.util.UserDoc;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;

public class ResetDataCommand {
  public ResetDataCommand(SlashCommandInteractionEvent event) {
    if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
      if (event.getOption("user") != null) {
        User user = event.getOption("user").getAsUser();
        if (!user.isBot()) {
          Data.deleteMemberData(event.getGuild(), user.getId());
          MongoCollection<Document> collection = Data.getGuildData(event.getGuild());
          collection.insertOne(new UserDoc(event.getUser()));
          event.replyEmbeds(new StatcordEmbed().setDescription("Sucessfully reset all data for " + user.getAsMention() + ".")
                          .build())
                  .queue();
          return;
        }
        event.replyEmbeds(new StatcordEmbed().setDescription("User is a bot and has no Score.")
                        .build())
                .queue();
        return;
      } else {
        Data.deleteGuildData(event.getGuild());
        Data.initNewGuildData(event.getGuild());
        event.replyEmbeds(new StatcordEmbed().setDescription("Sucessfully reset all data for everyone.")
                        .build())
                .queue();
        return;
      }
    }
    event.replyEmbeds(new StatcordEmbed().setDescription("Sorry, but you need the Administrator Permission to reset all data.")
                    .build())
            .setEphemeral(true)
            .queue();
  }
}
