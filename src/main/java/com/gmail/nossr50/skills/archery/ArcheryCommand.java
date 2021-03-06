package com.gmail.nossr50.skills.archery;

import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillCommand;
import com.gmail.nossr50.skills.SkillType;
import com.gmail.nossr50.util.Permissions;

public class ArcheryCommand extends SkillCommand {
    private String skillShotBonus;
    private String dazeChance;
    private String dazeChanceLucky;
    private String retrieveChance;
    private String retrieveChanceLucky;

    private boolean canSkillShot;
    private boolean canDaze;
    private boolean canRetrieve;

    public ArcheryCommand() {
        super(SkillType.ARCHERY);
    }

    @Override
    protected void dataCalculations() {
        //SKILL SHOT
        double bonus = (skillValue / Archery.skillShotIncreaseLevel) * Archery.skillShotIncreasePercentage;

        if (bonus > Archery.skillShotMaxBonusPercentage) {
            skillShotBonus = percent.format(Archery.skillShotMaxBonusPercentage);
        }
        else {
            skillShotBonus = percent.format(bonus);
        }

        //DAZE
        String[] dazeStrings = calculateAbilityDisplayValues(Archery.dazeMaxBonusLevel, Archery.dazeMaxBonus);
        dazeChance = dazeStrings[0];
        dazeChanceLucky = dazeStrings[1];

        //RETRIEVE
        String[] retrieveStrings = calculateAbilityDisplayValues(Archery.retrieveMaxBonusLevel, Archery.retrieveMaxChance);
        retrieveChance = retrieveStrings[0];
        retrieveChanceLucky = retrieveStrings[1];
    }

    @Override
    protected void permissionsCheck() {
        canSkillShot = Permissions.archeryBonus(player);
        canDaze = Permissions.daze(player);
        canRetrieve = Permissions.trackArrows(player);
    }

    @Override
    protected boolean effectsHeaderPermissions() {
        return canSkillShot || canDaze || canRetrieve;
    }

    @Override
    protected void effectsDisplay() {
        luckyEffectsDisplay();

        if (canSkillShot) {
            player.sendMessage(LocaleLoader.getString("Effects.Template", new Object[] { LocaleLoader.getString("Archery.Effect.0"), LocaleLoader.getString("Archery.Effect.1") }));
        }

        if (canDaze) {
            player.sendMessage(LocaleLoader.getString("Effects.Template", new Object[] { LocaleLoader.getString("Archery.Effect.2"), LocaleLoader.getString("Archery.Effect.3", new Object[] {Archery.dazeModifier}) }));
        }

        if (canRetrieve) {
            player.sendMessage(LocaleLoader.getString("Effects.Template", new Object[] { LocaleLoader.getString("Archery.Effect.4"), LocaleLoader.getString("Archery.Effect.5") }));
        }
    }

    @Override
    protected boolean statsHeaderPermissions() {
        return canSkillShot || canDaze || canRetrieve;
    }

    @Override
    protected void statsDisplay() {
        if (canSkillShot) {
            player.sendMessage(LocaleLoader.getString("Archery.Combat.SkillshotBonus", new Object[] { skillShotBonus }));
        }

        if (canDaze) {
            if (isLucky) {
                player.sendMessage(LocaleLoader.getString("Archery.Combat.DazeChance", new Object[] { dazeChance }) + LocaleLoader.getString("Perks.lucky.bonus", new Object[] { dazeChanceLucky }));
            }
            else {
                player.sendMessage(LocaleLoader.getString("Archery.Combat.DazeChance", new Object[] { dazeChance }));
            }
        }

        if (canRetrieve) {
            if (isLucky) {
                player.sendMessage(LocaleLoader.getString("Archery.Combat.RetrieveChance", new Object[] { retrieveChance }) + LocaleLoader.getString("Perks.lucky.bonus", new Object[] { retrieveChanceLucky }));
            }
            else {
                player.sendMessage(LocaleLoader.getString("Archery.Combat.RetrieveChance", new Object[] { retrieveChance }));
            }
        }
    }
}
