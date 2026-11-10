package ch.adeon.apps.docextract.audit.application;

/** Who acted: a person or a member of the configured agent group. */
public enum ActorKind {
  HUMAN,
  AGENT;

  public static ActorKind of(boolean agent) {
    return agent ? AGENT : HUMAN;
  }
}
