package com.expansemc.bending.classic.ability.fire

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.protection.BlockProtectionService
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.util.isStale
import com.expansemc.bending.api.util.isWater
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

@Serializable
data class FireCombustionAbility(
    override val cooldown: Long,
    val canBreakBlocks: Boolean,
    val damage: Double,
    val power: Float,
    val radius: Double,
    val range: Double,
    val speed: Double
) : CoroutineAbility {

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_COMBUSTION

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)
        val origin: Location = player.eyeLocation
        val direction: Vector = origin.direction.normalize()

        val raycast = FastRaycast(
            origin = origin,
            direction = direction,
            range = this@FireCombustionAbility.range,
            speed = this@FireCombustionAbility.speed,
            checkDiagonals = true
        )

        val affectedEntities = HashSet<Entity>()
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                return
            }

            val succeeded: Boolean = raycast.progress { current: Location ->
                if (BlockProtectionService.instance.isProtected(player, current)) {
                    // Can't bend here!
                    return@progress false
                }

                spawnParticle(Particle.FIREWORKS_SPARK, 5, Math.random() / 2)
                spawnParticle(Particle.FLAME, 2, Math.random() / 2)
                playSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.0f)

                if (current.block.type != Material.AIR && !current.block.type.isWater) {
                    createExplosion(current, this@FireCombustionAbility.power, this@FireCombustionAbility.canBreakBlocks)
                    return
                }

                affectEntities(player, affectedEntities, this@FireCombustionAbility.radius) { test: Entity ->
                    if (test !is LivingEntity || test.uniqueId == player.uniqueId) {
                        false
                    } else {
                        createExplosion(current, this@FireCombustionAbility.power, this@FireCombustionAbility.canBreakBlocks)
                        return
                    }
                }

                return@progress true
            }

            if (!succeeded) {
                // End the ability if the ray couldn't advance.
                return
            }
        }
    }

    private fun createExplosion(location: Location, power: Float, canBreakBlocks: Boolean) {
        location.world!!.createExplosion(location.x, location.y, location.z, power, true, canBreakBlocks)
    }
}