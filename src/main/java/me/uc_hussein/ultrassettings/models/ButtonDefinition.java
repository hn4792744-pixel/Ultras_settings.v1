package me.uc_hussein.ultrassettings.models;

public class ButtonDefinition {

    public String id;
    public int slot;
    public boolean enabled = true;
    public String permission; // null = no permission required

    public ButtonType type;

    // TOGGLE
    public String toggleSource; // "tpa" | "tpahere" | "tpaall" | "auto" | "gui"
    public VisualState on;
    public VisualState off;

    // BLOCK_STATUS / UNBLOCK_STATUS
    public VisualState active;
    public VisualState inactive;

    // ACTION / COMING_SOON
    public VisualState single;
    public ActionType action;
    public String targetGui;
}
