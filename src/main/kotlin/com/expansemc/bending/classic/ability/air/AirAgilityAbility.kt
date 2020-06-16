package com.expansemc.bending.classic.ability.air

import com.expansemc.bending.api.ability.*
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SPRINT_OFF
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SPRINT_ON
import com.expansemc.bending.classic.ability.ClassicAbilityTypes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.entity.Player
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

    override fun execute(context: AbilityContext, executionType: AbilityExecutionType, task: AbilityTask) {
        val player: Player = context.require(AbilityContextKeys.PLAYER)
        when (executionType) {
            SPRINT_ON -> player.addPotionEffects(effects)
            SPRINT_OFF -> effects.forEach { player.removePotionEffect(it.type) }
        }
    }
}