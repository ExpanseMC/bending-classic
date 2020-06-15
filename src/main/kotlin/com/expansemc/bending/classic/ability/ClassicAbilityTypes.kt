package com.expansemc.bending.classic.ability

import com.expansemc.bending.api.ability.Ability
import com.expansemc.bending.api.ability.AbilityExecutionTypes.FALL
import com.expansemc.bending.api.ability.AbilityExecutionTypes.LEFT_CLICK
import com.expansemc.bending.api.ability.AbilityExecutionTypes.SNEAK
import com.expansemc.bending.api.ability.AbilityType
import com.expansemc.bending.api.element.Elements.AIR
import com.expansemc.bending.api.element.Elements.FIRE
import com.expansemc.bending.api.util.NamespacedKeys.bending
import com.expansemc.bending.api.util.serialization.ConfigurationNodeParser
import com.expansemc.bending.classic.ability.air.*
import com.expansemc.bending.classic.ability.fire.*
import kotlinx.serialization.DeserializationStrategy

object ClassicAbilityTypes {

    val AIR_BLAST: AbilityType = AbilityType.builder()
        .key(bending("air_blast"))
        .name("AirBlast")
        .element(AIR)
        .executionTypes(LEFT_CLICK, SNEAK)
        .loader(AirBlastAbility.serializer())
        .description(
            "AirBlast is the most fundamental bending technique of an airbender." +
                    "\nIt allows the bender to be extremely agile and possess great mobility, " +
                    "but also has many utility options, such as cooling lava, opening doors and flicking levers."
        )
        .instructions(
            "(Push) LEFT CLICK while aiming at an entity to push them back." +
                    "\n(Throw) Tap SNEAK to select a location and LEFT CLICK in a direction to throw entities away from the selected location."
        )
        .build()

    val AIR_BURST: AbilityType = AbilityType.builder()
        .key(bending("air_burst"))
        .name("AirBurst")
        .element(AIR)
        .executionTypes(LEFT_CLICK, SNEAK, FALL)
        .loader(AirBurstAbility.serializer())
        .description(
            "AirBurst is one of the most powerful abilities in the airbender's arsenal." +
                    "\nIt allows the bender to create space between them and whoever is close to them." +
                    "\nAirBurst is extremely useful when you're surrounded by mobs, of if you're low in health and need to escape." +
                    "\nIt can also be useful for confusing your target also."
        )
        .instructions(
            "(Sphere) Hold SNEAK until particles appear and then release shift to create air that expands outwards, pushing entities back." +
                    "\n(Cone) While charging the move with SNEAK, LEFT CLICK to send the burst in a cone only going in one direction." +
                    "\nIf you FALL from a great height while you are on this slot, the burst will automatically activate."
        )
        .build()

    val AIR_SHIELD: AbilityType = AbilityType.builder()
        .key(bending("air_shield"))
        .name("AirShield")
        .element(AIR)
        .executionTypes(SNEAK)
        .loader(AirShieldAbility.serializer())
        .description(
            "Air Shield is one of the most powerful defensive techniques in existence." +
                    "\nThis ability is mainly used when you are low health and need protection." +
                    "\nIt's also useful when you're surrounded by mobs."
        )
        .instructions(
            "Hold SNEAK and a shield of air will form around you, blocking projectiles and pushing entities back."
        )
        .build()

    val AIR_SPOUT: AbilityType = AbilityType.builder()
        .key(bending("air_spout"))
        .name("AirSpout")
        .element(AIR)
        .executionTypes(LEFT_CLICK)
        .loader(AirSpoutAbility.serializer())
        .build()

    val AIR_SWIPE: AbilityType = AbilityType.builder()
        .key(bending("air_swipe"))
        .name("AirSwipe")
        .element(AIR)
        .executionTypes(LEFT_CLICK, SNEAK)
        .loader(AirSwipeAbility.serializer())
        .description(
            "AirSwipe is the most commonly used damage ability in an airbender's arsenal." +
                    "\nAn arc of air will flow from you towards the direction you're facing, cutting and pushing back anything in its path." +
                    "\nThis ability will extinguish fires, cool lava, and cut things like grass, mushrooms, and flowers."
        )
        .instructions(
            "(Uncharged) Simply LEFT CLICK to send an air swipe out that will damage targets that it comes into contact with." +
                    "\n(Charged) Hold SNEAK until particles appear, then release SNEAK to send a more powerful air swipe out that damages entities that it comes into contact with."
        )
        .build()

    val FIRE_BLAST: AbilityType = AbilityType.builder()
        .key(bending("fire_blast"))
        .name("FireBlast")
        .element(FIRE)
        .executionTypes(LEFT_CLICK)
        .loader(FireBlastAbility.serializer())
        .build()

    val FIRE_BURST: AbilityType = AbilityType.builder()
        .key(bending("fire_burst"))
        .name("FireBurst")
        .element(FIRE)
        .executionTypes(LEFT_CLICK, SNEAK)
        .loader(FireBurstAbility.serializer())
        .build()

    val FIRE_COMBUSTION: AbilityType = AbilityType.builder()
        .key(bending("fire_combustion"))
        .name("FireCombustion")
        .element(FIRE)
        .executionTypes(SNEAK)
        .loader(FireCombustionAbility.serializer())
        .build()

    val FIRE_JET: AbilityType = AbilityType.builder()
        .key(bending("fire_jet"))
        .name("FireJet")
        .element(FIRE)
        .executionTypes(LEFT_CLICK)
        .loader(FireJetAbility.serializer())
        .build()

    val FIRE_SHIELD: AbilityType = AbilityType.builder()
        .key(bending("fire_shield"))
        .name("FireShield")
        .element(FIRE)
        .executionTypes(SNEAK)
        .loader(FireShieldAbility.serializer())
        .build()

    val FIRE_WALL: AbilityType = AbilityType.builder()
        .key(bending("fire_wall"))
        .name("FireWall")
        .element(FIRE)
        .executionTypes(LEFT_CLICK)
        .loader(FireWallAbility.serializer())
        .build()

    val types: Array<AbilityType> = arrayOf(
        AIR_BLAST, AIR_BURST, AIR_SHIELD, AIR_SPOUT, AIR_SWIPE,
        FIRE_BLAST, FIRE_BURST, FIRE_COMBUSTION, FIRE_JET, FIRE_SHIELD, FIRE_WALL
    )

    private fun AbilityType.Builder.loader(serializer: DeserializationStrategy<out Ability>): AbilityType.Builder =
        this.loader { ConfigurationNodeParser.parse(it, serializer) }
}