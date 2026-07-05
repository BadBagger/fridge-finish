package com.fridgefinish.app.subscription

import org.junit.Assert.assertTrue
import org.junit.Test

class FridgeFinishFeatureGateTest {
    @Test
    fun freeUserCanAddItemBeforeLimit() {
        val state = FridgeFinishSubscriptionState(
            tier = SubscriptionTier.Free,
            activeItemCount = FridgeFinishSubscriptionState.FREE_ITEM_LIMIT - 1
        )

        val result = FridgeFinishFeatureGate.gateAddItem(state)

        assertTrue(result is FeatureGateResult.Allowed)
    }

    @Test
    fun freeUserSeesUpgradePromptAtLimit() {
        val state = FridgeFinishSubscriptionState(
            tier = SubscriptionTier.Free,
            activeItemCount = FridgeFinishSubscriptionState.FREE_ITEM_LIMIT
        )

        val result = FridgeFinishFeatureGate.gateAddItem(state)

        assertTrue(result is FeatureGateResult.UpgradeRequired)
    }

    @Test
    fun plusUserCanAddBeyondFreeLimit() {
        val state = FridgeFinishSubscriptionState(
            tier = SubscriptionTier.Plus,
            activeItemCount = 125
        )

        val result = FridgeFinishFeatureGate.gateAddItem(state)

        assertTrue(result is FeatureGateResult.Allowed)
    }

    @Test
    fun plusFeatureRequiresUpgradeForFreeUser() {
        val state = FridgeFinishSubscriptionState(tier = SubscriptionTier.Free)

        val result = FridgeFinishFeatureGate.gateFeature(PlusFeature.RecipeIdeas, state)

        assertTrue(result is FeatureGateResult.UpgradeRequired)
    }

    @Test
    fun plusFeatureIsAllowedForPlusUser() {
        val state = FridgeFinishSubscriptionState(tier = SubscriptionTier.Plus)

        val result = FridgeFinishFeatureGate.gateFeature(PlusFeature.MultipleStorageLocations, state)

        assertTrue(result is FeatureGateResult.Allowed)
    }

    @Test
    fun adminAccessAllowsPlusFeatureWithoutPurchase() {
        val state = FridgeFinishSubscriptionState(
            tier = SubscriptionTier.Free,
            hasAdminAccess = true
        )

        val result = FridgeFinishFeatureGate.gateFeature(PlusFeature.SmartGroceryList, state)

        assertTrue(result is FeatureGateResult.Allowed)
    }

    @Test
    fun adminAccessAllowsAddingBeyondFreeLimit() {
        val state = FridgeFinishSubscriptionState(
            tier = SubscriptionTier.Free,
            activeItemCount = 125,
            hasAdminAccess = true
        )

        val result = FridgeFinishFeatureGate.gateAddItem(state)

        assertTrue(result is FeatureGateResult.Allowed)
    }
}
