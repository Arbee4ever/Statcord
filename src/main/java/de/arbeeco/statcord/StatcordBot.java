package de.arbeeco.statcord;

import de.arbeeco.statcord.api.StatcordApi;
import de.arbeeco.statcord.commands.CommandManager;
import de.arbeeco.statcord.events.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;

public class StatcordBot {
    private final ShardManager shardManager;
    public static StatcordApi statcordApi;
    public StatcordBot(String[] args) throws LoginException {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(args[0])
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setEventPassthrough(true)
                .setActivity(Activity.watching("ARBEE's code."));

        shardManager = builder.build();

        shardManager.addEventListener(
                new ConnectionEvents(),
                new GuildEvents(),
                new GuildMemberEvents(),
                new GuildVoiceEvents(),
                new MessageSentEvent(),
                new CommandManager()
        );
    }

    public static void main(String[] args) throws LoginException {
        StatcordBot statcordBot = new StatcordBot(args);
        statcordApi = new StatcordApi(statcordBot.shardManager.retrieveApplicationInfo().getJDA());
    }
}