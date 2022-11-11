package de.arbeeco.statcord.commands;

import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static de.arbeeco.statcord.util.Data.genGraph;

public class GraphCommand {
    public GraphCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        try {
            int days = 30;
            List<Member> graphUser = List.of(event.getMember());
            if (event.getOption("role") != null) {
                if (event.getOption("role").getAsString().equals(guild.getId())) {
                    graphUser = guild.getMembers();
                } else {
                    graphUser = guild.getMembersWithRoles(event.getOption("role").getAsRole());
                }
            } else if (event.getOption("user") != null) {
                graphUser = List.of(event.getOption("user").getAsMember());
            }
            if (event.getOption("days") != null) {
                days = event.getOption("days").getAsInt();
            }
            String filter = "";
            if (event.getOption("filter") != null) {
                filter = event.getOption("filter").getAsString();
            }
            File img = genGraph(graphUser, days, filter);
            event.replyFiles(FileUpload.fromData(img))
                    .addEmbeds(new StatcordEmbed()
                            .setDescription(event.getMember().getAsMention())
                            .setImage("attachment://graph.png")
                            .setColor(Color.decode("#6f58ac"))
                            .build())
                    .queue();
            img.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
