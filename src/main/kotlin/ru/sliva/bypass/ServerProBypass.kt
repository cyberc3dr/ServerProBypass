package ru.sliva.bypass

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginLoadOrder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level


object ServerProBypass : JavaPlugin(), Listener {

    private lateinit var plugins: Array<Plugin>
    private var pluginManager = Bukkit.getPluginManager()

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
        } else {
            plugins = pluginManager.loadPlugins(pluginFolder)
            plugins.forEach {
                try {
                    it.logger.info("Loading ${it.pluginMeta.displayName}")
                    it.onLoad()
                } catch (ex: Throwable) {
                    logger.log(Level.SEVERE, "${ex.message} initializing ${it.pluginMeta.displayName} (Is it up to date?)", ex)
                }
            }
        }
    }

    private fun enablePlugins(type : PluginLoadOrder) {
        for (plugin in plugins) {
            if (!plugin.isEnabled && plugin.pluginMeta.loadOrder == type) {
                enablePlugin(plugin)
            }
        }
    }

    private fun enablePlugin(plugin: Plugin) {
        try {
            plugin.pluginMeta.permissions.forEach {
                try {
                    pluginManager.addPermission(it)
                } catch (ex: IllegalArgumentException) {
                    logger.log(Level.WARNING, "Plugin ${plugin.pluginMeta.displayName} tried to register permission '${it.name}' but it's already registered", ex)
                }
            }
            pluginManager.enablePlugin(plugin)
        } catch (ex: Throwable) {
            logger.log(Level.SEVERE, "${ex.message} loading ${plugin.pluginMeta.displayName} (Is it up to date?)", ex)
        }
    }

    private fun disablePlugins() {
        for(plugin in plugins) {
            pluginManager.disablePlugin(plugin)
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission("minecraft.command.say")) {
            val msg = args.copyOfRange(1, args.size - 1).joinToString(" ")

            if (sender is ConsoleCommandSender) {
                if (msg.contains("ads")) {
                    logger.info("Ad blocked!")
                } else {
                    for (p in Bukkit.getOnlinePlayers()) {
                        if (p.isOp) {
                            val ver = Bukkit.getVersion().split("\\.".toRegex()).toTypedArray()[1].toInt()
                            val sound = if (ver < 13) {
                                Sound.valueOf("BLOCK_NOTE_PLING")
                            } else {
                                Sound.valueOf("BLOCK_NOTE_BLOCK_PLING")
                            }
                            p.playSound(p.location, sound, 100f, 1f)
                            p.sendMessage(Component.text("[Server] $msg", NamedTextColor.WHITE))
                        }
                    }
                }
                return true
            }
            Bukkit.dispatchCommand(sender, "minecraft:say $msg")
        } else {
            sender.sendMessage(Component.text("Sorry, you don`t have permission to perform this command.", NamedTextColor.RED))
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
        config.getList("motd")?.let { Component.text(it.joinToString(" "), NamedTextColor.WHITE) }?.let { e.motd(it) }
    }
}