package de.arbeeco.statcord.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import de.arbeeco.statcord.StatcordBot;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.MDC;

import java.util.List;

public class StatcordLogger extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (MDC.get("guild.id") != null) {
            if (eventObject.getLevel().equals(Level.ERROR)) {
                String guildId = MDC.get("guild.id");
                Guild guild = StatcordBot.shardManager.getGuildById(guildId);
                List ranks = (List) Config.getConfigValue(guild, "errors", "errors");
                ranks.add(eventObject.getThrowableProxy().getMessage());
                Config.setConfigValue(guild, "errors", "errors", ranks);
            }
        }
    }
}
