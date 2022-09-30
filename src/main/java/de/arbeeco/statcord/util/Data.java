package de.arbeeco.statcord.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Math.max;

public class Data {
    static ConnectionString connectionString = new ConnectionString(System.getenv("CONNECTION_STRING"));
    static MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build())
            .build();
    static MongoClient mongoClient = MongoClients.create(settings);
    public static MongoDatabase database = mongoClient.getDatabase("Guilds");

    public static File genGraph(List<Member> user, int days, String filter) throws IOException {
        File img = new File("graph.png");
        XYChart chart = new XYChart(600, 400);
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
            List<Number> list = new ArrayList<>(List.of());
            switch (filter) {
                case "text" -> list = new ArrayList<>(textList);
                case "voice" -> list = new ArrayList<>(voiceList);
                default -> {
                    for (int i = 0; i < textList.size(); i++) {
                        list.add(i, voiceList.get(i).intValue()/10 + textList.get(i).intValue());
                    }
                }
            }
            List<Number> subList = list.subList(max(0, list.size()-days), list.size());
            chart.addSeries(member.getEffectiveName(), subList);
        }
        BitmapEncoder.saveBitmap(chart, img.getPath(), BitmapEncoder.BitmapFormat.PNG);
        return img;
    }

    public static boolean initNewData(Guild guild) {
        database.createCollection(guild.getId());
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        for (Member user : guild.getMembers()) {
            if (!user.getUser().isBot()) {
                collection.insertOne(new UserDoc(user));
            }
        }
        return true;
    }

    public static MongoCollection<Document> getGuildData(Guild guild) {
        return database.getCollection(guild.getId());
    }

    public static boolean deleteGuildData(Guild guild) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        collection.drop();
        return true;
    }

    public static boolean deleteMemberData(Guild guild, Member member) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        collection.deleteOne(eq("id", member.getId()));
        return true;
    }

    public static void awardVcPoints(Guild guild, Member member) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        Date lastjoin = collection.find(eq("id", member.getId())).first().getDate("voicestart");
        collection.updateOne(eq("id", member.getId()), Updates.unset("voicestart"));
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
    }

    public static void setVcStart(Guild guild, Member member) {
        MongoCollection<Document> collection = database.getCollection(guild.getId());
        collection.updateOne(eq("id", member.getId()), Updates.set("voicestart", new Timestamp(System.currentTimeMillis())));
    }

    public static void addTextScore(Member member, int x) {
        updateLastMsg(member);
        update(member, Updates.inc("textscore", x));
        appendTextHistory(member, false, x);
    }

    public static int getTextScore(Member member) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        return collection.find(eq("id", member.getId())).first().getInteger("textscore");
    }

    public static void addVoiceSeconds(Member member, int x) {
        update(member, Updates.inc("voicescore", x));
    }

    public static int getVoiceScore(Member member) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        return collection.find(eq("id", member.getId())).first().getInteger("voicescore") / 10;
    }

    public static int getVoiceSeconds(Member member) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        return collection.find(eq("id", member.getId())).first().getInteger("voicescore");
    }

    public static Date getLastMsg(Member member) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        return collection.find(eq("id", member.getId())).first().getDate("lastmsg");
    }

    public static void updateLastMsg(Member member) {
        update(member, Updates.set("lastmsg", new Timestamp(System.currentTimeMillis())));
    }

    public static List<Number> getTextHistory(Member member, boolean unprocessed) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        List<Number> beforeList = collection.find(eq("id", member.getId())).first().getList("texthistory", Number.class);
        if (unprocessed) {
            return beforeList;
        }
        List<Number> list = new ArrayList<>(List.of());
        for (int i = 0; i < beforeList.size(); i++) {
            int j = 0;
            if (i != 0) {
                j = list.get(i-1).intValue();
            }
            list.add(j + beforeList.get(i).intValue());
        }
        return list;
    }

    public static List<Number> getVoiceHistory(Member member, boolean unprocessed) {
        MongoCollection<Document> collection = database.getCollection(member.getGuild().getId());
        List<Number> beforeList = collection.find(eq("id", member.getId())).first().getList("voicehistory", Number.class);
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

    public static UpdateResult appendTextHistory(Member member, boolean newDay, int x) {
        List<Number> data = new ArrayList<>(getTextHistory(member, true));
        if (newDay) {
            data.add(x);
        } else {
            data.set(data.size() - 1, (int)data.get(data.size() - 1) + x);
        }
        Bson newData = Updates.set("texthistory", data);
        return update(member, newData);
    }

    public static UpdateResult appendVoiceHistory(Member member, boolean newDay, int x) {
        List<Number> data = getVoiceHistory(member, true);
        if (newDay) {
            data.add(x);
        } else {
            data.set(data.size(), (int)data.get(data.size() - 1) + x);
        }
        Bson newData = Updates.set("voicehistory", data);
        return update(member, Updates.combine(newData));
    }

    public static void syncHistoryDays(Member member) {
        List<Number> textList = getTextHistory(member, false);
        List<Number> voiceList = getVoiceHistory(member, false);
        if (textList.size() != voiceList.size()) {
            if (textList.size() > voiceList.size()) {
                for (int j = 0; j < (textList.size() - voiceList.size()) ; j++) {
                    appendVoiceHistory(member, true, 0);
                }
            } else {
                for (int j = 0; j < (voiceList.size() - textList.size()); j++) {
                    appendTextHistory(member, true, 0);
                }
            }
        }
    }

    public static UpdateResult update(Member member, Bson updates) {
        return database.getCollection(member.getGuild().getId()).updateOne(eq("id", member.getId()), updates);
    }
}
