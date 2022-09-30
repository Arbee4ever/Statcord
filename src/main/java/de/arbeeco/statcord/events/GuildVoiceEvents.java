package de.arbeeco.statcord.events;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static de.arbeeco.statcord.util.Data.awardVcPoints;
import static de.arbeeco.statcord.util.Data.setVcStart;

public class GuildVoiceEvents extends ListenerAdapter {
    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        setVcStart(event.getGuild(), event.getMember());
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        awardVcPoints(event.getGuild(), event.getMember());
    }
}
