package de.arbeeco.statcord.events;

import de.arbeeco.statcord.Statcord;
import de.arbeeco.statcord.util.Data;
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

public class ConnectionEvents extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<Guild> guilds = event.getJDA().getGuilds();
        Statcord.shardManager.setActivity(Activity.watching(guilds.size() + " Servers."));
        Statcord.updateTopGG();
        for (Guild guild: guilds) {
            List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
            for (VoiceChannel voiceChannel: voiceChannels) {
                List<Member> members = voiceChannel.getMembers();
                for (Member member : members) {
                    if (member.getUser().isBot()) return;
                    Data.setVcStart(member.getUser(), guild);
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
                    if (member.getUser().isBot()) return;
                    Data.awardVcPoints(member.getUser(), guild);
                }
            }
        }
    }
}
