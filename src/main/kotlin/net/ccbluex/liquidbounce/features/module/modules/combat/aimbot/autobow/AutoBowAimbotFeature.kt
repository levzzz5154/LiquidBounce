package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.projectiles.PolynomialProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.OverlayTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem

/**
 * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
 */
object AutoBowAimbotFeature : ToggleableConfigurable(ModuleAutoBow, "BowAimbot", true) {

    // Target
    val targetTracker = TargetTracker(PriorityEnum.DISTANCE)

    // Rotation
    val rotationConfigurable = RotationsConfigurable(this)

    val minExpectedPull by int("MinExpectedPull", 5, 0..20, suffix = "ticks")

    init {
        tree(targetTracker)
        tree(rotationConfigurable)
    }

    private val targetRenderer = tree(OverlayTargetRenderer(ModuleAutoBow))

    @Suppress("unused")
    val tickRepeatable = tickHandler {
        targetTracker.cleanup()

        // Should check if player is using bow
        val activeItem = player.activeItem?.item
        if (activeItem !is BowItem && activeItem !is TridentItem) {
            return@tickHandler
        }

        val projectileInfo = TrajectoryData.getRenderedTrajectoryInfo(
            player,
            activeItem,
            true
        ) ?: return@tickHandler

        var rotation: Rotation? = null

        for (enemy in targetTracker.enemies()) {
            val rot = SituationalProjectileAngleCalculator.calculateAngleForEntity(projectileInfo, enemy) ?: continue

            targetTracker.lock(enemy)
            rotation = rot
            break
        }

        if (rotation == null) {
            return@tickHandler
        }

        RotationManager.aimAt(
            rotation,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = ModuleAutoBow,
            configurable = rotationConfigurable
        )
    }

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> { event ->
        val target = targetTracker.lockedOnTarget ?: return@handler

        renderEnvironmentForGUI {
            targetRenderer.render(this, target, event.tickDelta)
        }
    }

}
