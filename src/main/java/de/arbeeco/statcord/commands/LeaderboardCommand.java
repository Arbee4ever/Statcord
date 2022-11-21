package de.arbeeco.statcord.commands;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.descending;

public class LeaderboardCommand {
    public LeaderboardCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        MongoCollection<Document> collection =Data.getGuildData(guild);
        EmbedBuilder embed = new StatcordEmbed()
                .setTitle("View full Leaderboard!", "https://statcord.arbeeco.de/leaderboards/" + event.getGuild().getId());
        String description = "";
        int count = 0;
        for (Document memberData : collection.find().sort(descending("textscore", "voicescore")).limit(10)) {
            count++;
            Member  member = guild.getMemberById(memberData.get("id").toString());
            int txtscore = Data.getTextScore(member);
            int vcscore = Data.getVoiceScore(member);
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
            description = description + count + ". " + guild.getMemberById(memberData.get("id").toString()).getAsMention() + ": **" + (txtscore + vcscore) + "** (**" + txtscore + "** text, **" + timeString + "** VC).\n";
        }
        embed.setDescription(description);
        event.replyEmbeds(embed.build()).queue();
    }
}
