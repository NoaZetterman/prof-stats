package com.profstats.gather;

public enum GatherState {
    INITIAL, // When node is ready to be clicked.
    TICKING, // When node timer is ticking down
    XP_GAIN, // When XP gain is displayed
    XP_GAIN_WITH_ITEM, // When Xp gain is displayed, but with a successful item
    COMPLETED_COOLDOWN // After xp gain finished displaying, when the node starts the cooldown timer
}
