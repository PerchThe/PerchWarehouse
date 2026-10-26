package dev.ev1dent.perchWarehouse.data;

public enum MarkerType {
    PLAYER_SPAWN("player-spawn", "player-spawns", "Player Spawn"),
    GUARANTEED("guaranteed", "guaranteed", "Guaranteed Crate Spot"),
    RANDOM("random", "random", "Random Crate Spot");

    /** key under editor.markers in config.yml */
    private final String configKey;
    /** key under markers: in warehouses/<name>.yml */
    private final String listKey;
    private final String displayName;

    MarkerType(String configKey, String listKey, String displayName) {
        this.configKey = configKey;
        this.listKey = listKey;
        this.displayName = displayName;
    }

    public String getConfigKey() { return configKey; }
    public String getListKey() { return listKey; }
    public String getDisplayName() { return displayName; }

    public boolean isBox() { return this != PLAYER_SPAWN; }
}
