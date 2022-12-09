package de.arbeeco.statcord.events;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import de.arbeeco.statcord.util.UserDoc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.entities.InteractionMentions;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class MessageSentEvent extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.isWebhookMessage()) return;
        if(event.getMessage().getAuthor().isBot()) return;
        Member member = event.getMember();
        MongoCollection collection = Data.getGuildData(member.getGuild());
        if (collection.find(eq("id", member.getId())).first() == null) {
            collection.insertOne(new UserDoc(member));
        }
        if (event.getMessage().getMentions().isMentioned(event.getGuild().getSelfMember())) {
            event.getMessage().replyEmbeds(new StatcordEmbed()
                    .setTitle("Quick help:")
                    .setDescription("> Every message you send counts \n" +
                                    "> Staying in a voice channel rewards you \n" +
                                    "> Use commands like </score:1014385853348466738> or </graph:1014385853348466739>.")
                    .addField("Links:",
                            "> Get Support: https://arbeeco.de/links/discord \n" +
                                    "> Website: https://statcord.arbeeco.de/ \n" +
                                    "> Vote: https://top.gg/bot/959915020152627271",
                            false)
                    .addField("Commands:",
                            "> </score:1014385853348466738>: View your current Score. \n" +
                                    "> </graph:1014385853348466739>: View your Score in the past. \n" +
                                    "> </leaderboard:1016808818128199792>: View the other Member's Scores.",
                            false)
                    .build()
            ).mentionRepliedUser(false).queue();
        }
        if (!event.isFromGuild()) return;
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
        Data.addTextScore(member, 1, true);
    }
}
