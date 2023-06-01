package de.arbeeco.statcord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.arbeeco.statcord.api.Api;
import de.arbeeco.statcord.events.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

public class StatcordBot {
    public static ShardManager shardManager;
    public static Logger logger = LoggerFactory.getLogger(StatcordBot.class);
    //region Config
    static JsonObject config;
    static FileReader fileReader;
    static ConnectionString connectionString;
    static MongoClientSettings settings;
    static MongoClient mongoClient;
    public static MongoDatabase guildsDB;
    public static MongoDatabase configsDB;

    //endregion
    public StatcordBot(String[] args) {
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(args[0], GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                .enableCache(CacheFlag.VOICE_STATE)
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
        loadConfig();
        StatcordBot statcordBot = new StatcordBot(args);
        JDA jda = statcordBot.shardManager.retrieveApplicationInfo().getJDA();
        new Api(jda);
        logger.info("Start: " + Date.from(Instant.now()));

        //region Commands
        new Thread(() -> {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.equalsIgnoreCase("updateconfigs")) {
                        for (int i = 0; i < shardManager.getShardsTotal(); i++) {
                            List<Guild> guilds = shardManager.getShardById(i).getGuilds();
                            for (Guild guild : guilds) {
                                logger.info(i + ": " + guild.getId());
                            }
                        }
                        logger.info("WIP");
                    } else if (line.equalsIgnoreCase("updatebotconfig")) {
                        mongoClient.close();
                        loadConfig();
                    } else if (line.equalsIgnoreCase("exit")) {
                        if (shardManager != null) {
                            shardManager.setStatus(OnlineStatus.OFFLINE);
                            shardManager.shutdown();
                            mongoClient.close();
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

    public static void loadConfig() {
        try {
            fileReader = new FileReader("config.json5");
            config = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (FileNotFoundException e) {
            logger.info("config.json missing.");
        }
        connectionString = new ConnectionString(config.get("connection_string").getAsString());
        settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10, SECONDS)
                                .readTimeout(15, SECONDS))
                .serverApi(ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build())
                .build();
        mongoClient = MongoClients.create(settings);
        guildsDB = mongoClient.getDatabase("Guilds");
        configsDB = mongoClient.getDatabase("Configs");
    }

    public static void updateTopGG() {
        //region top.gg
        DiscordBotListAPI api = new DiscordBotListAPI.Builder()
                .token(config.get("top_gg_api_key").getAsString())
                .botId(config.get("bot_id").getAsString())
                .build();

        api.setStats(shardManager.getGuilds().size());
        logger.info("Sent Servercount of {} to top.gg!", shardManager.getGuilds().size());
        //endregion
    }
}