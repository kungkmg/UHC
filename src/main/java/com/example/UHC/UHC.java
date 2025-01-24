package com.example.UHC;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class UHC extends JavaPlugin implements Listener {

    private double zombieHealth;
    private double zombieSpeed;
    private long jumpInterval;
    private double jumpHeight;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        loadConfigValues();

        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("UHC Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("UHC Plugin disabled!");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        zombieHealth = config.getDouble("zombie.health", 40.0);
        zombieSpeed = config.getDouble("zombie.speed", 0.5);
        jumpInterval = config.getLong("zombie.jump_interval", 60);
        jumpHeight = config.getDouble("zombie.jump_height", 0.5);
    }

    @EventHandler
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();

            zombie.setCustomName("Super Zombie");
            zombie.setCustomNameVisible(true);

            if (zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(zombieSpeed);
            }

            if (zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(zombieHealth);
                zombie.setHealth(zombieHealth);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!zombie.isDead() && zombie.isValid()) {
                        Vector jump = zombie.getVelocity();
                        jump.setY(jumpHeight);
                        zombie.setVelocity(jump);
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0, jumpInterval);
        }
    }

    @EventHandler
    public void onZombieTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (zombie.getTarget() != null && !zombie.isDead()) {
                        Block block = zombie.getTarget().getLocation().getBlock().getRelative(0, -1, 0);
                        if (block.getType() != Material.AIR) {
                            block.breakNaturally();
                        }
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0, 20);
        }
    }

    @EventHandler
    public void onZombieDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.FIRE) {
                event.setCancelled(true);
                zombie.setFireTicks(0);
            }
        }
    }
}
