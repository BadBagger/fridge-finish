package com.fridgefinish.app.subscription

enum class SubscriptionTier {
    Free,
    Plus
}

enum class PlusFeature {
    UnlimitedItems,
    MultipleStorageLocations,
    CustomExpirationAlerts,
    SmartGroceryList,
    RecipeIdeas,
    WasteAndSavingsInsights,
    MealPlanningCalendar,
    BackupExport,
    PremiumThemes
}

data class FridgeFinishSubscriptionState(
    val tier: SubscriptionTier = SubscriptionTier.Free,
    val activeItemCount: Int = 0,
    val billingMessage: String? = null,
    val hasAdminAccess: Boolean = false,
    val hasBetaTesterAccess: Boolean = false
) {
    val isPlus: Boolean = tier == SubscriptionTier.Plus || hasAdminAccess || hasBetaTesterAccess
    val freeSlotsRemaining: Int = (FREE_ITEM_LIMIT - activeItemCount).coerceAtLeast(0)

    companion object {
        const val FREE_ITEM_LIMIT = 25
    }
}

sealed class FeatureGateResult {
    data object Allowed : FeatureGateResult()
    data class UpgradeRequired(val title: String, val message: String) : FeatureGateResult()
}

object FridgeFinishFeatureGate {
    private val plusFeatures = setOf(
        PlusFeature.UnlimitedItems,
        PlusFeature.MultipleStorageLocations,
        PlusFeature.CustomExpirationAlerts,
        PlusFeature.SmartGroceryList,
        PlusFeature.RecipeIdeas,
        PlusFeature.WasteAndSavingsInsights,
        PlusFeature.MealPlanningCalendar,
        PlusFeature.BackupExport,
        PlusFeature.PremiumThemes
    )

    fun gateAddItem(state: FridgeFinishSubscriptionState): FeatureGateResult {
        return if (!state.isPlus && state.activeItemCount >= FridgeFinishSubscriptionState.FREE_ITEM_LIMIT) {
            FeatureGateResult.UpgradeRequired(
                title = "You reached 25 fridge items",
                message = "Basic tracking stays free. Fridge Finish Plus removes the item limit when you need more room."
            )
        } else {
            FeatureGateResult.Allowed
        }
    }

    fun gateFeature(feature: PlusFeature, state: FridgeFinishSubscriptionState): FeatureGateResult {
        return if (feature in plusFeatures && !state.isPlus) {
            FeatureGateResult.UpgradeRequired(
                title = feature.label,
                message = "This is a Fridge Finish Plus feature. Your basic fridge list still works without upgrading."
            )
        } else {
            FeatureGateResult.Allowed
        }
    }
}

val PlusFeature.label: String
    get() = when (this) {
        PlusFeature.UnlimitedItems -> "Unlimited fridge items"
        PlusFeature.MultipleStorageLocations -> "Multiple storage locations"
        PlusFeature.CustomExpirationAlerts -> "Custom expiration alerts"
        PlusFeature.SmartGroceryList -> "Smart grocery list"
        PlusFeature.RecipeIdeas -> "Recipe ideas"
        PlusFeature.WasteAndSavingsInsights -> "Waste and savings insights"
        PlusFeature.MealPlanningCalendar -> "Meal planning calendar"
        PlusFeature.BackupExport -> "Backup and export"
        PlusFeature.PremiumThemes -> "Premium themes"
    }

object FridgeFinishPlans {
    val freeBenefits = listOf(
        "Track up to 25 fridge items",
        "Use 1 storage location",
        "Add, edit, and delete items",
        "See basic expiration dates",
        "See a basic expiring soon list",
        "Use standard categories"
    )

    val plusBenefits = listOf(
        "Unlimited fridge items",
        "Multiple storage locations: Main Fridge, Freezer, Pantry, Garage Freezer, Mini Fridge, Other",
        "Custom expiration alerts",
        "Smart grocery list",
        "Recipe ideas based on expiring ingredients",
        "Waste and savings insights",
        "Meal planning calendar",
        "Backup and export",
        "Premium themes"
    )
}
