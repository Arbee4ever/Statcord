package de.arbeeco.statcord.messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.arbeeco.statcord.StatcordBot;
import de.arbeeco.statcord.util.Config;
import de.arbeeco.statcord.util.Data;
import de.arbeeco.statcord.util.variables.UserMentionVariable;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageType {
  private final String name;
  private final String[] variables;
  String patternString = "";
  Pattern pattern;

  public MessageType(String name, @Nullable String... variables) {
    this.name = name;
    this.variables = variables;
    if (variables != null && variables.length > 0) {
      patternString = Pattern.quote("${" + variables[0] + "}");
      for (int i = 1; i < variables.length; i++) {
        patternString = patternString.concat("|" + Pattern.quote("${" + variables[i] + "}"));
      }
      patternString = "(" + patternString + ")";
    }
    pattern = Pattern.compile(patternString);
  }

  public JsonObject getMessage(GenericInteractionCreateEvent event) {
    String message = new Gson().toJson(Config.getConfigValue(event.getGuild(), "messages", name));
    Matcher matcher = pattern.matcher(message);

    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String name = matcher.group().substring(2, matcher.group().length()-1);
      matcher.appendReplacement(sb, StatcordBot.variablesManager.get(name, event));
    }
    matcher.appendTail(sb);
    return new Gson().fromJson(sb.toString(), JsonObject.class);
  }

  public String[] getVariables() {
    return variables;
  }
}