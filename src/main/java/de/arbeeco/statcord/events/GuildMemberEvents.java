package de.arbeeco.statcord.events;

import com.mongodb.client.MongoCollection;
import de.arbeeco.statcord.util.Config;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.UserDoc;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

public class GuildMemberEvents extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        MDC.put("guild.id", event.getGuild().getId());
        MongoCollection<Document> collection = Data.getGuildData(event.getGuild());
        if (!event.getUser().isBot()) {
            collection.insertOne(new UserDoc(event.getUser()));
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        MDC.put("guild.id", event.getGuild().getId());
        Guild guild = event.getGuild();
        if (!(boolean)Config.getConfigValue(guild, "data", "deleteonleave")) return;
        Member member = event.getMember();
        if (!member.getUser().isBot()) {
            Data.deleteMemberData(guild, member.getId());
        }
    }
}
