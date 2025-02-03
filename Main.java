package com.blockcondition;

import de.tr7zw.nbtapi.NBTTileEntity;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class project extends JavaPlugin implements Listener {
  public static Economy econ;
  
  public static PlayerPoints playerPoints;
  
  public ArrayList<String> map = new ArrayList<>();
  
  public ArrayList<String> debug = new ArrayList<>();
  
  public void onEnable() {
    setupEconomy();
    setupPlayerPoints();
    saveDefaultConfig();
    reloadConfig();
    Bukkit.getPluginCommand("BlockCondition").setExecutor((CommandExecutor)this);
    Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
  }
  
  private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null)
      return false; 
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null)
      return false; 
    econ = (Economy)rsp.getProvider();
    return (econ != null);
  }
  
  public static boolean setupPlayerPoints() {
    Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
    playerPoints = PlayerPoints.class.cast(plugin);
    return (playerPoints != null);
  }
  
  public static String getBlockNBT(Block block) {
    BlockState bs = block.getState();
    String blockname = block.getType().toString();
    try {
      NBTTileEntity tent = new NBTTileEntity(bs);
      return String.valueOf(blockname) + "," + tent.asNBTString();
    } catch (Exception e) {
      return blockname;
    } 
  }
  
  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getHand() == EquipmentSlot.OFF_HAND)
      return; 
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
      return; 
    Block b = e.getClickedBlock();
    final Player p = e.getPlayer();
    ItemStack i = p.getItemInHand();
    if (this.debug.contains(p.getName())) {
      p.sendMessage("+ getBlockNBT(b));
      p.sendMessage("+ i.getType().toString());
      e.setCancelled(true);
      return;
    } 
    if (b == null || b.getType() == Material.AIR)
      return; 
    String itemname = i.getType().toString();
    String blockname = getBlockNBT(b);
    ConfigurationSection cs = getConfig().getConfigurationSection("");
    for (String temp : cs.getKeys(false)) {
      String BlockName = getConfig().getString(String.valueOf(temp) + ".Block");
      String NotContains = getConfig().getString(String.valueOf(temp) + ".NotContains");
      if (BlockName == null)
        continue; 
      if (blockname.toUpperCase().contains(BlockName.toUpperCase()) && !blockname.toUpperCase().contains(NotContains.toUpperCase())) {
        List<String> needItems = getConfig().getStringList(String.valueOf(temp) + ".CheckInItemHands");
        boolean check_hand_item = false;
        if (needItems.size() == 0) {
          check_hand_item = true;
        } else {
          for (int c = 0; c < needItems.size(); c++) {
            String needItem = needItems.get(c);
            if (needItem.equalsIgnoreCase(itemname)) {
              check_hand_item = true;
              break;
            } 
          } 
        } 
        if (!check_hand_item)
          continue; 
        String permission = getConfig().getString(String.valueOf(blockname) + ".PermissionNeed");
        if (permission != null && !permission.equalsIgnoreCase("") && 
          !p.hasPermission(permission)) {
          p.sendMessage(getConfig().getString("NoPermission").replace("<Permission>", permission));
          e.setCancelled(true);
          break;
        } 
        int MONEY = getConfig().getInt(String.valueOf(temp) + ".MONEY");
        if (MONEY != 0 && 
          econ.getBalance((OfflinePlayer)p) < MONEY) {
          p.sendMessage(getConfig().getString("NoMoney").replace("<MONEY>", String.valueOf(MONEY)));
          e.setCancelled(true);
          break;
        } 
        int POINT = getConfig().getInt(String.valueOf(temp) + ".POINT");
        if (POINT != 0 && 
          playerPoints.getAPI().look(p.getUniqueId()) < POINT) {
          p.sendMessage(getConfig().getString("NoPoint").replace("<POINT>", String.valueOf(POINT)));
          e.setCancelled(true);
          break;
        } 
        if (this.map.contains(p.getName())) {
          econ.withdrawPlayer((OfflinePlayer)p, MONEY);
          playerPoints.getAPI().take(p.getUniqueId(), POINT);
          this.map.remove(p.getName());
          break;
        } 
        e.setCancelled(true);
        this.map.add(p.getName());
        (new BukkitRunnable() {
            public void run() {
              if (project.this.map.contains(p.getName()))
                project.this.map.remove(p.getName()); 
            }
          }).runTaskLater((Plugin)this, 100L);
        p.sendMessage(getConfig().getString("Message").replace("<MONEY>", String.valueOf(MONEY)).replace("<POINT>", String.valueOf(POINT)));
        break;
      } 
    } 
  }
  
  @EventHandler
  public boolean onCommand(CommandSender sender, Command cmd, String Label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage("Reload -> );
      sender.sendMessage("Debug  -> );
      return false;
    } 
    if (args.length == 1 && args[0].equalsIgnoreCase("Reload")) {
      if (sender instanceof Player) {
        Player p = (Player)sender;
        if (!p.isOp()) {
          p.sendMessage(");
          return false;
        } 
      } 
      reloadConfig();
      sender.sendMessage(");
      return false;
    } 
    if (args.length == 1 && args[0].equalsIgnoreCase("Debug")) {
      if (sender instanceof Player) {
        Player player = (Player)sender;
        if (!player.isOp()) {
          player.sendMessage(");
          return false;
        } 
      } 
      Player p = (Player)sender;
      if (this.debug.contains(p.getName())) {
        this.debug.remove(p.getName());
        p.sendMessage(");
      } else {
        this.debug.add(p.getName());
        p.sendMessage(");
      } 
    } 
    return false;
  }
}
