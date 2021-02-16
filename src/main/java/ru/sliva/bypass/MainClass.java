package ru.sliva.bypass;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin implements Listener{
	
	private Plugin[] plugins;
	private PluginManager pManage;
	private Logger logger;
	private File pluginsFolder;
	private FileConfiguration config;
	
	@Override
	public void onEnable() {
		logger = getLogger();
		
		pManage = Bukkit.getPluginManager();
		pManage.registerEvents(this, this);
		
		File configFile = new File(getDataFolder() + File.separator + "config.yml");
		if(!configFile.exists()) {
			getLogger().info("Creating a new configuration...");
			getConfig().options().copyDefaults(true);
			saveDefaultConfig();
		}
		
		config = getConfig();
		
		pluginsFolder = new File(getDataFolder() + File.separator + "plugins");
		if(!pluginsFolder.exists()) {
			logger.warning("Plugins folder not found! Making a new one...");
			pluginsFolder.mkdirs();
		}
		plugins = pManage.loadPlugins(pluginsFolder);
		logger.warning("Plugins enabled successfully!");
		
	}
	
	@Override
	public void onDisable() {
		logger.warning("Disabling plugins!");
		for(Plugin p : plugins) {
			pManage.disablePlugin(p);
			unloadPlugin(p);
		}
		logger.info("Plugins disabled successfully!");
	}
	
    private void unloadPlugin(Plugin pl) {
        ClassLoader classLoader = pl.getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            try {
                ((URLClassLoader) classLoader).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.gc();
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    	return Collections.emptyList();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	if(sender.hasPermission("minecraft.command.say")) {
        	StringBuilder str = new StringBuilder();
        	for(int i = 0; i < args.length; i++) {
        		str.append(args[i]);
        		if(i < args.length) str.append(" ");
        	}
        	String msg = str.toString();
        	if(sender instanceof ConsoleCommandSender) {
        		if(msg.contains("ads")) {
        			sender.sendMessage("Ad blocked!");
        		} else {
        			for(Player p : Bukkit.getOnlinePlayers()) {
        				if(p.isOp()) {
        					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
        					String format = "[Server] " + msg;
        					p.sendMessage(format);
        					sender.sendMessage(format);
        				}
        			}
        		}
        		return true;
        	}
        	Bukkit.dispatchCommand(sender, "minecraft:say " + msg);
    	} else {
    		sender.sendMessage(Bukkit.getPermissionMessage());
    	}
    	return true;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent e) {
    	if(config.getBoolean("unlimited-slots")) {
        	if(e.getResult() == Result.KICK_FULL) {
        		e.allow();
        	}
    	}
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ServerListPingEvent e) {
    	if(config.getBoolean("unlimited-slots")) {
    		e.setMaxPlayers(e.getNumPlayers() + 1);
    	}
    	e.setMotd(ChatColor.translateAlternateColorCodes('&', config.getString("motd").replace("/n", "\n")));
    }
}