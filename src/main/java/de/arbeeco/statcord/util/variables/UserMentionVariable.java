package de.arbeeco.statcord.util.variables;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

public class UserMentionVariable extends GenericVariable {
  public UserMentionVariable(String name) {
    super(name);
  }

  @Override
  public String get(GenericInteractionCreateEvent event) {
    return event.getMember().getAsMention();
  }
}
