package peli1gamer.ourclient;

public enum AimTargetPart {
    HEAD("Head"),
    TORSO("Torso"),
    FEET("Feet"),
    CLOSEST_BODY_PART("Closest body part");

    private final String displayName;

    AimTargetPart(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
