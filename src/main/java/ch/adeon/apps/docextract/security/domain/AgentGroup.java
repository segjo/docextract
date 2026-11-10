package ch.adeon.apps.docextract.security.domain;

/**
 * The d.velop user group whose members are agents (MCP clients), configured as {@code
 * docextract.agent.group-id}. Blank or {@value #NONE} means there is no agent group.
 */
public final class AgentGroup {

  public static final String NONE = "none";
  public static final String AUTHORITY_PREFIX = "ROLE_GROUP_";

  private AgentGroup() {}

  /** Authority granted to members of the group, or {@code null} if there is no agent group. */
  public static String authority(String groupId) {
    if (groupId == null || groupId.isBlank() || NONE.equals(groupId.trim())) {
      return null;
    }
    return AUTHORITY_PREFIX + groupId.trim();
  }
}
