package com.expansemc.bending.classic.ability.fire

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
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class FireShieldAbility(
    override val cooldown: Long,
    val duration: Long,
    val radius: Double,
    val fireTicks: Int
) : CoroutineAbility {

    @Transient
    private val offsets: Array<Array<Vector>> = this.calculateOffsets(this.radius)

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_SHIELD

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        val iterator: Iterator<Array<Vector>> = offsets.loopedIterator()
        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (player.isStale) {
                // End if the player object is stale.
                return
            }

            if (!player.isSneaking) {
                // Player stopped sneaking
                return
            }

            if (this@FireShieldAbility.duration > 0 && startTime.elapsedNow() > this@FireShieldAbility.duration) {
                // Max shield use time reached.
                return
            }

            for (offset: Vector in iterator.next()) {
                // No iterator.hasNext() check is needed; the iterator loops indefinitely.
                val displayLoc: Location = player.location.add(offset)

                if (Random.nextInt(6) == 0) {
                    displayLoc.spawnParticle(Particle.FLAME, 3, 0.1, 0.1, 0.1)
                }
                if (Random.nextInt(4) == 0) {
                    displayLoc.spawnParticle(Particle.SMOKE_NORMAL, 3)
                }
                if (Random.nextInt(7) == 0) {
                    // Play fire bending sound, every now and then.
                    displayLoc.playSound(Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
                }
            }

            for (test: Location in player.location.getNearbyLocations(radius)) {
                // TODO: block protection

                if (test.block.type == Material.FIRE) {
                    test.block.type = Material.AIR
                    test.playSound(Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f)
                }
            }

            for (entity: Entity in player.location.getNearbyEntities(radius)) {
                // TODO: pvp protection

                if (entity is LivingEntity) {
                    if (player.uniqueId == entity.uniqueId) continue

                    entity.fireTicks = this@FireShieldAbility.fireTicks * 20
                } else if (entity is Projectile) {
                    entity.remove()
                }
            }
        }
    }

    private fun calculateOffsets(radius: Double): Array<Array<Vector>> =
        Array(3) { index: Int ->
            val offsets = ArrayList<Vector>()
            val increment: Int = index * 20 + 20

            for (theta: Int in 0 until 180 step increment) {
                val thetaRad: Double = Math.toRadians(theta.toDouble())
                val sinTheta: Double = sin(thetaRad)
                val cosTheta: Double = cos(thetaRad)

                for (phi: Int in 0 until 360 step increment) {
                    val phiRad: Double = Math.toRadians(phi.toDouble())
                    val sinPhi: Double = sin(phiRad)
                    val cosPhi: Double = cos(phiRad)

                    val radiusTwoThirds: Double = radius / 1.5

                    offsets += Vector(
                        radiusTwoThirds * sinTheta * cosPhi,
                        radiusTwoThirds * cosTheta,
                        radiusTwoThirds * sinTheta * sinPhi
                    )
                }
            }

            offsets.toTypedArray()
        }
}