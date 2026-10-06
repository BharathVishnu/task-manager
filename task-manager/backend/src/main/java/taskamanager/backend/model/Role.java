package taskamanager.backend.model;

public enum Role {
    VIEWER(1), EDITOR(2), OWNER(3);

    private final int level;
    Role(int level) { this.level = level; }
    public boolean atLeast(Role min) { return level >= min.level; }
}