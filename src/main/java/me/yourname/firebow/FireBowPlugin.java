package me.yourname.firebow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class FireBowPlugin extends JavaPlugin implements Listener {
    private static final String ITEM_NAME = "§cОгненный лук";
    private NamespacedKey bowKey;
    private NamespacedKey projectileKey;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> trackedProjectiles = new ConcurrentSkipListSet<>();
    private BukkitTask trailTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bowKey = new NamespacedKey(this, "fire_bow");
        projectileKey = new NamespacedKey(this, "fire_bow_projectile");

        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipe();
        startTrailTask();
    }

    @Override
    public void onDisable() {
        if (trailTask != null) {
            trailTask.cancel();
        }
        trackedProjectiles.clear();
        cooldowns.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        if (!player.hasPermission("firebow.give")) {
            player.sendMessage(ChatColor.RED + "Нет прав.");
            return true;
        }
        player.getInventory().addItem(createFireBow());
        player.sendMessage(ChatColor.GOLD + "Вы получили Огненный лук.");
        return true;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (!isFireBow(bow)) {
            return;
        }
        if (!checkCooldown(player)) {
            event.setCancelled(true);
            return;
        }
        if (!consumeMagma(player)) {
            event.setCancelled(true);
            player.sendMessage(getNoAmmoMessage());
            return;
        }

        event.setCancelled(true);
        Entity projectile = event.getProjectile();
        projectile.remove();

        launchMeteor(player, event.getForce());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (!isFireBow(bow)) {
            return;
        }
        if (hasAnyArrow(player)) {
            return;
        }
        if (!checkCooldown(player)) {
            event.setCancelled(true);
            return;
        }
        if (!consumeMagma(player)) {
            event.setCancelled(true);
            player.sendMessage(getNoAmmoMessage());
            return;
        }
        event.setCancelled(true);
        launchMeteor(player, 1.0f);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!isMeteorProjectile(projectile)) {
            return;
        }
        trackedProjectiles.remove(projectile.getUniqueId());

        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity living) {
            double damage = getConfig().getDouble("damage", 8.0);
            living.damage(damage, projectile.getShooter() instanceof Entity shooter ? shooter : null);
            int fireSeconds = getConfig().getInt("fireSeconds", 4);
            living.setFireTicks(fireSeconds * 20);
        }

        playImpactEffects(projectile);
        handleExplosion(projectile);
        projectile.remove();
    }

    private void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "fire_bow"), createFireBow());
        recipe.shape("BS ", "SB ", "BS ");
        recipe.setIngredient('B', Material.BLAZE_ROD);
        recipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createFireBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ITEM_NAME);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Стреляет огненными метеоритами.");
            lore.add(ChatColor.DARK_RED + "Боеприпасы: магма-блоки.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(bowKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    private boolean isFireBow(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BOW) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(bowKey, PersistentDataType.BYTE);
    }

    private void launchMeteor(Player player, float force) {
        float clamped = Math.max(0.1f, Math.min(force, 1.0f));
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setItem(new ItemStack(Material.MAGMA_BLOCK));
        snowball.setShooter(player);
        snowball.setPersistent(true);
        snowball.setGravity(true);
        Vector velocity = player.getLocation().getDirection().multiply(3.0 * clamped);
        snowball.setVelocity(velocity);
        snowball.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        snowball.setBounce(false);
        trackedProjectiles.add(snowball.getUniqueId());
    }

    private boolean isMeteorProjectile(Projectile projectile) {
        PersistentDataContainer container = projectile.getPersistentDataContainer();
        return container.has(projectileKey, PersistentDataType.BYTE);
    }

    private boolean hasAnyArrow(Player player) {
        return player.getInventory().contains(Material.ARROW)
                || player.getInventory().contains(Material.SPECTRAL_ARROW)
                || player.getInventory().contains(Material.TIPPED_ARROW);
    }

    private boolean consumeMagma(Player player) {
        int cost = getConfig().getInt("magmaCost", 1);
        if (cost <= 0) {
            return true;
        }
        ItemStack costStack = new ItemStack(Material.MAGMA_BLOCK, cost);
        if (!player.getInventory().containsAtLeast(costStack, cost)) {
            return false;
        }
        player.getInventory().removeItem(costStack);
        return true;
    }

    private boolean checkCooldown(Player player) {
        int cooldownTicks = getConfig().getInt("cooldownTicks", 12);
        long cooldownMs = cooldownTicks * 50L;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), now);
        return true;
    }

    private void startTrailTask() {
        trailTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("trailParticles", true)) {
                return;
            }
            trackedProjectiles.removeIf(uuid -> {
                Entity entity = Bukkit.getEntity(uuid);
                if (!(entity instanceof Projectile projectile) || projectile.isDead() || !projectile.isValid()) {
                    return true;
                }
                entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 3, 0.05, 0.05, 0.05, 0.01);
                entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entity.getLocation(), 2, 0.02, 0.02, 0.02, 0.01);
                return false;
            });
        }, 1L, 2L);
    }

    private void playImpactEffects(Projectile projectile) {
        projectile.getWorld().spawnParticle(Particle.LAVA, projectile.getLocation(), 6, 0.2, 0.2, 0.2, 0.01);
        projectile.getWorld().spawnParticle(Particle.SMOKE_LARGE, projectile.getLocation(), 4, 0.2, 0.2, 0.2, 0.01);
        projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
    }

    private void handleExplosion(Projectile projectile) {
        double power = getConfig().getDouble("explosionPower", 0.0);
        if (power <= 0.0) {
            return;
        }
        projectile.getWorld().createExplosion(projectile.getLocation(), (float) power, false, false, projectile.getShooter() instanceof Entity shooter ? shooter : null);
    }

    private String getNoAmmoMessage() {
        String raw = getConfig().getString("messageNoAmmo", "&cНужен магма-блок как боеприпас!");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
