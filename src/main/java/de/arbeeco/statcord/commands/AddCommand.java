package de.arbeeco.statcord.commands;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
                Data.addTextScore(member, addTextScore);
                Data.addVoiceSeconds(member, addVoiceScore);
                event.replyEmbeds(new EmbedBuilder().setDescription(member.getAsMention())
                        .addField("Textcore",  textScoreOld + " + " + addTextScore + " = " + Data.getTextScore(member), true)
                        .addField("Voicecore",  voiceScoreOld + " + " + addVoiceScore + " = " + Data.getVoiceScore(member), true)
                        .build()).setEphemeral(true).queue();
                return;
            }
            event.replyEmbeds(new EmbedBuilder().setDescription("User is a Bot and has no Score.").build()).setEphemeral(true).queue();
            return;
        }
        event.replyEmbeds(new EmbedBuilder().setDescription("Sorry, but you need the Administrator Permission to add Score.").build()).setEphemeral(true).queue();
    }
}
