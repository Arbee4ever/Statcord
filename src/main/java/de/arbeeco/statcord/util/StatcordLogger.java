package de.arbeeco.statcord.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import de.arbeeco.statcord.Statcord;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static de.arbeeco.statcord.Statcord.logger;

public class StatcordLogger extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent eventObject) {
        String guildId = MDC.get("guild.id");
        if (guildId != null) {
            if (eventObject.getLevel().equals(Level.ERROR)) {
                Guild guild = Statcord.shardManager.getGuildById(guildId);
                List errors = (List) Config.getConfigValue(guild, "errors", "errors");
                LinkedTreeMap<String, Object> error = new LinkedTreeMap<>();
                error.put("timestamp", eventObject.getTimeStamp());
                String errorString = eventObject.getThrowableProxy() == null ? null : eventObject.getThrowableProxy().getMessage();
                if (errorString == null) {
                    errorString = eventObject.getMessage();
                }
                error.put("message", errorString);
                errors.addFirst(error);
                Config.setConfigValue(guild, "errors", "errors", errors);
            }
        }
        if (eventObject.getLevel().equals(Level.ERROR)) {
            String notificationJson = null;
            try {
                notificationJson = Files.readString(Path.of("./notifications/error_notification.json"));
            } catch (NoSuchFileException e) {
                logger.warn(e.getFile() + " missing.");
            } catch (FileNotFoundException e) {
                logger.warn(e.getMessage() + " missing.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String pattern = "HH:mm:ss M.d.yy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            notificationJson = notificationJson.replace("$timenow", simpleDateFormat.format(new Date(eventObject.getTimeStamp())));
            Stream<String> logFiles = Stream.of(new File("./logs").listFiles())
                    .filter(file -> !file.isDirectory())
                    .sorted(Comparator.reverseOrder())
                    .map(File::getName);
            String fileName = logFiles.toArray()[0].toString();
            notificationJson = notificationJson.replace("$filename", fileName);
            if(eventObject.getThrowableProxy() != null) {
                notificationJson = notificationJson.replace("$error", eventObject.getThrowableProxy().getMessage());
            } else {
                notificationJson = notificationJson.replace("$error", eventObject.getMessage());
            }
            JsonObject body = new Gson().fromJson(notificationJson, JsonObject.class);
            HttpResponse<String> notificationResponse;
            try {
                notificationResponse = Statcord.notificationManager.sendNotification(body);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (notificationResponse.statusCode() == 200) {
                logger.info("Successfully sent error notification!");
            } else {
                logger.info("Failed to send error notification:\n" + notificationResponse.body());
            }
        }
    }
}
