package de.arbeeco.statcord;

import com.google.gson.Gson;
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
import de.arbeeco.statcord.util.NotificationManager;
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
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

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

  public static void main(String[] args) {
    try {
      start(args);
    } catch (IOException e) {
      logger.warn(e.getMessage() + " missing.");
    } catch (URISyntaxException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void start(String[] args) throws IOException, URISyntaxException, InterruptedException {
    Runtime.getRuntime().addShutdownHook(new Thread(StatcordBot::shutdown));

    loadConfig();

    DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(args[0], GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_VOICE_STATES)
            .disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
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
    JDA jda = shardManager.retrieveApplicationInfo().getJDA();
    new Api(jda);

    String notificationJson = Files.readString(Path.of("./notification.json"));
    String pattern = "HH:mm:ss M.d.yy";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    notificationJson = notificationJson.replace("$timenow", simpleDateFormat.format(Date.from(Instant.now())));
    Stream<String> logFiles = Stream.of(new File("./logs").listFiles())
            .filter(file -> !file.isDirectory())
            .sorted(Comparator.reverseOrder())
            .map(File::getName);
    String fileName = logFiles.toArray()[0].toString();
    notificationJson = notificationJson.replace("$filename", fileName);
    JsonObject body = new Gson().fromJson(notificationJson, JsonObject.class);
    HttpResponse<String> notificationResponse = new NotificationManager().sendNotification(body);
    if (notificationResponse.statusCode() == 200) {
      logger.info("Successfully sent start notification!");
    } else {
      logger.info("Failed to send start notification:\n" + notificationResponse.body());
    }

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
            reader.close();
            System.exit(0);
            break;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
    //endregion
  }

  private static void shutdown() {
    if (shardManager != null) {
      shardManager.setStatus(OnlineStatus.OFFLINE);
      shardManager.shutdown();
      mongoClient.close();
      logger.info("Bot shutdown at: " + Date.from(Instant.now()));
    }
  }

  public static void loadConfig() throws FileNotFoundException {
    fileReader = new FileReader("config.json5");
    config = JsonParser.parseReader(fileReader).getAsJsonObject();
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