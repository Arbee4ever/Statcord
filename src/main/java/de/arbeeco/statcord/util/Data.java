package de.arbeeco.statcord.util;

import com.google.gson.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bson.Document;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;

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
    public static Object getMemberValue(Member member, String valueName) {
        MongoCollection<Document> collection = guildsDB.getCollection(member.getGuild().getId());
        return collection.find(eq("id", member.getId())).first().get(valueName);
    }

    public static UpdateResult setMemberValue(Member member, String valueName, Object newValue) {
        MongoCollection<Document> collection = guildsDB.getCollection(member.getGuild().getId());
        if (newValue == null) {
            return collection.updateOne(eq("id", member.getId()), Updates.unset(valueName));
        }
        return collection.updateOne(eq("id", member.getId()), Updates.set(valueName, newValue));
    }

    public static boolean deleteMemberData(Guild guild, String id) {
        MongoCollection<Document> collection = guildsDB.getCollection(guild.getId());
        collection.deleteOne(eq("id", id));
        return true;
    }
    //endregion

    //region Misc
    public static File genGraph(List<Member> user, int days, String filter) throws IOException {
        File img = new File("graph.png");
        XYChart chart = new XYChart(1000, 600);
        chart.setTitle("Score over Time");
        chart.setYAxisTitle("Score");
        chart.setXAxisTitle("Time (in days)");
        for (Member member : user) {
            if (member.getUser().isBot()) continue;
            Date lastm = getLastMsg(member);
            Calendar zero = new GregorianCalendar();
            zero.set(Calendar.HOUR_OF_DAY, 0);
            zero.set(Calendar.MINUTE, 0);
            zero.set(Calendar.SECOND, 0);
            zero.set(Calendar.MILLISECOND, 0);
            if (lastm.before(zero.getTime())) {
                Date now = new Date();
                long diff = now.getTime() - lastm.getTime();
                appendTextHistory(member, true, 0);
                appendVoiceHistory(member, true, 0);
                for (int i = 0; i < TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); i++) {
                    appendTextHistory(member, true, 0);
                    appendVoiceHistory(member, true, 0);
                }
            }
            syncHistoryDays(member);
            List<Number> textList = getTextHistory(member, false);
            List<Number> voiceList = getVoiceHistory(member, false);
            List<Number> yList = new ArrayList<>(List.of());
            List<Number> xList = new ArrayList<>(List.of());
            for (int i = 0; i < days; i++) {
                xList.add(-i);
            }
            switch (filter) {
                case "text" -> {
                    for (int i = 0; i < textList.size(); i++) {
                        yList.add(0, textList.get(i).intValue());
                    }
                }
                case "voice" -> {
                    for (int i = 0; i < voiceList.size(); i++) {
                        yList.add(0, voiceList.get(i).intValue());
                    }
                }
                default -> {
                    for (int i = 0; i < textList.size(); i++) {
                        yList.add(0, textList.get(i).intValue() + voiceList.get(i).intValue());
                    }
                }
            }
            List<Number> ySubList = yList.subList(0, min(yList.size(), yList.size() - (yList.size() - days)));
            List<Number> xSubList = xList.subList(0, ySubList.size());
            chart.addSeries(member.getEffectiveName(), xSubList, ySubList);
        }
        BitmapEncoder.saveBitmap(chart, img.getPath(), BitmapEncoder.BitmapFormat.PNG);
        return img;
    }

    public static void syncHistoryDays(Member member) {
        List<Number> textList = getTextHistory(member, false);
        List<Number> voiceList = getVoiceHistory(member, false);
        if (textList.size() != voiceList.size()) {
            if (textList.size() > voiceList.size()) {
                for (int j = 0; j < (textList.size() - voiceList.size()); j++) {
                    appendVoiceHistory(member, true, 0);
                }
            } else {
                for (int j = 0; j < (voiceList.size() - textList.size()); j++) {
                    appendTextHistory(member, true, 0);
                }
            }
        }
    }

    private static void updateRoles(Member member, int value, String modified) {
        Object configValue = Config.getConfigValue(member.getGuild(), "roles", "roles");
        if (configValue == null) return;
        JsonArray ranks = new Gson().toJsonTree(configValue).getAsJsonArray();
        if (ranks.equals(JsonNull.INSTANCE)) return;

        List<Role> addRoles = new ArrayList<>();
        List<Role> removeRoles = new ArrayList<>();

        for (JsonElement possibleRank : ranks) {
            boolean requirementsMet = true;
            for (JsonElement req : possibleRank.getAsJsonObject().get("requirements").getAsJsonArray()) {
                if (!(req.getAsJsonObject().get("value").getAsInt() <= (int)getMemberValue(member, req.getAsJsonObject().get("id").getAsString()))) {
                    requirementsMet = false;
                    break;
                }
            }
            if (requirementsMet) {
                for (JsonElement rank : possibleRank.getAsJsonObject().get("roles").getAsJsonArray()) {
                    Role role = member.getGuild().getRoleById(rank.getAsJsonObject().get("id").getAsString());
                    if (role == null) continue;
                    addRoles.add(role);
                }
            } else {
                for (JsonElement rank : possibleRank.getAsJsonObject().get("roles").getAsJsonArray()) {
                    if (rank.getAsJsonObject().get("static").getAsBoolean()) continue;
                    Role role = member.getGuild().getRoleById(rank.getAsJsonObject().get("id").getAsString());
                    if (role == null) continue;
                    removeRoles.add(role);
                }
            }
        }

        member.getGuild().modifyMemberRoles(member, addRoles, removeRoles).queue();
    }
    //endregion

    //region Text-Interactors
    public static void addTextMessages(Member member, int x, boolean cooldown) {
        syncHistoryDays(member);
        Date lastm = Data.getLastMsg(member);
        Date now = Date.from(Instant.now());
        if (lastm == null || !cooldown || (lastm.getTime() - now.getTime()) < -(int) Config.getConfigValue(member.getGuild(), "values", "cooldown")) {
            if (cooldown) {
                updateLastMsg(member);
            }
            setMemberValue(member, "textmessages", getTextMessages(member) + x);
            appendTextHistory(member, false, x);
        }
        int textMessages = getTextMessages(member);
        updateRoles(member, textMessages, "textmessages");
    }

    public static int getTextScore(Member member) {
        return (int) getMemberValue(member, "textmessages") / (int) Config.getConfigValue(member.getGuild(), "values", "msgsperpoint");
    }

    public static int getTextMessages(Member member) {
        return (int) getMemberValue(member, "textmessages");
    }

    public static Date getLastMsg(Member member) {
        Object lastm = getMemberValue(member, "lastmsg");
        if (lastm == null) {
            return null;
        } else {
            return (Date) lastm;
        }
    }

    public static void updateLastMsg(Member member) {
        setMemberValue(member, "lastmsg", new Timestamp(System.currentTimeMillis()));
    }

    public static List<Number> getTextHistory(Member member, boolean unprocessed) {
        List<Number> beforeList = (List<Number>) getMemberValue(member, "texthistory");
        if (unprocessed) {
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

    public static UpdateResult appendTextHistory(Member member, boolean newDay, int x) {
        List<Number> data = new ArrayList<>(getTextHistory(member, true));
        if (newDay) {
            data.add(x);
        } else {
            data.set(data.size() - 1, (int) data.get(data.size() - 1) + x);
        }
        return setMemberValue(member, "texthistory", data);
    }
    //endregion

    //region VC-Interactors
    public static void setVcStart(Member member) {
        setMemberValue(member, "voicestart", new Timestamp(System.currentTimeMillis()));
    }

    public static void awardVcPoints(Guild guild, Member member) {
        syncHistoryDays(member);
        MongoCollection<Document> collection = Data.getGuildData(guild);
        Date lastjoin = collection.find(eq("id", member.getId())).first().getDate("voicestart");
        Date now = new Time(System.currentTimeMillis());
        long diff = (now.getTime() - lastjoin.getTime()) / 1000;
        Calendar zero = new GregorianCalendar();
        zero.set(Calendar.HOUR_OF_DAY, 0);
        zero.set(Calendar.MINUTE, 0);
        zero.set(Calendar.SECOND, 0);
        zero.set(Calendar.MILLISECOND, 0);
        if (lastjoin.before(zero.getTime())) {
            appendVoiceHistory(member, true, (int) diff);
        }
        appendVoiceHistory(member, false, (int) diff);
        addVoiceSeconds(member, (int) diff);
        setMemberValue(member, "voicestart", null);
    }

    public static void addVoiceSeconds(Member member, int x) {
        setMemberValue(member, "voiceseconds", getVoiceSeconds(member) + x);
        int voiceseconds = getVoiceSeconds(member);
        updateRoles(member, voiceseconds, "voiceseconds");
    }

    public static int getVoiceScore(Member member) {
        return (int) getMemberValue(member, "voiceseconds") / (int) Config.getConfigValue(member.getGuild(), "values", "vcsecondsperpoint");
    }

    public static int getVoiceSeconds(Member member) {
        return (int) getMemberValue(member, "voiceseconds");
    }

    public static List<Number> getVoiceHistory(Member member, boolean unprocessed) {
        List<Number> beforeList = (List<Number>) getMemberValue(member, "voicehistory");
        if (unprocessed) {
            return beforeList;
        }
        List<Number> list = new ArrayList<>();
        for (int i = 0; i < beforeList.size(); i++) {
            int j = 0;
            if (i != 0) {
                j = list.get(i - 1).intValue();
            }
            list.add(beforeList.get(i).intValue() + j);
        }
        return list;
    }

    public static UpdateResult appendVoiceHistory(Member member, boolean newDay, int x) {
        List<Number> data = getVoiceHistory(member, true);
        if (newDay) {
            data.add(x);
        } else {
            data.set(data.size() - 1, (int) data.get(data.size() - 1) + x);
        }
        return setMemberValue(member, "voicehistory", data);
    }
    //endregion
}
