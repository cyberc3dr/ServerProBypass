package ru.sliva.bypass;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin {
	
	private Plugin[] plugins;
	private PluginManager pManage;
	private Logger logger;
	private File pluginsFolder;
	
	@Override
	public void onEnable() {
		logger = getLogger();
		pManage = Bukkit.getPluginManager();
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
    	if(command.getName().equalsIgnoreCase("say")) {
			if(!(sender instanceof ConsoleCommandSender)) {
				return false;
			}
			StringBuilder str = new StringBuilder();
	        for (int i = 0; i< args.length;i++)
	        {
	            str.append(args[i] + " ");
	        }
	        String message = str.toString().trim();
	        if(message.contains("ads")) {
	        	logger.info("Ad blocked!");
	        	return true;
	        } else {
	        	for(Player p : Bukkit.getOnlinePlayers()) {
	        		if(p.isOp()) {
	        			p.sendMessage(message);
	        			p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING , 1.0f, 100);
	        		}
	        	}
	        	Bukkit.getConsoleSender().sendMessage(message);
	        }	
			return true;
    	}
    	return true;
    }
}
