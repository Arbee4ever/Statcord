package de.arbeeco.statcord.commands.context;

import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static de.arbeeco.statcord.util.Data.genGraph;

public class ContextGraphCommand {
    public ContextGraphCommand(UserContextInteractionEvent event) {
        if (StatcordBot.guildsDB.getCollection(event.getGuild().getId()).find(eq("id", event.getTargetMember().getId())).first() == null) {
            event.replyEmbeds(new StatcordEmbed()
                            .setDescription("User has not yet been active on this Server and has no score.")
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.deferReply().setEphemeral(true).queue();
        try {
            int days = 30;
            List<User> memberList = List.of(event.getTargetMember().getUser());
            String filter = "";
            File img = genGraph(memberList, event.getGuild(), days, filter);
            event.getHook().editOriginalAttachments(FileUpload.fromData(img))
                    .setEmbeds(new StatcordEmbed()
                            .setDescription(event.getMember().getAsMention())
                            .setImage("attachment://graph.png")
                            .build())
                    .queue();
            img.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
