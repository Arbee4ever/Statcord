package de.arbeeco.statcord.events;

import de.arbeeco.statcord.Statcord;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.MDC;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class GuildVoiceEvents extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        MDC.put("guild.id", event.getGuild().getId());
        if (event.getMember().getUser().isBot()) return;
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            User user = event.getMember().getUser();
            Statcord.logger.info("Someone joined VC! bot=" + user.isBot() + " Name: " + user.getEffectiveName());
            Date lastjoin = (Date) Data.getMemberValue(event.getMember().getUser(), event.getGuild(), "voicestart");
            if (lastjoin != null) {
                Date now = new Date();
                long diff = now.getTime() - lastjoin.getTime();
                for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
                    Data.appendVoiceHistory(event.getMember().getUser(), event.getGuild(), true, 0);
                }
            }
            Data.setVcStart(event.getMember().getUser(), event.getGuild());
        }
        if (event.getChannelJoined() == null && event.getChannelLeft() != null) {
            User user = event.getMember().getUser();
            Date lastjoin = (Date) Data.getMemberValue(event.getMember().getUser(), event.getGuild(), "voicestart");
            Statcord.logger.info("Someone left VC! bot=" + user.isBot() + " Name: " + user.getEffectiveName() + " Joined at: " + lastjoin + " Left at: " + new Timestamp(System.currentTimeMillis()));
            Data.awardVcPoints(event.getMember().getUser(), event.getGuild());
        }
    }
}
