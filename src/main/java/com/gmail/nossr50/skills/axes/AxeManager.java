package com.gmail.nossr50.skills.axes;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.skills.SkillType;
import com.gmail.nossr50.util.Misc;

public class AxeManager extends SkillManager {
    public AxeManager(Player player) {
        super(player, SkillType.AXES);
    }

    /**
     * Apply bonus to damage done by axes.
     *
     * @param event The event to modify
     */
    public void bonusDamage(EntityDamageByEntityEvent event) {
        AxeBonusDamageEventHandler eventHandler = new AxeBonusDamageEventHandler(this, event);

        eventHandler.calculateDamageBonus();
        eventHandler.modifyEventDamage();
    }

    /**
     * Check for critical chances on axe damage.
     *
     * @param event The event to modify
     */
    public void criticalHitCheck(EntityDamageByEntityEvent event, LivingEntity target) {
        CriticalHitEventHandler eventHandler = new CriticalHitEventHandler(this, event, target);

        double chance = (Axes.criticalHitMaxChance / Axes.criticalHitMaxBonusLevel) * eventHandler.skillModifier;

        if (chance > Misc.getRandom().nextInt(activationChance)) {
            eventHandler.modifyEventDamage();
            eventHandler.sendAbilityMessages();
        }
    }

    /**
     * Check for Impact ability.
     *
     * @param event The event to modify
     */
    public void impact(EntityDamageByEntityEvent event, LivingEntity target) {
        ImpactEventHandler eventHandler = new ImpactEventHandler(this, event, target);

        if (Misc.hasArmor(target)) {
            eventHandler.damageArmor();
        }
        else {
            eventHandler.applyGreaterImpact();
        }
    }

    /**
     * Check for Skull Splitter ability.
     *
     * @param event The event to process
     */
    public void skullSplitter(LivingEntity target, int damage) {
        SkullSplitterEventHandler eventHandler = new SkullSplitterEventHandler(player, damage, target);
        eventHandler.applyAbilityEffects();
    }
}
