package de.arbeeco.statcord.util;

import com.google.gson.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import de.arbeeco.statcord.StatcordBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static de.arbeeco.statcord.StatcordBot.guildsDB;
import static java.lang.Math.min;

public class Data {
  //region GuildConfig
  public static void initNewGuildData(Guild guild) {
    guildsDB.createCollection(guild.getId());
  }

  public static MongoCollection<Document> getGuildData(Guild guild) throws IllegalArgumentException {
    return guildsDB.getCollection(guild.getId());
  }

  public static boolean deleteGuildData(Guild guild) {
    MongoCollection<Document> collection = guildsDB.getCollection(guild.getId());
    MongoCollection<Document> configCollection = Config.getGuildConfig(guild);
    collection.drop();
    configCollection.drop();
    return true;
  }
  //endregion

  //region MemberConfig
  public static Object getMemberValue(User user, Guild guild, String valueName) {
    MongoCollection<Document> collection = guildsDB.getCollection(guild.getId());
    Document memberDoc = collection.find(eq("id", user.getId())).first();
    if (memberDoc == null) {
      return null;
    }
    return memberDoc.get(valueName);
  }

  public static UpdateResult setMemberValue(User user, Guild guild, String valueName, Object newValue) {
    MongoCollection<Document> collection = guildsDB.getCollection(guild.getId());
    if (newValue == null) {
      return collection.updateOne(eq("id", user.getId()), Updates.unset(valueName));
    }
    return collection.updateOne(eq("id", user.getId()), Updates.set(valueName, newValue));
  }

  public static boolean deleteMemberData(Guild guild, String id) {
    MongoCollection<Document> collection = guildsDB.getCollection(guild.getId());
    collection.deleteOne(eq("id", id));
    return true;
  }
  //endregion

  //region Misc
  public static File genGraph(List<User> userList, Guild guild, int days, String filter) throws IOException {
    File img = new File("graph.png");
    XYChart chart = new XYChart(1000, 600);
    chart.setTitle("Score over Time");
    chart.setYAxisTitle("Score");
    chart.setXAxisTitle("Time (in days)");
    for (User user : userList) {
      if (user.isBot()) continue;
      List<Number> list = new ArrayList<>(List.of());
      for (int i = 0; i < days; i++) {
        list.add(-i);
      }
      List<Number> yList = getYList(user, guild, days, filter);
      if (yList == null) {
        return null;
      }
      List<Number> subList = list.subList(0, min(days, yList.size()));
      chart.addSeries(user.getName(), subList, yList);
    }
    BitmapEncoder.saveBitmap(chart, img.getPath(), BitmapEncoder.BitmapFormat.PNG);
    return img;
  }

  public static File genGraph(List<Member> memberList, int days, String filter) throws IOException {
    File img = new File("graph.png");
    XYChart chart = new XYChart(1000, 600);
    chart.setTitle("Score over Time");
    chart.setYAxisTitle("Score");
    chart.setXAxisTitle("Time (in days)");
    for (Member member : memberList) {
      if (member.getUser().isBot()) continue;
      List<Number> list = new ArrayList<>(List.of());
      for (int i = 0; i < days; i++) {
        list.add(-i);
      }
      List<Number> yList = getYList(member.getUser(), member.getGuild(), days, filter);
      List<Number> subList = list.subList(0, min(days, yList.size()));
      chart.addSeries(member.getEffectiveName(), subList, yList);
    }
    BitmapEncoder.saveBitmap(chart, img.getPath(), BitmapEncoder.BitmapFormat.PNG);
    return img;
  }

  public static List<Number> getYList(User user, Guild guild, int days, String filter) {
    if (user.isBot()) return null;
    syncHistoryDays(user, guild);
    List<Number> list = new ArrayList<>(List.of());
    switch (filter) {
      case "text" -> {
        List<Number> textList = getTextHistory(user, guild, false);
        if (textList == null) {
          return null;
        }
        for (int i = 0; i < textList.size(); i++) {
          list.add(0, textList.get(i).intValue());
        }
      }
      case "voice" -> {
        List<Number> voiceList = getVoiceHistory(user, guild, false);
        if (voiceList == null) {
          return null;
        }
        for (int i = 0; i < voiceList.size(); i++) {
          list.add(0, voiceList.get(i).intValue());
        }
      }
      default -> {
        List<Number> textList = getTextHistory(user, guild, false);
        List<Number> voiceList = getVoiceHistory(user, guild, false);
        if (textList == null || voiceList == null) {
          return null;
        }
        for (int i = 0; i < textList.size(); i++) {
          list.add(0, textList.get(i).intValue() + voiceList.get(i).intValue());
        }
      }
    }
    List<Number> subList = list.subList(0, min(list.size(), list.size() - (list.size() - days)));
    return subList;
  }

