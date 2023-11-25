package de.arbeeco.statcord.messages.variables;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

public class UserValueVariable extends GenericVariable {
  public UserValueVariable(String name) {
    super(name);
  }

  @Override
  public String get(GenericInteractionCreateEvent event) {
    return String.valueOf(Data.getMemberValue(event.getUser(), event.getGuild(), getName()));
  }
}
