package de.arbeeco.statcord.events;

import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class MessageSentEvent extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.isWebhookMessage()) return;
        if(event.getMessage().getAuthor().isBot()) return;
        if (event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser()) && !event.getMessage().getMentions().mentionsEveryone()) {
            event.getMessage().replyEmbeds(new StatcordEmbed()
                    .setTitle("Quick help:")
                    .setDescription("""
                            > Every message you send counts\s
                            > Staying in a voice channel rewards you\s
                            > Use commands like </score:1014385853348466738> or </graph:1014385853348466739>.""")
                    .addField("Links:",
                            """
                                    > Get Support: https://arbeeco.de/links/discord\s
                                    > Website: https://statcord.arbeeco.de/\s
                                    > Vote: https://top.gg/bot/959915020152627271""",
                            false)
                    .addField("Commands:",
                            """
                                    > </score:1014385853348466738>: View your current Score.\s
                                    > </graph:1014385853348466739>: View your Score in the past.\s
                                    > </leaderboard:1016808818128199792>: View the other Member's Scores.""",
                            false)
                    .build()
            ).mentionRepliedUser(false).queue();
        }
        if (!event.isFromGuild()) return;
        Date lastm = Data.getLastMsg(event.getAuthor(), event.getGuild());
        Calendar zero = new GregorianCalendar();
        zero.set(Calendar.HOUR_OF_DAY, 0);
        zero.set(Calendar.MINUTE, 0);
        zero.set(Calendar.SECOND, 0);
        zero.set(Calendar.MILLISECOND, 0);
        if (lastm != null && lastm.before(zero.getTime())) {
            Date now = new Date();
            long diff = now.getTime() - lastm.getTime();
            for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
                Data.appendTextHistory(event.getAuthor(), event.getGuild(), true, 0);
            }
        }
        Data.addTextMessages(event.getAuthor(), event.getGuild(), 1, true);
    }
}
