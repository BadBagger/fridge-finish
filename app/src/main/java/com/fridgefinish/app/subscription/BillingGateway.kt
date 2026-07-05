package com.fridgefinish.app.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface BillingGateway {
    val subscriptionTier: Flow<SubscriptionTier>
    suspend fun startPlusPurchase(): BillingResult
    suspend fun restorePurchases(): BillingResult
}

sealed class BillingResult {
    data object Unavailable : BillingResult()
    data object Pending : BillingResult()
    data class Failed(val reason: String) : BillingResult()
}

class PlaceholderBillingGateway : BillingGateway {
    override val subscriptionTier: Flow<SubscriptionTier> = flowOf(SubscriptionTier.Free)

    override suspend fun startPlusPurchase(): BillingResult = BillingResult.Unavailable

    override suspend fun restorePurchases(): BillingResult = BillingResult.Unavailable
}

fun BillingResult.toUserMessage(): String {
    return when (this) {
        BillingResult.Unavailable -> "Google Play Billing is not connected yet. No purchase was started."
        BillingResult.Pending -> "Purchase is pending."
        is BillingResult.Failed -> reason
    }
}
