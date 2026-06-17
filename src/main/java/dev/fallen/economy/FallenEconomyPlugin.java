package dev.fallen.economy;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class FallenEconomyPlugin extends JavaPlugin implements Listener, TabExecutor {
  private final Map<Integer, AuctionListing> auctions = new LinkedHashMap<>();
  private final Map<Integer, BuyOrder> orders = new LinkedHashMap<>();
  private final Map<Integer, BuyShopItem> buyShopItems = new LinkedHashMap<>();
  private final Map<Material, Double> sellValues = new LinkedHashMap<>();
  private final Map<UUID, SortMode> auctionSorts = new HashMap<>();
  private final Map<UUID, SortMode> orderSorts = new HashMap<>();
  private final Map<UUID, SortMode> buySorts = new HashMap<>();
  private final Map<UUID, ConfirmData> confirmations = new HashMap<>();

  private File buyShopFile;
  private File sellValuesFile;
  private File auctionsFile;
  private File ordersFile;
  private File balancesFile;
  private EconomyBridge economy;
  private VaultCompatibilityHook vaultHook;
  private int nextBuyShopId = 1;
  private int nextAuctionId = 1;
  private int nextOrderId = 1;
  private String currencyName;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    currencyName = getConfig().getString("currency-name", "Essence");

    buyShopFile = new File(getDataFolder(), "buy-shop.yml");
    sellValuesFile = new File(getDataFolder(), "sell-values.yml");
    auctionsFile = new File(getDataFolder(), "auctions.yml");
    ordersFile = new File(getDataFolder(), "orders.yml");
    balancesFile = new File(getDataFolder(), "balances.yml");

    saveBundledDataFile("buy-shop.yml");
    saveBundledDataFile("sell-values.yml");
    economy = EconomyBridge.create(this);
    if (economy instanceof InternalEconomyBridge internal) {
      vaultHook = VaultCompatibilityHook.tryRegister(this, internal, currencyName);
    }
    loadBuyShop();
    loadSellValues();
    loadAuctions();
    loadOrders();

    Objects.requireNonNull(getCommand("shop")).setExecutor(this);
    Objects.requireNonNull(getCommand("shop")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("buy")).setExecutor(this);
    Objects.requireNonNull(getCommand("buy")).setTabCompleter(this);
    Objects.requireNonNull(getCommand("sell")).setExecutor(this);
    Objects.requireNonNull(getCommand("sell")).setTabCompleter(this);
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
  }

  @Override
  public void onDisable() {
    saveBuyShop();
    saveAuctions();
    saveOrders();
    if (vaultHook != null) vaultHook.unregister();
    if (economy instanceof InternalEconomyBridge internal) internal.save();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String name = command.getName().toLowerCase(Locale.ROOT);
    if (name.equals("shop")) return handleBuyCommand(sender, args);
    if (name.equals("buy")) return handleBuyCommand(sender, args);
    if (name.equals("sell")) return handleSellCommand(sender, args);
    if (name.equals("ah")) return handleAuctionCommand(sender, args);
    if (name.equals("order")) return handleOrderCommand(sender, args);
    if (name.equals("balance")) return handleBalanceCommand(sender);
    if (name.equals("pay")) return handlePayCommand(sender, args);
    if (name.equals("feconomy")) return handleAdminEconomyCommand(sender, args);
    return false;
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
      openBuyMenu(player, 0);
      return true;
    }

    if (args[0].equalsIgnoreCase("config")) {
      return handleBuyConfigCommand(player, args);
    }
    if (args[0].equalsIgnoreCase("sort")) {
      if (args.length < 2) {
        player.sendMessage(color("&eUsage: /buy sort <newest|oldest|price_asc|price_desc|amount>"));
        return true;
      }
      SortMode sort = SortMode.from(args[1]);
      if (sort == null) {
        player.sendMessage(color("&cUnknown sort mode."));
        return true;
      }
      buySorts.put(player.getUniqueId(), sort);
      player.sendMessage(color("&aBuy shop sort set to &f" + sort.id + "&a."));
      openBuyMenu(player, 0);
      return true;
    }
    if (args[0].equalsIgnoreCase("help")) {
      sendBuyHelp(player);
      return true;
    }

    Integer page = parseInt(args[0]);
    if (page != null) openBuyMenu(player, Math.max(0, page - 1));
    else sendBuyHelp(player);
    return true;
  }

  private boolean handleBuyConfigCommand(Player player, String[] args) {
    if (!player.hasPermission("falleneconomy.buy.config")) {
      player.sendMessage(color("&cYou do not have permission to configure the buy shop."));
      return true;
    }
    if (args.length == 1) {
      openBuyConfigMenu(player, 0);
      return true;
    }
    switch (args[1].toLowerCase(Locale.ROOT)) {
      case "add" -> {
        if (args.length < 3) {
          player.sendMessage(color("&eUsage: /buy config add <price>"));
          return true;
        }
        Double price = parseMoney(args[2]);
        if (price == null) {
          player.sendMessage(color("&cInvalid price."));
          return true;
        }
        addBuyShopItem(player, price);
        return true;
      }
      case "remove", "delete" -> {
        if (args.length < 3) {
          player.sendMessage(color("&eUsage: /buy config remove <id>"));
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
          player.sendMessage(color("&eUsage: /buy config price <id> <price>"));
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
      openSellMenu(player, 0);
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
      case "help" -> {
        sendSellHelp(player);
        return true;
      }
      default -> {
        Integer page = parseInt(args[0]);
        if (page != null) openSellMenu(player, Math.max(0, page - 1));
        else sendSellHelp(player);
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
    player.sendMessage(color("&aBalance: &f" + format(economy.balance(player)) + " " + currencyName));
    return true;
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
      player.sendMessage(color("&cYou need &f" + format(amount) + " " + currencyName + "&c."));
      return true;
    }
    if (!economy.withdraw(player, amount)) {
      player.sendMessage(color("&cPayment failed."));
      return true;
    }
    economy.deposit(target, amount);
    player.sendMessage(color("&aPaid &f" + target.getName() + " " + format(amount) + " " + currencyName + "&a."));
    target.sendMessage(color("&aReceived &f" + format(amount) + " " + currencyName + " &afrom &f" + player.getName() + "&a."));
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
      return true;
    }
    if (args[0].equalsIgnoreCase("balance") && args.length >= 2) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      sender.sendMessage(color("&a" + target.getName() + ": &f" + format(economy.balance(target)) + " " + currencyName));
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
      sender.sendMessage(color("&aGave &f" + format(amount) + " " + currencyName + " &ato &f" + target.getName() + "&a."));
      return true;
    }
    sender.sendMessage(color("&cUnknown admin command."));
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
    seller.sendMessage(color("&aListed &f" + listed.getAmount() + "x " + niceMaterial(listed.getType()) + " &afor &f" + format(price) + " " + currencyName + "&a."));
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
      buyer.sendMessage(color("&cYou need &f" + format(listing.price) + " " + currencyName + "&c."));
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
    buyer.sendMessage(color("&aPurchased auction #" + id + " for &f" + format(listing.price) + " " + currencyName + "&a."));
    if (seller.isOnline() && seller.getPlayer() != null) {
      seller.getPlayer().sendMessage(color("&aYour auction #" + id + " sold for &f" + format(listing.price) + " " + currencyName + "&a."));
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
      creator.sendMessage(color("&cYou need &f" + format(total) + " " + currencyName + " &cto fund this order."));
      return;
    }
    if (!economy.withdraw(creator, total)) {
      creator.sendMessage(color("&cPayment failed."));
      return;
    }
    BuyOrder order = new BuyOrder(nextOrderId++, creator.getUniqueId(), creator.getName(), hand.getType(), unitPrice, amount, amount, System.currentTimeMillis());
    orders.put(order.id, order);
    saveOrders();
    creator.sendMessage(color("&aCreated order #" + order.id + " for &f" + amount + "x " + niceMaterial(order.material) + " &aat &f" + format(unitPrice) + " " + currencyName + " each&a."));
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
    order.remaining -= fill;
    if (order.remaining <= 0) orders.remove(id);
    saveOrders();
    seller.sendMessage(color("&aFilled &f" + fill + "x " + niceMaterial(order.material) + " &afor &f" + format(payout) + " " + currencyName + "&a."));
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
    player.sendMessage(color("&aOrder #" + id + " cancelled. Refunded &f" + format(refund) + " " + currencyName + "&a."));
  }

  private void addBuyShopItem(Player player, double price) {
    double min = getConfig().getDouble("buy.min-price", 1);
    double max = getConfig().getDouble("buy.max-price", 1_000_000);
    if (price < min || price > max) {
      player.sendMessage(color("&cBuy price must be between &f" + format(min) + " &cand &f" + format(max) + "&c."));
      return;
    }
    int maxItems = getConfig().getInt("buy.max-items", 500);
    if (buyShopItems.size() >= maxItems) {
      player.sendMessage(color("&cBuy shop item limit reached."));
      return;
    }
    ItemStack hand = player.getInventory().getItemInMainHand();
    if (hand.getType().isAir() || hand.getAmount() <= 0) {
      player.sendMessage(color("&cHold the shop item stack you want to add."));
      return;
    }
    ItemStack item = hand.clone();
    BuyShopItem shopItem = new BuyShopItem(nextBuyShopId++, item, price, System.currentTimeMillis());
    buyShopItems.put(shopItem.id, shopItem);
    saveBuyShop();
    player.sendMessage(color("&aAdded buy item #&f" + shopItem.id + " &a(&f" + item.getAmount() + "x " + niceMaterial(item.getType()) + "&a) for &f" + format(price) + " " + currencyName + "&a."));
  }

  private void removeBuyShopItem(Player player, int id) {
    BuyShopItem removed = buyShopItems.remove(id);
    if (removed == null) {
      player.sendMessage(color("&cBuy item not found."));
      return;
    }
    saveBuyShop();
    player.sendMessage(color("&aRemoved buy item #&f" + id + "&a."));
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
    player.sendMessage(color("&aSet buy item #&f" + id + " &aprice to &f" + format(price) + " " + currencyName + "&a."));
  }

  private void listBuyShopItems(Player player) {
    if (buyShopItems.isEmpty()) {
      player.sendMessage(color("&eBuy shop is empty."));
      return;
    }
    player.sendMessage(color("&6Fallen Buy Shop Items"));
    buyShopItems.values().stream().limit(25).forEach(item -> player.sendMessage(color(
      "&e#" + item.id + " &7- &f" + item.item.getAmount() + "x " + niceMaterial(item.item.getType()) + " &7- &b" + format(item.price) + " " + currencyName
    )));
    if (buyShopItems.size() > 25) {
      player.sendMessage(color("&7Showing 25/" + buyShopItems.size() + " items. Use /buy config for GUI pages."));
    }
  }

  private void buyShopItem(Player buyer, int id) {
    BuyShopItem shopItem = buyShopItems.get(id);
    if (shopItem == null) {
      buyer.sendMessage(color("&cThis buy item is no longer available."));
      return;
    }
    if (!economy.has(buyer, shopItem.price)) {
      buyer.sendMessage(color("&cYou need &f" + format(shopItem.price) + " " + currencyName + "&c."));
      return;
    }
    if (!economy.withdraw(buyer, shopItem.price)) {
      buyer.sendMessage(color("&cPayment failed."));
      return;
    }
    giveOrDrop(buyer, shopItem.item.clone());
    buyer.sendMessage(color("&aBought &f" + shopItem.item.getAmount() + "x " + niceMaterial(shopItem.item.getType()) + " &afor &f" + format(shopItem.price) + " " + currencyName + "&a."));
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
    player.sendMessage(color("&aSold &f" + amount + "x " + niceMaterial(hand.getType()) + " &afor &f" + format(total) + " " + currencyName + "&a."));
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
    player.sendMessage(color("&aSold &f" + items + " items &7(" + stacks + " stacks)&a for &f" + format(total) + " " + currencyName + "&a."));
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
    player.sendMessage(color("&aSold &f" + amount + "x " + niceMaterial(material) + " &afor &f" + format(total) + " " + currencyName + "&a."));
  }

  private void openBuyMenu(Player player, int page) {
    List<BuyShopItem> sorted = sortedBuyShop(player);
    int maxPage = Math.max(0, (sorted.size() - 1) / 45);
    page = Math.max(0, Math.min(page, maxPage));
    PagedHolder holder = new PagedHolder(MenuType.BUY, page);
    Inventory inv = Bukkit.createInventory(holder, 54, color("&8Fallen Buy &7" + (page + 1) + "/" + (maxPage + 1)));
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

  private void openBuyConfigMenu(Player player, int page) {
    List<BuyShopItem> sorted = sortedBuyShop(player);
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
    inv.setItem(49, navItem(Material.BOOK, "&bUse /buy config add <price>"));
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

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    InventoryHolder holder = event.getInventory().getHolder();
    if (holder instanceof PagedHolder paged) {
      event.setCancelled(true);
      int slot = event.getRawSlot();
      if (slot < 0 || slot >= event.getInventory().getSize()) return;
      if (slot == 45) {
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, paged.page - 1);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, paged.page - 1);
        else if (paged.type == MenuType.SELL) openSellMenu(player, paged.page - 1);
        else if (paged.type == MenuType.BUY) openBuyMenu(player, paged.page - 1);
        else openBuyConfigMenu(player, paged.page - 1);
        return;
      }
      if (slot == 53) {
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, paged.page + 1);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, paged.page + 1);
        else if (paged.type == MenuType.SELL) openSellMenu(player, paged.page + 1);
        else if (paged.type == MenuType.BUY) openBuyMenu(player, paged.page + 1);
        else openBuyConfigMenu(player, paged.page + 1);
        return;
      }
      if (slot == 49) {
        if (paged.type == MenuType.BUY_CONFIG || paged.type == MenuType.SELL) return;
        cycleSort(player, paged.type);
        if (paged.type == MenuType.AUCTION) openAuctionMenu(player, 0);
        else if (paged.type == MenuType.ORDERS) openOrdersMenu(player, 0);
        else openBuyMenu(player, 0);
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
        buyShopItem(player, id);
        openBuyMenu(player, paged.page);
      } else if (paged.type == MenuType.BUY_CONFIG) {
        removeBuyShopItem(player, id);
        openBuyConfigMenu(player, paged.page);
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
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    confirmations.remove(event.getPlayer().getUniqueId());
  }

  private ItemStack auctionIcon(AuctionListing listing) {
    ItemStack icon = listing.item.clone();
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
      lore.add(color("&8&m----------------"));
      lore.add(color("&7Auction ID: &f#" + listing.id));
      lore.add(color("&7Seller: &f" + listing.sellerName));
      lore.add(color("&7Price: &b" + format(listing.price) + " " + currencyName));
      lore.add(color("&eClick to buy"));
      meta.setLore(lore);
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack buyIcon(BuyShopItem shopItem, boolean configView) {
    ItemStack icon = shopItem.item.clone();
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
      lore.add(color("&8&m----------------"));
      lore.add(color("&7Buy ID: &f#" + shopItem.id));
      lore.add(color("&7Price: &b" + format(shopItem.price) + " " + currencyName));
      if (configView) {
        lore.add(color("&cClick to remove"));
        lore.add(color("&7Set price: &f/buy config price " + shopItem.id + " <price>"));
      } else {
        lore.add(color("&eClick to buy"));
      }
      meta.setLore(lore);
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      icon.setItemMeta(meta);
    }
    return icon;
  }

  private ItemStack sellIcon(Material material, double value) {
    ItemStack icon = new ItemStack(material);
    ItemMeta meta = icon.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(color("&f" + niceMaterial(material)));
      meta.setLore(List.of(
        color("&7Value: &a" + format(value) + " " + currencyName),
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
        color("&7Unit price: &b" + format(order.unitPrice) + " " + currencyName),
        color("&7Total left: &b" + format(order.unitPrice * order.remaining) + " " + currencyName),
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

  private void cycleSort(Player player, MenuType type) {
    SortMode current = switch (type) {
      case AUCTION -> auctionSort(player);
      case ORDERS -> orderSort(player);
      case SELL -> SortMode.NEWEST;
      case BUY, BUY_CONFIG -> buySort(player);
    };
    SortMode[] modes = SortMode.values();
    SortMode next = modes[(current.ordinal() + 1) % modes.length];
    if (type == MenuType.AUCTION) auctionSorts.put(player.getUniqueId(), next);
    else if (type == MenuType.ORDERS) orderSorts.put(player.getUniqueId(), next);
    else buySorts.put(player.getUniqueId(), next);
  }

  private List<BuyShopItem> sortedBuyShop(Player player) {
    Comparator<BuyShopItem> comparator = switch (buySort(player)) {
      case OLDEST -> Comparator.comparingLong(i -> i.createdAt);
      case PRICE_ASC -> Comparator.comparingDouble(i -> i.price);
      case PRICE_DESC -> Comparator.<BuyShopItem>comparingDouble(i -> i.price).reversed();
      case AMOUNT -> Comparator.<BuyShopItem>comparingInt(i -> i.item.getAmount()).reversed();
      case NEWEST -> Comparator.<BuyShopItem>comparingLong(i -> i.createdAt).reversed();
    };
    return buyShopItems.values().stream().sorted(comparator).collect(Collectors.toList());
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
    FileConfiguration config = YamlConfiguration.loadConfiguration(buyShopFile);
    nextBuyShopId = Math.max(1, config.getInt("next-id", 1));
    ConfigurationSection section = config.getConfigurationSection("items");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      ConfigurationSection row = section.getConfigurationSection(key);
      if (row == null) continue;
      ItemStack item = loadConfiguredItem(row);
      if (item == null || item.getType().isAir()) continue;
      Integer parsedId = parseInt(key);
      int id = parsedId == null ? row.getInt("id") : parsedId;
      buyShopItems.put(id, new BuyShopItem(
        id,
        item,
        row.getDouble("price"),
        row.getLong("created-at")
      ));
      nextBuyShopId = Math.max(nextBuyShopId, id + 1);
    }
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
    YamlConfiguration config = new YamlConfiguration();
    config.set("next-id", nextBuyShopId);
    for (BuyShopItem item : buyShopItems.values()) {
      String path = "items." + item.id + ".";
      config.set(path + "item", item.item);
      config.set(path + "price", item.price);
      config.set(path + "created-at", item.createdAt);
    }
    saveYaml(config, buyShopFile);
  }

  private ItemStack loadConfiguredItem(ConfigurationSection row) {
    ConfigurationSection itemSection = row.getConfigurationSection("item");
    if (itemSection != null && !itemSection.contains("==")) {
      Material material = Material.matchMaterial(itemSection.getString("material", ""));
      if (material == null || material.isAir()) return null;
      int amount = Math.max(1, itemSection.getInt("amount", 1));
      return new ItemStack(material, amount);
    }
    return row.getItemStack("item");
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

  private void sendAuctionHelp(Player player) {
    player.sendMessage(color("&6Fallen Auction"));
    player.sendMessage(color("&e/ah &7- open auction house"));
    player.sendMessage(color("&e/ah sell <price> &7- sell held item stack"));
    player.sendMessage(color("&e/ah sort <newest|oldest|price_asc|price_desc|amount>"));
    player.sendMessage(color("&e/ah cancel <id> &7- cancel your listing"));
  }

  private void sendBuyHelp(Player player) {
    player.sendMessage(color("&6Fallen Buy"));
    player.sendMessage(color("&e/shop &7- open buy shop"));
    player.sendMessage(color("&e/buy &7- open buy shop"));
    player.sendMessage(color("&e/buy sort <newest|oldest|price_asc|price_desc|amount>"));
    if (player.hasPermission("falleneconomy.buy.config")) {
      player.sendMessage(color("&e/buy config &7- open buy shop config"));
      player.sendMessage(color("&e/buy config add <price> &7- add held item stack"));
    }
  }

  private void sendSellHelp(Player player) {
    player.sendMessage(color("&6Fallen Sell"));
    player.sendMessage(color("&e/sell &7- open sell values"));
    player.sendMessage(color("&e/sell hand &7- sell held item stack"));
    player.sendMessage(color("&e/sell all &7- sell inventory storage"));
  }

  private void sendBuyConfigHelp(Player player) {
    player.sendMessage(color("&6Fallen Buy Config"));
    player.sendMessage(color("&e/buy config &7- open config GUI"));
    player.sendMessage(color("&e/buy config add <price> &7- add held item stack"));
    player.sendMessage(color("&e/buy config remove <id> &7- remove shop item"));
    player.sendMessage(color("&e/buy config price <id> <price> &7- set price"));
    player.sendMessage(color("&e/buy config list &7- list shop items"));
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
    if (name.equals("shop") || name.equals("buy")) {
      if (args.length == 1) return filter(List.of("config", "sort", "help"), args[0]);
      if (args.length == 2 && args[0].equalsIgnoreCase("sort")) return filter(SortMode.ids(), args[1]);
      if (args.length == 2 && args[0].equalsIgnoreCase("config")) return filter(List.of("add", "remove", "delete", "price", "list", "help"), args[1]);
    }
    if (name.equals("sell") && args.length == 1) return filter(List.of("hand", "all", "help"), args[0]);
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
    if (name.equals("feconomy") && args.length == 1) return filter(List.of("balance", "give"), args[0]);
    return List.of();
  }

  private List<String> filter(List<String> values, String prefix) {
    String lower = prefix.toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
  }

  private static String color(String message) {
    return ChatColor.translateAlternateColorCodes('&', message);
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

  private static Integer parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static final class BuyShopItem {
    private final int id;
    private final ItemStack item;
    private double price;
    private final long createdAt;

    private BuyShopItem(int id, ItemStack item, double price, long createdAt) {
      this.id = id;
      this.item = item;
      this.price = price;
      this.createdAt = createdAt;
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
    BUY,
    BUY_CONFIG,
    AUCTION,
    ORDERS,
    SELL
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
    private final Map<Integer, Integer> slotIds = new HashMap<>();
    private final Map<Integer, Material> materialSlots = new HashMap<>();
    private Inventory inventory;

    private PagedHolder(MenuType type, int page) {
      this.type = type;
      this.page = page;
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

  private interface EconomyBridge {
    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    void deposit(OfflinePlayer player, double amount);

    double balance(OfflinePlayer player);

    static EconomyBridge create(FallenEconomyPlugin plugin) {
      plugin.getLogger().info("Using Fallen internal Essence balances.");
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
        return plugin.getConfig().getDouble("internal-economy.starting-balance", 0);
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
    private static VaultCompatibilityHook tryRegister(JavaPlugin plugin, EconomyBridge bridge, String currencyName) {
      try {
        Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
        Object provider = Proxy.newProxyInstance(
          economyClass.getClassLoader(),
          new Class<?>[] { economyClass },
          (proxy, method, args) -> handleVaultCall(plugin, bridge, currencyName, method, args)
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

    private static Object handleVaultCall(JavaPlugin plugin, EconomyBridge bridge, String currencyName, Method method, Object[] args) throws Exception {
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
      if (name.equals("currencyNameSingular") || name.equals("currencyNamePlural")) return currencyName;
      if (name.equals("format")) return formatVaultAmount(args, currencyName);
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

    private static String formatVaultAmount(Object[] args, String currencyName) {
      double amount = vaultAmount(args);
      if (Math.abs(amount - Math.rint(amount)) < 0.0001) return (long) Math.rint(amount) + " " + currencyName;
      return String.format(Locale.US, "%.2f %s", amount, currencyName);
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
