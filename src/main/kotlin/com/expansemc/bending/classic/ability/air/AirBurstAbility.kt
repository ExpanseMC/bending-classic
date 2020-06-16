package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityExecutionTypes.FALL
import com.expansemc.bending.api.ability.AbilityExecutionTypes.LEFT_CLICK
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.ray.AirRaycast
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.ray.progressAll
import com.expansemc.bending.api.util.EpochTime
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
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.random.Random

@Serializable
data class AirBurstAbility(
    override val cooldown: Long,
    val chargeTime: Long,
    val blastRadius: Double,
    val damage: Double,
    val knockback: Double,
    val range: Double,
    val speed: Double,
    val fallThreshold: Double,
    val numSneakParticles: Int,
    val angleTheta: Double,
    val anglePhi: Double,
    val maxConeDegrees: Double = 30.0,
    val collisionPriority: Int,
    val canCoolLava: Boolean,
    val canOpenDoors: Boolean,
    val canFlickLevers: Boolean
) : CoroutineAbility {

    @Transient
    private val maxConeRadians: Double = Math.toRadians(this.maxConeDegrees)

    @Transient
    private val fallDirections: Array<Vector> = getSphereDirections(75.0, 105.0, this.angleTheta, this.anglePhi)

    @Transient
    private val sphereDirections: Array<Vector> = getSphereDirections(0.0, 180.0, this.angleTheta, this.anglePhi)

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_BURST

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        when (executionType) {
            FALL -> {
                if (context.require(AbilityContextKeys.FALL_DISTANCE) > this@AirBurstAbility.fallThreshold) {
                    this.burst(
                        source = player,
                        origin = player.location,
                        directions = fallDirections
                    )
                }
                return
            }
            LEFT_CLICK -> {
                this.burst(
                    source = player,
                    origin = player.eyeLocation,
                    directions = sphereDirections,
                    targetDirection = player.eyeLocation.direction.normalize(),
                    maxAngle = this@AirBurstAbility.maxConeRadians
                )
                return
            }
        }

        var charged = false
        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                return
            }

            if (!charged && startTime.elapsedNow() >= this@AirBurstAbility.chargeTime) {
                // AirBurst is now fully charged.
                charged = true
            }

            if (!player.isSneaking) {
                if (charged) {
                    // Charged AirBurst activated.
                    this.burst(
                        source = player,
                        origin = player.eyeLocation,
                        directions = sphereDirections
                    )
                    return
                } else {
                    // AirBurst wasn't charged; do nothing.
                    return
                }
            }

            if (charged) {
                player.eyeLocation.spawnParticle(
                    particle = Particle.CLOUD,
                    count = this@AirBurstAbility.numSneakParticles,
                    offsetX = Math.random(),
                    offsetY = Math.random(),
                    offsetZ = Math.random(),
                    extra = 0.0,
                    data = null,
                    force = true
                )
            } else {
                player.eyeLocation.spawnParticle(
                    particle = Particle.SMOKE_NORMAL,
                    count = 4,
                    offsetX = 0.4,
                    offsetY = 0.4,
                    offsetZ = 0.4,
                    extra = 0.0,
                    data = null,
                    force = true
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
            range = this@AirBurstAbility.range,
            speed = this@AirBurstAbility.speed,
            targetDirection = targetDirection,
            maxAngle = maxAngle
        )

        val affectedLocations = HashSet<Location>()
        val affectedEntities = HashSet<Entity>()
        abilityLoopUnsafe {
            if (source.isStale) {
                // Player object went stale.
                return
            }

            // TODO: collision checking

            val anySucceeded: Boolean = raycasts.progressAll {
                // TODO: block protection

                affectLocations(source, affectedLocations, this@AirBurstAbility.blastRadius) { test: Location ->
                    AirRaycast.extinguishFlames(test)
                            // TODO: canCoolLava
                            || (this@AirBurstAbility.canOpenDoors && AirRaycast.toggleDoor(test))
                            || (this@AirBurstAbility.canFlickLevers && AirRaycast.toggleLever(test))
                }
                affectEntities(source, affectedEntities, this@AirBurstAbility.blastRadius) { test: Entity ->
                    AirRaycast.pushEntity(this, source, test, false, 0.0, this@AirBurstAbility.knockback)

                    damageEntity(test, damage, source)
                }

                spawnParticle(Particle.CLOUD, 2, 0.275)
                if (Random.nextInt(9) == 0) {
                    playSound(Sound.ENTITY_CREEPER_HURT, 0.5f, 1.0f)
                }

                return@progressAll true
            }

            if (!anySucceeded) {
                // Stop when no more rays are advancing.
                return
            }
        }
    }
}