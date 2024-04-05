package de.arbeeco.statcord.messages;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.arbeeco.statcord.Statcord;
import de.arbeeco.statcord.util.Config;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import org.checkerframework.checker.units.qual.N;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericMessage {
  private final String name;
  private final MessageType type;
  private final String[] variables;
  String patternString = "";
  Pattern pattern;

  public GenericMessage(String name, MessageType type, @Nullable String... variables) {
    this.name = name;
    this.type = type;
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
    String message = new Gson().toJsonTree(Config.getConfigValue(event.getGuild(), "messages", type.label)).getAsJsonObject().get(name).getAsJsonObject().toString();
    Matcher matcher = pattern.matcher(message);

    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String name = matcher.group().substring(2, matcher.group().length() - 1);
      matcher.appendReplacement(sb, Statcord.variablesManager.get(name, event));
    }
    matcher.appendTail(sb);
    return new Gson().fromJson(sb.toString(), JsonObject.class);
  }

  public String[] getVariables() {
    return variables;
  }
}