  public static void syncHistoryDays(User user, Guild guild) {
    Date lastm = getLastMsg(user, guild);
    Date lastjoin = getVoiceStart(user, guild);
    Calendar zero = new GregorianCalendar();
    zero.set(Calendar.HOUR_OF_DAY, 0);
    zero.set(Calendar.MINUTE, 0);
    zero.set(Calendar.SECOND, 0);
    zero.set(Calendar.MILLISECOND, 0);
    if (lastm == null || lastm.before(zero.getTime())) {
      Date now = new Date();
      if (lastm != null) {
        setLastMsg(user, guild);
        long diff = now.getTime() - lastm.getTime();
        appendTextHistory(user, guild, true, 0);
        for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
          appendTextHistory(user, guild, true, 0);
        }
      }
    }
    if (lastjoin == null || lastjoin.before(zero.getTime())) {
      Date now = new Date();
      if (lastjoin != null) {
        setVcStart(user, guild);
        long diff = now.getTime() - lastjoin.getTime();
        appendVoiceHistory(user, guild, true, 0);
        for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
          appendVoiceHistory(user, guild, true, 0);
        }
      }
    }
    List<Number> textList = getTextHistory(user, guild, false);
    List<Number> voiceList = getVoiceHistory(user, guild, false);
    if (textList != null && voiceList != null) {
      if (textList.size() != voiceList.size()) {
        if (textList.size() > voiceList.size()) {
          for (int j = 0; j < (textList.size() - voiceList.size()); j++) {
            appendVoiceHistory(user, guild, true, 0);
          }
        } else {
          for (int j = 0; j < (voiceList.size() - textList.size()); j++) {
            appendTextHistory(user, guild, true, 0);
          }
        }
      }
    }
  }

  private static void updateRoles(User user, Guild guild, int value, String modified) {
    Object configValue = Config.getConfigValue(guild, "roles", "roles");
    if (configValue == null) return;
    JsonArray ranks = new Gson().toJsonTree(configValue).getAsJsonArray();
    if (ranks.equals(JsonNull.INSTANCE)) return;

    List<Role> addRoles = new ArrayList<>();
    List<Role> removeRoles = new ArrayList<>();

    for (JsonElement possibleRank : ranks) {
      boolean requirementsMet = true;
      for (JsonElement req : possibleRank.getAsJsonObject().get("requirements").getAsJsonArray()) {
        if (!(req.getAsJsonObject().get("value").getAsInt() <= (int) getMemberValue(user, guild, req.getAsJsonObject().get("id").getAsString()))) {
          requirementsMet = false;
          break;
        }
      }
      if (requirementsMet) {
        for (JsonElement rank : possibleRank.getAsJsonObject().get("roles").getAsJsonArray()) {
          Role role = guild.getRoleById(rank.getAsJsonObject().get("id").getAsString());
          if (role == null) continue;
          addRoles.add(role);
        }
      } else {
        for (JsonElement rank : possibleRank.getAsJsonObject().get("roles").getAsJsonArray()) {
          if (rank.getAsJsonObject().get("static").getAsBoolean()) continue;
          Role role = guild.getRoleById(rank.getAsJsonObject().get("id").getAsString());
          if (role == null) continue;
          removeRoles.add(role);
        }
      }
    }

    Member member = guild.getMember(user);
    if (member != null) {
      guild.modifyMemberRoles(member, addRoles, removeRoles).queue();
    }
  }
  //endregion

  //region Text-Interactors
  public static void addTextMessages(User user, Guild guild, int x, boolean cooldown) {
    MongoCollection<Document> collection = Data.getGuildData(guild);
    if (collection.find(eq("id", user.getId())).first() == null) {
      collection.insertOne(new UserDoc(user));
    }
    syncHistoryDays(user, guild);
    Date lastm = Data.getLastMsg(user, guild);
    Date now = Date.from(Instant.now());
    if (lastm == null || !cooldown || (lastm.getTime() - now.getTime()) < -(int) Config.getConfigValue(guild, "values", "cooldown")) {
      if (cooldown) {
        setLastMsg(user, guild);
      }
      setMemberValue(user, guild, "textmessages", getTextMessages(user, guild) + x);
      appendTextHistory(user, guild, false, x);
    }
    int textMessages = getTextMessages(user, guild);
    updateRoles(user, guild, textMessages, "textmessages");
  }

  public static int getTextScore(User user, Guild guild) {
    return (int) getMemberValue(user, guild, "textmessages") / (int) Config.getConfigValue(guild, "values", "msgsperpoint");
  }

  public static int getTextMessages(User user, Guild guild) {
    return (int) getMemberValue(user, guild, "textmessages");
  }

  public static Date getLastMsg(User user, Guild guild) {
    return (Date) getMemberValue(user, guild, "lastmsg");
  }

  public static void setLastMsg(User user, Guild guild) {
    MongoCollection<Document> collection = Data.getGuildData(guild);
    if (collection.find(eq("id", user.getId())).first() == null) {
      collection.insertOne(new UserDoc(user));
    }
    Timestamp nowTime = new Timestamp(System.currentTimeMillis());
    setMemberValue(user, guild, "lastmsg", nowTime);
  }

  public static List<Number> getTextHistory(User user, Guild guild, boolean unprocessed) {
    List<Number> beforeList = (List<Number>) getMemberValue(user, guild, "texthistory");
    if (unprocessed || beforeList == null) {
      return beforeList;
    }
    List<Number> list = new ArrayList<>(List.of());
    for (int i = 0; i < beforeList.size(); i++) {
      int j = 0;
      if (i != 0) {
        j = list.get(i - 1).intValue();
      }
      list.add(beforeList.get(i).intValue() + j);
    }
    return list;
  }

  public static UpdateResult appendTextHistory(User user, Guild guild, boolean newDay, int x) {
    List<Number> data = new ArrayList<>(getTextHistory(user, guild, true));
    if (newDay) {
      data.add(x);
    } else {
      data.set(data.size() - 1, (int) data.get(data.size() - 1) + x);
    }
    return setMemberValue(user, guild, "texthistory", data);
  }
  //endregion

    //region VC-Interactors
    public static void setVcStart(User user, Guild guild) {
        MongoCollection<Document> collection = Data.getGuildData(guild);
        if (collection.find(eq("id", user.getId())).first() == null) {
            collection.insertOne(new UserDoc(user));
        }
        Timestamp nowTime = new Timestamp(System.currentTimeMillis());
        setMemberValue(user, guild, "voicestart", nowTime);
    }

  public static void awardVcPoints(User user, Guild guild) {
    syncHistoryDays(user, guild);
    Date lastjoin = (Date) getMemberValue(user, guild, "voicestart");
    Date now = new Time(System.currentTimeMillis());
    Calendar zero = new GregorianCalendar();
    zero.set(Calendar.HOUR_OF_DAY, 0);
    zero.set(Calendar.MINUTE, 0);
    zero.set(Calendar.SECOND, 0);
    zero.set(Calendar.MILLISECOND, 0);
    long diff = now.getTime() - lastjoin.getTime();
    for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
      appendVoiceHistory(user, guild, true, 0);
    }
    appendVoiceHistory(user, guild, false, (int) diff / 1000);
    addVoiceSeconds(user, guild, (int) diff / 1000);
    setMemberValue(user, guild, "voicestart", null);
  }

  public static void addVoiceSeconds(User user, Guild guild, int x) {
    setMemberValue(user, guild, "voiceseconds", getVoiceSeconds(user, guild) + x);
    int voiceseconds = getVoiceSeconds(user, guild);
    updateRoles(user, guild, voiceseconds, "voiceseconds");
  }

  public static int getVoiceScore(User user, Guild guild) {
    return (int) getMemberValue(user, guild, "voiceseconds") / (int) Config.getConfigValue(guild, "values", "vcsecondsperpoint");
  }

  public static int getVoiceSeconds(User user, Guild guild) {
    return (int) getMemberValue(user, guild, "voiceseconds");
  }

  public static Date getVoiceStart(User user, Guild guild) {
    return (Date) getMemberValue(user, guild, "voicestart");
  }

  public static List<Number> getVoiceHistory(User user, Guild guild, boolean unprocessed) {
    List<Number> beforeList = (List<Number>) getMemberValue(user, guild, "voicehistory");
    if (unprocessed || beforeList == null) {
      return beforeList;
    }
    List<Number> list = new ArrayList<>(List.of());
    for (int i = 0; i < beforeList.size(); i++) {
      int j = 0;
      if (i != 0) {
        j = list.get(i - 1).intValue();
      }
      list.add(beforeList.get(i).intValue() + j);
    }
    return list;
  }

  public static UpdateResult appendVoiceHistory(User user, Guild guild, boolean newDay, int x) {
    List<Number> data = getVoiceHistory(user, guild, true);
    if (newDay) {
      data.add(x);
    } else {
      data.set(data.size() - 1, (int) data.get(data.size() - 1) + x);
    }
    return setMemberValue(user, guild, "voicehistory", data);
  }
  //endregion
}
