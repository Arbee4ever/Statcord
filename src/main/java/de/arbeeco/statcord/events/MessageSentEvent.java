package de.arbeeco.statcord.events;

import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class MessageSentEvent extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getMentions().isMentioned(event.getGuild().getSelfMember())) {
            event.getMessage().replyEmbeds(new EmbedBuilder()
                    .setTitle("Quick help:")
                    .setDescription("> Every message you send counts \n" +
                                    "> Staying in a voice channel rewards you \n" +
                                    "> Use commands like </score:1014385853348466738> or </graph:1014385853348466739>.")
                    .addField("Links:",
                            "> Get Support: https://arbeeco.de/links/discord \n" +
                                    "> Website: https://statcord.arbeeco.de/",
                            false)
                    .addField("Commands:",
                            "> </score:1014385853348466738>: View your current Score. \n" +
                                    "> </graph:1014385853348466739>: View your Score in the past. \n" +
                                    "> </leaderboard:1016808818128199792>: View the other Member's Scores.",
                            false)
                    .setColor(Color.decode("#6f58ac"))
                    .build()
            ).mentionRepliedUser(false).queue();
        }
        if (!event.isFromGuild()) return;
        Member member = event.getMember();
        if (!member.getUser().isBot()) {
            Date lastm = Data.getLastMsg(member);
            Calendar zero = new GregorianCalendar();
            zero.set(Calendar.HOUR_OF_DAY, 0);
            zero.set(Calendar.MINUTE, 0);
            zero.set(Calendar.SECOND, 0);
            zero.set(Calendar.MILLISECOND, 0);
            if (lastm.before(zero.getTime())) {
                Date now = new Date();
                long diff = now.getTime() - lastm.getTime();
                for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
                    Data.appendTextHistory(member, true, 0);
                }
            }
            Data.addTextScore(member, 1);
        }
    }
}
