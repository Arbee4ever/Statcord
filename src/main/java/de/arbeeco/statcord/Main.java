package de.arbeeco.statcord;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class Main extends ListenerAdapter {
    ConnectionString connectionString = new ConnectionString(System.getenv("CONNECTION_STRING"));
    MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
            .build();
    MongoClient mongoClient = MongoClients.create(settings);
    MongoDatabase database = mongoClient.getDatabase("Guilds");

    public static void main(String[] args) throws LoginException, InterruptedException {
        JDA jda = JDABuilder.createDefault(args[0])
                .addEventListeners(new Main())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setEventPassthrough(true)
                .setActivity(Activity.watching("ARBEE's code."))
                .build();

        jda.awaitReady();

        jda.updateCommands().addCommands(
                Commands.slash("score", "Displays a Members current Score")
                        .addOption(OptionType.USER, "user", "User to display score of"),
                Commands.slash("graph", "Displays a Members Score over Time in a graph.")
                        .addOption(OptionType.USER, "user", "User to display graph of")
                        .addOption(OptionType.ROLE, "role", "Role to display graph of"),
                Commands.slash("resetdata", "Rssets all data back to 0. IMPOSSIBLE TO UNDO AFTER!")
                        .addOption(OptionType.USER, "user", "User to reset data of")
        ).queue();
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        Member user = event.getMember();
        if (!user.getUser().isBot()) {
            collection.insertOne(getNewDoc(user));
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        Member user = event.getMember();
        if (!user.getUser().isBot()) {
            collection.deleteOne(eq("id", user.getId()));
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild guild = event.getGuild();
        JDA jda = event.getJDA();
        initNewData(event.getGuild());
        jda.retrieveUserById(391979592883372042L).queue(user ->
                user.openPrivateChannel()
                        .flatMap(privateChannel -> privateChannel.sendMessage("Joined Server: " + guild.getName() + " Membercount:" + (guild.getMemberCount() - 1)))
                        .queue());
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        JDA jda = event.getJDA();
        deleteAllData(guild);
        jda.retrieveUserById(391979592883372042L).queue(user ->
                user.openPrivateChannel()
                        .flatMap(privateChannel -> privateChannel.sendMessage("Left Server: " + guild.getName() + " Membercount:" + (guild.getMemberCount() - 1)))
                        .queue());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        Guild guild = event.getGuild();
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        Member user = event.getMember();
        if (!user.getUser().isBot()) {
            Date lastm = collection.find(eq("id", user.getId())).first().getDate("lastmodified");
            Calendar zero = new GregorianCalendar();
            zero.set(Calendar.HOUR_OF_DAY, 0);
            zero.set(Calendar.MINUTE, 0);
            zero.set(Calendar.SECOND, 0);
            zero.set(Calendar.MILLISECOND, 0);
            if (lastm.before(zero.getTime())) {
                Date now = new Date();
                long diff = now.getTime() - lastm.getTime();
                for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
                    collection.updateOne(eq("id", user.getId()), replaceInList(collection.find(eq("id", user.getId())).first(), true));
                }
            }
            collection.updateOne(eq("id", user.getId()), Updates.inc("score", 1));
            collection.updateOne(eq("id", user.getId()), replaceInList(collection.find(eq("id", user.getId())).first(), false));
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        Member user = event.getMember();
        if (event.getName().equals("score")) {
            if (event.getOption("user") != null) {
                if (event.getOption("user").getAsUser().isBot()) {
                    event.replyEmbeds(new EmbedBuilder().setDescription("User is a bot and has no score.").build()).queue();
                    return;
                }
                event.replyEmbeds(new EmbedBuilder().setDescription(event.getOption("user").getAsUser().getAsMention() + "'s current Score is: " + collection.find(eq("id", event.getOption("user").getAsUser().getId())).first().getInteger("score").toString()).build()).queue();
                return;
            }
            event.replyEmbeds(new EmbedBuilder().setDescription(event.getMember().getAsMention() + "'s current Score is: " + collection.find(eq("id", user.getId())).first().getInteger("score").toString()).build()).queue();
        } else if (event.getName().equals("graph")) {
            try {
                List<Member> graphUser = List.of(event.getMember());
                if (event.getOption("role") != null) {
                    if (event.getOption("role").getAsString().equals(guild.getId())) {
                        graphUser = guild.getMembers();
                    } else {
                        graphUser = guild.getMembersWithRoles(event.getOption("role").getAsRole());
                    }
                } else if (event.getOption("user") != null) {
                    graphUser = List.of(event.getOption("user").getAsMember());
                }
                File img = genGraph(collection, graphUser);
                event.replyFiles(FileUpload.fromData(img))
                        .addEmbeds(new EmbedBuilder()
                                .setDescription(event.getUser().getAsMention() + " " + collection.find(eq("id", user.getId())).first().getDate("lastmodified"))
                                .setImage("attachment://graph.png")
                                .build())
                        .queue();
                img.delete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (event.getName().equals("resetdata")) {
            if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                deleteAllData(event.getGuild());
                initNewData(event.getGuild());
                event.replyEmbeds(new EmbedBuilder().setDescription("Sucessfully reset all data for everyone.").build()).queue();
                return;
            }
            event.replyEmbeds(new EmbedBuilder().setDescription("Sorry, but you need the Administrator Permission to reset all data.").build()).queue();
        }
    }

    public Document getNewDoc(Member user) {
        return new Document()
                .append("id", user.getId())
                .append("name", user.getUser().getName())
                .append("score", 0)
                .append("30d", List.of(0))
                .append("lastmodified", new Timestamp(System.currentTimeMillis()));
    }

    public Bson replaceInList(Document id, boolean newDay) {
        if (id == null) return null;
        List<Object> data = id.getList("30d", Object.class);
        if (newDay) {
            data.add(id.getInteger("score"));
        } else {
            data.set(data.size() - 1, id.getInteger("score"));
        }
        Bson newData = Updates.set("30d", data);
        Bson timestamp = Updates.set("lastmodified", new Timestamp(System.currentTimeMillis()));
        return Updates.combine(newData, timestamp);
    }

    public File genGraph(MongoCollection<Document> collection, List<Member> user) throws IOException {
        File img = new File("graph.png");
        XYChart chart = new XYChart(600, 400);
        chart.setTitle("Score over Time");
        chart.setYAxisTitle("Score");
        chart.setXAxisTitle("Time");
        for (Member member : user) {
            if (member.getUser().isBot()) continue;
            chart.addSeries(member.getEffectiveName(), collection.find(eq("id", member.getId())).first().getList("30d", Number.class));
        }
        BitmapEncoder.saveBitmap(chart, img.getPath(), BitmapEncoder.BitmapFormat.PNG);
        return img;
    }

    public boolean initNewData(Guild guild) {
        database.createCollection(guild.getId());
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        for (Member user : guild.getMembers()) {
            if (!user.getUser().isBot()) {
                collection.insertOne(getNewDoc(user));
            }
        }
        return true;
    }
    public boolean deleteAllData(Guild guild) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        collection.drop();
        return true;
    }
}