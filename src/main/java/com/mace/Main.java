package com.mace;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 90 * 1000; // 90 seconds cooldown

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MaceBoost plugin has loaded!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only trigger for main hand interactions
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if holding mace in main hand
        if (item.getType() == Material.MACE && event.getAction().toString().contains("RIGHT")) {
            // Check cooldown first
            if (checkCooldown(player)) {
                // Allow normal right-click functionality by not cancelling the event
                return;
            }

            // Activate ability
            event.setCancelled(true);
            launchPlayer(player);
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(ChatColor.GOLD + "Whoosh! Mace boost activated!");
        }
    }

    private boolean checkCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            long remaining = (cooldowns.get(player.getUniqueId()) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage(ChatColor.RED + "Ability on cooldown (" + remaining + "s)");
                return true;
            }
        }
        return false;
    }

    private void launchPlayer(Player player) {
        player.setMetadata("MaceBoost", new FixedMetadataValue(this, true));
        Vector velocity = player.getLocation().getDirection().multiply(0.5);
        velocity.setY(1.5);
        player.setVelocity(velocity);

        // Effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        spawnParticles(player);
    }

    private void spawnParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks++ >= 20) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 15, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.3, 0.3, 0.05);
            }
        }.runTaskTimer(this, 0, 1);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player &&
                event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                event.getEntity().hasMetadata("MaceBoost")) {
            event.setCancelled(true);
            event.getEntity().removeMetadata("MaceBoost", this);
        }
    }
}