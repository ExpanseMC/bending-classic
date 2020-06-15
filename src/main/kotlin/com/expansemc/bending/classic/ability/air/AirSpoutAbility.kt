package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.*
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.bender.Bender
import com.expansemc.bending.api.util.*
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

@Serializable
data class AirSpoutAbility(
    override val cooldown: Long,
    val duration: Long,
    val animationInterval: Long,
    val maxHeight: Double,
    val collisionPriority: Int
) : CoroutineAbility {

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_SPOUT

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)
        val bender: Bender = context.require(AbilityContextKeys.BENDER)

        val startTime: EpochTime = EpochTime.now()

        var animationTime: EpochTime = EpochTime.now()
        val defer: CompletableFuture<Void?> = bender.waitForExecution(type, AbilityExecutionTypes.LEFT_CLICK)
        abilityLoopUnsafe {
            if (defer.isDone) {
                // Player left clicked, end spout.
                return
            }

            if (player.isStale) {
                // End if the player object is stale.
                defer.cancel(false)
                return
            }

            if (startTime.elapsedNow() > duration) {
                // Reached maximum spout use time.
                defer.cancel(false)
                return
            }

            val eyeLocation: Location = player.eyeLocation
            if (eyeLocation.block.type.isSolid || eyeLocation.block.type.isWater) {
                // Can't use AirSpout while underwater or suffocating.
                defer.cancel(false)
                return
            }

            player.fallDistance = 0.0f
            player.isSprinting = false

            if (Random.nextInt(4) == 0) {
                // Play the sounds every now and then.
                player.world.playSound(player.location, Sound.ENTITY_CREEPER_HURT, 0.5f, 1.0f)
            }

            // TODO: collision checking

            val floor: Location = player.location.floor
            val height: Double = player.location.y - floor.y

            val isBelowMax: Boolean = height < maxHeight
            player.allowFlight = isBelowMax
            player.isFlying = isBelowMax

            if (animationTime.elapsedNow() >= animationInterval) {
                animationTime = EpochTime.now()

                val particleLoc: Location = floor.clone()

                var i = 1
                while (i < height) {
                    particleLoc.add(0.0, 1.0, 0.0)
                    particleLoc.spawnParticle(Particle.CLOUD, 3, 0.4, 0.4, 0.4)
                    i++
                }
            }
        }
    }

    override fun cleanup(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)
        player.allowFlight = false
        player.isFlying = false
    }
}