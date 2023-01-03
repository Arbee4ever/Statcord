package de.arbeeco.statcord.events;

import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Timestamp;
import java.util.Date;

import static com.mongodb.client.model.Filters.eq;

public class GuildVoiceEvents extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return;
        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            Data.setVcStart(event.getMember());
        }
        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            User user = event.getMember().getUser();
            Date lastjoin = Data.getGuildData(event.getGuild()).find(eq("id", event.getMember().getId())).first().getDate("voicestart");
            StatcordBot.logger.info("Someone left VC! bot=" + user.isBot() + " Name#disciminator: " + user.getName() + "#" + user.getDiscriminator() + " Joined at: " + lastjoin + " Left at: " + new Timestamp(System.currentTimeMillis()));
            Data.awardVcPoints(event.getGuild(), event.getMember());
        }
    }
}
