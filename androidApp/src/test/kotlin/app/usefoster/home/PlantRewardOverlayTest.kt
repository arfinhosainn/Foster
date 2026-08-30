package app.usefoster.home

import app.usefoster.home.presentation.badges.PlantRewardStage
import app.usefoster.home.presentation.badges.plantRewardStage
import app.usefoster.home.presentation.badges.reveal
import org.junit.Assert.assertEquals
import org.junit.Test

class PlantRewardOverlayTest {

    @Test
    fun rewardStartsWithSoilUntilItIsExplicitlyRevealed() {
        assertEquals(PlantRewardStage.Soil, plantRewardStage(initiallyRevealed = false))
        assertEquals(PlantRewardStage.Unlocked, PlantRewardStage.Soil.reveal())
    }

    @Test
    fun tappingAnywhereOnSoilStageRevealsThePlant() {
        var stage = plantRewardStage(initiallyRevealed = false)

        stage = stage.reveal()

        assertEquals(PlantRewardStage.Unlocked, stage)
    }

    @Test
    fun alreadyRevealedRewardDoesNotReturnToSoil() {
        assertEquals(PlantRewardStage.Unlocked, plantRewardStage(initiallyRevealed = true))
        assertEquals(PlantRewardStage.Unlocked, PlantRewardStage.Unlocked.reveal())
    }
}