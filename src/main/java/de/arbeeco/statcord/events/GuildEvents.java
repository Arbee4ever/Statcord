package de.arbeeco.statcord.events;

import de.arbeeco.statcord.Statcord;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

import static de.arbeeco.statcord.Statcord.shardManager;

public class GuildEvents extends ListenerAdapter {
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        MDC.put("guild.id", event.getGuild().getId());
        Guild guild = event.getGuild();
        JDA jda = event.getJDA();
        Data.initNewGuildData(event.getGuild());
        jda.retrieveUserById(391979592883372042L).queue(user ->
                user.openPrivateChannel()
                        .flatMap(privateChannel -> privateChannel.sendMessage("Joined Server: " + guild.getName() + " Membercount:" + (guild.getMemberCount() - 1)))
                        .queue());
        shardManager.setActivity(Activity.watching(event.getJDA().getGuilds().size() + " Servers."));
        Statcord.updateTopGG();
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        MDC.put("guild.id", event.getGuild().getId());
        Guild guild = event.getGuild();
        JDA jda = event.getJDA();
        Data.deleteGuildData(guild);
        jda.retrieveUserById(391979592883372042L).queue(user ->
                user.openPrivateChannel()
                        .flatMap(privateChannel -> privateChannel.sendMessage("Left Server: " + guild.getName() + " Membercount:" + (guild.getMemberCount() - 1)))
                        .queue());
        shardManager.setActivity(Activity.watching(event.getJDA().getGuilds().size() + " Servers."));
        Statcord.updateTopGG();
    }
}
