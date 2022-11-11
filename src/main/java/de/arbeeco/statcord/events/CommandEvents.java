package de.arbeeco.statcord.events;

import de.arbeeco.statcord.commands.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import static de.arbeeco.statcord.util.Data.collectionExists;
import static de.arbeeco.statcord.util.Data.initNewData;

public class CommandEvents extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!collectionExists(event.getGuild().getId())) {
            initNewData(event.getGuild());
        }
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
            case "ping" -> {
                new PingCommand(event);
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
                                new OptionData(OptionType.INTEGER, "voiceseconds", "Amount of Seconds to add")),
                Commands.slash("ping", "Retrieve the Bots ping")
        ).queue();
    }
}
