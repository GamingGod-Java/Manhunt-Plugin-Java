package me.champion.manhuntplugin;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class DamageNerf implements Listener {
    private TeamManager teamManager;
    public DamageNerf(TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        System.out.println("damage");
        if (event.getDamager().getType() == EntityType.ARROW) {
            System.out.println("arrow damage");
            // Check if the shooter is a skeleton
            ProjectileSource shooter = ((org.bukkit.entity.Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Skeleton) {
                System.out.println("skeleton shooter");
                // Adjust the damage here (e.g., reduce it by 50%)
                double originalDamage = event.getDamage();
                double reducedDamage = originalDamage * 0.5; // Adjust the multiplier as needed
                event.setDamage(reducedDamage);
            }
        }
    }
}
