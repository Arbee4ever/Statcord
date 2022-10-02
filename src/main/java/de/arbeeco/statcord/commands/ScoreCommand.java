package de.arbeeco.statcord.commands;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ScoreCommand {
    public ScoreCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (event.getOption("user") != null)  {
            member = event.getOption("user").getAsMember();
        }
        int txtscore = Data.getTextScore(member);
        int vcscore = Data.getVoiceScore(member);
        if (member.getUser().isBot()) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription("User is a bot and has no score.")
                    .setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getAvatarUrl())
                    .setColor(Color.decode("#6f58ac"))
                    .build())
                    .queue();
            return;
        }
        int vcseconds = Data.getVoiceSeconds(member);
        String hours = "";
        String minutes = "0m";
        String seconds = "";
        String timeString;
        if (vcseconds/3600 != 0) {
            hours = vcseconds / 3600 + "h:";
        }
        if ((vcseconds % 3600) / 60 != 0) {
            minutes = (vcseconds % 3600) / 60 + "m";
        }
        if (vcseconds % 60 != 0) {
            seconds = ":" + vcseconds % 60 + "s";
        }

        timeString = String.join("", hours, minutes, seconds);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(member.getAsMention() + "'s current Score is: **" + (txtscore + vcscore) + "** (**" + txtscore + "** text, **" + timeString + "** VC).")
                .setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getAvatarUrl())
                .setColor(Color.decode("#6f58ac"))
                .build())
                .queue();
    }
}
