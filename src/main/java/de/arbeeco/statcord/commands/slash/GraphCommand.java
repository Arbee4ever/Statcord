package de.arbeeco.statcord.commands.slash;

import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static de.arbeeco.statcord.util.Data.genGraph;

public class GraphCommand {
    public GraphCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Guild guild = event.getGuild();
        try {
            int days = 30;
            List<User> userList = List.of(event.getMember().getUser());
            List<Member> memberList = List.of();
            if (event.getOption("role") != null) {
                if (event.getOption("role").getAsString().equals(guild.getId())) {
                    memberList = guild.getMembers();
                } else {
                    memberList = guild.getMembersWithRoles(event.getOption("role").getAsRole());
                }
            } else if (event.getOption("user") != null) {
                userList = List.of(event.getOption("user").getAsUser());
            }
            if (event.getOption("days") != null) {
                days = event.getOption("days").getAsInt();
            }
            String filter = "";
            if (event.getOption("filter") != null) {
                filter = event.getOption("filter").getAsString();
            }
            File img;
            if (memberList.size() != 0) {
                img = genGraph(memberList, days, filter);
            } else {
                img = genGraph(userList, guild, days, filter);
            }
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
