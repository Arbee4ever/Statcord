package de.arbeeco.statcord.commands;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.UserDoc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;

import java.awt.*;

import static de.arbeeco.statcord.util.Data.*;

public class ResetDataCommand {
    public ResetDataCommand(SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            if (event.getOption("user") != null) {
                Member member = event.getOption("user").getAsMember();
                if (!member.getUser().isBot()) {
                    deleteMemberData(event.getGuild(), member);
                    MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
                    collection.insertOne(new UserDoc(member));
                    event.replyEmbeds(new EmbedBuilder().setDescription("Sucessfully reset all data for " + member.getAsMention() + ".")
                                    .setColor(Color.decode("#6f58ac"))
                                    .build())
                            .queue();
                    return;
                }
                event.replyEmbeds(new EmbedBuilder().setDescription("User is a bot and has no Score.")
                                .setColor(Color.decode("#6f58ac"))
                                .build())
                        .queue();
                return;
            } else {
                deleteGuildData(event.getGuild());
                initNewData(event.getGuild());
                event.replyEmbeds(new EmbedBuilder().setDescription("Sucessfully reset all data for everyone.")
                        .setColor(Color.decode("#6f58ac"))
                        .build())
                        .queue();
                return;
            }
        }
        event.replyEmbeds(new EmbedBuilder().setDescription("Sorry, but you need the Administrator Permission to reset all data.")
                .setColor(Color.decode("#6f58ac"))
                .build())
                .queue();
    }
}
