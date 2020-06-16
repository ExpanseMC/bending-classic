package com.expansemc.bending.classic

import com.expansemc.bending.api.bender.Bender
import com.expansemc.bending.api.bender.BenderService
import com.expansemc.bending.api.registry.CatalogRegistry
import com.expansemc.bending.api.registry.registerAll
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import com.expansemc.bending.classic.ability.air.*
import com.expansemc.bending.classic.ability.fire.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class BendingClassic : JavaPlugin(), Listener {

    companion object {
        lateinit var PLUGIN: Plugin
            private set
    }

    override fun onEnable() {
        PLUGIN = this

        this.logger.info("Registering ability types: ${ClassicAbilityTypes.types.map { it.key }}")

        CatalogRegistry.instance.registerAll(*ClassicAbilityTypes.types)

        this.logger.info("Registering listeners...")

        Bukkit.getPluginManager().registerEvents(this, this)
    }

    override fun onDisable() {

    }
}