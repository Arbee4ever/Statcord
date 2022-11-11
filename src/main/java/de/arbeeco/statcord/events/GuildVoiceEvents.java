package de.arbeeco.statcord.events;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static de.arbeeco.statcord.util.Data.awardVcPoints;
import static de.arbeeco.statcord.util.Data.setVcStart;

public class GuildVoiceEvents extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            setVcStart(event.getGuild(), event.getMember());
        }
        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            awardVcPoints(event.getGuild(), event.getMember());
        }
    }
}
