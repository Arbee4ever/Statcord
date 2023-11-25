package de.arbeeco.statcord.messages.variables;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

public abstract class GenericVariable {
  private final String name;

  public GenericVariable(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract String get(GenericInteractionCreateEvent event);
}
