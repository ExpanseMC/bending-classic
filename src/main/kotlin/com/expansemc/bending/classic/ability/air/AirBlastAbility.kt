package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityExecutionTypes.LEFT_CLICK
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SNEAK
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.bender.Bender
import com.expansemc.bending.api.ray.AirRaycast
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.util.getTargetLocation
import com.expansemc.bending.api.util.isLiquid
import com.expansemc.bending.api.util.isStale
import com.expansemc.bending.api.util.spawnParticle
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

@Serializable
data class AirBlastAbility(
    override val cooldown: Long,
    val damage: Double,
    val knockbackOther: Double,
    val knockbackSelf: Double,
    val radius: Double,
    val range: Double,
    val selectRange: Double,
    val speed: Double,
    val numParticles: Int,
    val canCoolLava: Boolean,
    val canOpenDoors: Boolean,
    val canFlickLevers: Boolean
) : CoroutineAbility {

    @Transient
    private val selectRangeSquared: Double = this.selectRange * this.selectRange

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_BLAST

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        when (executionType) {
            SNEAK -> {
                val bender: Bender = context.require(AbilityContextKeys.BENDER)
                this.sneak(player.getTargetLocation(this@AirBlastAbility.selectRange), bender, player)
            }
            LEFT_CLICK -> this.leftClick(player.eyeLocation, player, false)
        }
    }

    private suspend fun CoroutineTask.sneak(origin: Location, bender: Bender, player: Player) {
        val defer: CompletableFuture<Void?> = bender.waitForExecution(this@AirBlastAbility.type, LEFT_CLICK)
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                defer.cancel(false)
                return
            }
            if (origin.distanceSquared(player.eyeLocation) > selectRangeSquared) {
                // Went beyond blast range.
                defer.cancel(false)
                return
            }

            origin.spawnParticle(
                Particle.CLOUD, 4,
                Math.random(), Math.random(), Math.random(), 0.0, null, true
            )

            if (defer.isDone) {
                // The player left clicked.

                if (defer.isCancelled) {
                    // Already stopped waiting for them.
                    return
                }

                this.leftClick(origin, player, true)
                return
            }
        }
    }

    private suspend fun CoroutineTask.leftClick(origin: Location, player: Player, canPushSelf: Boolean) {
        if (player.eyeLocation.block.type.isLiquid) return

        val affectedLocations = HashSet<Location>()
        val affectedEntities = HashSet<Entity>()
        val direction: Vector = player.location.direction.normalize()

        val raycast = FastRaycast(
            origin = origin,
            direction = direction,
            range = this@AirBlastAbility.range,
            speed = this@AirBlastAbility.speed,
            checkDiagonals = true
        )

        abilityLoopUnsafe {
            if (player.isStale) {
                // End if the player object is stale.
                return
            }

            // TODO: collision checking

            val succeeded: Boolean = raycast.progress { current: Location ->
                // TODO: block protection

                affectLocations(player, affectedLocations, this@AirBlastAbility.radius) { test ->
                    AirRaycast.extinguishFlames(test)
                            // TODO: canCoolLava
                            || (this@AirBlastAbility.canOpenDoors && AirRaycast.toggleDoor(test))
                            || (this@AirBlastAbility.canFlickLevers && AirRaycast.toggleLever(test))
                }
                affectEntities(player, affectedEntities, this@AirBlastAbility.radius) { test ->
                    // pushEntity return value ignored because we want to push entities multiple times.
                    AirRaycast.pushEntity(
                        this, player, test,
                        canPushSelf, this@AirBlastAbility.knockbackSelf, this@AirBlastAbility.knockbackOther
                    )

                    damageEntity(test, this@AirBlastAbility.damage, player)
                }

                spawnParticle(Particle.CLOUD, this@AirBlastAbility.numParticles, 0.275)

                if (Random.nextInt(4) == 0) {
                    playSound(Sound.ENTITY_CREEPER_HURT, 1.0f, 1.0f)
                }

                if (current.block.type.isSolid || current.block.type.isLiquid) {
                    return@progress false
                }

                return@progress true
            }

            if (!succeeded) {
                // End the ability if the ray couldn't advance.
                return
            }
        }
    }
}