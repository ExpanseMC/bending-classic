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
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
data class AirTornadoAbility(
    override val cooldown: Long,
    val duration: Long,
    val knockback: Double,
    val maxHeight: Double,
    val radius: Double,
    val range: Double,
    val speed: Double
) : CoroutineAbility {

    private val numStreams: Int = (this.maxHeight * 0.3).toInt()

    private val angleMap: Map<Int, Int> = createAngleMap()

    override val type: AbilityType get() = ClassicAbilityTypes.AIR_TORNADO

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        var curHeight: Double = 2.0
        var curRadius: Double

        val angles: MutableMap<Int, Int> = angleMap.toMutableMap()

        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (player.isStale) {
                // Player object went stale.
                return
            }
            if (player.eyeLocation.block.type.isLiquid) {
                // Can't tornado while underwater.
                return
            }
            if (!player.isSneaking) {
                // Player stopped sneaking.
                return
            }

            // TODO: block protection

            if (this@AirTornadoAbility.duration > 0 && startTime.elapsedNow() > this@AirTornadoAbility.duration) {
                // Max tornado use time reached.
                return
            }

            val origin: Location = player.getTargetBlock(null, this@AirTornadoAbility.range.toInt()).location
            val timeFactor: Double = curHeight / this@AirTornadoAbility.maxHeight

            curRadius = timeFactor * radius

            if (!origin.block.type.isAir && origin.block.type !== Material.BARRIER) {
                origin.y -= 0.1 * curHeight

                for (entity: Entity in origin.getNearbyEntities(curHeight)) {
                    // TODO: pvp protection

                    val entityY: Double = entity.location.y
                    val factor: Double

                    if (entityY - origin.y in 0.0..curHeight) {
                        factor = (entityY - origin.y) / curHeight

                        val testLoc: Location = origin.clone()
                        testLoc.y = entityY

                        if (testLoc.distanceSquared(entity.location) < curRadius * curRadius * factor * factor) {
                            val dx: Double = entity.location.x - origin.x
                            val dz: Double = entity.location.z - origin.z

                            val magnitude: Double = sqrt(dx * dx + dz * dz)

                            var vx: Double = 0.0
                            var vy: Double = 0.05 * knockback
                            var vz: Double = 0.0
                            if (magnitude != 0.0) {
                                vx = (dx * COS_100_DEG - dz * SIN_100_DEG) / magnitude
                                vz = (dx * SIN_100_DEG - dz * COS_100_DEG) / magnitude
                            }

                            if (entity.uniqueId == player.uniqueId) {
                                val direction: Vector = player.eyeLocation.direction.normalize()
                                vx = direction.x
                                vz = direction.z

                                val playerLoc: Location = player.location
                                val dy: Double = playerLoc.y - origin.y

                                vy = when {
                                    dy >= curHeight * 0.95 -> 0.0
                                    dy >= curHeight * 0.85 -> 6.0 * (0.95 - dy / curHeight)
                                    else -> 0.6
                                }
                            }

                            // TODO: check invincible

                            entity.velocity = Vector(vx, vy, vz).multiply(timeFactor)
                            entity.fallDistance = 0.0f
                        }
                    }
                }

                for (entry: MutableMap.MutableEntry<Int, Int> in angles) {
                    val angleRad: Double = Math.toRadians(entry.value.toDouble())

                    val factor: Double = entry.key / curHeight
                    val x: Double = origin.x + timeFactor * factor * curRadius * cos(angleRad)
                    val y: Double = origin.y + timeFactor * entry.key
                    val z: Double = origin.z + timeFactor * factor * curRadius * sin(angleRad)

                    val particleLoc = Location(origin.world, x, y, z)
                    // TODO: block protection

                    particleLoc.spawnParticle(Particle.CLOUD, 3, 0.4, 0.4, 0.4)
                    if (Random.nextInt(20) == 0) {
                        particleLoc.playSound(Sound.ENTITY_CREEPER_HURT, 1.0f, 1.0f)
                    }

                    entry.setValue(entry.key + 25 * speed.toInt())
                }
            }

            curHeight = (curHeight + 1).coerceAtMost(maxHeight)
        }
    }

    private val Material.isAir: Boolean
        get() = this === Material.AIR || this === Material.CAVE_AIR || this === Material.CAVE_AIR

    private fun createAngleMap(): Map<Int, Int> {
        val angles = HashMap<Int, Int>()
        var angle = 0

        for (i: Double in 0.0..this.maxHeight step this.maxHeight / this.numStreams) {
            angles[i.roundToInt()] = angle
            angle += 90

            if (angle == 360) {
                angle = 0
            }
        }

        return angles
    }

    companion object {
        val COS_100_DEG: Double = cos(Math.toRadians(100.0))
        val SIN_100_DEG: Double = sin(Math.toRadians(100.0))
    }
}