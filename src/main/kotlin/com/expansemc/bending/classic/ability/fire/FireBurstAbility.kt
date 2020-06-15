package com.expansemc.bending.classic.ability.fire

import com.expansemc.bending.api.ability.*
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.ray.progressAll
import com.expansemc.bending.api.util.EpochTime
import com.expansemc.bending.api.util.isLiquid
import com.expansemc.bending.api.util.isStale
import com.expansemc.bending.api.util.spawnParticle
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import com.expansemc.bending.classic.util.ZERO_VECTOR
import com.expansemc.bending.classic.util.createSphereRaycasts
import com.expansemc.bending.classic.util.getSphereDirections
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.random.Random

@Serializable
data class FireBurstAbility(
    override val cooldown: Long,
    val chargeTime: Long,
    val fireTicks: Int,
    val angleTheta: Double,
    val anglePhi: Double,
    val blastRadius: Double,
    val damage: Double,
    val knockback: Double,
    val range: Double,
    val speed: Double,
    val maxConeDegrees: Double = 30.0
) : CoroutineAbility {

    @Transient
    private val maxConeRadians: Double = Math.toRadians(this.maxConeDegrees)

    @Transient
    private val directions: Array<Vector> = getSphereDirections(0.0, 180.0, this.angleTheta, this.anglePhi)

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_BURST

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        if (executionType == AbilityExecutionTypes.LEFT_CLICK) {
            this.burst(
                source = player,
                origin = player.eyeLocation,
                directions = directions,
                targetDirection = player.eyeLocation.direction.normalize(),
                maxAngle = this@FireBurstAbility.maxConeRadians
            )
            return
        }

        var charged = false
        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                return
            }

            if (!charged && startTime.elapsedNow() >= chargeTime) {
                // FireBurst is now fully charged.
                charged = true
            }

            if (!player.isSneaking) {
                if (charged) {
                    // Charged FireBurst activated.
                    this.burst(
                        source = player,
                        origin = player.eyeLocation,
                        directions = directions
                    )
                    return
                } else {
                    // FireBurst wasn't charged; do nothing.
                    return
                }
            }

            if (charged) {
                player.eyeLocation.spawnParticle(
                    particle = Particle.FLAME,
                    count = 3,
                    offsetX = Math.random(),
                    offsetY = Math.random(),
                    offsetZ = Math.random()
                )
            } else {
                player.eyeLocation.spawnParticle(
                    particle = Particle.SMOKE_NORMAL,
                    count = 4,
                    offsetX = 0.4,
                    offsetY = 0.4,
                    offsetZ = 0.4
                )
            }
        }
    }

    private suspend fun CoroutineTask.burst(
        source: Player, origin: Location, directions: Array<Vector>,
        targetDirection: Vector = ZERO_VECTOR, maxAngle: Double = 0.0
    ) {
        val raycasts: List<FastRaycast> = createSphereRaycasts(
            origin = origin,
            directions = directions,
            range = this@FireBurstAbility.range,
            speed = this@FireBurstAbility.speed,
            targetDirection = targetDirection,
            maxAngle = maxAngle
        )

        val affectedLocations = HashSet<Location>()
        val affectedEntities = HashSet<Entity>()

        var curRange = 0
        abilityLoopUnsafe {
            if (source.isStale) {
                // Player object went stale.
                return
            }

            // TODO: collision checking

            val anySucceeded: Boolean = raycasts.progressAll { current: Location ->
                // TODO: block protection

                affectLocations(source, affectedLocations, this@FireBurstAbility.blastRadius) { test: Location ->
                    if (curRange < range / 4) {
                        return@affectLocations true
                    }

                    if (test.block.type == Material.AIR && test.block.getRelative(BlockFace.DOWN).type.isSolid) {
                        test.block.type = Material.FIRE
                        return@affectLocations true
                    }

                    return@affectLocations false
                }
                affectEntities(source, affectedEntities, blastRadius) { test: Entity ->
                    FireRaycast.pushAndBurn(
                        source = source,
                        target = test,
                        direction = this.direction.clone(),
                        knockback = this@FireBurstAbility.knockback,
                        fireTicks = this@FireBurstAbility.fireTicks,
                        damage = this@FireBurstAbility.damage
                    )
                }

                spawnParticle(Particle.FLAME, 6, 0.4)
                if (Random.nextInt(4) == 0) {
                    playSound(Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
                }

                if (current.block.type.isSolid || current.block.type.isLiquid) {
                    return@progressAll false
                }

                return@progressAll true
            }

            if (!anySucceeded) {
                // Stop when no more rays are advancing.
                return
            }

            curRange++
        }
    }
}