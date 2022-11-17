package de.arbeeco.statcord.events;

import de.arbeeco.statcord.StatcordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static de.arbeeco.statcord.util.Data.awardVcPoints;
import static de.arbeeco.statcord.util.Data.setVcStart;

public class ConnectionEvents extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<Guild> guilds = event.getJDA().getGuilds();
        StatcordBot.shardManager.setActivity(Activity.watching(guilds.size() + " Servers."));
        for (Guild guild: guilds) {
            List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
            for (VoiceChannel voiceChannel: voiceChannels) {
                List<Member> members = voiceChannel.getMembers();
                for (Member member : members) {
                    if (member.getUser().isBot()) return;
                    setVcStart(guild, member);
                }
            }
        }
    }

    @Override
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        awardPoints(event.getJDA());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        awardPoints(event.getJDA());
    }

    private void awardPoints(JDA jda) {
        List<Guild> guilds = jda.getGuilds();
        for (Guild guild: guilds) {
            List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
            for (VoiceChannel voiceChannel : voiceChannels) {
                List<Member> members = voiceChannel.getMembers();
                for (Member member : members) {
                    awardVcPoints(guild, member);
                }
            }
        }
    }
}
