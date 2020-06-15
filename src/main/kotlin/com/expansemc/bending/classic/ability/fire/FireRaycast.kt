package com.expansemc.bending.classic.ability.fire

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object FireRaycast {

    fun pushAndBurn(source: Player, target: Entity, direction: Vector, knockback: Double, fireTicks: Int, damage: Double): Boolean {
        if (source.uniqueId == target.uniqueId) {
            // Don't hurt ourselves.
            return false
        }

        // TODO: pvp protection

        // Push the entity.
        target.velocity.add(direction.multiply(knockback))

        if (target is LivingEntity) {
            // Set ablaze the entity if it's alive.
            target.fireTicks = fireTicks * 20
            target.damage(damage, source)
            return true
        }

        return false
    }
}