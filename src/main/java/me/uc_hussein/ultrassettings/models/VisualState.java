package me.uc_hussein.ultrassettings.models;

import java.util.List;

/** Everything a single visible state of a button needs: how it looks, sounds like, and what it does. */
public class VisualState {

    public String material = "STONE";
    public String name = "";
    public List<String> lore = List.of();
    public String sound;
    public double volume = 0.5;
    public double pitch = 1.0;
    public boolean glow = false;
    public int customModelData = 0;
    public String command;   // optional — executed on click (PLAYER_COMMAND-style)
    public String message;   // optional — sent on click, in addition to/instead of a command
}
