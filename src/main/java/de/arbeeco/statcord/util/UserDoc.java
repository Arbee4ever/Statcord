package de.arbeeco.statcord.util;

import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.List;

public class UserDoc extends Document {
    public UserDoc(User user) {
        this
                .append("id", user.getId())
                .append("name", user.getName())
                .append("textmessages", 0)
                .append("texthistory", List.of(0))
                .append("voiceseconds", 0)
                .append("voicehistory", List.of(0));
    }
}
