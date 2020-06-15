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

    private val abilityAirBlast by lazy {
        AirBlastAbility(
            cooldown = 600,
            damage = 0.0,
            knockbackOther = 1.6,
            knockbackSelf = 2.0,
            radius = 2.0,
            range = 20.0,
            selectRange = 10.0,
            speed = 25.0,
            numParticles = 6,
            canCoolLava = true,
            canOpenDoors = true,
            canFlickLevers = true
        )
    }

    private val abilityAirBurst by lazy {
        AirBurstAbility(
            cooldown = 10000,
            chargeTime = 1750,
            blastRadius = 2.0,
            damage = 0.0,
            knockback = 2.8,
            range = 20.0,
            speed = 25.0,
            fallThreshold = 10.0,
            numSneakParticles = 10,
            angleTheta = 10.0,
            anglePhi = 10.0,
            collisionPriority = 0,
            canCoolLava = true,
            canOpenDoors = true,
            canFlickLevers = true
        )
    }

    private val abilityAirSwipe by lazy {
        AirSwipeAbility(
            cooldown = 1500,
            chargeTime = 2500,
            maxChargeFactor = 3.0,
            arcDegrees = 16.0,
            arcIncrementDegrees = 4.0,
            damage = 2.0,
            knockback = 0.5,
            radius = 2.0,
            range = 14.0,
            speed = 25.0,
            numParticles = 3,
            collisionPriority = 0
        )
    }

    private val abilityAirShield by lazy {
        AirShieldAbility(
            cooldown = 0,
            duration = 0,
            speed = 10.0,
            maxRadius = 7.0,
            initialRadius = 1.0,
            numStreams = 5,
            numParticles = 5,
            collisionPriority = 0
        )
    }

    private val abilityAirSpout by lazy {
        AirSpoutAbility(
            cooldown = 10000,
            duration = 10000,
            animationInterval = 100,
            maxHeight = 16.0,
            collisionPriority = 0
        )
    }

    private val abilityFireJet by lazy {
        FireJetAbility(
            cooldown = 7000,
            duration = 2000,
            speed = 0.8,
            showGliding = false
        )
    }

    private val abilityFireBlast by lazy {
        FireBlastAbility(
            cooldown = 1500,
            damage = 3.0,
            fireTicks = 1,
            knockback = 0.3,
            radius = 1.5,
            range = 20.0,
            speed = 20.0,
            showParticles = true,
            flameRadius = 0.275,
            smokeRadius = 0.3,
            collisionPriority = 0
        )
    }

    private val abilityFireBurst by lazy {
        FireBurstAbility(
            cooldown = 5000,
            chargeTime = 2000,
            fireTicks = 20,
            angleTheta = 10.0,
            anglePhi = 10.0,
            blastRadius = 2.0,
            damage = 2.0,
            knockback = 0.3,
            range = 15.0,
            speed = 25.0
        )
    }

    private val abilityFireCombustion by lazy {
        FireCombustionAbility(
            cooldown = 10000,
            canBreakBlocks = false,
            damage = 4.0,
            power = 1.0f,
            radius = 4.0,
            range = 35.0,
            speed = 25.0
        )
    }

    private val abilityFireShield by lazy {
        FireShieldAbility(
            cooldown = 0,
            duration = 0,
            radius = 5.0,
            fireTicks = 2
        )
    }

    private val abilityFireWall by lazy {
        FireWallAbility(
            cooldown = 11000,
            duration = 5000,
            displayInterval = 250,
            damage = 1.0,
            damageInterval = 500,
            fireTicks = 1,
            width = 4,
            height = 4
        )
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

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val bender: Bender = BenderService.instance.getOrCreateBender(event.player)

        bender.equipped[0] = abilityAirBlast
        bender.equipped[1] = abilityAirBurst
        bender.equipped[2] = abilityAirSwipe
        bender.equipped[3] = abilityAirShield
        bender.equipped[4] = abilityAirSpout
        bender.equipped[5] = abilityFireJet
        bender.equipped[6] = abilityFireBlast
        bender.equipped[7] = abilityFireBurst
        bender.equipped[8] = abilityFireWall
    }
}