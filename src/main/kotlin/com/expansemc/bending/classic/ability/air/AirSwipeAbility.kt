package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.*
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.ray.AirRaycast
import com.expansemc.bending.api.ray.FastRaycast
import com.expansemc.bending.api.ray.progressAll
import com.expansemc.bending.api.util.*
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
import org.spongepowered.math.matrix.Matrix3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class AirSwipeAbility(
    override val cooldown: Long,
    val chargeTime: Long,
    val maxChargeFactor: Double,
    val arcDegrees: Double,
    val arcIncrementDegrees: Double,
    val damage: Double,
    val knockback: Double,
    val radius: Double,
    val range: Double,
    val speed: Double,
    val numParticles: Int,
    val collisionPriority: Int
) : CoroutineAbility {

    @Transient
    private val arcRadians: Double = Math.toRadians(this.arcDegrees)

    @Transient
    private val arcIncrementRadians: Double = Math.toRadians(this.arcIncrementDegrees)

    @Transient
    private val transformationMatrices: Array<Matrix3d> = this.createTransformationMatrices().toTypedArray()

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_SWIPE

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        if (executionType == AbilityExecutionTypes.LEFT_CLICK) {
            this.swipe(
                player, player.eyeLocation,
                this@AirSwipeAbility.damage, this@AirSwipeAbility.knockback
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

            val elapsed = startTime.elapsedNow()
            if (elapsed >= chargeTime) {
                charged = true
            }

            if (!player.isSneaking) {
                val factor: Double = if (charged) {
                    maxChargeFactor
                } else {
                    maxChargeFactor * (elapsed / chargeTime)
                }

                this.swipe(
                    player, player.eyeLocation,
                    this@AirSwipeAbility.damage * factor, this@AirSwipeAbility.knockback * factor
                )
                return
            }

            if (charged) {
                player.eyeLocation.spawnParticle(
                    Particle.CLOUD, this@AirSwipeAbility.numParticles,
                    0.2, 0.2, 0.2, 0.0, null, true
                )
            }
        }
    }

    private suspend fun CoroutineTask.swipe(source: Player, origin: Location, damage: Double, knockback: Double) {
        val raycasts: Array<FastRaycast> = createRaycasts(origin, source.eyeLocation.direction.normalize())

        val affectedEntities = HashSet<Entity>()
        abilityLoopUnsafe {
            if (source.isStale) {
                // End if the player object is stale.
                return
            }

            // TODO: collision checking

            val anySucceeded: Boolean = raycasts.progressAll {
                // TODO: block protection

                affectEntities(source, affectedEntities, this@AirSwipeAbility.radius) { test: Entity ->
                    AirRaycast.pushEntity(
                        this, source, test,
                        false, 0.0, knockback
                    )

                    damageEntity(test, damage, source)
                }

                spawnParticle(Particle.CLOUD, this@AirSwipeAbility.numParticles, 0.2)
                if (Random.nextInt(4) == 0) {
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

    private fun createRaycasts(origin: Location, direction: Vector): Array<FastRaycast> =
        this.transformationMatrices.mapToArray { matrix: Matrix3d ->
            FastRaycast(
                origin = origin,
                direction = direction.clone().transform(matrix),
                range = this.range,
                speed = this.speed,
                checkDiagonals = true
            )
        }

    private fun createTransformationMatrices(): List<Matrix3d> {
        val matrices = ArrayList<Matrix3d>()

        forInclusive(from = -this.arcRadians, to = this.arcRadians, step = this.arcIncrementRadians) { angle: Double ->
            val sinAngle: Double = sin(angle)
            val cosAngle: Double = cos(angle)

            matrices += Matrix3d(
                cosAngle, 0.0, -sinAngle,
                0.0, 1.0, 0.0,
                sinAngle, 0.0, cosAngle
            )
        }

        return matrices
    }
}