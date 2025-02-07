package com.example.UHC;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class UHC extends JavaPlugin implements Listener {

    private double zombieHealth;
    private double zombieSpeed;
    private double zombieDamage;
    private long jumpInterval;
    private double jumpHeight;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("UHC Plugin enabled!");

        spawnZombieEvery15Minutes();
        startRedZone();
    }

    @Override
    public void onDisable() {
        getLogger().info("UHC Plugin disabled!");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        zombieHealth = config.getDouble("zombie.health", 40.0);
        zombieSpeed = config.getDouble("zombie.speed", 0.5);
        zombieDamage = config.getDouble("zombie.damage", 5.0); // เพิ่มดาเมจ
        jumpInterval = config.getLong("zombie.jump_interval", 60);
        jumpHeight = config.getDouble("zombie.jump_height", 0.5);
    }

    @EventHandler
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            Zombie zombie = (Zombie) event.getEntity();
            zombie.setCustomNameVisible(true);

            if (zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(zombieSpeed);
            }

            if (zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(zombieHealth);
                zombie.setHealth(zombieHealth);
            }

            if (zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0 + zombieDamage);
            }

            if (zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
                zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50.0);
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

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!zombie.isDead() && zombie.isValid()) {
                        if (zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                            zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.23);
                        }
                        if (zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                            zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
                            zombie.setHealth(20.0);
                        }
                        if (zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                            zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0);
                        }
                        zombie.setCustomNameVisible(false);
                    }
                }
            }.runTaskLater(this, 6000);
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

    private void spawnZombieEvery15Minutes() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorlds().get(0);
                if (world == null) return;

                Random random = new Random();
                int x = random.nextInt(1000) - 500;
                int z = random.nextInt(1000) - 500;
                int y = world.getHighestBlockYAt(x, z);

                Location spawnLocation = new Location(world, x, y, z);
                world.spawnEntity(spawnLocation, EntityType.ZOMBIE);
            }
        }.runTaskTimer(this, 0, 18000);
    }

    private void startRedZone() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorlds().get(0);
                if (world == null) return;

                Random random = new Random();

                for (int i = 0; i < 5; i++) { 
                    int x = random.nextInt(1000) - 500;
                    int z = random.nextInt(1000) - 500;
                    int y = world.getHighestBlockYAt(x, z) + 20; 

                    Location dropLocation = new Location(world, x, y, z);
                    spawnTNT(dropLocation);
                }
            }
        }.runTaskTimer(this, 0, 6000); 
    }

    private void spawnTNT(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(location, EntityType.PRIMED_TNT);
        tnt.setFuseTicks(60);
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.PRIMED_TNT) {
            event.blockList().clear(); 

            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                Location explosionLocation = event.getLocation();
                if (player.getLocation().distance(explosionLocation) < 5) {
                    player.damage(8.0); 
                }
            }
        }
    }
}
