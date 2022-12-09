package de.arbeeco.statcord.commands;

import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class AddCommand {
    public AddCommand(SlashCommandInteractionEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            Member member = event.getOption("user").getAsMember();
            if (!member.getUser().isBot()) {
                int textScoreOld = Data.getTextScore(member);
                int voiceScoreOld = Data.getVoiceScore(member);
                int addTextScore = 0;
                int addVoiceScore = 0;
                if (event.getOption("textscore") != null) {
                    addTextScore = event.getOption("textscore").getAsInt();
                }
                if (event.getOption("voicescore") != null) {
                    addVoiceScore = event.getOption("voicescore").getAsInt();
                }
                Data.addTextScore(member, addTextScore, false);
                Data.addVoiceSeconds(member, addVoiceScore);
                event.replyEmbeds(new StatcordEmbed().setDescription(member.getAsMention())
                        .addField("Textcore",  textScoreOld + " + " + addTextScore + " = " + Data.getTextScore(member), true)
                        .addField("Voicecore",  voiceScoreOld + " + " + addVoiceScore + " = " + Data.getVoiceScore(member), true)
                        .build()).setEphemeral(true).queue();
                return;
            }
            event.replyEmbeds(new StatcordEmbed().setDescription("User is a Bot and has no Score.")
                    .setColor(Color.decode("#6f58ac"))
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.replyEmbeds(new StatcordEmbed().setDescription("Sorry, but you need the Administrator Permission to add Score.")
                .setColor(Color.decode("#6f58ac"))
                .build())
                .setEphemeral(true)
                .queue();
    }
}
