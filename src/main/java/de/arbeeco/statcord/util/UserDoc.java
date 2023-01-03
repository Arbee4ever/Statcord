package de.arbeeco.statcord.util;

import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;

import java.sql.Timestamp;
import java.util.List;

public class UserDoc extends Document {
    public UserDoc(Member member) {
        this
                .append("id", member.getId())
                .append("name", member.getUser().getName())
                .append("textmessages", 0)
                .append("texthistory", List.of(0))
                .append("voiceseconds", 0)
                .append("voicehistory", List.of(0));
    }
}
