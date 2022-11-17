package de.arbeeco.statcord;

import de.arbeeco.statcord.api.Api;
import de.arbeeco.statcord.events.*;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class StatcordBot {
    public static ShardManager shardManager;
    public static Logger logger = LoggerFactory.getLogger(StatcordBot.class);
    public StatcordBot(String[] args) {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(args[0])
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setEventPassthrough(true);

        shardManager = builder.build();

        shardManager.addEventListener(
                new ConnectionEvents(),
                new GuildEvents(),
                new GuildMemberEvents(),
                new GuildVoiceEvents(),
                new MessageSentEvent(),
                new CommandEvents()
        );
    }

    public static void main(String[] args) {
        StatcordBot statcordBot = new StatcordBot(args);
        JDA jda = statcordBot.shardManager.retrieveApplicationInfo().getJDA();
        new Api(jda);
        logger.info("Start: " + Date.from(Instant.now()));

        //region Commands
        new Thread(() -> {
            String line = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while((line = reader.readLine()) != null) {
                    if(line.equalsIgnoreCase("updateconfigs")) {
                        for (int i = 0; i < shardManager.getShardsTotal(); i++) {
                            List<Guild> guilds = shardManager.getShardById(i).getGuilds();
                            for (Guild guild: guilds) {
                                logger.info(i + ": " + guild.getId());
                            }
                        }
                        logger.info("WIP");
                    } else if(line.equalsIgnoreCase("exit")) {
                        if(shardManager != null) {
                            shardManager.setStatus(OnlineStatus.OFFLINE);
                            shardManager.shutdown();
                            Data.mongoClient.close();
                            logger.info("Bot shutdown at: " + Date.from(Instant.now()));
                            System.exit(0);
                        }
                        reader.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        //endregion
    }
}