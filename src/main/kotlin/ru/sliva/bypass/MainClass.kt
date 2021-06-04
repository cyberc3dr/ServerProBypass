package ru.sliva.bypass

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.permissions.Permission
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginLoadOrder
import org.bukkit.plugin.SimplePluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level


class MainClass : JavaPlugin(), Listener {

    private lateinit var plugins: Array<Plugin>
    private var pluginManager = Bukkit.getPluginManager() as SimplePluginManager

    override fun onLoad() {
        dataFolder.mkdirs()
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            logger.info("Creating a new configuration...")
            config.options().copyDefaults(true)
            saveDefaultConfig()
        }
        loadPlugins()
        enablePlugins(PluginLoadOrder.STARTUP)
    }

    override fun onEnable() {
        pluginManager.registerEvents(this, this)
        enablePlugins(PluginLoadOrder.POSTWORLD)
        logger.warning("Plugins enabled successfully!")
    }

    override fun onDisable() {
        disablePlugins()
        logger.info("Plugins disabled successfully!")
    }

    private fun loadPlugins() {
        val pluginFolder = File(dataFolder, "plugins")
        if (!pluginFolder.exists()) {
            logger.warning("Plugins folder not found! Making a new one...")
            pluginFolder.mkdirs()
        }
        if (pluginFolder.exists()) {
            plugins = pluginManager.loadPlugins(pluginFolder)
            for (plugin in plugins) {
                try {
                    val message = String.format("Loading %s", plugin.description.fullName)
                    plugin.logger.info(message)
                    plugin.onLoad()
                } catch (ex: Throwable) {
                    logger.log(Level.SEVERE, ex.message + " initializing " + plugin.description.fullName + " (Is it up to date?)", ex)
                }
            }
        } else {
            pluginFolder.mkdir()
        }
    }

    private fun enablePlugins(type : PluginLoadOrder) {
        for (plugin in plugins) {
            if (!plugin.isEnabled && plugin.description.load == type) {
                enablePlugin(plugin)
            }
        }
    }

    private fun enablePlugin(plugin: Plugin) {
        try {
            val perms: List<Permission> = plugin.description.permissions
            for (perm in perms) {
                try {
                    pluginManager.addPermission(perm, false)
                } catch (ex: IllegalArgumentException) {
                    logger.log(Level.WARNING, "Plugin " + plugin.description.fullName + " tried to register permission '" + perm.name + "' but it's already registered", ex)
                }
            }
            pluginManager.dirtyPermissibles()
            pluginManager.enablePlugin(plugin)
        } catch (ex: Throwable) {
            logger.log(Level.SEVERE, ex.message + " loading " + plugin.description.fullName + " (Is it up to date?)", ex)
        }
    }

    fun disablePlugins() {
        for(plugin in plugins) {
            pluginManager.disablePlugin(plugin, true)
        }
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