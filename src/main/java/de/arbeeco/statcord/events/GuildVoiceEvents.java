package de.arbeeco.statcord.events;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildVoiceEvents extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return;
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            Data.setVcStart(event.getGuild(), event.getMember());
        }
        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            Data.awardVcPoints(event.getGuild(), event.getMember());
        }
    }
}
