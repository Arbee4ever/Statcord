package de.arbeeco.statcord.messages;

public enum MessageType {
  DEFAULT("default_messages"),
  CUSTOM("messages");

  public final String label;

  private MessageType(String label) {
    this.label = label;
  }
}
