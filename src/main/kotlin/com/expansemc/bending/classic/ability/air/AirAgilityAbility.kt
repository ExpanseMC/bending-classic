package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.*
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SPRINT_OFF
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SPRINT_ON
import com.expansemc.bending.api.bender.Bender
import com.expansemc.bending.api.bender.BenderService
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Serializable
data class AirAgilityAbility(
    override val cooldown: Long,
    val jumpPower: Int,
    val speedPower: Int
) : Ability {

    override val type: AbilityType
        get() = ClassicAbilityTypes.AIR_AGILITY

    @Transient
    private val effects: List<PotionEffect> = listOf(
        PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, this.jumpPower, true, false, false),
        PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, this.speedPower, true, false, false)
    )

    private fun execute(player: Player, executionType: AbilityExecutionType) {
        when (executionType) {
            SPRINT_ON -> player.addPotionEffects(effects)
            SPRINT_OFF -> effects.forEach { player.removePotionEffect(it.type) }
        }
    }

    override fun execute(context: AbilityContext, executionType: AbilityExecutionType, task: AbilityTask) {
        // Uses a custom listener so the ability is always available.
    }

    companion object : Listener {
        @EventHandler
        fun onToggleSprint(event: PlayerToggleSprintEvent) {
            val player: Player = event.player
            val bender: Bender = BenderService.instance.getOrCreateBender(player)

            val agility: AirAgilityAbility = bender.getPassive(ClassicAbilityTypes.AIR_AGILITY) as? AirAgilityAbility ?: return

            agility.execute(player, if (event.isSprinting) SPRINT_ON else SPRINT_OFF)
        }
    }
}