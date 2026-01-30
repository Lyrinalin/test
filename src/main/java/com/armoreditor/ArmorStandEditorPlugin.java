package com.armoreditor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.RayTraceResult;

public class ArmorStandEditorPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private final Map<UUID, UUID> selectedStands = new HashMap<>();
    private final Map<UUID, PoseSnapshot> poseClipboard = new HashMap<>();
    private final Map<UUID, BodyPart> activePart = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("ase") != null) {
            getCommand("ase").setExecutor(this);
            getCommand("ase").setTabCompleter(this);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ase.use")) {
            sender.sendMessage(message("messages.no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("messages.player-only"));
            return true;
        }

        if (args.length == 0) {
            Optional<ArmorStand> target = rayTraceArmorStand(player);
            if (target.isPresent()) {
                selectStand(player, target.get());
                return true;
            }
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(player);
            case "select" -> handleSelect(player);
            case "open" -> handleOpen(player);
            case "copy" -> handleCopy(player);
            case "paste" -> handlePaste(player);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "select", "open", "copy", "paste", "reload");
        }
        return List.of();
    }

    private void sendHelp(Player player) {
        List<String> lines = getConfig().getStringList("messages.help");
        if (lines.isEmpty()) {
            player.sendMessage(message("messages.prefix") + ChatColor.YELLOW + "/ase help");
            return;
        }
        for (String line : lines) {
            player.sendMessage(color(line));
        }
    }

    private void handleSelect(Player player) {
        if (!player.hasPermission("ase.select")) {
            player.sendMessage(message("messages.no-permission"));
            return;
        }
        Optional<ArmorStand> target = rayTraceArmorStand(player);
        if (target.isEmpty()) {
            player.sendMessage(message("messages.stand-not-found"));
            return;
        }
        selectStand(player, target.get());
    }

    private void handleOpen(Player player) {
        if (!player.hasPermission("ase.edit")) {
            player.sendMessage(message("messages.no-permission"));
            return;
        }
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            return;
        }
        openMainMenu(player, stand);
        player.sendMessage(message("messages.gui-opened"));
    }

    private void handleCopy(Player player) {
        if (!player.hasPermission("ase.copy")) {
            player.sendMessage(message("messages.no-permission"));
            return;
        }
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            return;
        }
        poseClipboard.put(player.getUniqueId(), PoseSnapshot.from(stand));
        player.sendMessage(message("messages.pose-copied"));
    }

    private void handlePaste(Player player) {
        if (!player.hasPermission("ase.paste")) {
            player.sendMessage(message("messages.no-permission"));
            return;
        }
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            return;
        }
        PoseSnapshot snapshot = poseClipboard.get(player.getUniqueId());
        if (snapshot == null) {
            player.sendMessage(message("messages.no-copied-pose"));
            return;
        }
        snapshot.applyTo(stand);
        player.sendMessage(message("messages.pose-pasted"));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("ase.reload")) {
            player.sendMessage(message("messages.no-permission"));
            return;
        }
        reloadConfig();
        player.sendMessage(message("messages.config-reloaded"));
    }

    private void selectStand(Player player, ArmorStand stand) {
        selectedStands.put(player.getUniqueId(), stand.getUniqueId());
        player.sendMessage(message("messages.stand-selected"));
    }

    private Optional<ArmorStand> rayTraceArmorStand(Player player) {
        double radius = getConfig().getDouble("selection-radius", 5.0);
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            radius,
            entity -> entity instanceof ArmorStand
        );
        if (result == null) {
            return Optional.empty();
        }
        Entity hit = result.getHitEntity();
        if (hit instanceof ArmorStand stand) {
            return Optional.of(stand);
        }
        return Optional.empty();
    }

    private ArmorStand getSelectedStand(Player player, boolean sendMessages) {
        UUID standId = selectedStands.get(player.getUniqueId());
        if (standId == null) {
            if (sendMessages) {
                player.sendMessage(message("messages.stand-required"));
            }
            return null;
        }
        Entity entity = Bukkit.getEntity(standId);
        if (!(entity instanceof ArmorStand stand)) {
            selectedStands.remove(player.getUniqueId());
            if (sendMessages) {
                player.sendMessage(message("messages.stand-missing"));
            }
            return null;
        }
        double radius = getConfig().getDouble("selection-radius", 5.0);
        if (!stand.getWorld().equals(player.getWorld()) || stand.getLocation().distance(player.getLocation()) > radius) {
            if (sendMessages) {
                player.sendMessage(message("messages.stand-too-far"));
            }
            return null;
        }
        return stand;
    }

    private void openMainMenu(Player player, ArmorStand stand) {
        Inventory inventory = Bukkit.createInventory(player, 54, color(getConfig().getString("menus.main-title")));
        inventory.setItem(10, toggleItem(Material.ARMOR_STAND, "Arms", stand.hasArms()));
        inventory.setItem(11, toggleItem(Material.STONE_SLAB, "Base Plate", stand.hasBasePlate()));
        inventory.setItem(12, toggleItem(Material.FEATHER, "Gravity", stand.hasGravity()));
        inventory.setItem(13, toggleItem(Material.SLIME_BALL, "Small", stand.isSmall()));
        inventory.setItem(14, toggleItem(Material.GLASS_PANE, "Visible", stand.isVisible()));
        inventory.setItem(15, toggleItem(Material.NAME_TAG, "Marker", stand.isMarker()));
        inventory.setItem(28, actionItem(Material.ARROW, "Rotate -15°", List.of("Left click")));
        inventory.setItem(34, actionItem(Material.ARROW, "Rotate +15°", List.of("Left click")));
        inventory.setItem(31, actionItem(Material.LECTERN, "Pose Editor", List.of("Edit body part poses")));
        inventory.setItem(32, actionItem(Material.CHEST, "Equipment", List.of("Copy gear from your inventory")));
        inventory.setItem(49, actionItem(Material.BARRIER, "Close", List.of("Close menu")));
        player.openInventory(inventory);
    }

    private void openPoseMenu(Player player, ArmorStand stand) {
        Inventory inventory = Bukkit.createInventory(player, 27, color(getConfig().getString("menus.pose-title")));
        inventory.setItem(10, partItem(BodyPart.HEAD, stand));
        inventory.setItem(11, partItem(BodyPart.BODY, stand));
        inventory.setItem(12, partItem(BodyPart.LEFT_ARM, stand));
        inventory.setItem(13, partItem(BodyPart.RIGHT_ARM, stand));
        inventory.setItem(14, partItem(BodyPart.LEFT_LEG, stand));
        inventory.setItem(15, partItem(BodyPart.RIGHT_LEG, stand));
        inventory.setItem(22, actionItem(Material.ARROW, "Back", List.of("Return to main menu")));
        player.openInventory(inventory);
    }

    private void openPoseEditor(Player player, ArmorStand stand, BodyPart part) {
        String title = getConfig().getString("menus.part-title", "Edit Pose: %part%")
            .replace("%part%", part.getDisplayName());
        Inventory inventory = Bukkit.createInventory(player, 27, color(title));
        inventory.setItem(10, axisItem(part, stand, "X+", 0, true));
        inventory.setItem(11, axisItem(part, stand, "X-", 0, false));
        inventory.setItem(12, axisItem(part, stand, "Y+", 1, true));
        inventory.setItem(13, axisItem(part, stand, "Y-", 1, false));
        inventory.setItem(14, axisItem(part, stand, "Z+", 2, true));
        inventory.setItem(15, axisItem(part, stand, "Z-", 2, false));
        inventory.setItem(21, actionItem(Material.BARRIER, "Reset Part", List.of("Reset this part to 0")));
        inventory.setItem(23, actionItem(Material.REDSTONE_BLOCK, "Reset All", List.of("Reset all parts")));
        inventory.setItem(26, actionItem(Material.ARROW, "Back", List.of("Return to parts menu")));
        activePart.put(player.getUniqueId(), part);
        player.openInventory(inventory);
    }

    private void openEquipmentMenu(Player player, ArmorStand stand) {
        Inventory inventory = Bukkit.createInventory(player, 27, color(getConfig().getString("menus.equipment-title")));
        inventory.setItem(10, actionItem(Material.IRON_SWORD, "Set Main Hand", List.of("Use item in your main hand")));
        inventory.setItem(11, actionItem(Material.SHIELD, "Set Off Hand", List.of("Use item in your off hand")));
        inventory.setItem(12, actionItem(Material.IRON_HELMET, "Set Helmet", List.of("Use your helmet slot")));
        inventory.setItem(13, actionItem(Material.IRON_CHESTPLATE, "Set Chestplate", List.of("Use your chestplate slot")));
        inventory.setItem(14, actionItem(Material.IRON_LEGGINGS, "Set Leggings", List.of("Use your leggings slot")));
        inventory.setItem(15, actionItem(Material.IRON_BOOTS, "Set Boots", List.of("Use your boots slot")));
        inventory.setItem(22, actionItem(Material.BARRIER, "Clear Equipment", List.of("Remove all equipment")));
        inventory.setItem(26, actionItem(Material.ARROW, "Back", List.of("Return to main menu")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) {
            return;
        }
        String mainTitle = stripMenu("menus.main-title");
        String poseTitle = stripMenu("menus.pose-title");
        String equipmentTitle = stripMenu("menus.equipment-title");
        String partPrefix = stripMenu("menus.part-title").replace("%part%", "");

        if (title.equals(mainTitle)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getSlot());
            return;
        }
        if (title.equals(poseTitle)) {
            event.setCancelled(true);
            handlePoseMenuClick(player, event.getSlot());
            return;
        }
        if (title.equals(equipmentTitle)) {
            event.setCancelled(true);
            handleEquipmentClick(player, event.getSlot());
            return;
        }
        if (title.startsWith(partPrefix)) {
            event.setCancelled(true);
            handlePartMenuClick(player, event.getSlot(), event.getClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            activePart.remove(player.getUniqueId());
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            player.closeInventory();
            return;
        }
        switch (slot) {
            case 10 -> stand.setArms(!stand.hasArms());
            case 11 -> stand.setBasePlate(!stand.hasBasePlate());
            case 12 -> stand.setGravity(!stand.hasGravity());
            case 13 -> stand.setSmall(!stand.isSmall());
            case 14 -> stand.setVisible(!stand.isVisible());
            case 15 -> stand.setMarker(!stand.isMarker());
            case 28 -> stand.setRotation(stand.getLocation().getYaw() - 15.0f, stand.getLocation().getPitch());
            case 34 -> stand.setRotation(stand.getLocation().getYaw() + 15.0f, stand.getLocation().getPitch());
            case 31 -> {
                openPoseMenu(player, stand);
                return;
            }
            case 32 -> {
                openEquipmentMenu(player, stand);
                return;
            }
            case 49 -> {
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        openMainMenu(player, stand);
    }

    private void handlePoseMenuClick(Player player, int slot) {
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            player.closeInventory();
            return;
        }
        BodyPart part = switch (slot) {
            case 10 -> BodyPart.HEAD;
            case 11 -> BodyPart.BODY;
            case 12 -> BodyPart.LEFT_ARM;
            case 13 -> BodyPart.RIGHT_ARM;
            case 14 -> BodyPart.LEFT_LEG;
            case 15 -> BodyPart.RIGHT_LEG;
            default -> null;
        };
        if (slot == 22) {
            openMainMenu(player, stand);
            return;
        }
        if (part == null) {
            return;
        }
        openPoseEditor(player, stand, part);
    }

    private void handlePartMenuClick(Player player, int slot, ClickType click) {
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            player.closeInventory();
            return;
        }
        BodyPart part = activePart.get(player.getUniqueId());
        if (part == null) {
            openPoseMenu(player, stand);
            return;
        }
        int step = getAngleStep(click);
        switch (slot) {
            case 10 -> adjustPart(part, stand, Axis.X, step);
            case 11 -> adjustPart(part, stand, Axis.X, -step);
            case 12 -> adjustPart(part, stand, Axis.Y, step);
            case 13 -> adjustPart(part, stand, Axis.Y, -step);
            case 14 -> adjustPart(part, stand, Axis.Z, step);
            case 15 -> adjustPart(part, stand, Axis.Z, -step);
            case 21 -> resetPart(part, stand);
            case 23 -> resetAll(stand);
            case 26 -> {
                openPoseMenu(player, stand);
                return;
            }
            default -> {
                return;
            }
        }
        openPoseEditor(player, stand, part);
    }

    private void handleEquipmentClick(Player player, int slot) {
        ArmorStand stand = getSelectedStand(player, true);
        if (stand == null) {
            player.closeInventory();
            return;
        }
        PlayerInventory inventory = player.getInventory();
        switch (slot) {
            case 10 -> stand.getEquipment().setItemInMainHand(cloneItem(inventory.getItemInMainHand()));
            case 11 -> stand.getEquipment().setItemInOffHand(cloneItem(inventory.getItemInOffHand()));
            case 12 -> stand.getEquipment().setHelmet(cloneItem(inventory.getHelmet()));
            case 13 -> stand.getEquipment().setChestplate(cloneItem(inventory.getChestplate()));
            case 14 -> stand.getEquipment().setLeggings(cloneItem(inventory.getLeggings()));
            case 15 -> stand.getEquipment().setBoots(cloneItem(inventory.getBoots()));
            case 22 -> stand.getEquipment().clear();
            case 26 -> {
                openMainMenu(player, stand);
                return;
            }
            default -> {
                return;
            }
        }
        openEquipmentMenu(player, stand);
    }

    private ItemStack cloneItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        return item.clone();
    }

    private void adjustPart(BodyPart part, ArmorStand stand, Axis axis, int deltaDegrees) {
        EulerAngle current = part.getPose(stand);
        double x = Math.toDegrees(current.getX());
        double y = Math.toDegrees(current.getY());
        double z = Math.toDegrees(current.getZ());
        switch (axis) {
            case X -> x = normalizeDegrees(x + deltaDegrees);
            case Y -> y = normalizeDegrees(y + deltaDegrees);
            case Z -> z = normalizeDegrees(z + deltaDegrees);
        }
        part.setPose(stand, new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z)));
    }

    private void resetPart(BodyPart part, ArmorStand stand) {
        part.setPose(stand, new EulerAngle(0, 0, 0));
    }

    private void resetAll(ArmorStand stand) {
        for (BodyPart part : BodyPart.values()) {
            part.setPose(stand, new EulerAngle(0, 0, 0));
        }
    }

    private double normalizeDegrees(double value) {
        double result = value;
        while (result > 180) {
            result -= 360;
        }
        while (result < -180) {
            result += 360;
        }
        return result;
    }

    private int getAngleStep(ClickType click) {
        if (click.isShiftClick()) {
            return getConfig().getInt("angle-steps.large", 15);
        }
        if (click.isRightClick()) {
            return getConfig().getInt("angle-steps.small", 1);
        }
        return getConfig().getInt("angle-steps.normal", 5);
    }

    private ItemStack toggleItem(Material material, String label, boolean enabled) {
        String state = enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
        return actionItem(material, label, List.of("State: " + state));
    }

    private ItemStack partItem(BodyPart part, ArmorStand stand) {
        EulerAngle pose = part.getPose(stand);
        String lore = String.format("X: %.1f°, Y: %.1f°, Z: %.1f°",
            Math.toDegrees(pose.getX()),
            Math.toDegrees(pose.getY()),
            Math.toDegrees(pose.getZ()));
        return actionItem(Material.ARMOR_STAND, part.getDisplayName(), List.of(lore));
    }

    private ItemStack axisItem(BodyPart part, ArmorStand stand, String label, int axis, boolean positive) {
        EulerAngle pose = part.getPose(stand);
        double value = switch (axis) {
            case 0 -> Math.toDegrees(pose.getX());
            case 1 -> Math.toDegrees(pose.getY());
            case 2 -> Math.toDegrees(pose.getZ());
            default -> 0;
        };
        String sign = positive ? "+" : "-";
        return actionItem(Material.COMPASS, label, List.of("Current: " + String.format("%.1f°", value),
            "Left click: " + sign + getConfig().getInt("angle-steps.normal", 5) + "°",
            "Right click: " + sign + getConfig().getInt("angle-steps.small", 1) + "°",
            "Shift click: " + sign + getConfig().getInt("angle-steps.large", 15) + "°"));
    }

    private ItemStack actionItem(Material material, String label, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + label);
            meta.setLore(loreLines.stream().map(this::color).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String stripMenu(String path) {
        return ChatColor.stripColor(color(getConfig().getString(path, "")));
    }

    private String message(String path) {
        String prefix = color(getConfig().getString("messages.prefix", ""));
        if ("messages.prefix".equals(path)) {
            return prefix;
        }
        String value = color(getConfig().getString(path, ""));
        if (value.isEmpty()) {
            return prefix;
        }
        return prefix + value;
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private enum BodyPart {
        HEAD("Head"),
        BODY("Body"),
        LEFT_ARM("Left Arm"),
        RIGHT_ARM("Right Arm"),
        LEFT_LEG("Left Leg"),
        RIGHT_LEG("Right Leg");

        private final String displayName;

        BodyPart(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public EulerAngle getPose(ArmorStand stand) {
            return switch (this) {
                case HEAD -> stand.getHeadPose();
                case BODY -> stand.getBodyPose();
                case LEFT_ARM -> stand.getLeftArmPose();
                case RIGHT_ARM -> stand.getRightArmPose();
                case LEFT_LEG -> stand.getLeftLegPose();
                case RIGHT_LEG -> stand.getRightLegPose();
            };
        }

        public void setPose(ArmorStand stand, EulerAngle angle) {
            switch (this) {
                case HEAD -> stand.setHeadPose(angle);
                case BODY -> stand.setBodyPose(angle);
                case LEFT_ARM -> stand.setLeftArmPose(angle);
                case RIGHT_ARM -> stand.setRightArmPose(angle);
                case LEFT_LEG -> stand.setLeftLegPose(angle);
                case RIGHT_LEG -> stand.setRightLegPose(angle);
            }
        }
    }

    private static class PoseSnapshot {
        private final EnumMap<BodyPart, EulerAngle> parts;

        private PoseSnapshot(EnumMap<BodyPart, EulerAngle> parts) {
            this.parts = parts;
        }

        public static PoseSnapshot from(ArmorStand stand) {
            EnumMap<BodyPart, EulerAngle> map = new EnumMap<>(BodyPart.class);
            for (BodyPart part : BodyPart.values()) {
                map.put(part, standPose(part, stand));
            }
            return new PoseSnapshot(map);
        }

        public void applyTo(ArmorStand stand) {
            for (Map.Entry<BodyPart, EulerAngle> entry : parts.entrySet()) {
                entry.getKey().setPose(stand, entry.getValue());
            }
        }

        private static EulerAngle standPose(BodyPart part, ArmorStand stand) {
            EulerAngle angle = part.getPose(stand);
            return new EulerAngle(angle.getX(), angle.getY(), angle.getZ());
        }
    }
}
