package de.arbeeco.statcord.events;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.entities.Member;
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
