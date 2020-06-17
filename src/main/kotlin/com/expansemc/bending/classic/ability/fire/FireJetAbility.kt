package com.expansemc.bending.classic.ability.fire

import com.expansemc.bending.api.ability.AbilityContext
import com.expansemc.bending.api.ability.AbilityContextKeys
import com.expansemc.bending.api.ability.AbilityExecutionType
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.ability.coroutine.CoroutineAbility
import com.expansemc.bending.api.ability.coroutine.CoroutineTask
import com.expansemc.bending.api.protection.BlockProtectionService
import com.expansemc.bending.api.util.EpochTime
import com.expansemc.bending.api.util.isLiquid
import com.expansemc.bending.api.util.isStale
import com.expansemc.bending.api.util.spawnParticle
import com.expansemc.bending.classic.BendingClassic
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.random.Random

@Serializable
data class FireJetAbility(
    override val cooldown: Long,
    val duration: Long,
    val speed: Double,
    val showGliding: Boolean
) : CoroutineAbility {

    override val type: AbilityType get() = ClassicAbilityTypes.FIRE_JET

    override val plugin: Plugin get() = BendingClassic.PLUGIN

    override suspend fun CoroutineTask.activate(context: AbilityContext, executionType: AbilityExecutionType) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)

        val startTime: EpochTime = EpochTime.now()
        abilityLoopUnsafe {
            if (BlockProtectionService.instance.isProtected(player, player.location)) {
                // Can't bend here!
                return
            }

            if (player.isStale) {
                // End if the player object is stale.
                return
            }
            if (player.location.block.type.isLiquid) {
                // Crashed into water/lava.
                return
            }
            if (startTime.elapsedNow() >= duration) {
                // Reached maximum jet use time.
                return
            }

            if (Random.nextInt(2) == 0) {
                // Play fire bending sound, every now and then.
                player.world.playSound(player.location, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
            }

            player.location.spawnParticle(Particle.FLAME, 20, 0.6, 0.6, 0.6)
            player.location.spawnParticle(Particle.SMOKE_NORMAL, 10, 0.6, 0.6, 0.6)

            val timeFactor: Double = 1 - startTime.elapsedNow() / (2.0 * duration)
            player.velocity = player.eyeLocation.direction.normalize().multiply(speed * timeFactor)
            player.fallDistance = 0.0f
        }
    }
}