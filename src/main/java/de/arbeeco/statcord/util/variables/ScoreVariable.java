package de.arbeeco.statcord.util.variables;

import de.arbeeco.statcord.util.Data;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

public class ScoreVariable extends GenericVariable {
  public ScoreVariable(String name) {
    super(name);
  }

  @Override
  public String get(GenericInteractionCreateEvent event) {
    return String.valueOf(
            Data.getTextScore(event.getUser(), event.getGuild()) + Data.getVoiceScore(event.getUser(), event.getGuild())
    );
  }
}
