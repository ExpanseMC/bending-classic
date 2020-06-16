package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.util.*
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.spongepowered.math.vector.Vector3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
data class AirShieldAbility(
    override val cooldown: Long,
    val duration: Long,
    val speed: Double,
    val maxRadius: Double,
    val initialRadius: Double,
    val numStreams: Int,
    val numParticles: Int,
    val collisionPriority: Int
) : CoroutineAbility {

    @Transient
    private val offsetSize: Int = ((this.maxRadius - this.initialRadius) / 0.3).toInt()

    @Transient
    private val offsets: Array<Array<Vector3d>> = calculateRangedOffsets()

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_SHIELD

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        var index = 0
        var curRadius: Double = initialRadius
        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                return
            }

            if (player.eyeLocation.block.type.isLiquid) {
                // Can't bend in water/lava.
                return
            }

            if (!player.isSneaking) {
                // Player stopped sneaking.
                return
            }

            if (this@AirShieldAbility.duration > 0 && startTime.elapsedNow() > this@AirShieldAbility.duration) {
                // Max shield use time reached.
                return
            }

            val origin: Location = player.location

            // TODO: collision checking

            // Push entities around.
            for (entity: Entity in origin.getNearbyEntities(curRadius)) {
                // TODO: pvp protection

                val entityLoc: Location = entity.location

                if (origin.distanceSquared(entityLoc) > 4) {
                    val x: Double = entityLoc.x - origin.x
                    val z: Double = entityLoc.z - origin.z
                    val magnitude: Double = sqrt(x * x + z * z)
                    val vx: Double = (x * COS_50_DEG - z * SIN_50_DEG) / magnitude
                    val vz: Double = (x * SIN_50_DEG - z * COS_50_DEG) / magnitude

                    entity.velocity = Vector(vx, entity.velocity.y, vz).multiply(0.5)
                    entity.fallDistance = 0.0f
                }
            }

            // Extinguish nearby fires.
            for (test: Location in origin.getNearbyLocations(curRadius)) {
                // TODO: block protection

                if (test.block.type == Material.FIRE) {
                    test.block.type = Material.AIR
                    test.spawnParticle(
                        Particle.SMOKE_NORMAL, 1,
                        0.2, 0.2, 0.2, 0.0, null, true
                    )
                }
            }

            // Render the particle shield.
            for (offset: Vector3d in offsets[index]) {
                val displayLoc: Location = origin + offset

                // TODO: block protection

                displayLoc.spawnParticle(
                    Particle.CLOUD,
                    this@AirShieldAbility.numParticles,
                    Math.random(),
                    Math.random(),
                    Math.random(),
                    0.0,
                    null,
                    true
                )
                if (Random.nextInt(4) == 0) {
                    displayLoc.playSound(Sound.ENTITY_CREEPER_HURT, 0.5f, 1.0f)
                }
            }

            if (index >= offsetSize) {
                index = 0
            }

            if (curRadius < maxRadius) {
                curRadius += 0.3
            }
        }
    }

    private fun calculateRangedOffsets(): Array<Array<Vector3d>> {
        val angles: MutableMap<Int, Int> = createAngleDegMap()
        val rangedOffsets = ArrayList<Array<Vector3d>>(this.offsetSize)

        for (radius: Double in this.initialRadius..this.maxRadius step 0.3) {
            rangedOffsets.add(calculateOffsets(radius, angles))
        }

        return rangedOffsets.toTypedArray()
    }

    private fun calculateOffsets(radius: Double, angles: MutableMap<Int, Int>): Array<Vector3d> {
        val offsets = ArrayList<Vector3d>()

        for ((index: Int, angleDeg: Int) in angles) {
            val angleRad: Double = Math.toRadians(angleDeg.toDouble())

            val factor: Double = radius / this.maxRadius
            val f: Double = sqrt(1 - factor * factor * (index / radius) * (index / radius))

            offsets += Vector3d(
                radius * cos(angleRad) * f,
                factor * index,
                radius * sin(angleRad) * f
            )

            angles[index] = angleDeg + this.speed.toInt()
        }

        return offsets.toTypedArray()
    }

    private fun createAngleDegMap(): MutableMap<Int, Int> {
        val angles = HashMap<Int, Int>()
        var angle = 0
        val di: Double = 2 * this.maxRadius / this.numStreams
        for (i: Double in -this.maxRadius + di..this.maxRadius step di) {
            angles[i.toInt()] = angle
            angle += 90

            if (angle == 360) {
                angle = 0
            }
        }
        return angles
    }

    companion object {
        private val COS_50_DEG: Double = cos(Math.toRadians(50.0))
        private val SIN_50_DEG: Double = sin(Math.toRadians(50.0))
    }
}