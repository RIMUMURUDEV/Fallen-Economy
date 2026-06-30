package dev.fallen.economy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FallenEconomyPlugin extends JavaPlugin implements Listener, TabExecutor {
  private final Map<Integer, AuctionListing> auctions = new LinkedHashMap<>();
  private final Map<Integer, BuyOrder> orders = new LinkedHashMap<>();
  private final Map<UUID, List<ItemStack>> orderDeliveries = new HashMap<>();
  private final Map<Integer, BuyShopItem> buyShopItems = new LinkedHashMap<>();
  private final Map<Integer, BuyShopItem> essenceShopItems = new LinkedHashMap<>();
  private final Map<Material, Double> sellValues = new LinkedHashMap<>();
  private final Map<UUID, SortMode> auctionSorts = new HashMap<>();
  private final Map<UUID, SortMode> orderSorts = new HashMap<>();
  private final Map<UUID, SortMode> buySorts = new HashMap<>();
  private final Map<UUID, ConfirmData> confirmations = new HashMap<>();
  private final Map<UUID, LastBlockFace> lastBlockFaces = new HashMap<>();
  private final Map<UUID, Long> toolOnlineMillis = new HashMap<>();
  private final Map<UUID, Long> toolOnlineSessionStartedAt = new HashMap<>();
  private final Set<Location> utilityBreakBlocks = new HashSet<>();

  private File buyShopFile;
  private File essenceShopFile;
  private File sellValuesFile;
  private File auctionsFile;
  private File ordersFile;
  private File orderDeliveriesFile;
  private File toolTimersFile;
  private File balancesFile;
  private EconomyBridge economy;
  private PlayerPointsEssenceBridge essence;
  private VaultCompatibilityHook vaultHook;
  private int nextBuyShopId = 1;
  private int nextEssenceShopId = 1;
  private int nextAuctionId = 1;
  private int nextOrderId = 1;
  private String moneyName;
  private String essenceName;
  private NamespacedKey toolKey;
  private NamespacedKey toolExpiresAtKey;
  private NamespacedKey toolOwnerUuidKey;
  private NamespacedKey toolOwnerNameKey;
  private NamespacedKey toolTimerTotalMsKey;
  private NamespacedKey toolTimerStartOwnerOnlineMsKey;
  private NamespacedKey toolRebindTokenKey;
  private NamespacedKey sellWandUsesKey;

  private static final String TOOL_PICKAXE = "pickaxe_3x3";
  private static final String TOOL_SHOVEL = "shovel_3x3";
  private static final String TOOL_AXE = "treecapitator_axe";
  private static final String TOOL_SELL_WAND = "sell_wand";
  private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  @Override
  public void onEnable() {
    saveDefaultConfig();
    toolKey = new NamespacedKey(this, "tool");
    toolExpiresAtKey = new NamespacedKey(this, "tool_expires_at");
    toolOwnerUuidKey = new NamespacedKey(this, "tool_owner_uuid");
    toolOwnerNameKey = new NamespacedKey(this, "tool_owner_name");
    toolTimerTotalMsKey = new NamespacedKey(this, "tool_timer_total_ms");
    toolTimerStartOwnerOnlineMsKey = new NamespacedKey(this, "tool_timer_start_owner_online_ms");
    toolRebindTokenKey = new NamespacedKey(this, "tool_rebind_token");
    sellWandUsesKey = new NamespacedKey(this, "sellwand_uses_remaining");
    moneyName = getConfig().getString("money.name", getConfig().getString("currency-name", "$"));
    essenceName = getConfig().getString("essence.name", "Essence");

    buyShopFile = new File(getDataFolder(), "buy-shop.yml");
    essenceShopFile = new File(getDataFolder(), "essence-shop.yml");
    sellValuesFile = new File(getDataFolder(), "sell-values.yml");
    auctionsFile = new File(getDataFolder(), "auctions.yml");
    ordersFile = new File(getDataFolder(), "orders.yml");
    orderDeliveriesFile = new File(getDataFolder(), "order-deliveries.yml");
    toolTimersFile = new File(getDataFolder(), "tool-timers.yml");
    balancesFile = new File(getDataFolder(), getConfig().getString("money.storage-file", "money.yml"));

    saveBundledDataFile("buy-shop.yml");
    saveBundledDataFile("essence-shop.yml");
    saveBundledDataFile("sell-values.yml");
    economy = EconomyBridge.create(this);
    essence = PlayerPointsEssenceBridge.create(this);
    if (economy instanceof InternalEconomyBridge internal) {
      vaultHook = VaultCompatibilityHook.tryRegister(this, internal, moneyName);
    }
    loadBuyShop();
    loadEssenceShop();
    loadSellValues();
    loadAuctions();
    loadOrders();
    loadOrderDeliveries();
    loadToolTimers();
    Bukkit.getOnlinePlayers().forEach(this::startToolTimerSession);

    Objects.requireNonNull(getCommand("shop")).setExecutor(this);
    Objects.requireNonNull(getCommand("shop")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("sell")).setExecutor(this);
    Objects.requireNonNull(getCommand("sell")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("essence")).setExecutor(this);
    Objects.requireNonNull(getCommand("essence")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("essenceshop")).setExecutor(this);
    Objects.requireNonNull(getCommand("essenceshop")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("ah")).setExecutor(this);
    Objects.requireNonNull(getCommand("ah")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("order")).setExecutor(this);
    Objects.requireNonNull(getCommand("order")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("balance")).setExecutor(this);
    Objects.requireNonNull(getCommand("balance")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("pay")).setExecutor(this);
    Objects.requireNonNull(getCommand("pay")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("feconomy")).setExecutor(this);
    Objects.requireNonNull(getCommand("feconomy")).setTabCompleter(this);
    Bukkit.getPluginManager().registerEvents(this, this);
    if (getConfig().getBoolean("commands.force-shop-hook", true)) {
      getLogger().info("Force /shop hook enabled; FallenEconomy will handle player /shop before command dispatch.");
    }
  }

  @Override
  public void onDisable() {
    saveBuyShop();
    saveEssenceShop();
    saveAuctions();
    saveOrders();
    saveOrderDeliveries();
    Bukkit.getOnlinePlayers().forEach(this::stopToolTimerSession);
    saveToolTimers();
    if (vaultHook != null) vaultHook.unregister();
    if (economy instanceof InternalEconomyBridge internal) internal.save();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String name = command.getName().toLowerCase(Locale.ROOT);
    if (name.equals("shop")) return handleBuyCommand(sender, args);
    if (name.equals("sell")) return handleSellCommand(sender, args);
    if (name.equals("essence")) return handleEssenceCommand(sender);
    if (name.equals("essenceshop")) return handleEssenceShopCommand(sender, args);
    if (name.equals("ah")) return handleAuctionCommand(sender, args);
    if (name.equals("order")) return handleOrderCommand(sender, args);
    if (name.equals("balance")) return handleBalanceCommand(sender);
    if (name.equals("pay")) return handlePayCommand(sender, args);
    if (name.equals("feconomy")) return handleAdminEconomyCommand(sender, args);
    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (!getConfig().getBoolean("commands.force-shop-hook", true)) return;
    String message = event.getMessage();
    if (message == null) return;

    String commandLine = message.trim();
    if (!commandLine.startsWith("/")) return;
    commandLine = commandLine.substring(1).trim();
    if (commandLine.isEmpty()) return;

    String[] parts = commandLine.split("\\s+");
    if (!parts[0].equalsIgnoreCase("shop")) return;

    event.setCancelled(true);
    String[] args = Arrays.copyOfRange(parts, 1, parts.length);
    handleBuyCommand(event.getPlayer(), args);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUtilityInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null && event.getBlockFace() != null) {
      lastBlockFaces.put(player.getUniqueId(), new LastBlockFace(event.getClickedBlock().getLocation(), event.getBlockFace(), System.currentTimeMillis()));
      return;
    }
    if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
    ItemStack hand = player.getInventory().getItemInMainHand();
    if (!isUtilityTool(hand, TOOL_SELL_WAND)) return;
    if (!getConfig().getBoolean("tools.sellwand.enabled", true)) {
      player.sendMessage(color("&cSell Wand is disabled."));
      return;
    }
    if (!player.hasPermission("falleneconomy.tools.sellwand")) {
      player.sendMessage(color("&cYou do not have permission to use this tool."));
      return;
    }
    if (!(event.getClickedBlock().getState() instanceof Container container)) return;
    event.setCancelled(true);
    if (sellContainerContents(player, container.getInventory())) {
      consumeSellWandUse(player, hand);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onTimedUtilityBlockBreak(BlockBreakEvent event) {
    ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
    if (!isTimedUtilityTool(tool)) return;
    if (ensureTimedToolUsable(event.getPlayer(), tool)) return;
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUtilityToolDrop(PlayerDropItemEvent event) {
    Item item = event.getItemDrop();
    ItemStack stack = item.getItemStack();
    if (!isTimedUtilityTool(stack)) return;
    migrateLegacyTimedTool(event.getPlayer(), stack);
    if (!isTimedToolOwner(event.getPlayer(), stack)) {
      removeStackRebindToken(stack);
      item.getPersistentDataContainer().remove(toolRebindTokenKey);
      item.setItemStack(stack);
      return;
    }
    removeStackRebindToken(stack);
    tagDroppedTimedToolForRebind(item, stack);
    updateTimedToolLore(stack);
    item.setItemStack(stack);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onUtilityToolDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    for (ItemStack stack : event.getDrops()) {
      if (!isTimedUtilityTool(stack)) continue;
      migrateLegacyTimedTool(player, stack);
      if (!isTimedToolOwner(player, stack)) continue;
      tagTimedToolStackForRebind(stack);
      updateTimedToolLore(stack);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUtilityToolPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    Item item = event.getItem();
    ItemStack stack = item.getItemStack();
    if (!isTimedUtilityTool(stack)) return;
    if (isLegacyTimedTool(stack)) {
      migrateLegacyTimedTool(player, stack);
    }
    if (hasValidRebindToken(item, stack)) {
      rebindTimedTool(player, stack);
      item.getPersistentDataContainer().remove(toolRebindTokenKey);
      removeStackRebindToken(stack);
    }
    updateTimedToolLore(stack);
    item.setItemStack(stack);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onUtilityBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    if (utilityBreakBlocks.contains(block.getLocation())) return;
    Player player = event.getPlayer();
    ItemStack tool = player.getInventory().getItemInMainHand();
    if (isUtilityTool(tool, TOOL_PICKAXE)) {
      handlePickaxeBreak(player, block, tool);
      return;
    }
    if (isUtilityTool(tool, TOOL_SHOVEL)) {
      handleShovelBreak(player, block, tool);
      return;
    }
    if (isUtilityTool(tool, TOOL_AXE)) {
      handleTreecapitatorBreak(player, block, tool);
    }
  }

  private boolean handleBuyCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use buy commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.buy")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      openShopCategories(player);
      return true;
    }

    if (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("edit")) {
      return handleBuyConfigCommand(player, args);
    }
    if (args[0].equalsIgnoreCase("sort")) {
      if (args.length < 2) {
        player.sendMessage(color("&eUsage: /shop sort <newest|oldest|price_asc|price_desc|amount>"));
        return true;
      }
      SortMode sort = SortMode.from(args[1]);
      if (sort == null) {
        player.sendMessage(color("&cUnknown sort mode."));
        return true;
      }
      buySorts.put(player.getUniqueId(), sort);
      player.sendMessage(color("&aShop sort set to &f" + sort.id + "&a."));
      openShopCategories(player);
      return true;
    }
    if (args[0].equalsIgnoreCase("help")) {
      sendBuyHelp(player);
      return true;
    }

    Integer page = parseInt(args[0]);
    if (page != null) openShopCategories(player);
    else {
      String category = canonicalCategory(args[0]);
      if (category.equalsIgnoreCase("Spawners")) {
        player.sendMessage(color("&eSpawner items are in &f/essenceshop&e."));
      } else {
        openBuyMenu(player, category, 0);
      }
    }
    return true;
  }

  private boolean handleBuyConfigCommand(Player player, String[] args) {
    if (!player.hasPermission("falleneconomy.buy.config")) {
      player.sendMessage(color("&cYou do not have permission to configure the shop."));
      return true;
    }
    if (args.length == 1) {
      openBuyConfigMenu(player, 0);
      return true;
    }
    switch (args[1].toLowerCase(Locale.ROOT)) {
      case "add" -> {
        if (args.length < 4) {
          player.sendMessage(color("&eUsage: /shop edit add <price> <category>"));
          return true;
        }
        Double price = parseMoney(args[2]);
        if (price == null) {
          player.sendMessage(color("&cInvalid price."));
          return true;
        }
        addBuyShopItem(player, price, args[3], ShopCurrency.MONEY, buyShopItems, true);
        return true;
      }
      case "remove", "delete" -> {
        if (args.length < 3) {
          player.sendMessage(color("&eUsage: /shop edit remove <id>"));
          return true;
        }
        Integer id = parseInt(args[2]);
        if (id == null) {
          player.sendMessage(color("&cInvalid item id."));
          return true;
        }
        removeBuyShopItem(player, id);
        return true;
      }
      case "price" -> {
        if (args.length < 4) {
          player.sendMessage(color("&eUsage: /shop edit price <id> <price>"));
          return true;
        }
        Integer id = parseInt(args[2]);
        Double price = parseMoney(args[3]);
        if (id == null || price == null) {
          player.sendMessage(color("&cInvalid item id or price."));
          return true;
        }
        setBuyShopPrice(player, id, price);
        return true;
      }
      case "list" -> {
        listBuyShopItems(player);
        return true;
      }
      case "help" -> {
        sendBuyConfigHelp(player);
        return true;
      }
      default -> {
        sendBuyConfigHelp(player);
        return true;
      }
    }
  }

  private boolean handleSellCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use sell commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.sell")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      openSellChest(player);
      return true;
    }
    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "hand" -> {
        if (!getConfig().getBoolean("sell.allow-hand", true)) {
          player.sendMessage(color("&c/sell hand is disabled."));
          return true;
        }
        sellHand(player);
        return true;
      }
      case "all" -> {
        if (!getConfig().getBoolean("sell.allow-all", true)) {
          player.sendMessage(color("&c/sell all is disabled."));
          return true;
        }
        sellAll(player);
        return true;
      }
      case "values" -> {
        openSellMenu(player, 0);
        return true;
      }
      case "help" -> {
        sendSellHelp(player);
        return true;
      }
      default -> {
        sendSellHelp(player);
        return true;
      }
    }
  }

  private boolean handleBalanceCommand(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use balance commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.balance")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    player.sendMessage(color("&aBalance: &f" + format(economy.balance(player)) + " " + moneyName));
    return true;
  }

  private boolean handleEssenceCommand(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use Essence commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.essence")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (!essence.available()) {
      player.sendMessage(color("&cPlayerPoints is required for Essence."));
      return true;
    }
    player.sendMessage(color("&aEssence: &f" + format(essence.balance(player)) + " " + essenceName));
    return true;
  }

  private boolean handleEssenceShopCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use Essence shop commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.essenceshop")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      openEssenceShopMenu(player, "Spawners", 0);
      return true;
    }
    if (args[0].equalsIgnoreCase("config")) {
      return handleEssenceShopConfigCommand(player, args);
    }
    if (args[0].equalsIgnoreCase("help")) {
      sendEssenceShopHelp(player);
      return true;
    }
    Integer page = parseInt(args[0]);
    if (page != null) openEssenceShopMenu(player, "Spawners", Math.max(0, page - 1));
    else openEssenceShopMenu(player, canonicalCategory(args[0]), 0);
    return true;
  }

  private boolean handleEssenceShopConfigCommand(Player player, String[] args) {
    if (!player.hasPermission("falleneconomy.essenceshop.config")) {
      player.sendMessage(color("&cYou do not have permission to configure the Essence shop."));
      return true;
    }
    if (args.length == 1) {
      openEssenceShopConfigMenu(player, 0);
      return true;
    }
    switch (args[1].toLowerCase(Locale.ROOT)) {
      case "add" -> {
        if (args.length < 4) {
          player.sendMessage(color("&eUsage: /essenceshop config add <price> <category>"));
          return true;
        }
        Double price = parseMoney(args[2]);
        if (price == null) {
          player.sendMessage(color("&cInvalid price."));
          return true;
        }
        addBuyShopItem(player, price, args[3], ShopCurrency.ESSENCE, essenceShopItems, false);
        return true;
      }
      case "remove", "delete" -> {
        if (args.length < 3) {
          player.sendMessage(color("&eUsage: /essenceshop config remove <id>"));
          return true;
        }
        Integer id = parseInt(args[2]);
        if (id == null) {
          player.sendMessage(color("&cInvalid item id."));
          return true;
        }
        removeEssenceShopItem(player, id);
        return true;
      }
      case "price" -> {
        if (args.length < 4) {
          player.sendMessage(color("&eUsage: /essenceshop config price <id> <price>"));
          return true;
        }
        Integer id = parseInt(args[2]);
        Double price = parseMoney(args[3]);
        if (id == null || price == null) {
          player.sendMessage(color("&cInvalid item id or price."));
          return true;
        }
        setEssenceShopPrice(player, id, price);
        return true;
      }
      case "list" -> {
        listShopItems(player, essenceShopItems, "Fallen Essence Shop Items");
        return true;
      }
      default -> {
        sendEssenceShopHelp(player);
        return true;
      }
    }
  }

  private boolean handlePayCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use pay commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.pay")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length < 2) {
      player.sendMessage(color("&eUsage: /pay <player> <amount>"));
      return true;
    }
    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      player.sendMessage(color("&cThat player is not online."));
      return true;
    }
    if (target.getUniqueId().equals(player.getUniqueId())) {
      player.sendMessage(color("&cYou cannot pay yourself."));
      return true;
    }
    Double amount = parseMoney(args[1]);
    if (amount == null) {
      player.sendMessage(color("&cInvalid amount."));
      return true;
    }
    if (!economy.has(player, amount)) {
      player.sendMessage(color("&cYou need &f" + format(amount) + " " + moneyName + "&c."));
      return true;
    }
    if (!economy.withdraw(player, amount)) {
      player.sendMessage(color("&cPayment failed."));
      return true;
    }
    economy.deposit(target, amount);
    player.sendMessage(color("&aPaid &f" + target.getName() + " " + format(amount) + " " + moneyName + "&a."));
    target.sendMessage(color("&aReceived &f" + format(amount) + " " + moneyName + " &afrom &f" + player.getName() + "&a."));
    return true;
  }

  private boolean handleAuctionCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use auction commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.ah")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      openAuctionMenu(player, 0);
      return true;
    }

    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "sell" -> {
        if (!player.hasPermission("falleneconomy.ah.sell")) {
          player.sendMessage(color("&cYou do not have permission to sell auctions."));
          return true;
        }
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /ah sell <price>"));
          return true;
        }
        Double price = parseMoney(args[1]);
        if (price == null) {
          player.sendMessage(color("&cInvalid price."));
          return true;
        }
        createAuction(player, price);
        return true;
      }
      case "sort" -> {
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /ah sort <newest|oldest|price_asc|price_desc>"));
          return true;
        }
        SortMode sort = SortMode.from(args[1]);
        if (sort == null) {
          player.sendMessage(color("&cUnknown sort mode."));
          return true;
        }
        auctionSorts.put(player.getUniqueId(), sort);
        player.sendMessage(color("&aAuction sort set to &f" + sort.id + "&a."));
        openAuctionMenu(player, 0);
        return true;
      }
      case "cancel" -> {
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /ah cancel <id>"));
          return true;
        }
        Integer id = parseInt(args[1]);
        if (id == null) {
          player.sendMessage(color("&cInvalid auction id."));
          return true;
        }
        cancelAuction(player, id);
        return true;
      }
      case "help" -> {
        sendAuctionHelp(player);
        return true;
      }
      default -> {
        Integer page = parseInt(args[0]);
        if (page != null) openAuctionMenu(player, Math.max(0, page - 1));
        else sendAuctionHelp(player);
        return true;
      }
    }
  }

  private boolean handleOrderCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(color("&cOnly players can use order commands."));
      return true;
    }
    if (!player.hasPermission("falleneconomy.order")) {
      player.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      openOrdersMenu(player, 0);
      return true;
    }

    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "create" -> {
        if (!player.hasPermission("falleneconomy.order.create")) {
          player.sendMessage(color("&cYou do not have permission to create orders."));
          return true;
        }
        if (args.length < 3) {
          player.sendMessage(color("&eUsage: /order create <unitPrice> <amount>"));
          return true;
        }
        Double unitPrice = parseMoney(args[1]);
        Integer amount = parseInt(args[2]);
        if (unitPrice == null || amount == null) {
          player.sendMessage(color("&cInvalid unit price or amount."));
          return true;
        }
        createOrder(player, unitPrice, amount);
        return true;
      }
      case "fill" -> {
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /order fill <id> [amount]"));
          return true;
        }
        Integer id = parseInt(args[1]);
        Integer amount = args.length >= 3 ? parseInt(args[2]) : null;
        if (id == null || (args.length >= 3 && amount == null)) {
          player.sendMessage(color("&cInvalid order id or amount."));
          return true;
        }
        fillOrder(player, id, amount == null ? 1 : amount);
        return true;
      }
      case "cancel" -> {
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /order cancel <id>"));
          return true;
        }
        Integer id = parseInt(args[1]);
        if (id == null) {
          player.sendMessage(color("&cInvalid order id."));
          return true;
        }
        cancelOrder(player, id);
        return true;
      }
      case "sort" -> {
        if (args.length < 2) {
          player.sendMessage(color("&eUsage: /order sort <newest|oldest|price_asc|price_desc|amount>"));
          return true;
        }
        SortMode sort = SortMode.from(args[1]);
        if (sort == null) {
          player.sendMessage(color("&cUnknown sort mode."));
          return true;
        }
        orderSorts.put(player.getUniqueId(), sort);
        player.sendMessage(color("&aOrder sort set to &f" + sort.id + "&a."));
        openOrdersMenu(player, 0);
        return true;
      }
      case "help" -> {
        sendOrderHelp(player);
        return true;
      }
      default -> {
        Integer page = parseInt(args[0]);
        if (page != null) openOrdersMenu(player, Math.max(0, page - 1));
        else sendOrderHelp(player);
        return true;
      }
    }
  }

  private boolean handleAdminEconomyCommand(CommandSender sender, String[] args) {
    if (!sender.hasPermission("falleneconomy.admin")) {
      sender.sendMessage(color("&cYou do not have permission."));
      return true;
    }
    if (args.length == 0) {
      sender.sendMessage(color("&e/feconomy balance <player>"));
      sender.sendMessage(color("&e/feconomy give <player> <amount>"));
      sender.sendMessage(color("&e/feconomy take <player> <amount>"));
      sender.sendMessage(color("&e/feconomy set <player> <amount>"));
      sender.sendMessage(color("&e/feconomy essence balance <player>"));
      sender.sendMessage(color("&e/feconomy essence give|take|set <player> <amount>"));
      sender.sendMessage(color("&e/feconomy tools give <player> <pickaxe|shovel|axe>"));
      sender.sendMessage(color("&e/feconomy tools give timed <player> <pickaxe|shovel|axe> <hours>"));
      sender.sendMessage(color("&e/feconomy tools give <player> sellwand <uses>"));
      return true;
    }
    if (args[0].equalsIgnoreCase("essence")) {
      return handleAdminEssenceCommand(sender, args);
    }
    if (args[0].equalsIgnoreCase("tools")) {
      return handleAdminToolsCommand(sender, args);
    }
    if (args[0].equalsIgnoreCase("balance") && args.length >= 2) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      sender.sendMessage(color("&a" + target.getName() + ": &f" + format(economy.balance(target)) + " " + moneyName));
      return true;
    }
    if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      Double amount = parseMoney(args[2]);
      if (amount == null) {
        sender.sendMessage(color("&cInvalid amount."));
        return true;
      }
      economy.deposit(target, amount);
      sender.sendMessage(color("&aGave &f" + format(amount) + " " + moneyName + " &ato &f" + target.getName() + "&a."));
      return true;
    }
    if (args[0].equalsIgnoreCase("take") && args.length >= 3) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      Double amount = parseMoney(args[2]);
      if (amount == null) {
        sender.sendMessage(color("&cInvalid amount."));
        return true;
      }
      if (!economy.withdraw(target, amount)) {
        sender.sendMessage(color("&cTarget does not have enough " + moneyName + "."));
        return true;
      }
      sender.sendMessage(color("&aTook &f" + format(amount) + " " + moneyName + " &afrom &f" + target.getName() + "&a."));
      return true;
    }
    if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      Double amount = parseMoneyAllowZero(args[2]);
      if (amount == null) {
        sender.sendMessage(color("&cInvalid amount."));
        return true;
      }
      if (economy instanceof InternalEconomyBridge internal) {
        internal.setBalance(target, amount);
        sender.sendMessage(color("&aSet &f" + target.getName() + " &ato &f" + format(amount) + " " + moneyName + "&a."));
      } else {
        sender.sendMessage(color("&cInternal money backend is unavailable."));
      }
      return true;
    }
    sender.sendMessage(color("&cUnknown admin command."));
    return true;
  }

  private boolean handleAdminToolsCommand(CommandSender sender, String[] args) {
    if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
      sendToolsHelp(sender);
      return true;
    }
    boolean timed = args[2].equalsIgnoreCase("timed") || args[2].equalsIgnoreCase("temp");
    if (timed && args.length != 6) {
      sender.sendMessage(color("&e/feconomy tools give timed <player> <pickaxe|shovel|axe> <hours>"));
      return true;
    }
    String targetName = timed ? args[3] : args[2];
    String toolName = timed ? args[4] : args[3];
    if (!timed && toolName.equalsIgnoreCase("sellwand")) {
      return giveLimitedSellWand(sender, args, targetName);
    }
    if (!timed && args.length != 4) {
      sendToolsHelp(sender);
      return true;
    }
    if (timed && isSellWandName(toolName)) {
      sender.sendMessage(color("&cSell Wand uses charges instead of time. Use /feconomy tools give <player> sellwand <uses>."));
      return true;
    }
    Player target = Bukkit.getPlayerExact(targetName);
    if (target == null) {
      sender.sendMessage(color("&cPlayer must be online."));
      return true;
    }
    double hours = 0;
    if (timed) {
      Double parsedHours = parseMoney(args[5]);
      if (parsedHours == null || parsedHours > 8760) {
        sender.sendMessage(color("&cHours must be greater than 0 and no more than 8760."));
        return true;
      }
      hours = parsedHours;
    }
    ItemStack tool = timed ? createTimedUtilityTool(toolName, target, hours) : createUtilityTool(toolName);
    if (tool == null) {
      sender.sendMessage(color("&cUnknown tool. Use pickaxe, shovel, or axe."));
      return true;
    }
    giveOrDrop(target, tool);
    if (timed) {
      sender.sendMessage(color("&aGave &f" + displayItemName(tool) + " &ato &f" + target.getName() + " &afor &f" + format(hours) + " online hour(s)&a."));
    } else {
      sender.sendMessage(color("&aGave &f" + displayItemName(tool) + " &ato &f" + target.getName() + "&a."));
    }
    return true;
  }

  private void sendToolsHelp(CommandSender sender) {
    sender.sendMessage(color("&e/feconomy tools give <player> <pickaxe|shovel|axe>"));
    sender.sendMessage(color("&e/feconomy tools give timed <player> <pickaxe|shovel|axe> <hours>"));
    sender.sendMessage(color("&e/feconomy tools give <player> sellwand <uses>"));
  }

  private boolean giveLimitedSellWand(CommandSender sender, String[] args, String targetName) {
    if (args.length != 5) {
      sender.sendMessage(color("&e/feconomy tools give <player> sellwand <uses>"));
      return true;
    }
    Player target = Bukkit.getPlayerExact(targetName);
    if (target == null) {
      sender.sendMessage(color("&cPlayer must be online."));
      return true;
    }
    Integer uses = parseInt(args[4]);
    if (uses == null || uses <= 0 || uses > 1_000_000) {
      sender.sendMessage(color("&cUses must be between 1 and 1000000."));
      return true;
    }
    ItemStack tool = createSellWand(uses);
    giveOrDrop(target, tool);
    sender.sendMessage(color("&aGave &f" + displayItemName(tool) + " &ato &f" + target.getName() + " &awith &f" + uses + " use(s)&a."));
    return true;
  }

  private boolean handleAdminEssenceCommand(CommandSender sender, String[] args) {
    if (!essence.available()) {
      sender.sendMessage(color("&cPlayerPoints is required for Essence commands."));
      return true;
    }
    if (args.length < 3) {
      sender.sendMessage(color("&e/feconomy essence balance <player>"));
      sender.sendMessage(color("&e/feconomy essence give|take|set <player> <amount>"));
      return true;
    }
    OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
    if (args[1].equalsIgnoreCase("balance")) {
      sender.sendMessage(color("&a" + target.getName() + ": &f" + format(essence.balance(target)) + " " + essenceName));
      return true;
    }
    if (args.length < 4) {
      sender.sendMessage(color("&cAmount is required."));
      return true;
    }
    Double amount = parseMoneyAllowZero(args[3]);
    if (amount == null) {
      sender.sendMessage(color("&cInvalid amount."));
      return true;
    }
    switch (args[1].toLowerCase(Locale.ROOT)) {
      case "give" -> {
        essence.deposit(target, amount);
        sender.sendMessage(color("&aGave &f" + format(amount) + " " + essenceName + " &ato &f" + target.getName() + "&a."));
      }
      case "take" -> {
        if (!essence.withdraw(target, amount)) sender.sendMessage(color("&cTarget does not have enough " + essenceName + "."));
        else sender.sendMessage(color("&aTook &f" + format(amount) + " " + essenceName + " &afrom &f" + target.getName() + "&a."));
      }
      case "set" -> {
        essence.set(target, amount);
        sender.sendMessage(color("&aSet &f" + target.getName() + " &ato &f" + format(amount) + " " + essenceName + "&a."));
      }
      default -> sender.sendMessage(color("&cUnknown Essence admin command."));
    }
    return true;
  }

  private void createAuction(Player seller, double price) {
    double min = getConfig().getDouble("auction.min-price", 1);
    double max = getConfig().getDouble("auction.max-price", 1_000_000);
    if (price < min || price > max) {
      seller.sendMessage(color("&cAuction price must be between &f" + format(min) + " &cand &f" + format(max) + "&c."));
      return;
    }
    long current = auctions.values().stream().filter(a -> a.seller.equals(seller.getUniqueId())).count();
    int maxListings = getConfig().getInt("auction.max-listings-per-player", 25);
    if (current >= maxListings) {
      seller.sendMessage(color("&cYou reached the auction listing limit."));
      return;
    }
    ItemStack hand = seller.getInventory().getItemInMainHand();
    if (hand.getType().isAir() || hand.getAmount() <= 0) {
      seller.sendMessage(color("&cHold the item stack you want to sell."));
      return;
    }
    ItemStack listed = hand.clone();
    seller.getInventory().setItemInMainHand(null);
    AuctionListing listing = new AuctionListing(nextAuctionId++, seller.getUniqueId(), seller.getName(), listed, price, System.currentTimeMillis());
    auctions.put(listing.id, listing);
    saveAuctions();
    seller.sendMessage(color("&aListed &f" + listed.getAmount() + "x " + niceMaterial(listed.getType()) + " &afor &f" + format(price) + " " + moneyName + "&a."));
  }

  private void cancelAuction(Player player, int id) {
    AuctionListing listing = auctions.get(id);
    if (listing == null) {
      player.sendMessage(color("&cAuction not found."));
      return;
    }
    if (!listing.seller.equals(player.getUniqueId()) && !player.hasPermission("falleneconomy.admin")) {
      player.sendMessage(color("&cYou can only cancel your own auctions."));
      return;
    }
    auctions.remove(id);
    giveOrDrop(player, listing.item.clone());
    saveAuctions();
    player.sendMessage(color("&aAuction #" + id + " cancelled."));
  }

  private void buyAuction(Player buyer, int id) {
    AuctionListing listing = auctions.get(id);
    if (listing == null) {
      buyer.sendMessage(color("&cThis auction is no longer available."));
      return;
    }
    if (listing.seller.equals(buyer.getUniqueId())) {
      buyer.sendMessage(color("&cUse /ah cancel " + id + " to reclaim your own listing."));
      return;
    }
    if (!economy.has(buyer, listing.price)) {
      buyer.sendMessage(color("&cYou need &f" + format(listing.price) + " " + moneyName + "&c."));
      return;
    }
    if (!economy.withdraw(buyer, listing.price)) {
      buyer.sendMessage(color("&cPayment failed."));
      return;
    }
    OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.seller);
    economy.deposit(seller, listing.price);
    auctions.remove(id);
    giveOrDrop(buyer, listing.item.clone());
    saveAuctions();
    buyer.sendMessage(color("&aPurchased auction #" + id + " for &f" + format(listing.price) + " " + moneyName + "&a."));
    if (seller.isOnline() && seller.getPlayer() != null) {
      seller.getPlayer().sendMessage(color("&aYour auction #" + id + " sold for &f" + format(listing.price) + " " + moneyName + "&a."));
    }
  }

  private void createOrder(Player creator, double unitPrice, int amount) {
    double min = getConfig().getDouble("orders.min-unit-price", 1);
    double max = getConfig().getDouble("orders.max-unit-price", 1_000_000);
    int maxAmount = getConfig().getInt("orders.max-amount", 3456);
    if (unitPrice < min || unitPrice > max || amount < 1 || amount > maxAmount) {
      creator.sendMessage(color("&cOrder must be within configured price and amount limits."));
      return;
    }
    long current = orders.values().stream().filter(o -> o.creator.equals(creator.getUniqueId())).count();
    int maxOrders = getConfig().getInt("orders.max-orders-per-player", 25);
    if (current >= maxOrders) {
      creator.sendMessage(color("&cYou reached the order limit."));
      return;
    }
    ItemStack hand = creator.getInventory().getItemInMainHand();
    if (hand.getType().isAir()) {
      creator.sendMessage(color("&cHold the item type you want to order."));
      return;
    }
    double total = unitPrice * amount;
    if (!economy.has(creator, total)) {
      creator.sendMessage(color("&cYou need &f" + format(total) + " " + moneyName + " &cto fund this order."));
      return;
    }
    if (!economy.withdraw(creator, total)) {
      creator.sendMessage(color("&cPayment failed."));
      return;
    }
    BuyOrder order = new BuyOrder(nextOrderId++, creator.getUniqueId(), creator.getName(), hand.getType(), unitPrice, amount, amount, System.currentTimeMillis());
    orders.put(order.id, order);
    saveOrders();
    creator.sendMessage(color("&aCreated order #" + order.id + " for &f" + amount + "x " + niceMaterial(order.material) + " &aat &f" + format(unitPrice) + " " + moneyName + " each&a."));
  }

  private void fillOrder(Player seller, int id, int requestedAmount) {
    BuyOrder order = orders.get(id);
    if (order == null) {
      seller.sendMessage(color("&cOrder not found."));
      return;
    }
    if (order.creator.equals(seller.getUniqueId())) {
      seller.sendMessage(color("&cYou cannot fill your own order."));
      return;
    }
    ItemStack hand = seller.getInventory().getItemInMainHand();
    if (hand.getType() != order.material) {
      seller.sendMessage(color("&cHold &f" + niceMaterial(order.material) + " &cto fill this order."));
      return;
    }
    int fill = Math.min(Math.min(requestedAmount, hand.getAmount()), order.remaining);
    if (fill <= 0) {
      seller.sendMessage(color("&cNothing to fill."));
      return;
    }
    hand.setAmount(hand.getAmount() - fill);
    seller.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
    double payout = fill * order.unitPrice;
    economy.deposit(seller, payout);
    deliverOrderItems(order.creator, new ItemStack(order.material, fill));
    order.remaining -= fill;
    if (order.remaining <= 0) orders.remove(id);
    saveOrders();
    saveOrderDeliveries();
    seller.sendMessage(color("&aFilled &f" + fill + "x " + niceMaterial(order.material) + " &afor &f" + format(payout) + " " + moneyName + "&a."));
    OfflinePlayer creator = Bukkit.getOfflinePlayer(order.creator);
    if (creator.isOnline() && creator.getPlayer() != null) {
      creator.getPlayer().sendMessage(color("&aYour order #" + id + " received &f" + fill + "x " + niceMaterial(order.material) + "&a."));
    }
  }

  private void cancelOrder(Player player, int id) {
    BuyOrder order = orders.get(id);
    if (order == null) {
      player.sendMessage(color("&cOrder not found."));
      return;
    }
    if (!order.creator.equals(player.getUniqueId()) && !player.hasPermission("falleneconomy.admin")) {
      player.sendMessage(color("&cYou can only cancel your own orders."));
      return;
    }
    orders.remove(id);
    double refund = order.remaining * order.unitPrice;
    economy.deposit(Bukkit.getOfflinePlayer(order.creator), refund);
    saveOrders();
    player.sendMessage(color("&aOrder #" + id + " cancelled. Refunded &f" + format(refund) + " " + moneyName + "&a."));
  }

  private void addBuyShopItem(Player player, double price, String category, ShopCurrency currency, Map<Integer, BuyShopItem> targetShop, boolean moneyShop) {
    double min = getConfig().getDouble("buy.min-price", 1);
    double max = getConfig().getDouble("buy.max-price", 1_000_000);
    if (price < min || price > max) {
      player.sendMessage(color("&cBuy price must be between &f" + format(min) + " &cand &f" + format(max) + "&c."));
      return;
    }
    int maxItems = getConfig().getInt("buy.max-items", 500);
    if (targetShop.size() >= maxItems) {
      player.sendMessage(color("&cShop item limit reached."));
      return;
    }
    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand.getType().isAir() || hand.getAmount() <= 0) {
      player.sendMessage(color("&cHold the shop item stack you want to add."));
      return;
    }
    ItemStack item = hand.clone();
    int id = moneyShop ? nextBuyShopId++ : nextEssenceShopId++;
    BuyShopItem shopItem = new BuyShopItem(id, item, price, System.currentTimeMillis(), canonicalCategory(category), currency);
    targetShop.put(shopItem.id, shopItem);
    if (moneyShop) saveBuyShop();
    else saveEssenceShop();
    player.sendMessage(color("&aAdded shop item #&f" + shopItem.id + " &a(&f" + item.getAmount() + "x " + niceMaterial(item.getType()) + "&a) for &f" + format(price) + " " + currencyLabel(currency) + "&a."));
  }

  private void removeBuyShopItem(Player player, int id) {
    BuyShopItem removed = buyShopItems.remove(id);
    if (removed == null) {
      player.sendMessage(color("&cBuy item not found."));
      return;
    }
    saveBuyShop();
    player.sendMessage(color("&aRemoved shop item #&f" + id + "&a."));
  }

  private void setBuyShopPrice(Player player, int id, double price) {
    double min = getConfig().getDouble("buy.min-price", 1);
    double max = getConfig().getDouble("buy.max-price", 1_000_000);
    if (price < min || price > max) {
      player.sendMessage(color("&cBuy price must be between &f" + format(min) + " &cand &f" + format(max) + "&c."));
      return;
    }
    BuyShopItem item = buyShopItems.get(id);
    if (item == null) {
      player.sendMessage(color("&cBuy item not found."));
      return;
    }
    item.price = price;
    saveBuyShop();
    player.sendMessage(color("&aSet shop item #&f" + id + " &aprice to &f" + format(price) + " " + moneyName + "&a."));
  }

  private void removeEssenceShopItem(Player player, int id) {
    BuyShopItem removed = essenceShopItems.remove(id);
    if (removed == null) {
      player.sendMessage(color("&cEssence shop item not found."));
      return;
    }
    saveEssenceShop();
    player.sendMessage(color("&aRemoved Essence shop item #&f" + id + "&a."));
  }

  private void setEssenceShopPrice(Player player, int id, double price) {
    BuyShopItem item = essenceShopItems.get(id);
    if (item == null) {
      player.sendMessage(color("&cEssence shop item not found."));
      return;
    }
    item.price = price;
    saveEssenceShop();
    player.sendMessage(color("&aSet Essence shop item #&f" + id + " &aprice to &f" + format(price) + " " + essenceName + "&a."));
  }

  private void listBuyShopItems(Player player) {
    listShopItems(player, buyShopItems, "Fallen Buy Shop Items");
  }

  private void listShopItems(Player player, Map<Integer, BuyShopItem> items, String title) {
    if (items.isEmpty()) {
      player.sendMessage(color("&eShop is empty."));
      return;
    }
    player.sendMessage(color("&6" + title));
    items.values().stream().limit(25).forEach(item -> player.sendMessage(color(
      "&e#" + item.id + " &7- &f" + item.item.getAmount() + "x " + displayItemName(item.item) + " &7- &b" + format(item.price) + " " + currencyLabel(item.currency) + " &8(" + item.category + ")"
    )));
    if (items.size() > 25) {
      player.sendMessage(color("&7Showing 25/" + items.size() + " items. Use the config GUI for pages."));
    }
  }

  private void buyShopItem(Player buyer, int id, Map<Integer, BuyShopItem> sourceShop) {
    buyShopItem(buyer, id, sourceShop, 0);
  }

  private void buyShopItem(Player buyer, int id, Map<Integer, BuyShopItem> sourceShop, int amount) {
    BuyShopItem shopItem = sourceShop.get(id);
    if (shopItem == null) {
      buyer.sendMessage(color("&cThis shop item is no longer available."));
      return;
    }
    int purchaseAmount = amount <= 0 ? Math.max(1, shopItem.item.getAmount()) : Math.max(1, Math.min(64, amount));
    double totalPrice = shopPriceForAmount(shopItem, purchaseAmount);
    if (!hasCurrency(buyer, shopItem.currency, totalPrice)) {
      buyer.sendMessage(color("&cYou need &f" + format(totalPrice) + " " + currencyLabel(shopItem.currency) + "&c."));
      return;
    }
    if (!withdrawCurrency(buyer, shopItem.currency, totalPrice)) {
      buyer.sendMessage(color("&cPayment failed."));
      return;
    }
    giveShopItems(buyer, shopItem.item, purchaseAmount);
    buyer.sendMessage(color("&aBought &f" + purchaseAmount + "x " + displayItemName(shopItem.item) + " &afor &f" + format(totalPrice) + " " + currencyLabel(shopItem.currency) + "&a."));
  }

  private void giveShopItems(Player player, ItemStack template, int amount) {
    int remaining = Math.max(1, amount);
    int maxStack = Math.max(1, template.getMaxStackSize());
    while (remaining > 0) {
      int stackAmount = Math.min(maxStack, remaining);
      ItemStack stack = template.clone();
      stack.setAmount(stackAmount);
      giveOrDrop(player, stack);
      remaining -= stackAmount;
    }
  }

  private double unitShopPrice(BuyShopItem shopItem) {
    return shopItem.price / Math.max(1, shopItem.item.getAmount());
  }

  private double shopPriceForAmount(BuyShopItem shopItem, int amount) {
    return unitShopPrice(shopItem) * Math.max(1, amount);
  }

  private ItemStack createUtilityTool(String id) {
    String normalized = id.toLowerCase(Locale.ROOT);
    String toolId = switch (normalized) {
      case "pickaxe", "3x3", "3x3pickaxe" -> TOOL_PICKAXE;
      case "shovel", "3x3shovel" -> TOOL_SHOVEL;
      case "axe", "treecapitator", "treeaxe" -> TOOL_AXE;
      default -> null;
    };
    return toolId == null ? null : taggedTool(toolId);
  }

  private ItemStack createTimedUtilityTool(String id, Player owner, double hours) {
    ItemStack item = createUtilityTool(id);
    if (item == null) return null;
    long totalMillis = Math.max(1L, Math.round(hours * 60D * 60D * 1000D));
    bindTimedTool(item, owner, totalMillis);
    return item;
  }

  private ItemStack createSellWand(int uses) {
    ItemStack item = taggedTool(TOOL_SELL_WAND);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.getPersistentDataContainer().set(sellWandUsesKey, PersistentDataType.INTEGER, Math.max(1, uses));
      item.setItemMeta(meta);
    }
    updateSellWandLore(item);
    return item;
  }

  private ItemStack taggedTool(String toolId) {
    ItemStack item = new ItemStack(toolMaterial(toolId));
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(color(toolName(toolId)));
      meta.setLore(baseToolLore(toolId).stream().map(FallenEconomyPlugin::color).toList());
      meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, toolId);
      item.setItemMeta(meta);
    }
    applyDefaultToolEnchantments(item, toolId);
    return item;
  }

  private void applyDefaultToolEnchantments(ItemStack item, String toolId) {
    if (TOOL_SELL_WAND.equals(toolId)) return;
    item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
    item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
    item.addUnsafeEnchantment(Enchantment.MENDING, 1);
    item.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
  }

  private boolean isSellWandName(String id) {
    String normalized = id.toLowerCase(Locale.ROOT);
    return normalized.equals("sellwand") || normalized.equals("wand") || normalized.equals("sell_wand");
  }

  private Material toolMaterial(String toolId) {
    return switch (toolId) {
      case TOOL_PICKAXE -> Material.NETHERITE_PICKAXE;
      case TOOL_SHOVEL -> Material.NETHERITE_SHOVEL;
      case TOOL_AXE -> Material.NETHERITE_AXE;
      case TOOL_SELL_WAND -> Material.STICK;
      default -> Material.STICK;
    };
  }

  private String toolName(String toolId) {
    return switch (toolId) {
      case TOOL_PICKAXE -> "&#E2E6EE&lꜰ&#CFD4E0&lᴀ&#BBC2D1&lʟ&#A8B0C3&lʟ&#959DB5&lᴇ&#818BA6&lɴ &#7986A7&lᴅ&#8492B6&lʀ&#909FC6&lɪ&#9BABD5&lʟ&#A6B8E4&lʟ";
      case TOOL_SHOVEL -> "&#E2E6EE&lꜰ&#CFD4E0&lᴀ&#BBC2D1&lʟ&#A8B0C3&lʟ&#959DB5&lᴇ&#818BA6&lɴ &#7986A7&lꜱ&#8492B6&lᴘ&#909FC6&lᴀ&#9BABD5&lᴅ&#A6B8E4&lᴇ";
      case TOOL_AXE -> "&#E2E6EE&lꜰ&#D4D8E3&lᴀ&#C5CBD9&lʟ&#B7BDCE&lʟ&#A8B0C3&lᴇ&#9AA2B8&lɴ &#7D87A3&lᴛ&#6E7998&lʀ&#7581A2&lᴇ&#7C89AB&lᴇ&#8391B5&lᴄ&#8A99BE&lᴜ&#91A0C8&lᴛ&#98A8D1&lᴛ&#9FB0DB&lᴇ&#A6B8E4&lʀ";
      case TOOL_SELL_WAND -> "&bFallen Sell Wand";
      default -> "&bFallen Tool";
    };
  }

  private List<String> baseToolLore(String toolId) {
    return switch (toolId) {
      case TOOL_PICKAXE -> List.of("&7Mines 3x3x1", "&8Fallen Utility Tool");
      case TOOL_SHOVEL -> List.of("&7Digs 3x3x1", "&8Fallen Utility Tool");
      case TOOL_AXE -> List.of("&7Breaks connected logs", "&8Fallen Utility Tool");
      case TOOL_SELL_WAND -> List.of("&7Right-click a container to sell contents", "&8Fallen Utility Tool");
      default -> List.of("&8Fallen Utility Tool");
    };
  }

  private boolean isFallenUtilityTool(ItemStack item) {
    if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
  }

  private boolean isUtilityTool(ItemStack item, String toolId) {
    if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    return toolId.equals(meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING));
  }

  private boolean isTimedUtilityTool(ItemStack item) {
    if (!isFallenUtilityTool(item)) return false;
    String toolId = toolId(item);
    if (TOOL_SELL_WAND.equals(toolId)) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    return meta.getPersistentDataContainer().has(toolTimerTotalMsKey, PersistentDataType.LONG) || isLegacyTimedTool(item);
  }

  private boolean isLegacyTimedTool(ItemStack item) {
    if (!isFallenUtilityTool(item) || !item.hasItemMeta()) return false;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    return meta.getPersistentDataContainer().has(toolExpiresAtKey, PersistentDataType.LONG) &&
      !meta.getPersistentDataContainer().has(toolOwnerUuidKey, PersistentDataType.STRING);
  }

  private String toolId(ItemStack item) {
    if (!isFallenUtilityTool(item)) return "";
    ItemMeta meta = item.getItemMeta();
    return meta == null ? "" : meta.getPersistentDataContainer().getOrDefault(toolKey, PersistentDataType.STRING, "");
  }

  private void bindTimedTool(ItemStack item, Player owner, long totalMillis) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    meta.getPersistentDataContainer().set(toolOwnerUuidKey, PersistentDataType.STRING, owner.getUniqueId().toString());
    meta.getPersistentDataContainer().set(toolOwnerNameKey, PersistentDataType.STRING, owner.getName());
    meta.getPersistentDataContainer().set(toolTimerTotalMsKey, PersistentDataType.LONG, Math.max(1L, totalMillis));
    meta.getPersistentDataContainer().set(toolTimerStartOwnerOnlineMsKey, PersistentDataType.LONG, ownerOnlineMillis(owner.getUniqueId()));
    meta.getPersistentDataContainer().remove(toolExpiresAtKey);
    meta.getPersistentDataContainer().remove(toolRebindTokenKey);
    item.setItemMeta(meta);
    updateTimedToolLore(item);
  }

  private void migrateLegacyTimedTool(Player owner, ItemStack item) {
    if (!isLegacyTimedTool(item)) return;
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    Long expiresAt = meta.getPersistentDataContainer().get(toolExpiresAtKey, PersistentDataType.LONG);
    long remaining = expiresAt == null ? 0 : expiresAt - System.currentTimeMillis();
    bindTimedTool(item, owner, Math.max(1L, remaining));
  }

  private boolean ensureTimedToolUsable(Player player, ItemStack item) {
    migrateLegacyTimedTool(player, item);
    if (!isTimedUtilityTool(item)) return true;
    if (!isTimedToolOwner(player, item)) {
      player.sendMessage(color("&cThis Fallen tool belongs to &f" + timedToolOwnerName(item) + "&c."));
      return false;
    }
    if (timedToolRemainingMillis(item) <= 0) {
      player.getInventory().setItemInMainHand(null);
      player.sendMessage(color("&cThis Fallen tool has expired."));
      return false;
    }
    updateTimedToolLore(item);
    return true;
  }

  private boolean isTimedToolOwner(Player player, ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return false;
    String owner = meta.getPersistentDataContainer().get(toolOwnerUuidKey, PersistentDataType.STRING);
    return player.getUniqueId().toString().equals(owner);
  }

  private String timedToolOwnerName(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return "Unknown";
    return meta.getPersistentDataContainer().getOrDefault(toolOwnerNameKey, PersistentDataType.STRING, "Unknown");
  }

  private UUID timedToolOwnerId(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return null;
    String raw = meta.getPersistentDataContainer().get(toolOwnerUuidKey, PersistentDataType.STRING);
    if (raw == null || raw.isBlank()) return null;
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private long timedToolRemainingMillis(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return 0;
    Long total = meta.getPersistentDataContainer().get(toolTimerTotalMsKey, PersistentDataType.LONG);
    Long start = meta.getPersistentDataContainer().get(toolTimerStartOwnerOnlineMsKey, PersistentDataType.LONG);
    UUID owner = timedToolOwnerId(item);
    if (total == null || start == null || owner == null) return 0;
    long elapsed = Math.max(0L, ownerOnlineMillis(owner) - start);
    return Math.max(0L, total - elapsed);
  }

  private void rebindTimedTool(Player newOwner, ItemStack item) {
    long remaining = Math.max(1L, timedToolRemainingMillis(item));
    bindTimedTool(item, newOwner, remaining);
  }

  private void tagDroppedTimedToolForRebind(Item itemEntity, ItemStack stack) {
    String owner = ownerToken(stack);
    if (owner == null) return;
    itemEntity.getPersistentDataContainer().set(toolRebindTokenKey, PersistentDataType.STRING, owner);
  }

  private void tagTimedToolStackForRebind(ItemStack stack) {
    String owner = ownerToken(stack);
    if (owner == null) return;
    ItemMeta meta = stack.getItemMeta();
    if (meta == null) return;
    meta.getPersistentDataContainer().set(toolRebindTokenKey, PersistentDataType.STRING, owner);
    stack.setItemMeta(meta);
  }

  private boolean hasValidRebindToken(Item itemEntity, ItemStack stack) {
    String owner = ownerToken(stack);
    if (owner == null) return false;
    String entityToken = itemEntity.getPersistentDataContainer().get(toolRebindTokenKey, PersistentDataType.STRING);
    if (owner.equals(entityToken)) return true;
    ItemMeta meta = stack.getItemMeta();
    if (meta == null) return false;
    return owner.equals(meta.getPersistentDataContainer().get(toolRebindTokenKey, PersistentDataType.STRING));
  }

  private String ownerToken(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    return meta == null ? null : meta.getPersistentDataContainer().get(toolOwnerUuidKey, PersistentDataType.STRING);
  }

  private void removeStackRebindToken(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    if (meta == null) return;
    meta.getPersistentDataContainer().remove(toolRebindTokenKey);
    stack.setItemMeta(meta);
  }

  private void updateTimedToolLore(ItemStack item) {
    String toolId = toolId(item);
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    List<String> lore = new ArrayList<>(baseToolLore(toolId));
    lore.add("&7Owner: &f" + timedToolOwnerName(item));
    lore.add("&7Time Left: &f" + formatDuration(timedToolRemainingMillis(item)));
    meta.setLore(lore.stream().map(FallenEconomyPlugin::color).toList());
    item.setItemMeta(meta);
  }

  private void updateSellWandLore(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    Integer uses = meta.getPersistentDataContainer().get(sellWandUsesKey, PersistentDataType.INTEGER);
    List<String> lore = new ArrayList<>(baseToolLore(TOOL_SELL_WAND));
    lore.add("&7Uses: &f" + (uses == null ? "Unlimited" : uses));
    meta.setLore(lore.stream().map(FallenEconomyPlugin::color).toList());
    item.setItemMeta(meta);
  }

  private void consumeSellWandUse(Player player, ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    Integer uses = meta.getPersistentDataContainer().get(sellWandUsesKey, PersistentDataType.INTEGER);
    if (uses == null) return;
    int remaining = uses - 1;
    if (remaining <= 0) {
      player.getInventory().setItemInMainHand(null);
      player.sendMessage(color("&cYour Fallen Sell Wand has no uses left."));
      return;
    }
    meta.getPersistentDataContainer().set(sellWandUsesKey, PersistentDataType.INTEGER, remaining);
    item.setItemMeta(meta);
    updateSellWandLore(item);
  }

  private String formatDuration(long millis) {
    if (millis <= 0) return "expired";
    long totalMinutes = Math.max(1L, (millis + 59_999L) / 60_000L);
    long hours = totalMinutes / 60L;
    long minutes = totalMinutes % 60L;
    if (hours <= 0) return minutes + "m";
    if (minutes <= 0) return hours + "h";
    return hours + "h " + minutes + "m";
  }

  private void loadToolTimers() {
    toolOnlineMillis.clear();
    FileConfiguration config = YamlConfiguration.loadConfiguration(toolTimersFile);
    ConfigurationSection section = config.getConfigurationSection("players");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      try {
        UUID uuid = UUID.fromString(key);
        toolOnlineMillis.put(uuid, Math.max(0L, section.getLong(key + ".online-ms", 0L)));
      } catch (IllegalArgumentException ignored) {
        // Ignore malformed UUID rows written by hand.
      }
    }
  }

  private void saveToolTimers() {
    FileConfiguration config = new YamlConfiguration();
    for (Map.Entry<UUID, Long> entry : toolOnlineMillis.entrySet()) {
      long onlineMs = Math.max(0L, ownerOnlineMillis(entry.getKey()));
      config.set("players." + entry.getKey() + ".online-ms", onlineMs);
    }
    try {
      config.save(toolTimersFile);
    } catch (Exception exception) {
      getLogger().severe("Failed to save tool-timers.yml: " + exception.getMessage());
    }
  }

  private void startToolTimerSession(Player player) {
    toolOnlineSessionStartedAt.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
    toolOnlineMillis.putIfAbsent(player.getUniqueId(), 0L);
  }

  private void stopToolTimerSession(Player player) {
    UUID uuid = player.getUniqueId();
    Long startedAt = toolOnlineSessionStartedAt.remove(uuid);
    if (startedAt == null) return;
    long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
    toolOnlineMillis.put(uuid, Math.max(0L, toolOnlineMillis.getOrDefault(uuid, 0L)) + elapsed);
  }

  private long ownerOnlineMillis(UUID owner) {
    long stored = Math.max(0L, toolOnlineMillis.getOrDefault(owner, 0L));
    Long startedAt = toolOnlineSessionStartedAt.get(owner);
    if (startedAt == null) return stored;
    return stored + Math.max(0L, System.currentTimeMillis() - startedAt);
  }

  private void sellHand(Player player) {
    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand.getType().isAir() || hand.getAmount() <= 0) {
      player.sendMessage(color("&cHold the item stack you want to sell."));
      return;
    }
    double unitValue = sellValue(hand.getType());
    if (unitValue <= 0) {
      player.sendMessage(color("&c" + niceMaterial(hand.getType()) + " cannot be sold."));
      return;
    }
    int amount = hand.getAmount();
    double total = unitValue * amount;
    player.getInventory().setItemInMainHand(null);
    economy.deposit(player, total);
    player.sendMessage(color("&aSold &f" + amount + "x " + niceMaterial(hand.getType()) + " &afor &f" + format(total) + " " + moneyName + "&a."));
  }

  private void sellAll(Player player) {
    ItemStack[] contents = player.getInventory().getStorageContents();
    double total = 0;
    int stacks = 0;
    int items = 0;
    for (int slot = 0; slot < contents.length; slot++) {
      ItemStack item = contents[slot];
      if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
      double unitValue = sellValue(item.getType());
      if (unitValue <= 0) continue;
      total += unitValue * item.getAmount();
      items += item.getAmount();
      stacks++;
      contents[slot] = null;
    }
    if (total <= 0) {
      player.sendMessage(color("&cNo sellable items found in your inventory."));
      return;
    }
    player.getInventory().setStorageContents(contents);
    economy.deposit(player, total);
    player.sendMessage(color("&aSold &f" + items + " items &7(" + stacks + " stacks)&a for &f" + format(total) + " " + moneyName + "&a."));
  }

  private void sellAllMaterial(Player player, Material material) {
    double unitValue = sellValue(material);
    if (unitValue <= 0) return;
    ItemStack[] contents = player.getInventory().getStorageContents();
    int amount = 0;
    for (int slot = 0; slot < contents.length; slot++) {
      ItemStack item = contents[slot];
      if (item == null || item.getType() != material) continue;
      amount += item.getAmount();
      contents[slot] = null;
    }
    if (amount <= 0) {
      player.sendMessage(color("&cYou do not have any " + niceMaterial(material) + " to sell."));
      return;
    }
    player.getInventory().setStorageContents(contents);
    double total = amount * unitValue;
    economy.deposit(player, total);
    player.sendMessage(color("&aSold &f" + amount + "x " + niceMaterial(material) + " &afor &f" + format(total) + " " + moneyName + "&a."));
  }

  private boolean sellContainerContents(Player player, Inventory inventory) {
    double total = 0;
    int soldItems = 0;
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      if (item == null || item.getType().isAir() || item.getAmount() <= 0) continue;
      double unitValue = sellValue(item.getType());
      if (unitValue <= 0) continue;
      soldItems += item.getAmount();
      total += unitValue * item.getAmount();
      inventory.setItem(slot, null);
    }
    if (total <= 0) {
      player.sendMessage(color("&cThis container has no sellable items."));
      return false;
    }
    economy.deposit(player, total);
    player.sendMessage(color("&aSold &f" + soldItems + " &aitem(s) from the container for &f" + format(total) + " " + moneyName + "&a."));
    return true;
  }

  private void handlePickaxeBreak(Player player, Block origin, ItemStack tool) {
    if (!getConfig().getBoolean("tools.pickaxe.enabled", true)) return;
    if (!player.hasPermission("falleneconomy.tools.pickaxe")) return;
    if (!isPickaxeMineable(origin)) return;
    BlockFace face = lastFaceFor(player, origin);
    int maxExtra = Math.max(0, getConfig().getInt("tools.pickaxe.max-extra-blocks", 8));
    int broken = 0;
    for (Block extra : toolArea(origin, face)) {
      if (broken >= maxExtra || tool.getType().isAir()) return;
      if (extra.equals(origin) || !isPickaxeMineable(extra)) continue;
      if (breakUtilityBlock(player, extra, tool)) broken++;
    }
  }

  private void handleShovelBreak(Player player, Block origin, ItemStack tool) {
    if (!getConfig().getBoolean("tools.shovel.enabled", true)) return;
    if (!player.hasPermission("falleneconomy.tools.shovel")) return;
    if (!isShovelMineable(origin)) return;
    BlockFace face = lastFaceFor(player, origin);
    int maxExtra = Math.max(0, getConfig().getInt("tools.shovel.max-extra-blocks", 8));
    int broken = 0;
    for (Block extra : toolArea(origin, face)) {
      if (broken >= maxExtra || tool.getType().isAir()) return;
      if (extra.equals(origin) || !isShovelMineable(extra)) continue;
      if (breakUtilityBlock(player, extra, tool)) broken++;
    }
  }

  private List<Block> toolArea(Block origin, BlockFace face) {
    List<Block> blocks = new ArrayList<>();
    if (face == BlockFace.UP || face == BlockFace.DOWN) {
      for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) blocks.add(origin.getRelative(x, 0, z));
      return blocks;
    }
    if (face == BlockFace.EAST || face == BlockFace.WEST) {
      for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) blocks.add(origin.getRelative(0, y, z));
      return blocks;
    }
    for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) blocks.add(origin.getRelative(x, y, 0));
    return blocks;
  }

  private BlockFace lastFaceFor(Player player, Block origin) {
    LastBlockFace last = lastBlockFaces.get(player.getUniqueId());
    if (last != null && last.location.equals(origin.getLocation()) && System.currentTimeMillis() - last.createdAt <= 2_500) {
      return last.face;
    }
    double x = Math.abs(player.getLocation().getDirection().getX());
    double y = Math.abs(player.getLocation().getDirection().getY());
    double z = Math.abs(player.getLocation().getDirection().getZ());
    if (y >= x && y >= z) return BlockFace.UP;
    return x >= z ? BlockFace.EAST : BlockFace.NORTH;
  }

  private boolean isPickaxeMineable(Block block) {
    Material material = block.getType();
    if (material.isAir() || block.isLiquid() || !material.isSolid()) return false;
    if (!Tag.MINEABLE_PICKAXE.isTagged(material)) return false;
    if (block.getState() instanceof Container) return false;
    return !isUnsafeUtilityBlock(material);
  }

  private boolean isShovelMineable(Block block) {
    Material material = block.getType();
    if (material.isAir() || block.isLiquid() || !material.isSolid()) return false;
    if (!Tag.MINEABLE_SHOVEL.isTagged(material)) return false;
    if (block.getState() instanceof Container) return false;
    return !isUnsafeUtilityBlock(material);
  }

  private boolean isUnsafeUtilityBlock(Material material) {
    String name = material.name();
    return material == Material.BEDROCK ||
      material == Material.BARRIER ||
      material == Material.STRUCTURE_VOID ||
      material == Material.END_PORTAL_FRAME ||
      name.contains("COMMAND_BLOCK") ||
      name.contains("PORTAL") ||
      name.contains("SPAWNER");
  }

  private void handleTreecapitatorBreak(Player player, Block origin, ItemStack tool) {
    if (!getConfig().getBoolean("tools.axe.enabled", true)) return;
    if (!player.hasPermission("falleneconomy.tools.axe")) return;
    if (!isLogBlock(origin.getType())) return;
    int maxLogs = Math.max(0, getConfig().getInt("tools.axe.max-logs", 128));
    Material logType = origin.getType();
    Set<Location> visited = new HashSet<>();
    Deque<Block> queue = new ArrayDeque<>();
    visited.add(origin.getLocation());
    queue.add(origin);
    int broken = 0;
    while (!queue.isEmpty() && broken < maxLogs && !tool.getType().isAir()) {
      Block current = queue.removeFirst();
      for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
        Block next = current.getRelative(face);
        if (!visited.add(next.getLocation())) continue;
        if (next.equals(origin) || next.getType() != logType) continue;
        if (breakUtilityBlock(player, next, tool)) {
          broken++;
          queue.add(next);
        }
        if (broken >= maxLogs || tool.getType().isAir()) break;
      }
    }
  }

  private boolean isLogBlock(Material material) {
    String name = material.name();
    return name.endsWith("_LOG") ||
      name.endsWith("_WOOD") ||
      name.endsWith("_STEM") ||
      name.endsWith("_HYPHAE");
  }

  private boolean breakUtilityBlock(Player player, Block block, ItemStack tool) {
    Location location = block.getLocation();
    BlockBreakEvent synthetic = new BlockBreakEvent(block, player);
    utilityBreakBlocks.add(location);
    try {
      Bukkit.getPluginManager().callEvent(synthetic);
      if (synthetic.isCancelled()) return false;
      boolean broken = block.breakNaturally(tool);
      if (broken) damageUtilityTool(player, tool);
      return broken;
    } finally {
      utilityBreakBlocks.remove(location);
    }
  }

  private void damageUtilityTool(Player player, ItemStack tool) {
    if (player.getGameMode() == GameMode.CREATIVE) return;
    if (!(tool.getItemMeta() instanceof Damageable damageable)) return;
    int maxDurability = tool.getType().getMaxDurability();
    if (maxDurability <= 0) return;
    int newDamage = damageable.getDamage() + 1;
    if (newDamage >= maxDurability) {
      player.getInventory().setItemInMainHand(null);
      return;
    }
    damageable.setDamage(newDamage);
    tool.setItemMeta(damageable);
  }

  private void openBuyMenu(Player player, int page) {
    openShopCategories(player);
  }

  private void openShopCategories(Player player) {
    PagedHolder holder = new PagedHolder(MenuType.BUY_CATEGORIES, 0);
    Inventory inv = Bukkit.createInventory(holder, 27, color("&8Fallen Shop"));
    holder.inventory = inv;
    addCategoryButton(inv, holder, 10, "End", Material.END_STONE, buyShopItems);
    addCategoryButton(inv, holder, 12, "Nether", Material.NETHERRACK, buyShopItems);
    addCategoryButton(inv, holder, 14, "Gear", Material.TOTEM_OF_UNDYING, buyShopItems);
    addCategoryButton(inv, holder, 16, "Food", Material.COOKED_BEEF, buyShopItems);
    player.openInventory(inv);
  }

  private void addCategoryButton(Inventory inv, PagedHolder holder, int slot, String category, Material icon, Map<Integer, BuyShopItem> source) {
    long count = source.values().stream().filter(item -> item.category.equalsIgnoreCase(category)).count();
    ItemStack button = navItem(icon, "&b" + category);
    ItemMeta meta = button.getItemMeta();
    if (meta != null) {
      meta.setLore(List.of(
        color("&7Items: &f" + count),
        color("&eClick to open")
      ));
      button.setItemMeta(meta);
    }
    inv.setItem(slot, button);
    holder.categorySlots.put(slot, category);
  }

  private void openBuyMenu(Player player, String category, int page) {
    String normalizedCategory = canonicalCategory(category);
    List<BuyShopItem> sorted = sortedShop(player, buyShopItems, normalizedCategory);
    int itemsPerPage = 18;
    int maxPage = Math.max(0, (sorted.size() - 1) / itemsPerPage);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.BUY, page, normalizedCategory);
    Inventory inv = Bukkit.createInventory(holder, 27, color("&8" + normalizedCategory + " Shop &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * itemsPerPage;
    for (int slot = 0; slot < itemsPerPage && start + slot < sorted.size(); slot++) {
      BuyShopItem item = sorted.get(start + slot);
      holder.slotIds.put(slot, item.id);
      inv.setItem(slot, buyIcon(item, false));
    }
    ItemStack filler = navItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int slot = 18; slot < 27; slot++) inv.setItem(slot, filler);
    inv.setItem(18, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(22, navItem(Material.COMPASS, "&bBack to Categories"));
    inv.setItem(24, navItem(Material.HOPPER, "&bSort: &f" + buySort(player).id));
    inv.setItem(26, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openEssenceShopMenu(Player player, String category, int page) {
    String normalizedCategory = canonicalCategory(category);
    List<BuyShopItem> sorted = sortedShop(player, essenceShopItems, normalizedCategory);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.ESSENCE_SHOP, page, normalizedCategory);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Essence " + normalizedCategory + " &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      BuyShopItem item = sorted.get(start + slot);
      holder.slotIds.put(slot, item.id);
      inv.setItem(slot, buyIcon(item, false));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.HOPPER, "&bSort: &f" + buySort(player).id));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openSellMenu(Player player, int page) {
    List<Map.Entry<Material, Double>> sorted = sortedSellValues();
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.SELL, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Sell Values &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      Map.Entry<Material, Double> entry = sorted.get(start + slot);
      holder.materialSlots.put(slot, entry.getKey());
      inv.setItem(slot, sellIcon(entry.getKey(), entry.getValue()));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.EMERALD, "&b/sell hand &7or &b/sell all"));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openSellChest(Player player) {
    SellChestHolder holder = new SellChestHolder();
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Sell Items"));
    holder.inventory = inv;
    ItemStack filler = navItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    for (int slot = 45; slot < 54; slot++) {
      inv.setItem(slot, filler);
    }
    updateSellChestButton(inv);
    player.openInventory(inv);
  }

  private void updateSellChestButton(Inventory inv) {
    double total = 0;
    for (int slot = 0; slot < 45; slot++) {
      ItemStack item = inv.getItem(slot);
      if (item != null && !item.getType().isAir()) {
        total += sellValue(item.getType()) * item.getAmount();
      }
    }
    ItemStack button;
    if (total > 0) {
      button = navItem(Material.EMERALD, "&aSell &7(" + format(total) + " " + moneyName + ")");
    } else {
      button = navItem(Material.GRAY_STAINED_GLASS_PANE, "&7Place sellable items above");
    }
    inv.setItem(49, button);
  }

  private void sellChestContents(Player player, Inventory inv) {
    double total = 0;
    for (int slot = 0; slot < 45; slot++) {
      ItemStack item = inv.getItem(slot);
      if (item == null || item.getType().isAir()) continue;
      double unitValue = sellValue(item.getType());
      if (unitValue <= 0) continue;
      total += unitValue * item.getAmount();
      inv.setItem(slot, null);
    }
    if (total <= 0) {
      player.sendMessage(color("&cNone of those items have a sell value."));
      return;
    }
    economy.deposit(player, total);
    player.sendMessage(color("&aSold for &f" + format(total) + " " + moneyName + "&a."));
    updateSellChestButton(inv);
  }

  private void openBuyConfigMenu(Player player, int page) {
    List<BuyShopItem> sorted = sortedShop(player, buyShopItems, null);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.BUY_CONFIG, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Buy Config &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      BuyShopItem item = sorted.get(start + slot);
      holder.slotIds.put(slot, item.id);
      inv.setItem(slot, buyIcon(item, true));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.BOOK, "&bUse /shop edit add <price> <category>"));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openEssenceShopConfigMenu(Player player, int page) {
    List<BuyShopItem> sorted = sortedShop(player, essenceShopItems, null);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.ESSENCE_CONFIG, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Essence Config &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      BuyShopItem item = sorted.get(start + slot);
      holder.slotIds.put(slot, item.id);
      inv.setItem(slot, buyIcon(item, true));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.BOOK, "&bUse /essenceshop config add <price> <category>"));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openAuctionMenu(Player player, int page) {
    List<AuctionListing> sorted = sortedAuctions(player);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.AUCTION, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Fallen AH &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      AuctionListing listing = sorted.get(start + slot);
      holder.slotIds.put(slot, listing.id);
      inv.setItem(slot, auctionIcon(listing));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.HOPPER, "&bSort: &f" + auctionSort(player).id));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openOrdersMenu(Player player, int page) {
    List<BuyOrder> sorted = sortedOrders(player);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.ORDERS, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Fallen Orders &7" + (page + 1) + "/" + (maxPage + 1)));
    holder.inventory = inv;
    int start = page * 45;
    for (int slot = 0; slot < 45 && start + slot < sorted.size(); slot++) {
      BuyOrder order = sorted.get(start + slot);
      holder.slotIds.put(slot, order.id);
      inv.setItem(slot, orderIcon(order));
    }
    inv.setItem(45, navItem(Material.ARROW, "&ePrevious Page"));
    inv.setItem(49, navItem(Material.HOPPER, "&bSort: &f" + orderSort(player).id));
    inv.setItem(53, navItem(Material.ARROW, "&eNext Page"));
    player.openInventory(inv);
  }

  private void openConfirm(Player player, int auctionId) {
    AuctionListing listing = auctions.get(auctionId);
    if (listing == null) {
      player.sendMessage(color("&cThis auction is no longer available."));
      return;
    }
    ConfirmHolder holder = new ConfirmHolder(auctionId);
    Inventory inv = Bukkit.createInventory(holder, 27, color("&8Confirm Purchase #" + auctionId));
    holder.inventory = inv;
    inv.setItem(11, auctionIcon(listing));
    inv.setItem(15, navItem(Material.LIME_CONCRETE, "&aConfirm Purchase"));
    inv.setItem(13, navItem(Material.RED_CONCRETE, "&cCancel"));
    confirmations.put(player.getUniqueId(), new ConfirmData(auctionId));
    player.openInventory(inv);
  }

  private void openShopConfirm(Player player, int itemId, MenuType sourceType, String category, int page, Map<Integer, BuyShopItem> sourceShop) {
    BuyShopItem shopItem = sourceShop.get(itemId);
    if (shopItem == null) {
      player.sendMessage(color("&cThis shop item is no longer available."));
      return;
    }
    ShopConfirmHolder holder = new ShopConfirmHolder(itemId, sourceType, category, page, Math.max(1, shopItem.item.getAmount()));
    Inventory inv = Bukkit.createInventory(holder, 27, color("&8Confirm Shop Purchase"));
    holder.inventory = inv;
    addAmountButton(inv, holder, 9, -64);
    addAmountButton(inv, holder, 10, -32);
    addAmountButton(inv, holder, 11, -16);
    addAmountButton(inv, holder, 12, -1);
    addAmountButton(inv, holder, 14, 1);
    addAmountButton(inv, holder, 15, 16);
    addAmountButton(inv, holder, 16, 32);
    addAmountButton(inv, holder, 17, 64);
    inv.setItem(22, navItem(Material.LIME_CONCRETE, "&aBuy"));
    inv.setItem(24, navItem(Material.RED_CONCRETE, "&cDo Not Buy"));
    redrawShopConfirm(holder);
    player.openInventory(inv);
  }

  private void addAmountButton(Inventory inv, ShopConfirmHolder holder, int slot, int delta) {
    holder.amountSlots.put(slot, delta);
    String prefix = delta > 0 ? "+" : "";
    Material material = delta > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
    ItemStack button = navItem(material, "&e" + prefix + delta);
    button.setAmount(Math.max(1, Math.min(button.getMaxStackSize(), Math.abs(delta))));
    inv.setItem(slot, button);
  }

  private void redrawShopConfirm(ShopConfirmHolder holder) {
    BuyShopItem shopItem = shopSource(holder.sourceType).get(holder.itemId);
    if (shopItem == null || holder.inventory == null) return;
    holder.inventory.setItem(13, confirmShopIcon(shopItem, holder.amount));
    double totalPrice = shopPriceForAmount(shopItem, holder.amount);
    holder.inventory.setItem(22, navItem(Material.LIME_CONCRETE, "&aBuy &f" + holder.amount + "x &7(" + format(totalPrice) + " " + currencyLabel(shopItem.currency) + ")"));
  }

  private ItemStack confirmShopIcon(BuyShopItem shopItem, int amount) {
    ItemStack icon = shopPreviewItem(shopItem.item);
    icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), amount)));
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
      double totalPrice = shopPriceForAmount(shopItem, amount);
      lore.add(color("&8&m----------------"));
      lore.add(color("&7Amount: &f" + amount));
      lore.add(color("&7Total: &b" + format(totalPrice) + " " + currencyLabel(shopItem.currency)));
      lore.add(color("&7Unit: &b" + format(unitShopPrice(shopItem)) + " " + currencyLabel(shopItem.currency)));
      lore.add(color("&eUse buttons below to change amount"));
      meta.setLore(lore);
      hideShopTooltipNoise(meta);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private void returnToShop(Player player, ShopConfirmHolder holder) {
    if (holder.sourceType == MenuType.ESSENCE_SHOP) openEssenceShopMenu(player, holder.category, holder.page);
    else openBuyMenu(player, holder.category, holder.page);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    InventoryHolder holder = event.getInventory().getHolder();
    if (holder instanceof SellChestHolder) {
      int slot = event.getRawSlot();
      int invSize = event.getInventory().getSize();
      if (slot >= 45 && slot < invSize) {
        event.setCancelled(true);
        if (slot == 49) sellChestContents(player, event.getInventory());
        return;
      }
      // Allow free item movement in slots 0-44 and player inventory, then refresh button
      Bukkit.getScheduler().runTask(this, () -> {
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof SellChestHolder) {
          updateSellChestButton(player.getOpenInventory().getTopInventory());
        }
      });
      return;
    }
    if (holder instanceof PagedHolder paged) {
      event.setCancelled(true);
      int slot = event.getRawSlot();
      if (slot < 0 || slot >= event.getInventory().getSize()) return;
      if (paged.type == MenuType.BUY_CATEGORIES) {
        String category = paged.categorySlots.get(slot);
        if (category == null) return;
        openBuyMenu(player, category, 0);
        return;
      }
      if (paged.type == MenuType.BUY) {
        if (slot == 18) {
          openBuyMenu(player, paged.category, paged.page - 1);
          return;
        }
        if (slot == 22) {
          openShopCategories(player);
          return;
        }
        if (slot == 24) {
          cycleSort(player, paged.type);
          openBuyMenu(player, paged.category, 0);
          return;
        }
        if (slot == 26) {
          openBuyMenu(player, paged.category, paged.page + 1);
          return;
        }
      }
      if (slot == 45) {
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, paged.page - 1);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, paged.page - 1);
        else if (paged.type == MenuType.SELL) openSellMenu(player, paged.page - 1);
        else if (paged.type == MenuType.ESSENCE_SHOP) openEssenceShopMenu(player, paged.category, paged.page - 1);
        else if (paged.type == MenuType.ESSENCE_CONFIG) openEssenceShopConfigMenu(player, paged.page - 1);
        else openBuyConfigMenu(player, paged.page - 1);
        return;
      }
      if (slot == 53) {
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, paged.page + 1);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, paged.page + 1);
        else if (paged.type == MenuType.SELL) openSellMenu(player, paged.page + 1);
        else if (paged.type == MenuType.ESSENCE_SHOP) openEssenceShopMenu(player, paged.category, paged.page + 1);
        else if (paged.type == MenuType.ESSENCE_CONFIG) openEssenceShopConfigMenu(player, paged.page + 1);
        else openBuyConfigMenu(player, paged.page + 1);
        return;
      }
      if (slot == 49) {
        if (paged.type == MenuType.BUY_CONFIG || paged.type == MenuType.ESSENCE_CONFIG || paged.type == MenuType.SELL) return;
        cycleSort(player, paged.type);
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, 0);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, 0);
        else if (paged.type == MenuType.ESSENCE_SHOP) openEssenceShopMenu(player, paged.category, 0);
        else openBuyMenu(player, paged.category, 0);
        return;
      }
      if (paged.type == MenuType.SELL) {
        Material material = paged.materialSlots.get(slot);
        if (material == null) return;
        sellAllMaterial(player, material);
        openSellMenu(player, paged.page);
        return;
      }
      Integer id = paged.slotIds.get(slot);
      if (id == null) return;
      if (paged.type == MenuType.BUY) {
        openShopConfirm(player, id, MenuType.BUY, paged.category, paged.page, buyShopItems);
      } else if (paged.type == MenuType.BUY_CONFIG) {
        removeBuyShopItem(player, id);
        openBuyConfigMenu(player, paged.page);
      } else if (paged.type == MenuType.ESSENCE_SHOP) {
        openShopConfirm(player, id, MenuType.ESSENCE_SHOP, paged.category, paged.page, essenceShopItems);
      } else if (paged.type == MenuType.ESSENCE_CONFIG) {
        removeEssenceShopItem(player, id);
        openEssenceShopConfigMenu(player, paged.page);
      } else if (paged.type == MenuType.AUCTION) {
        if (getConfig().getBoolean("auction.confirm-purchase", true)) openConfirm(player, id);
        else buyAuction(player, id);
      } else {
        int amount = event.isShiftClick() ? 64 : 1;
        fillOrder(player, id, amount);
        openOrdersMenu(player, paged.page);
      }
      return;
    }
    if (holder instanceof ConfirmHolder confirm) {
      event.setCancelled(true);
      int slot = event.getRawSlot();
      if (slot == 15) {
        buyAuction(player, confirm.auctionId);
        player.closeInventory();
      } else if (slot == 13) {
        player.closeInventory();
      }
    } else if (holder instanceof ShopConfirmHolder confirm) {
      event.setCancelled(true);
      int slot = event.getRawSlot();
      if (slot < 0 || slot >= event.getInventory().getSize()) return;
      Integer delta = confirm.amountSlots.get(slot);
      if (delta != null) {
        confirm.amount = Math.max(1, Math.min(64, confirm.amount + delta));
        redrawShopConfirm(confirm);
        return;
      }
      if (slot == 22) {
        buyShopItem(player, confirm.itemId, shopSource(confirm.sourceType), confirm.amount);
        returnToShop(player, confirm);
      } else if (slot == 24) {
        returnToShop(player, confirm);
      }
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    confirmations.remove(event.getPlayer().getUniqueId());
    if (!(event.getPlayer() instanceof Player player)) return;
    if (!(event.getInventory().getHolder() instanceof SellChestHolder)) return;
    for (int slot = 0; slot < 45; slot++) {
      ItemStack item = event.getInventory().getItem(slot);
      if (item != null && !item.getType().isAir()) {
        giveOrDrop(player, item.clone());
        event.getInventory().setItem(slot, null);
      }
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    if (!(event.getInventory().getHolder() instanceof SellChestHolder)) return;
    for (int slot : event.getRawSlots()) {
      if (slot >= 45 && slot < event.getInventory().getSize()) {
        event.setCancelled(true);
        return;
      }
    }
    Bukkit.getScheduler().runTask(this, () -> {
      if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof SellChestHolder) {
        updateSellChestButton(player.getOpenInventory().getTopInventory());
      }
    });
  }

  private ItemStack auctionIcon(AuctionListing listing) {
    ItemStack icon = listing.item.clone();
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
      lore.add(color("&8&m----------------"));
      lore.add(color("&7Auction ID: &f#" + listing.id));
      lore.add(color("&7Seller: &f" + listing.sellerName));
      lore.add(color("&7Price: &b" + format(listing.price) + " " + moneyName));
      lore.add(color("&eClick to buy"));
      meta.setLore(lore);
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack buyIcon(BuyShopItem shopItem, boolean configView) {
    ItemStack icon = shopPreviewItem(shopItem.item);
    if (!configView && shopItem.currency == ShopCurrency.MONEY) {
      icon.setAmount(Math.max(1, icon.getMaxStackSize()));
    }
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
      lore.add(color("&8&m----------------"));
      lore.add(color("&7Buy ID: &f#" + shopItem.id));
      lore.add(color("&7Category: &f" + shopItem.category));
      lore.add(color("&7Price: &b" + format(shopItem.price) + " " + currencyLabel(shopItem.currency)));
      if (configView) {
        lore.add(color("&cClick to remove"));
        lore.add(color("&7Set price: &f/shop edit price " + shopItem.id + " <price>"));
      } else {
        lore.add(color("&eClick to choose amount"));
      }
      meta.setLore(lore);
      hideShopTooltipNoise(meta);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack shopPreviewItem(ItemStack item) {
    if (item.getType() != Material.SPAWNER) return item.clone();
    ItemStack preview = new ItemStack(Material.SPAWNER, Math.max(1, Math.min(item.getAmount(), item.getMaxStackSize())));
    ItemMeta sourceMeta = item.getItemMeta();
    ItemMeta previewMeta = preview.getItemMeta();
    if (sourceMeta != null && previewMeta != null) {
      if (sourceMeta.hasDisplayName()) previewMeta.setDisplayName(sourceMeta.getDisplayName());
      if (sourceMeta.hasLore()) previewMeta.setLore(sourceMeta.getLore());
      preview.setItemMeta(previewMeta);
    }
    return preview;
  }

  private ItemStack sellIcon(Material material, double value) {
    ItemStack icon = new ItemStack(material);
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(color("&f" + niceMaterial(material)));
      meta.setLore(List.of(
        color("&7Value: &a" + format(value) + " " + moneyName),
        color("&eClick to sell all matching items"),
        color("&8Armor and offhand are not touched")
      ));
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack orderIcon(BuyOrder order) {
    ItemStack icon = new ItemStack(order.material);
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(color("&f" + niceMaterial(order.material)));
      meta.setLore(List.of(
        color("&7Order ID: &f#" + order.id),
        color("&7Buyer: &f" + order.creatorName),
        color("&7Remaining: &f" + order.remaining + "/" + order.originalAmount),
        color("&7Unit price: &b" + format(order.unitPrice) + " " + moneyName),
        color("&7Total left: &b" + format(order.unitPrice * order.remaining) + " " + moneyName),
        color("&eClick with matching item in hand to fill 1"),
        color("&eShift-click to fill up to a stack")
      ));
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack navItem(Material material, String name) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(color(name));
      item.setItemMeta(meta);
    }
    return item;
  }

  private void hideShopTooltipNoise(ItemMeta meta) {
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
  }

  private void cycleSort(Player player, MenuType type) {
    SortMode current = switch (type) {
      case AUCTION -> auctionSort(player);
      case ORDERS -> orderSort(player);
      case SELL, BUY_CATEGORIES, ESSENCE_CONFIG -> SortMode.NEWEST;
      case BUY, BUY_CONFIG, ESSENCE_SHOP -> buySort(player);
    };
    SortMode[] modes = SortMode.values();
    SortMode next = modes[(current.ordinal() + 1) % modes.length];
    if (type == MenuType.AUCTION) auctionSorts.put(player.getUniqueId(), next);
    else if (type == MenuType.ORDERS) orderSorts.put(player.getUniqueId(), next);
    else buySorts.put(player.getUniqueId(), next);
  }

  private List<BuyShopItem> sortedShop(Player player, Map<Integer, BuyShopItem> source, String category) {
    Comparator<BuyShopItem> comparator = switch (buySort(player)) {
      case OLDEST -> Comparator.comparingLong(i -> i.createdAt);
      case PRICE_ASC -> Comparator.comparingDouble(i -> i.price);
      case PRICE_DESC -> Comparator.<BuyShopItem>comparingDouble(i -> i.price).reversed();
      case AMOUNT -> Comparator.<BuyShopItem>comparingInt(i -> i.item.getAmount()).reversed();
      case NEWEST -> Comparator.<BuyShopItem>comparingLong(i -> i.createdAt).reversed();
    };
    return source.values().stream()
      .filter(item -> category == null || item.category.equalsIgnoreCase(category))
      .sorted(comparator)
      .collect(Collectors.toList());
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    startToolTimerSession(event.getPlayer());
    deliverPendingOrderItems(event.getPlayer());
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    stopToolTimerSession(event.getPlayer());
    saveToolTimers();
  }

  private List<Map.Entry<Material, Double>> sortedSellValues() {
    return sellValues.entrySet().stream()
      .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
      .collect(Collectors.toList());
  }

  private List<AuctionListing> sortedAuctions(Player player) {
    Comparator<AuctionListing> comparator = switch (auctionSort(player)) {
      case OLDEST -> Comparator.comparingLong(a -> a.createdAt);
      case PRICE_ASC -> Comparator.comparingDouble(a -> a.price);
      case PRICE_DESC -> Comparator.<AuctionListing>comparingDouble(a -> a.price).reversed();
      case AMOUNT -> Comparator.<AuctionListing>comparingInt(a -> a.item.getAmount()).reversed();
      case NEWEST -> Comparator.<AuctionListing>comparingLong(a -> a.createdAt).reversed();
    };
    return auctions.values().stream().sorted(comparator).collect(Collectors.toList());
  }

  private List<BuyOrder> sortedOrders(Player player) {
    Comparator<BuyOrder> comparator = switch (orderSort(player)) {
      case OLDEST -> Comparator.comparingLong(o -> o.createdAt);
      case PRICE_ASC -> Comparator.comparingDouble(o -> o.unitPrice);
      case PRICE_DESC -> Comparator.<BuyOrder>comparingDouble(o -> o.unitPrice).reversed();
      case AMOUNT -> Comparator.<BuyOrder>comparingInt(o -> o.remaining).reversed();
      case NEWEST -> Comparator.<BuyOrder>comparingLong(o -> o.createdAt).reversed();
    };
    return orders.values().stream().sorted(comparator).collect(Collectors.toList());
  }

  private SortMode auctionSort(Player player) {
    return auctionSorts.computeIfAbsent(player.getUniqueId(), ignored -> defaultSort("auction.default-sort"));
  }

  private SortMode orderSort(Player player) {
    return orderSorts.computeIfAbsent(player.getUniqueId(), ignored -> defaultSort("orders.default-sort"));
  }

  private SortMode buySort(Player player) {
    return buySorts.computeIfAbsent(player.getUniqueId(), ignored -> defaultSort("buy.default-sort"));
  }

  private double sellValue(Material material) {
    return sellValues.getOrDefault(material, 0.0);
  }

  private boolean hasCurrency(OfflinePlayer player, ShopCurrency currency, double amount) {
    return currency == ShopCurrency.ESSENCE ? essence.available() && essence.has(player, amount) : economy.has(player, amount);
  }

  private boolean withdrawCurrency(OfflinePlayer player, ShopCurrency currency, double amount) {
    return currency == ShopCurrency.ESSENCE ? essence.available() && essence.withdraw(player, amount) : economy.withdraw(player, amount);
  }

  private Map<Integer, BuyShopItem> shopSource(MenuType sourceType) {
    return sourceType == MenuType.ESSENCE_SHOP ? essenceShopItems : buyShopItems;
  }

  private String currencyLabel(ShopCurrency currency) {
    return currency == ShopCurrency.ESSENCE ? essenceName : moneyName;
  }

  private String canonicalCategory(String raw) {
    if (raw == null || raw.isBlank()) return "Misc";
    return titleCase(raw.replace('-', '_').replace(' ', '_'));
  }

  private String displayItemName(ItemStack item) {
    if (item.hasItemMeta()) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null && meta.hasDisplayName()) return ChatColor.stripColor(meta.getDisplayName());
    }
    return niceMaterial(item.getType());
  }

  private SortMode defaultSort(String path) {
    SortMode sort = SortMode.from(getConfig().getString(path, "NEWEST"));
    return sort == null ? SortMode.NEWEST : sort;
  }

  private void saveBundledDataFile(String fileName) {
    File file = new File(getDataFolder(), fileName);
    if (!file.exists()) saveResource(fileName, false);
  }

  private void loadBuyShop() {
    buyShopItems.clear();
    nextBuyShopId = loadShopFile(buyShopFile, buyShopItems, ShopCurrency.MONEY);
    if (buyShopItems.isEmpty()) {
      getLogger().warning("Loaded 0 buy shop items from " + buyShopFile.getAbsolutePath() + ". Restoring bundled buy-shop.yml.");
      restoreBundledDataFile("buy-shop.yml", buyShopFile);
      nextBuyShopId = loadShopFile(buyShopFile, buyShopItems, ShopCurrency.MONEY);
    }
    if (!buyShopItems.isEmpty() && !hasAnyMainBuyCategory()) {
      getLogger().warning("Buy shop loaded without End/Nether/Gear/Food categories from " + buyShopFile.getAbsolutePath() + ". Restoring bundled buy-shop.yml.");
      buyShopItems.clear();
      restoreBundledDataFile("buy-shop.yml", buyShopFile);
      nextBuyShopId = loadShopFile(buyShopFile, buyShopItems, ShopCurrency.MONEY);
    }
    logShopLoad("Buy shop", buyShopFile, buyShopItems);
  }

  private void loadEssenceShop() {
    essenceShopItems.clear();
    nextEssenceShopId = loadShopFile(essenceShopFile, essenceShopItems, ShopCurrency.ESSENCE);
    logShopLoad("Essence shop", essenceShopFile, essenceShopItems);
  }

  private void restoreBundledDataFile(String resourceName, File targetFile) {
    try {
      if (targetFile.exists()) {
        File backupFile = new File(targetFile.getParentFile(), resourceName + ".backup-" + System.currentTimeMillis());
        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        getLogger().warning("Backed up existing " + resourceName + " to " + backupFile.getName());
      }
      saveResource(resourceName, true);
    } catch (IllegalArgumentException exception) {
      getLogger().severe("Bundled resource missing: " + resourceName);
    } catch (Exception exception) {
      getLogger().severe("Failed to restore " + resourceName + ": " + exception.getMessage());
    }
  }

  private void logShopLoad(String label, File file, Map<Integer, BuyShopItem> items) {
    Map<String, Long> counts = items.values().stream().collect(Collectors.groupingBy(item -> item.category, LinkedHashMap::new, Collectors.counting()));
    String summary = counts.isEmpty()
      ? "no categories"
      : counts.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(", "));
    getLogger().info(label + " loaded " + items.size() + " items from " + file.getAbsolutePath() + " (" + summary + ").");
  }

  private boolean hasAnyMainBuyCategory() {
    return buyShopItems.values().stream().anyMatch(item ->
      item.category.equalsIgnoreCase("End") ||
      item.category.equalsIgnoreCase("Nether") ||
      item.category.equalsIgnoreCase("Gear") ||
      item.category.equalsIgnoreCase("Food")
    );
  }

  private int loadShopFile(File file, Map<Integer, BuyShopItem> target, ShopCurrency defaultCurrency) {
    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
    int nextId = Math.max(1, config.getInt("next-id", 1));
    ConfigurationSection section = config.getConfigurationSection("items");
    if (section == null) return nextId;
    for (String key : section.getKeys(false)) {
      ConfigurationSection row = section.getConfigurationSection(key);
      if (row == null) continue;
      ItemStack item = loadConfiguredItem(row);
      if (item == null || item.getType().isAir()) continue;
      Integer parsedId = parseInt(key);
      int id = parsedId == null ? row.getInt("id") : parsedId;
      ShopCurrency currency = ShopCurrency.from(row.getString("currency", defaultCurrency.name()));
      if (currency == null) currency = defaultCurrency;
      target.put(id, new BuyShopItem(
        id,
        item,
        row.getDouble("price"),
        row.getLong("created-at"),
        canonicalCategory(row.getString("category", defaultCurrency == ShopCurrency.ESSENCE ? "Spawners" : "Misc")),
        currency
      ));
      nextId = Math.max(nextId, id + 1);
    }
    return nextId;
  }

  private void loadSellValues() {
    sellValues.clear();
    FileConfiguration config = YamlConfiguration.loadConfiguration(sellValuesFile);
    ConfigurationSection section = config.getConfigurationSection("items");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      Material material = Material.matchMaterial(key);
      if (material == null || material.isAir()) continue;
      double value = section.getDouble(key, 0);
      if (value > 0) sellValues.put(material, value);
    }
  }

  private void saveBuyShop() {
    saveShopFile(buyShopFile, buyShopItems, nextBuyShopId);
  }

  private void saveEssenceShop() {
    saveShopFile(essenceShopFile, essenceShopItems, nextEssenceShopId);
  }

  private void saveShopFile(File file, Map<Integer, BuyShopItem> items, int nextId) {
    YamlConfiguration config = new YamlConfiguration();
    config.set("next-id", nextId);
    for (BuyShopItem item : items.values()) {
      String path = "items." + item.id + ".";
      config.set(path + "category", item.category);
      config.set(path + "currency", item.currency.name());
      config.set(path + "item", item.item);
      config.set(path + "price", item.price);
      config.set(path + "created-at", item.createdAt);
    }
    saveYaml(config, file);
  }

  private ItemStack loadConfiguredItem(ConfigurationSection row) {
    ConfigurationSection itemSection = row.getConfigurationSection("item");
    if (itemSection != null && !itemSection.contains("==")) {
      return loadSimpleConfiguredItem(
        itemSection.getString("material", ""),
        itemSection.getInt("amount", 1),
        itemSection.getString("name"),
        itemSection.getString("entity-type"),
        itemSection.getString("potion-effect")
      );
    }
    Object rawItem = row.get("item");
    if (rawItem instanceof Map<?, ?> itemMap) {
      return loadSimpleConfiguredItem(
        String.valueOf(itemMap.get("material")),
        parseMapInt(itemMap.get("amount"), 1),
        mapString(itemMap.get("name")),
        mapString(itemMap.get("entity-type")),
        mapString(itemMap.get("potion-effect"))
      );
    }
    return row.getItemStack("item");
  }

  private ItemStack loadSimpleConfiguredItem(String materialName, int amountValue, String name, String entityType, String potionEffect) {
    Material material = Material.matchMaterial(materialName == null ? "" : materialName);
    if (material == null || material.isAir()) return null;
    int amount = Math.max(1, amountValue);
    ItemStack item = new ItemStack(material, amount);
    if (material == Material.SPAWNER && entityType != null && !entityType.isBlank()) {
      applySpawnerType(item, entityType);
    }
    if (item.getItemMeta() instanceof PotionMeta potionMeta && potionEffect != null && !potionEffect.isBlank()) {
      PotionEffectType type = PotionEffectType.getByName(potionEffect);
      if (type != null) {
        potionMeta.addCustomEffect(new PotionEffect(type, 20 * 120, 0), true);
        potionMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(potionMeta);
      }
    }
    if (name != null && !name.isBlank()) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        meta.setDisplayName(color("&f" + name));
        item.setItemMeta(meta);
      }
    }
    return item;
  }

  private int parseMapInt(Object value, int fallback) {
    if (value instanceof Number number) return number.intValue();
    if (value instanceof String string) {
      Integer parsed = parseInt(string);
      if (parsed != null) return parsed;
    }
    return fallback;
  }

  private String mapString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private void applySpawnerType(ItemStack item, String entityTypeName) {
    EntityType entityType;
    try {
      entityType = EntityType.valueOf(entityTypeName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      getLogger().warning("Unknown spawner entity type in shop config: " + entityTypeName);
      return;
    }
    if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return;
    if (meta.getBlockState() instanceof CreatureSpawner spawner) {
      spawner.setSpawnedType(entityType);
      meta.setBlockState(spawner);
      item.setItemMeta(meta);
    }
  }

  private void loadAuctions() {
    auctions.clear();
    FileConfiguration config = YamlConfiguration.loadConfiguration(auctionsFile);
    nextAuctionId = Math.max(1, config.getInt("next-id", 1));
    ConfigurationSection section = config.getConfigurationSection("auctions");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      ConfigurationSection row = section.getConfigurationSection(key);
      if (row == null) continue;
      ItemStack item = row.getItemStack("item");
      if (item == null || item.getType().isAir()) continue;
      int id = parseInt(key) == null ? row.getInt("id") : parseInt(key);
      auctions.put(id, new AuctionListing(
        id,
        UUID.fromString(row.getString("seller")),
        row.getString("seller-name", "Unknown"),
        item,
        row.getDouble("price"),
        row.getLong("created-at")
      ));
      nextAuctionId = Math.max(nextAuctionId, id + 1);
    }
  }

  private void saveAuctions() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("next-id", nextAuctionId);
    for (AuctionListing listing : auctions.values()) {
      String path = "auctions." + listing.id + ".";
      config.set(path + "seller", listing.seller.toString());
      config.set(path + "seller-name", listing.sellerName);
      config.set(path + "item", listing.item);
      config.set(path + "price", listing.price);
      config.set(path + "created-at", listing.createdAt);
    }
    saveYaml(config, auctionsFile);
  }

  private void loadOrders() {
    orders.clear();
    FileConfiguration config = YamlConfiguration.loadConfiguration(ordersFile);
    nextOrderId = Math.max(1, config.getInt("next-id", 1));
    ConfigurationSection section = config.getConfigurationSection("orders");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      ConfigurationSection row = section.getConfigurationSection(key);
      if (row == null) continue;
      Material material = Material.matchMaterial(row.getString("material", ""));
      if (material == null || material.isAir()) continue;
      int id = parseInt(key) == null ? row.getInt("id") : parseInt(key);
      orders.put(id, new BuyOrder(
        id,
        UUID.fromString(row.getString("creator")),
        row.getString("creator-name", "Unknown"),
        material,
        row.getDouble("unit-price"),
        row.getInt("original-amount"),
        row.getInt("remaining"),
        row.getLong("created-at")
      ));
      nextOrderId = Math.max(nextOrderId, id + 1);
    }
  }

  private void saveOrders() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("next-id", nextOrderId);
    for (BuyOrder order : orders.values()) {
      String path = "orders." + order.id + ".";
      config.set(path + "creator", order.creator.toString());
      config.set(path + "creator-name", order.creatorName);
      config.set(path + "material", order.material.name());
      config.set(path + "unit-price", order.unitPrice);
      config.set(path + "original-amount", order.originalAmount);
      config.set(path + "remaining", order.remaining);
      config.set(path + "created-at", order.createdAt);
    }
    saveYaml(config, ordersFile);
  }

  private void loadOrderDeliveries() {
    orderDeliveries.clear();
    FileConfiguration config = YamlConfiguration.loadConfiguration(orderDeliveriesFile);
    ConfigurationSection section = config.getConfigurationSection("players");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      UUID uuid;
      try {
        uuid = UUID.fromString(key);
      } catch (IllegalArgumentException ignored) {
        continue;
      }
      List<?> rawItems = section.getList(key);
      if (rawItems == null) continue;
      List<ItemStack> items = new ArrayList<>();
      for (Object rawItem : rawItems) {
        if (rawItem instanceof ItemStack item && !item.getType().isAir() && item.getAmount() > 0) {
          items.add(item.clone());
        }
      }
      if (!items.isEmpty()) orderDeliveries.put(uuid, items);
    }
  }

  private void saveOrderDeliveries() {
    YamlConfiguration config = new YamlConfiguration();
    for (Map.Entry<UUID, List<ItemStack>> entry : orderDeliveries.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        config.set("players." + entry.getKey(), entry.getValue());
      }
    }
    saveYaml(config, orderDeliveriesFile);
  }

  private void saveYaml(YamlConfiguration config, File file) {
    try {
      if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
      config.save(file);
    } catch (Exception exception) {
      getLogger().severe("Could not save " + file.getName() + ": " + exception.getMessage());
    }
  }

  private void giveOrDrop(Player player, ItemStack item) {
    Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
    leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
  }

  private void deliverOrderItems(UUID buyerId, ItemStack item) {
    Player buyer = Bukkit.getPlayer(buyerId);
    if (buyer != null && buyer.isOnline()) {
      giveOrDrop(buyer, item);
      return;
    }
    orderDeliveries.computeIfAbsent(buyerId, ignored -> new ArrayList<>()).add(item.clone());
  }

  private void deliverPendingOrderItems(Player player) {
    List<ItemStack> items = orderDeliveries.remove(player.getUniqueId());
    if (items == null || items.isEmpty()) return;
    for (ItemStack item : items) {
      giveOrDrop(player, item.clone());
    }
    saveOrderDeliveries();
    player.sendMessage(color("&aDelivered &f" + items.size() + " &apending order stack(s)."));
  }

  private void sendAuctionHelp(Player player) {
    player.sendMessage(color("&6Fallen Auction"));
    player.sendMessage(color("&e/ah &7- open auction house"));
    player.sendMessage(color("&e/ah sell <price> &7- sell held item stack"));
    player.sendMessage(color("&e/ah sort <newest|oldest|price_asc|price_desc|amount>"));
    player.sendMessage(color("&e/ah cancel <id> &7- cancel your listing"));
  }

  private void sendBuyHelp(Player player) {
    player.sendMessage(color("&6Fallen Shop"));
    player.sendMessage(color("&e/shop &7- open shop categories"));
    player.sendMessage(color("&e/shop <end|nether|gear|food> &7- open $ category"));
    player.sendMessage(color("&e/essenceshop &7- open Essence spawners"));
    player.sendMessage(color("&e/shop sort <newest|oldest|price_asc|price_desc|amount>"));
    if (player.hasPermission("falleneconomy.buy.config")) {
      player.sendMessage(color("&e/shop edit &7- open shop editor"));
      player.sendMessage(color("&e/shop edit add <price> <category> &7- add held item stack for $"));
    }
  }

  private void sendSellHelp(Player player) {
    player.sendMessage(color("&6Fallen Sell"));
    player.sendMessage(color("&e/sell &7- open sell GUI"));
    player.sendMessage(color("&e/sell hand &7- sell held item stack"));
    player.sendMessage(color("&e/sell all &7- sell inventory storage"));
    player.sendMessage(color("&e/sell values &7- view item sell values"));
  }

  private void sendBuyConfigHelp(Player player) {
    player.sendMessage(color("&6Fallen Shop Config"));
    player.sendMessage(color("&e/shop edit &7- open editor GUI"));
    player.sendMessage(color("&e/shop edit add <price> <category> &7- add held item stack for $"));
    player.sendMessage(color("&e/shop edit remove <id> &7- remove shop item"));
    player.sendMessage(color("&e/shop edit price <id> <price> &7- set price"));
    player.sendMessage(color("&e/shop edit list &7- list shop items"));
  }

  private void sendEssenceShopHelp(Player player) {
    player.sendMessage(color("&6Fallen Essence Shop"));
    player.sendMessage(color("&e/essence &7- show PlayerPoints Essence"));
    player.sendMessage(color("&e/essenceshop &7- open Essence shop"));
    player.sendMessage(color("&e/essenceshop <category> &7- open category"));
    if (player.hasPermission("falleneconomy.essenceshop.config")) {
      player.sendMessage(color("&e/essenceshop config &7- open config GUI"));
      player.sendMessage(color("&e/essenceshop config add <price> <category> &7- add held item stack"));
      player.sendMessage(color("&e/essenceshop config remove <id>"));
      player.sendMessage(color("&e/essenceshop config price <id> <price>"));
    }
  }

  private void sendOrderHelp(Player player) {
    player.sendMessage(color("&6Fallen Orders"));
    player.sendMessage(color("&e/order &7- open buy orders"));
    player.sendMessage(color("&e/order create <unitPrice> <amount> &7- order held item type"));
    player.sendMessage(color("&e/order fill <id> [amount] &7- sell held items into an order"));
    player.sendMessage(color("&e/order cancel <id> &7- cancel your order"));
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    String name = command.getName().toLowerCase(Locale.ROOT);
    if (name.equals("shop")) {
      if (args.length == 1) return filter(List.of("End", "Nether", "Gear", "Food", "edit", "config", "sort", "help"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("sort")) return filter(SortMode.ids(), args[1]);
      if (args.length == 2 && (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("edit"))) return filter(List.of("add", "remove", "delete", "price", "list", "help"), args[1]);
      if (args.length == 4 && (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("edit")) && args[1].equalsIgnoreCase("add")) return filter(List.of("End", "Nether", "Gear", "Food"), args[3]);
    }
    if (name.equals("essenceshop")) {
      if (args.length == 1) return filter(List.of("Spawners", "config", "help"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("config")) return filter(List.of("add", "remove", "delete", "price", "list", "help"), args[1]);
      if (args.length == 4 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("add")) return filter(List.of("Spawners", "Keys"), args[3]);
    }
    if (name.equals("sell") && args.length == 1) return filter(List.of("hand", "all", "values", "help"), args[0]);
    if (name.equals("ah")) {
      if (args.length == 1) return filter(List.of("sell", "sort", "cancel", "help"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("sort")) return filter(SortMode.ids(), args[1]);
    }
    if (name.equals("order")) {
      if (args.length == 1) return filter(List.of("create", "fill", "sort", "cancel", "help"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("sort")) return filter(SortMode.ids(), args[1]);
    }
    if (name.equals("pay") && args.length == 1) {
      return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(playerName -> playerName.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
        .toList();
    }
    if (name.equals("feconomy")) {
      if (args.length == 1) return filter(List.of("balance", "give", "take", "set", "essence", "tools"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("essence")) return filter(List.of("balance", "give", "take", "set"), args[1]);
      if (args.length == 2 && args[0].equalsIgnoreCase("tools")) return filter(List.of("give"), args[1]);
      if (args.length == 3 && args[0].equalsIgnoreCase("tools") && args[1].equalsIgnoreCase("give")) {
        List<String> values = new ArrayList<>();
        values.add("timed");
        values.addAll(Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(playerName -> playerName.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
          .toList());
        return filter(values, args[2]);
      }
      if (args.length == 4 && args[0].equalsIgnoreCase("tools") && args[1].equalsIgnoreCase("give")) {
        if (args[2].equalsIgnoreCase("timed") || args[2].equalsIgnoreCase("temp")) {
          return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(playerName -> playerName.toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT)))
            .toList();
        }
        return filter(List.of("pickaxe", "shovel", "axe", "sellwand"), args[3]);
      }
      if (args.length == 5 && args[0].equalsIgnoreCase("tools") && args[1].equalsIgnoreCase("give") && (args[2].equalsIgnoreCase("timed") || args[2].equalsIgnoreCase("temp"))) {
        return filter(List.of("pickaxe", "shovel", "axe"), args[4]);
      }
      if (args.length == 6 && args[0].equalsIgnoreCase("tools") && args[1].equalsIgnoreCase("give") && (args[2].equalsIgnoreCase("timed") || args[2].equalsIgnoreCase("temp"))) {
        return filter(List.of("1", "6", "12", "24", "48", "72", "168"), args[5]);
      }
      if (args.length == 5 && args[0].equalsIgnoreCase("tools") && args[1].equalsIgnoreCase("give") && args[3].equalsIgnoreCase("sellwand")) {
        return filter(List.of("5", "10", "25", "50", "100"), args[4]);
      }
    }
    return List.of();
  }

  private List<String> filter(List<String> values, String prefix) {
    String lower = prefix.toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
  }

  private static String color(String message) {
    Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String hex = matcher.group(1);
      StringBuilder replacement = new StringBuilder("§x");
      for (char character : hex.toCharArray()) {
        replacement.append('§').append(character);
      }
      matcher.appendReplacement(buffer, replacement.toString());
    }
    matcher.appendTail(buffer);
    return ChatColor.translateAlternateColorCodes('&', buffer.toString());
  }

  private String format(double value) {
    if (Math.abs(value - Math.rint(value)) < 0.0001) return String.valueOf((long) Math.rint(value));
    return String.format(Locale.US, "%.2f", value);
  }

  private String niceMaterial(Material material) {
    return titleCase(material.name());
  }

  private static String titleCase(String id) {
    String[] parts = id.toLowerCase(Locale.ROOT).split("_");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      if (part.isEmpty()) continue;
      result.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
    }
    return String.join(" ", result);
  }

  private static Double parseMoney(String value) {
    try {
      double parsed = Double.parseDouble(value);
      return Double.isFinite(parsed) && parsed > 0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static Double parseMoneyAllowZero(String value) {
    try {
      double parsed = Double.parseDouble(value);
      return Double.isFinite(parsed) && parsed >= 0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static Integer parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static final class LastBlockFace {
    private final Location location;
    private final BlockFace face;
    private final long createdAt;

    private LastBlockFace(Location location, BlockFace face, long createdAt) {
      this.location = location;
      this.face = face;
      this.createdAt = createdAt;
    }
  }

  private static final class BuyShopItem {
    private final int id;
    private final ItemStack item;
    private double price;
    private final long createdAt;
    private final String category;
    private final ShopCurrency currency;

    private BuyShopItem(int id, ItemStack item, double price, long createdAt, String category, ShopCurrency currency) {
      this.id = id;
      this.item = item;
      this.price = price;
      this.createdAt = createdAt;
      this.category = category;
      this.currency = currency;
    }
  }

  private record AuctionListing(int id, UUID seller, String sellerName, ItemStack item, double price, long createdAt) {}

  private static final class BuyOrder {
    private final int id;
    private final UUID creator;
    private final String creatorName;
    private final Material material;
    private final double unitPrice;
    private final int originalAmount;
    private int remaining;
    private final long createdAt;

    private BuyOrder(int id, UUID creator, String creatorName, Material material, double unitPrice, int originalAmount, int remaining, long createdAt) {
      this.id = id;
      this.creator = creator;
      this.creatorName = creatorName;
      this.material = material;
      this.unitPrice = unitPrice;
      this.originalAmount = originalAmount;
      this.remaining = Math.max(0, remaining);
      this.createdAt = createdAt;
    }
  }

  private record ConfirmData(int auctionId) {}

  private enum MenuType {
    BUY_CATEGORIES,
    BUY,
    BUY_CONFIG,
    ESSENCE_SHOP,
    ESSENCE_CONFIG,
    AUCTION,
    ORDERS,
    SELL
  }

  private enum ShopCurrency {
    MONEY,
    ESSENCE;

    private static ShopCurrency from(String raw) {
      if (raw == null) return null;
      String normalized = raw.trim().toUpperCase(Locale.ROOT);
      if (normalized.equals("$")) return MONEY;
      for (ShopCurrency currency : values()) {
        if (currency.name().equals(normalized)) return currency;
      }
      return null;
    }
  }

  private enum SortMode {
    NEWEST("newest"),
    OLDEST("oldest"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    AMOUNT("amount");

    private final String id;

    SortMode(String id) {
      this.id = id;
    }

    private static SortMode from(String raw) {
      if (raw == null) return NEWEST;
      String normalized = raw.toLowerCase(Locale.ROOT);
      for (SortMode mode : values()) {
        if (mode.id.equals(normalized) || mode.name().equalsIgnoreCase(raw)) return mode;
      }
      return null;
    }

    private static List<String> ids() {
      List<String> ids = new ArrayList<>();
      for (SortMode mode : values()) ids.add(mode.id);
      return ids;
    }
  }

  private static final class PagedHolder implements InventoryHolder {
    private final MenuType type;
    private final int page;
    private final String category;
    private final Map<Integer, Integer> slotIds = new HashMap<>();
    private final Map<Integer, Material> materialSlots = new HashMap<>();
    private final Map<Integer, String> categorySlots = new HashMap<>();
    private Inventory inventory;

    private PagedHolder(MenuType type, int page) {
      this(type, page, null);
    }

    private PagedHolder(MenuType type, int page, String category) {
      this.type = type;
      this.page = page;
      this.category = category;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  private static final class ConfirmHolder implements InventoryHolder {
    private final int auctionId;
    private Inventory inventory;

    private ConfirmHolder(int auctionId) {
      this.auctionId = auctionId;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  private static final class ShopConfirmHolder implements InventoryHolder {
    private final int itemId;
    private final MenuType sourceType;
    private final String category;
    private final int page;
    private final Map<Integer, Integer> amountSlots = new HashMap<>();
    private int amount;
    private Inventory inventory;

    private ShopConfirmHolder(int itemId, MenuType sourceType, String category, int page, int amount) {
      this.itemId = itemId;
      this.sourceType = sourceType;
      this.category = category;
      this.page = page;
      this.amount = Math.max(1, Math.min(64, amount));
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  private static final class SellChestHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  private static final class PlayerPointsEssenceBridge {
    private final FallenEconomyPlugin plugin;
    private final Object api;
    private final Method lookMethod;
    private final Method giveMethod;
    private final Method takeMethod;
    private final Method setMethod;

    private PlayerPointsEssenceBridge(FallenEconomyPlugin plugin, Object api, Method lookMethod, Method giveMethod, Method takeMethod, Method setMethod) {
      this.plugin = plugin;
      this.api = api;
      this.lookMethod = lookMethod;
      this.giveMethod = giveMethod;
      this.takeMethod = takeMethod;
      this.setMethod = setMethod;
    }

    private static PlayerPointsEssenceBridge create(FallenEconomyPlugin plugin) {
      if (!plugin.getConfig().getBoolean("essence.enabled", true)) {
        plugin.getLogger().info("Essence is disabled in config.yml.");
        return unavailable(plugin);
      }
      org.bukkit.plugin.Plugin playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
      if (playerPoints == null || !playerPoints.isEnabled()) {
        plugin.getLogger().info("PlayerPoints not found. Essence shop and Essence commands will be unavailable.");
        return unavailable(plugin);
      }
      try {
        Object api = playerPoints.getClass().getMethod("getAPI").invoke(playerPoints);
        Class<?> apiClass = api.getClass();
        Method look = apiClass.getMethod("look", UUID.class);
        Method give = apiClass.getMethod("give", UUID.class, int.class);
        Method take = apiClass.getMethod("take", UUID.class, int.class);
        Method set = apiClass.getMethod("set", UUID.class, int.class);
        plugin.getLogger().info("Hooked PlayerPoints for Essence.");
        return new PlayerPointsEssenceBridge(plugin, api, look, give, take, set);
      } catch (Exception exception) {
        plugin.getLogger().warning("Could not hook PlayerPoints: " + exception.getMessage());
        return unavailable(plugin);
      }
    }

    private static PlayerPointsEssenceBridge unavailable(FallenEconomyPlugin plugin) {
      return new PlayerPointsEssenceBridge(plugin, null, null, null, null, null);
    }

    private boolean available() {
      return api != null;
    }

    private double balance(OfflinePlayer player) {
      if (!available()) return 0;
      try {
        Object result = lookMethod.invoke(api, player.getUniqueId());
        return result instanceof Number number ? number.doubleValue() : 0;
      } catch (Exception exception) {
        plugin.getLogger().warning("Could not read PlayerPoints balance: " + exception.getMessage());
        return 0;
      }
    }

    private boolean has(OfflinePlayer player, double amount) {
      return balance(player) >= amount;
    }

    private boolean withdraw(OfflinePlayer player, double amount) {
      if (!available() || !has(player, amount)) return false;
      return invokeMutation(takeMethod, player, amount);
    }

    private void deposit(OfflinePlayer player, double amount) {
      if (available()) invokeMutation(giveMethod, player, amount);
    }

    private void set(OfflinePlayer player, double amount) {
      if (available()) invokeMutation(setMethod, player, amount);
    }

    private boolean invokeMutation(Method method, OfflinePlayer player, double amount) {
      try {
        Object result = method.invoke(api, player.getUniqueId(), (int) Math.round(Math.max(0, amount)));
        return !(result instanceof Boolean bool) || bool;
      } catch (Exception exception) {
        plugin.getLogger().warning("Could not update PlayerPoints: " + exception.getMessage());
        return false;
      }
    }
  }

  private interface EconomyBridge {
    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    void deposit(OfflinePlayer player, double amount);

    double balance(OfflinePlayer player);

    static EconomyBridge create(FallenEconomyPlugin plugin) {
      plugin.getLogger().info("Using Fallen internal money balances.");
      return new InternalEconomyBridge(plugin);
    }
  }

  private static final class InternalEconomyBridge implements EconomyBridge {
    private final FallenEconomyPlugin plugin;
    private final YamlConfiguration balancesConfig;

    private InternalEconomyBridge(FallenEconomyPlugin plugin) {
      this.plugin = plugin;
      this.balancesConfig = YamlConfiguration.loadConfiguration(plugin.balancesFile);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
      return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
      if (!has(player, amount)) return false;
      setBalance(player, balance(player) - amount);
      return true;
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
      setBalance(player, balance(player) + amount);
    }

    @Override
    public double balance(OfflinePlayer player) {
      String key = player.getUniqueId().toString();
      if (!balancesConfig.contains(key)) {
        return plugin.getConfig().getDouble("money.starting-balance", plugin.getConfig().getDouble("internal-economy.starting-balance", 0));
      }
      return balancesConfig.getDouble(key);
    }

    private void setBalance(OfflinePlayer player, double amount) {
      balancesConfig.set(player.getUniqueId().toString(), Math.max(0, amount));
      save();
    }

    private void save() {
      plugin.saveYaml(balancesConfig, plugin.balancesFile);
    }
  }

  private static final class VaultCompatibilityHook {
    private final JavaPlugin plugin;
    private final Class<?> economyClass;
    private final Object provider;

    private VaultCompatibilityHook(JavaPlugin plugin, Class<?> economyClass, Object provider) {
      this.plugin = plugin;
      this.economyClass = economyClass;
      this.provider = provider;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static VaultCompatibilityHook tryRegister(JavaPlugin plugin, EconomyBridge bridge, String moneyName) {
      try {
        Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
        Object provider = Proxy.newProxyInstance(
          economyClass.getClassLoader(),
          new Class<?>[] { economyClass },
          (proxy, method, args) -> handleVaultCall(plugin, bridge, moneyName, method, args)
        );
        Bukkit.getServicesManager().register((Class) economyClass, provider, plugin, ServicePriority.Normal);
        plugin.getLogger().info("Registered Fallen Economy as a Vault economy provider.");
        return new VaultCompatibilityHook(plugin, economyClass, provider);
      } catch (ClassNotFoundException exception) {
        plugin.getLogger().info("Vault not found. Running standalone without Vault compatibility.");
        return null;
      } catch (Exception exception) {
        plugin.getLogger().warning("Could not register Vault compatibility: " + exception.getMessage());
        return null;
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void unregister() {
      Bukkit.getServicesManager().unregister((Class) economyClass, provider);
    }

    private static Object handleVaultCall(JavaPlugin plugin, EconomyBridge bridge, String moneyName, Method method, Object[] args) throws Exception {
      String name = method.getName();
      Class<?> returnType = method.getReturnType();
      if (method.getDeclaringClass() == Object.class) {
        return switch (name) {
          case "toString" -> "FallenEconomyVaultProvider";
          case "hashCode" -> System.identityHashCode(bridge);
          case "equals" -> args != null && args.length == 1 && args[0] == bridge;
          default -> null;
        };
      }
      if (name.equals("isEnabled")) return true;
      if (name.equals("getName")) return "FallenEconomy";
      if (name.equals("hasBankSupport")) return false;
      if (name.equals("fractionalDigits")) return 2;
      if (name.equals("moneyNameSingular") || name.equals("moneyNamePlural")) return moneyName;
      if (name.equals("format")) return formatVaultAmount(args, moneyName);
      if (name.equals("getBanks")) return List.of();
      if (name.equals("hasAccount") || name.equals("createPlayerAccount")) return true;

      OfflinePlayer player = vaultPlayer(args);
      double amount = vaultAmount(args);
      if (name.equals("getBalance")) return player == null ? 0.0 : bridge.balance(player);
      if (name.equals("has")) return player != null && bridge.has(player, amount);
      if (name.equals("withdrawPlayer")) {
        boolean success = player != null && bridge.withdraw(player, amount);
        double balance = player == null ? 0.0 : bridge.balance(player);
        return vaultResponse(success, amount, balance, success ? "" : "Insufficient funds or invalid player");
      }
      if (name.equals("depositPlayer")) {
        if (player != null) bridge.deposit(player, amount);
        double balance = player == null ? 0.0 : bridge.balance(player);
        return vaultResponse(player != null, amount, balance, player == null ? "Invalid player" : "");
      }
      if (returnType.getName().equals("net.milkbowl.vault.economy.EconomyResponse")) {
        return vaultResponse(false, 0, 0, "Banks are not supported by Fallen Economy.");
      }
      if (returnType == boolean.class || returnType == Boolean.class) return false;
      if (returnType == int.class || returnType == Integer.class) return 0;
      if (returnType == double.class || returnType == Double.class) return 0.0;
      if (returnType == String.class) return "";
      plugin.getLogger().fine("Unhandled Vault economy method: " + name);
      return null;
    }

    private static OfflinePlayer vaultPlayer(Object[] args) {
      if (args == null) return null;
      for (Object arg : args) {
        if (arg instanceof OfflinePlayer player) return player;
        if (arg instanceof String name && !name.isBlank()) return Bukkit.getOfflinePlayer(name);
      }
      return null;
    }

    private static double vaultAmount(Object[] args) {
      if (args == null) return 0;
      for (Object arg : args) {
        if (arg instanceof Number number) return number.doubleValue();
      }
      return 0;
    }

    private static String formatVaultAmount(Object[] args, String moneyName) {
      double amount = vaultAmount(args);
      if (Math.abs(amount - Math.rint(amount)) < 0.0001) return (long) Math.rint(amount) + " " + moneyName;
      return String.format(Locale.US, "%.2f %s", amount, moneyName);
    }

    private static Object vaultResponse(boolean success, double amount, double balance, String error) throws Exception {
      Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
      Class<?> responseTypeClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse$ResponseType");
      Object type = Enum.valueOf((Class<Enum>) responseTypeClass.asSubclass(Enum.class), success ? "SUCCESS" : "FAILURE");
      return responseClass
        .getConstructor(double.class, double.class, responseTypeClass, String.class)
        .newInstance(amount, balance, type, error);
    }
  }
}
