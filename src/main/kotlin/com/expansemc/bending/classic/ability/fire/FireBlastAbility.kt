package com.expansemc.bending.classic.ability.fire

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.protection.BlockProtectionService
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.util.isLiquid
import com.expansemc.bending.api.util.isStale
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.random.Random

@Serializable
data class FireBlastAbility(
    override val cooldown: Long,
    val damage: Double,
    val fireTicks: Int,
    val knockback: Double,
    val radius: Double,
    val range: Double,
    val speed: Double,
    val showParticles: Boolean,
    val flameRadius: Double,
    val smokeRadius: Double,
    val collisionPriority: Int
) : CoroutineAbility {

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_BLAST

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        val origin: Location = player.eyeLocation.clone()
        val direction: Vector = origin.direction.normalize()

        val raycast = FastRaycast(
            origin = origin,
            direction = direction,
            range = this@FireBlastAbility.range,
            speed = this@FireBlastAbility.speed,
            checkDiagonals = true
        )

        val affectedEntities = HashSet<Entity>()
        abilityLoopUnsafe {
            if (player.isStale) {
                // End if the player object is stale.
                return
            }

            // TODO: collision checking

            val succeeded: Boolean = raycast.progress { current: Location ->
                if (BlockProtectionService.instance.isProtected(player, current)) {
                    // Can't bend here!
                    return@progress false
                }

                affectEntities(player, affectedEntities, radius) { test: Entity ->
                    FireRaycast.pushAndBurn(
                        source = player,
                        target = test,
                        direction = this.direction.clone(),
                        knockback = this@FireBlastAbility.knockback,
                        fireTicks = this@FireBlastAbility.fireTicks,
                        damage = this@FireBlastAbility.damage
                    )
                }

                if (this@FireBlastAbility.showParticles) {
                    // Show the particles.
                    spawnParticle(Particle.FLAME, 6, this@FireBlastAbility.flameRadius)
                    spawnParticle(Particle.SMOKE_NORMAL, 3, this@FireBlastAbility.smokeRadius)
                }

                if (Random.nextInt(4) == 0) {
                    // Play the sounds every now and then.
                    playSound(Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
                }

                if (affectedEntities.size > 0) {
                    // Attacked at least one entity. End now.
                    return
                }
                if (current.block.type.isSolid || current.block.type.isLiquid) {
                    // Reached a wall or body of liquid.
                    return
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