package de.arbeeco.statcord.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.requests.Route;
import org.bson.Document;
import org.eclipse.jetty.util.ajax.JSON;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Indexes.descending;
import static de.arbeeco.statcord.util.Data.database;
import static spark.Spark.get;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "score" -> {
                new ScoreCommand(event);
            }
            case "graph" -> {
                new GraphCommand(event);
            }
            case "leaderboard" -> {
                new LeaderboardCommand(event);
            }
            case "resetdata" -> {
                new ResetDataCommand(event);
            }
            case "add" -> {
                new AddCommand(event);
            }
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().updateCommands().addCommands(
                Commands.slash("score", "Displays a Members current Score")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User to display Score of.")),
                Commands.slash("graph", "Displays a Members Score over Time in a graph.")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User to display graph of"),
                                new OptionData(OptionType.ROLE, "role", "Role to display graph of"),
                                new OptionData(OptionType.INTEGER, "days", "Amount of days to display the graph of."),
                                new OptionData(OptionType.STRING, "filter", "Filter for a specific Scoretype.")
                                        .addChoice("text", "text")
                                        .addChoice("voice", "voice")),
                Commands.slash("leaderboard", "Displays a leaderboard for this guild."),
                Commands.slash("resetdata", "Resets all data back to 0. IMPOSSIBLE TO UNDO AFTER!")
                        .addOptions(new OptionData(OptionType.USER, "user", "User to reset data of")),
                Commands.slash("add", "Adds to a Users Score")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User to add score to", true),
                                new OptionData(OptionType.INTEGER, "textscore", "Textscore to add"),
                                new OptionData(OptionType.INTEGER, "voiceseconds", "Amount of Seconds to add"))
        ).queue();


        get("/guilds", ((request, response) -> {
            List<Guild> mutGuilds;
            List<Guild> allGuilds = new ArrayList<>(event.getJDA().getGuilds());
            JsonObject respArr = new JsonObject();
            if (request.queryParams("user") != null) {
                mutGuilds = new ArrayList<>(event.getJDA().getMutualGuilds(event.getJDA().getUserById(request.queryParams("user"))));
                JsonArray mutGuildsArr = new JsonArray();
                for (Guild guild: mutGuilds) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("id", guild.getId());
                    jsonObject.addProperty("name", guild.getName());
                    jsonObject.addProperty("icon", guild.getIconUrl());
                    mutGuildsArr.add(jsonObject);
                }
                respArr.add("mutual_guilds", mutGuildsArr);
                allGuilds.removeAll(mutGuilds);
            }

            JsonArray allGuildsArr = new JsonArray();
            for (Guild guild: allGuilds) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", guild.getId());
                jsonObject.addProperty("name", guild.getName());
                jsonObject.addProperty("icon", guild.getIconUrl());
                allGuildsArr.add(jsonObject);
            }
            respArr.add("other_guilds", allGuildsArr);
            return respArr;
        }));

        get("/guilds/:guildId", ((request, response) -> {
            MongoCollection<Document> collection = Data.getGuildData(event.getJDA().getGuildById(request.params(":guildId")));
            JsonArray jsonArray = new JsonArray();
            int count = 0;
            for (Document memberData : collection.find().sort(descending("textscore", "voicescore"))) {
                count++;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pos", count);
                jsonObject.addProperty("id", memberData.getString("id"));
                jsonObject.addProperty("score", memberData.getInteger("textscore") + memberData.getInteger("voicescore") / 10);
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }));

        get("/user/:userId", ((request, response) -> {
            User user = event.getJDA().getUserById(request.params(":userId"));
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", user.getName() + "#" + user.getDiscriminator());
            jsonObject.addProperty("pfp", user.getAvatarUrl());
            return jsonObject;
        }));
    }
}
