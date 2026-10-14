package com.closky.rpgmod.skill;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Branches of the skill tree (section 19.2). Magic branches are shown on the "Magic" tab, the rest on "Skills".
 */
public enum SkillBranch {
    FIRE(true, 0xE0532A),
    FROST(true, 0x6FC3F7),
    LIGHTNING(true, 0xF2E35A),
    NATURE(true, 0x5BBE55),
    ARCANE(true, 0xB36FE0),
    COMBAT(false, 0xC84A3F),
    DEFENSE(false, 0x8C9BB0),
    RANGED(false, 0x7FB15E),
    EXPLORATION(false, 0xD9A441),
    SOCIAL(false, 0x5C8FD6);

    private final boolean magic;
    private final int color;
    private final String key = name().toLowerCase(Locale.ROOT);

    SkillBranch(boolean magic, int color) {
        this.magic = magic;
        this.color = color;
    }

    public boolean isMagic() {
        return magic;
    }

    public int getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("skill_branch.rpgmod." + key);
    }

    public static SkillBranch byKey(String key) {
        for (SkillBranch b : values()) {
            if (b.key.equalsIgnoreCase(key)) {
                return b;
            }
        }
        return COMBAT;
    }
}
