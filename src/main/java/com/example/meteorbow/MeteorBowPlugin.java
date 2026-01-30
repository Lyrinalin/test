package com.example.meteorbow;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public final class MeteorBowPlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private NamespacedKey bowKey;
    private NamespacedKey projectileKey;
    private NamespacedKey ownerKey;

    private final Set<UUID> activeProjectiles = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private int cooldownTicks;
    private double impactRadius;
    private double impactDamage;
    private int fireSeconds;
    private boolean trailParticles;
    private boolean debug;

    private BukkitTask trailTask;

    @Override
    public void onEnable() {
        bowKey = new NamespacedKey(this, "bow");
        projectileKey = new NamespacedKey(this, "projectile");
        ownerKey = new NamespacedKey(this, "owner");

        saveDefaultConfig();
        reloadSettings();

        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("meteorbow"), "meteorbow command not found")
                .setExecutor(this);
        Objects.requireNonNull(getCommand("meteorbow"), "meteorbow command not found")
                .setTabCompleter(this);

        registerRecipe();
        startTrailTask();
    }

    @Override
    public void onDisable() {
        if (trailTask != null) {
            trailTask.cancel();
            trailTask = null;
        }
        activeProjectiles.clear();
        cooldowns.clear();
    }

    private void reloadSettings() {
        reloadConfig();
        cooldownTicks = getConfig().getInt("cooldown_ticks", 10);
        impactRadius = getConfig().getDouble("impact_radius", 3.0);
        impactDamage = getConfig().getDouble("impact_damage", 6.0);
        fireSeconds = getConfig().getInt("fire_seconds", 4);
        trailParticles = getConfig().getBoolean("trail_particles", true);
        debug = getConfig().getBoolean("debug", false);
    }

    private void registerRecipe() {
        ItemStack bow = createMeteorBow();
        NamespacedKey recipeKey = new NamespacedKey(this, "meteor_bow");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, bow);
        recipe.shape("BSB", "S S", " B ");
        recipe.setIngredient('B', Material.BLAZE_ROD);
        recipe.setIngredient('S', Material.STRING);
        Bukkit.removeRecipe(recipeKey);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createMeteorBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(LEGACY.deserialize("§6Огненный лук"));
        List<Component> lore = Arrays.asList(
                LEGACY.deserialize("§7Стреляет метеоритами"),
                LEGACY.deserialize("§7Боеприпасы: §6Магма-блок")
        );
        meta.lore(lore);
        meta.getPersistentDataContainer().set(bowKey, PersistentDataType.BYTE, (byte) 1);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMeteorBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Byte value = container.get(bowKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack bow = event.getBow();
        if (!isMeteorBow(bow)) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownTicks * 50L;
        Long lastShot = cooldowns.get(player.getUniqueId());
        if (lastShot != null && (now - lastShot) < cooldownMillis) {
            player.sendActionBar(LEGACY.deserialize("§cПодождите немного!"));
            event.setCancelled(true);
            event.getProjectile().remove();
            return;
        }

        if (!consumeAmmo(player.getInventory())) {
            player.sendActionBar(LEGACY.deserialize("§cНужен магма-блок!"));
            event.setCancelled(true);
            event.getProjectile().remove();
            return;
        }

        cooldowns.put(player.getUniqueId(), now);

        Projectile originalProjectile = event.getProjectile();
        Vector velocity = originalProjectile.getVelocity();
        org.bukkit.Location spawnLocation = originalProjectile.getLocation();
        originalProjectile.remove();

        Snowball meteor = spawnLocation.getWorld().spawn(spawnLocation, Snowball.class, spawned -> {
            spawned.setVelocity(velocity);
            spawned.setShooter(player);
            spawned.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
            spawned.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });

        activeProjectiles.add(meteor.getUniqueId());
        if (debug) {
            getLogger().info("Spawned meteor projectile " + meteor.getUniqueId());
        }
    }

    private boolean consumeAmmo(PlayerInventory inventory) {
        ItemStack ammo = new ItemStack(Material.MAGMA_BLOCK, 1);
        if (!inventory.containsAtLeast(ammo, 1)) {
            return false;
        }
        inventory.removeItem(ammo);
        return true;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!isMeteorProjectile(projectile)) {
            return;
        }

        activeProjectiles.remove(projectile.getUniqueId());

        org.bukkit.Location location = projectile.getLocation();
        location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0, 0, 0, 0);
        location.getWorld().spawnParticle(Particle.FLAME, location, 30, 0.3, 0.3, 0.3, 0.02);
        location.getWorld().spawnParticle(Particle.SMOKE, location, 20, 0.3, 0.3, 0.3, 0.02);
        location.getWorld().spawnParticle(Particle.LAVA, location, 10, 0.2, 0.2, 0.2, 0.01);

        UUID ownerId = getOwnerId(projectile);
        for (Entity nearby : location.getWorld().getNearbyEntities(location, impactRadius, impactRadius, impactRadius)) {
            if (!(nearby instanceof LivingEntity living)) {
                continue;
            }
            if (ownerId != null && living.getUniqueId().equals(ownerId)) {
                continue;
            }
            living.damage(impactDamage, projectile);
            living.setFireTicks(fireSeconds * 20);
        }

        projectile.remove();
    }

    private boolean isMeteorProjectile(Projectile projectile) {
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        Byte value = container.get(projectileKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private UUID getOwnerId(Projectile projectile) {
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        String owner = container.get(ownerKey, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void startTrailTask() {
        if (trailTask != null) {
            trailTask.cancel();
        }
        trailTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!trailParticles || activeProjectiles.isEmpty()) {
                return;
            }
            Iterator<UUID> iterator = activeProjectiles.iterator();
            while (iterator.hasNext()) {
                UUID id = iterator.next();
                Entity entity = Bukkit.getEntity(id);
                if (!(entity instanceof Projectile projectile) || !entity.isValid()) {
                    iterator.remove();
                    continue;
                }
                if (!isMeteorProjectile(projectile)) {
                    iterator.remove();
                    continue;
                }
                org.bukkit.Location location = projectile.getLocation();
                location.getWorld().spawnParticle(Particle.FLAME, location, 2, 0.05, 0.05, 0.05, 0.005);
                location.getWorld().spawnParticle(Particle.SMOKE, location, 2, 0.05, 0.05, 0.05, 0.005);
                location.getWorld().spawnParticle(Particle.LAVA, location, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }, 1L, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /meteorbow give | /meteorbow reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Команду может выполнить только игрок.");
                return true;
            }
            player.getInventory().addItem(createMeteorBow());
            player.sendMessage(ChatColor.GOLD + "Вы получили Огненный лук!");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadSettings();
            sender.sendMessage(ChatColor.GREEN + "MeteorBow конфиг перезагружен.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Использование: /meteorbow give | /meteorbow reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "reload");
        }
        return Collections.emptyList();
    }
}
