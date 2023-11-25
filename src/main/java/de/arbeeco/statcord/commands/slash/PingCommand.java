package de.arbeeco.statcord.commands.slash;

import de.arbeeco.statcord.util.StatcordEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PingCommand {
  public PingCommand(SlashCommandInteractionEvent event) {
    event.replyEmbeds(new StatcordEmbed()
            .setDescription("Gateway Ping: **" + event.getJDA().getGatewayPing() + "**ms \n" +
                    "API Ping: **" + event.getJDA().getRestPing().complete() + "**ms")
            .setFooter("Shard: " + event.getJDA().getShardInfo().getShardId() + "/" + event.getJDA().getShardInfo().getShardTotal() + " - Guilds: " + event.getJDA().getShardManager().getGuilds().size())
            .build()
    ).queue();
  }
}
