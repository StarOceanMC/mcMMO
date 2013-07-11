package com.gmail.nossr50.skills.archery;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.SkillUtils;

public class ArcheryManager extends SkillManager {
    public ArcheryManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, SkillType.ARCHERY);
    }

    public boolean canDaze(LivingEntity target) {
        return target instanceof Player && Permissions.daze(getPlayer());
    }

    public boolean canSkillShot() {
        return getSkillLevel() >= Archery.skillShotIncreaseLevel && Permissions.bonusDamage(getPlayer(), skill);
    }

    public boolean canTrackArrows() {
        return Permissions.arrowRetrieval(getPlayer());
    }

    /**
     * Calculate bonus XP awarded for Archery when hitting a far-away target.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     */
    public void distanceXpBonus(LivingEntity target, Entity damager) {
        Location firedLocation = Archery.stringToLocation(damager.getMetadata(mcMMO.arrowDistanceKey).get(0).asString());
        Location targetLocation = target.getLocation();

        if (firedLocation.getWorld() != targetLocation.getWorld()) {
            return;
        }

        applyXpGain((int) (firedLocation.distanceSquared(targetLocation) * Archery.DISTANCE_XP_MULTIPLIER));
    }

    /**
     * Track arrows fired for later retrieval.
     *
     * @param target The {@link LivingEntity} damaged by the arrow
     */
    public void trackArrows(LivingEntity target) {
        if (SkillUtils.activationSuccessful(getSkillLevel(), getActivationChance(), Archery.retrieveMaxChance, Archery.retrieveMaxBonusLevel)) {
            Archery.incrementTrackerValue(target);
        }
    }

    /**
     * Handle the effects of the Daze ability
     *
     * @param defender The {@link Player} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage if the ability was successful, the original event damage otherwise
     */
    public double dazeCheck(Player defender, double damage) {
        if (SkillUtils.activationSuccessful(getSkillLevel(), getActivationChance(), Archery.dazeMaxBonus, Archery.dazeMaxBonusLevel)) {
            Location dazedLocation = defender.getLocation();
            dazedLocation.setPitch(90 - Misc.getRandom().nextInt(181));

            defender.teleport(dazedLocation);
            defender.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 10));

            if (UserManager.getPlayer(defender).useChatNotifications()) {
                defender.sendMessage(LocaleLoader.getString("Combat.TouchedFuzzy"));
            }

            if (mcMMOPlayer.useChatNotifications()) {
                getPlayer().sendMessage(LocaleLoader.getString("Combat.TargetDazed"));
            }

            return damage + Archery.dazeModifier;
        }

        return damage;
    }

    /**
     * Handle the effects of the Skill Shot ability
     *
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage
     */
    public double skillShotCheck(double damage) {
        double damageBonusPercent = Math.min(((getSkillLevel() / Archery.skillShotIncreaseLevel) * Archery.skillShotIncreasePercentage), Archery.skillShotMaxBonusPercentage);
        double archeryBonus = damage * damageBonusPercent;

        return damage + archeryBonus;
    }
}
