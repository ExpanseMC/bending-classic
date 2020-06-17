package com.expansemc.bending.classic.ability.fire

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.protection.BlockProtectionService
import com.expansemc.bending.api.protection.EntityProtectionService
import com.expansemc.bending.api.util.*
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import com.expansemc.bending.classic.util.ZERO_VECTOR
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.random.Random

@Serializable
data class FireWallAbility(
    override val cooldown: Long,
    val duration: Long,
    val displayInterval: Long,
    val damage: Double,
    val damageInterval: Long,
    val fireTicks: Int,
    val width: Int,
    val height: Int
) : CoroutineAbility {

    @Transient
    private val wallDistanceSquared: Double = 1.5 * 1.5

    @Transient
    private val damageRadiusCheck: Int = max(this@FireWallAbility.width, this@FireWallAbility.height) + 1

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_WALL

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        val origin: Location = player.eyeLocation.add(player.eyeLocation.direction.multiply(2.0))
        val locations: List<Location> = calculateLocations(player, origin, player.eyeLocation.direction)

        val startTime: EpochTime = EpochTime.now()

        var displayTick: Long = 0
        var damageTick: Long = 0
        abilityLoopUnsafe {
            if (player.isStale) {
                // End if the player object is stale.
                return
            }

            val elapsed: Long = startTime.elapsedNow()

            if (elapsed > this@FireWallAbility.duration) {
                // Wall max lifetime reached.
                return
            }

            if (elapsed > displayTick * this@FireWallAbility.displayInterval) {
                displayTick++
                for (location: Location in locations) {
                    location.spawnParticle(Particle.FLAME, 3, 0.6, 0.6, 0.6)
                    location.spawnParticle(Particle.SMOKE_NORMAL, 2, 0.6, 0.6, 0.6)

                    if (Random.nextInt(7) == 0) {
                        // Play fire bending sound, every now and then.
                        location.playSound(Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
                    }
                }
            }

            if (elapsed > damageTick * this@FireWallAbility.damageInterval) {
                damageTick++

                for (entity: Entity in origin.getNearbyEntities(damageRadiusCheck.toDouble())) {
                    if (EntityProtectionService.instance.isProtected(player, entity)) {
                        // Can't bend this entity!
                        continue
                    }

                    for (location: Location in locations) {
                        if (entity.location.distanceSquared(location) < this@FireWallAbility.wallDistanceSquared) {
                            entity.velocity = ZERO_VECTOR
                            if (entity is LivingEntity) {
                                entity.damage(this@FireWallAbility.damage, player)
                            }
                            entity.fireTicks = this@FireWallAbility.fireTicks * 20
                            break
                        }
                    }
                }
            }
        }
    }

    private fun calculateLocations(player: Player, origin: Location, direction: Vector): List<Location> {
        val result = HashSet<Location>()
        val horizontal: Vector = direction.getOrthogonal(0.0, 1.0).normalize()
        val vertical: Vector = direction.getOrthogonal(90.0, 1.0).normalize()

        for (w: Int in -this.width..this.width) {
            for (h: Int in -this.height..this.height) {
                val location: Location = (origin + horizontal * w).add(vertical * h)

                if (BlockProtectionService.instance.isProtected(player, location)) {
                    // Can't bend here!
                    continue
                }

                result += location
            }
        }

        return result.toList()
    }
}