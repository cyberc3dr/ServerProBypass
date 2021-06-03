package ru.sliva.bypass

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import java.lang.StringBuilder
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import java.io.File
import java.util.logging.Logger

class MainClass : JavaPlugin(), Listener {

    private lateinit var plugins: Array<Plugin>
    private var pManage = Bukkit.getPluginManager()

    override fun onEnable() {
        dataFolder.mkdirs()
        pManage.registerEvents(this, this)
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            logger.info("Creating a new configuration...")
            config.options().copyDefaults(true)
            saveDefaultConfig()
        }
        val pluginsFolder = File(dataFolder, "plugins")
        if (!pluginsFolder.exists()) {
            logger.warning("Plugins folder not found! Making a new one...")
            pluginsFolder.mkdirs()
        }
        plugins = pManage.loadPlugins(pluginsFolder)
        logger.warning("Plugins enabled successfully!")
    }

    override fun onDisable() {
        logger.warning("Disabling plugins!")
        for (p in plugins) {
            pManage.disablePlugin(p, true)
        }
        logger.info("Plugins disabled successfully!")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission("minecraft.command.say")) {
            val str = StringBuilder()
            for (i in args.indices) {
                str.append(args[i])
                if (i < args.size - 1) str.append(" ")
            }
            val msg = str.toString()
            if (sender is ConsoleCommandSender) {
                if (msg.contains("ads")) {
                    sender.sendMessage("Ad blocked!")
                } else {
                    val format = "[Server] $msg"
                    for (p in Bukkit.getOnlinePlayers()) {
                        if (p.isOp) {
                            val ver = Bukkit.getVersion().split("\\.".toRegex()).toTypedArray()[1].toInt()
                            val sound = if (ver < 13) {
                                Sound.valueOf("BLOCK_NOTE_PLING")
                            } else {
                                Sound.valueOf("BLOCK_NOTE_BLOCK_PLING")
                            }
                            p.playSound(p.location, sound, 100f, 1f)
                            p.sendMessage(format)
                        }
                    }
                }
                return true
            }
            Bukkit.dispatchCommand(sender, "minecraft:say $msg")
        } else {
            sender.sendMessage("§cSorry, you don`t have permission to perform this command.")
        }
        return true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onLogin(e: PlayerLoginEvent) {
        if (config.getBoolean("unlimited-slots")) {
            if (e.result == PlayerLoginEvent.Result.KICK_FULL) {
                e.allow()
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPing(e: ServerListPingEvent) {
        if (config.getBoolean("unlimited-slots")) {
            e.maxPlayers = e.numPlayers + 1
        }
        e.motd = ChatColor.translateAlternateColorCodes('&', config!!.getString("motd").replace("/n", "\n"))
    }
}