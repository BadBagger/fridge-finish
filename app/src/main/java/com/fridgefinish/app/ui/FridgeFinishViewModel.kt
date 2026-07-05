package com.fridgefinish.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fridgefinish.app.FridgeFinishApplication
import com.fridgefinish.app.data.BarcodeProduct
import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.ProductLookupRepository
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import com.fridgefinish.app.data.RestockItemEntity
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodLocation
import com.fridgefinish.app.domain.FreshnessCalculator
import com.fridgefinish.app.domain.FreshnessStatus
import com.fridgefinish.app.subscription.BillingGateway
import com.fridgefinish.app.subscription.BillingResult
import com.fridgefinish.app.subscription.FridgeFinishSubscriptionState
import com.fridgefinish.app.subscription.PlaceholderBillingGateway
import com.fridgefinish.app.subscription.toUserMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FridgeFinishUiState(
    val foods: List<FoodItemEntity> = emptyList(),
    val restock: List<RestockItemEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeIngredients: List<RecipeIngredientEntity> = emptyList(),
    val subscription: FridgeFinishSubscriptionState = FridgeFinishSubscriptionState()
) {
    val activeFoods: List<FoodItemEntity> =
        foods.filterNot { it.isFinished }.sortedBy {
            FreshnessCalculator.urgencyRank(it.expirationDate, it.reminderDaysBefore, it.isFinished)
        }

    val expiredCount: Int = activeFoods.count { statusOf(it) == FreshnessStatus.EXPIRED }
    val expiresTodayCount: Int = activeFoods.count { statusOf(it) == FreshnessStatus.EXPIRES_TODAY }
    val eatSoonCount: Int = activeFoods.count { statusOf(it) == FreshnessStatus.EAT_SOON }
    val topPriority: List<FoodItemEntity> = activeFoods.take(8)
    val restockOpen: List<RestockItemEntity> = restock.filterNot { it.isPurchased }

    fun statusOf(item: FoodItemEntity): FreshnessStatus =
        FreshnessCalculator.status(item.expirationDate, item.reminderDaysBefore, item.isFinished)
}

class FridgeFinishViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FridgeFinishApplication).repository
    private val productLookup = ProductLookupRepository()
    private val billingGateway: BillingGateway = PlaceholderBillingGateway()
    private val _barcodeLookup = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val barcodeLookup = _barcodeLookup.asStateFlow()
    private val billingMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repository.seedRecipeDatabaseIfNeeded() }
    }

    val uiState: StateFlow<FridgeFinishUiState> = combine(
        combine(repository.foods, repository.restockItems) { foods, restock -> foods to restock },
        combine(repository.recipes, repository.recipeIngredients) { recipes, ingredients -> recipes to ingredients },
        combine(billingGateway.subscriptionTier, billingMessage) { tier, message -> tier to message }
    ) { foodData, recipeData, billingData ->
        val activeItemCount = foodData.first.count { !it.isFinished }
        FridgeFinishUiState(
            foods = foodData.first,
            restock = foodData.second,
            recipes = recipeData.first,
            recipeIngredients = recipeData.second,
            subscription = FridgeFinishSubscriptionState(
                tier = billingData.first,
                activeItemCount = activeItemCount,
                billingMessage = billingData.second
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FridgeFinishUiState())

    fun saveFood(item: FoodItemEntity) {
        viewModelScope.launch { repository.saveFood(item) }
    }

    fun deleteFood(item: FoodItemEntity) {
        viewModelScope.launch { repository.deleteFood(item) }
    }

    fun markFinished(item: FoodItemEntity, addToRestock: Boolean = true) {
        viewModelScope.launch { repository.markFinished(item, addToRestock) }
    }

    fun saveRestock(item: RestockItemEntity) {
        viewModelScope.launch { repository.saveRestock(item) }
    }

    fun toggleRestock(item: RestockItemEntity) {
        viewModelScope.launch { repository.saveRestock(item.copy(isPurchased = !item.isPurchased)) }
    }

    fun deleteRestock(item: RestockItemEntity) {
        viewModelScope.launch { repository.deleteRestock(item) }
    }

    fun addSampleData() {
        viewModelScope.launch { repository.addSampleData() }
    }

    fun lookupBarcode(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            _barcodeLookup.value = BarcodeLookupState.Loading(barcode)
            val product = productLookup.lookupBarcode(barcode)
            _barcodeLookup.value = if (product == null) BarcodeLookupState.NotFound(barcode) else BarcodeLookupState.Found(product)
        }
    }

    fun clearBarcodeLookup() {
        _barcodeLookup.value = BarcodeLookupState.Idle
    }

    fun startPlusPurchase() {
        viewModelScope.launch {
            billingMessage.value = billingGateway.startPlusPurchase().toUserMessage()
        }
    }

    fun restorePlusPurchases() {
        viewModelScope.launch {
            billingMessage.value = billingGateway.restorePurchases().toUserMessage()
        }
    }

    fun presetFood(category: FoodCategory, location: FoodLocation = FoodLocation.FRIDGE): FoodItemEntity {
        val today = LocalDate.now()
        val expires = when (category) {
            FoodCategory.LEFTOVERS -> today.plusDays(3)
            FoodCategory.MEAT -> today.plusDays(2)
            FoodCategory.DAIRY -> today.plusDays(7)
            FoodCategory.PRODUCE -> today.plusDays(5)
            FoodCategory.FROZEN -> today.plusMonths(3)
            FoodCategory.PANTRY -> today.plusMonths(6)
            else -> today.plusDays(7)
        }
        val resolvedLocation = when (category) {
            FoodCategory.FROZEN -> FoodLocation.FREEZER
            FoodCategory.PANTRY -> FoodLocation.PANTRY
            else -> location
        }
        return FoodItemEntity(
            name = "",
            category = category,
            location = resolvedLocation,
            expirationDate = expires,
            reminderDaysBefore = FreshnessCalculator.defaultReminderDays(category)
        )
    }
}

sealed interface BarcodeLookupState {
    data object Idle : BarcodeLookupState
    data class Loading(val barcode: String) : BarcodeLookupState
    data class Found(val product: BarcodeProduct) : BarcodeLookupState
    data class NotFound(val barcode: String) : BarcodeLookupState
}
