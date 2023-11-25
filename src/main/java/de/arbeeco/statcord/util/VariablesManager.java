package de.arbeeco.statcord.util;

import de.arbeeco.statcord.messages.variables.GenericVariable;
import de.arbeeco.statcord.messages.variables.ScoreVariable;
import de.arbeeco.statcord.messages.variables.UserMentionVariable;
import de.arbeeco.statcord.messages.variables.UserValueVariable;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

import java.util.HashMap;

public class VariablesManager {
  HashMap<String, GenericVariable> variablesHashMap = new HashMap<>();

  public VariablesManager() {
    add(
            new UserMentionVariable("userping"),
            new UserValueVariable("textmessages"),
            new UserValueVariable("voiceseconds"),
            new ScoreVariable("score")
    );
  }

  public void add(GenericVariable... variables) {
    for (GenericVariable variable : variables) {
      variablesHashMap.put(variable.getName(), variable);
    }
  }

  public String get(String name, GenericInteractionCreateEvent event) {
    GenericVariable variable = variablesHashMap.get(name);
    if (variable == null) return "/";
    return variable.get(event);
  }
}
