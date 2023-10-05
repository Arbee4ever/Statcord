package de.arbeeco.statcord.util;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static de.arbeeco.statcord.StatcordBot.guildsDB;

abstract public class Member implements net.dv8tion.jda.api.entities.Member {
  public Object get(String valueName) {
    MongoCollection<Document> collection = guildsDB.getCollection(getGuild().getId());
    Document memberDoc = collection.find(eq("id", this.getId())).first();
    if (memberDoc == null) {
      return null;
    }
    return memberDoc.get(valueName);
  }

  public Object set(String valueName)
}
