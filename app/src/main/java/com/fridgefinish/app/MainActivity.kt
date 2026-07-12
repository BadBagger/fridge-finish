package com.fridgefinish.app

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeFeedbackAction
import com.fridgefinish.app.data.RecipeFeedbackEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import com.fridgefinish.app.data.RestockItemEntity
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodItemState
import com.fridgefinish.app.domain.FoodLocation
import com.fridgefinish.app.domain.FreshnessCalculator
import com.fridgefinish.app.domain.FreshnessStatus
import com.fridgefinish.app.domain.DateTextParser
import com.fridgefinish.app.domain.ReceiptImportCandidate
import com.fridgefinish.app.domain.ReceiptTextParser
import com.fridgefinish.app.domain.CookingTool
import com.fridgefinish.app.domain.CookingConfidence
import com.fridgefinish.app.domain.DietaryStyle
import com.fridgefinish.app.domain.RecipeFilter
import com.fridgefinish.app.domain.RecipeAllergen
import com.fridgefinish.app.domain.LocalTemplateRecipeGenerator
import com.fridgefinish.app.domain.RecipeGeneratorInput
import com.fridgefinish.app.domain.RecipeSuggestion
import com.fridgefinish.app.domain.RecipeSuggestionPreferences
import com.fridgefinish.app.domain.SpiceLevel
import com.fridgefinish.app.domain.TemplateRecipeSuggestion
import com.fridgefinish.app.domain.cleanShoppingName
import com.fridgefinish.app.domain.containsWholeFoodTerm
import com.fridgefinish.app.domain.normalizeIngredientName
import com.fridgefinish.app.domain.missingItemsNotAlreadyInShop
import com.fridgefinish.app.domain.recipeIngredientMatchesFood
import com.fridgefinish.app.domain.isAgedLeftover
import com.fridgefinish.app.domain.isHighRiskFood
import com.fridgefinish.app.motion.FridgeFinishMotion
import com.fridgefinish.app.subscription.FeatureGateResult
import com.fridgefinish.app.subscription.FridgeFinishFeatureGate
import com.fridgefinish.app.subscription.FridgeFinishPlans
import com.fridgefinish.app.subscription.FridgeFinishSubscriptionState
import com.fridgefinish.app.subscription.PlusFeature
import com.fridgefinish.app.theme.AppearanceMode
import com.fridgefinish.app.theme.AppThemeStyle
import com.fridgefinish.app.theme.FridgeFinishColors
import com.fridgefinish.app.theme.FridgeFinishTheme
import com.fridgefinish.app.theme.ThemePreferences
import com.fridgefinish.app.theme.appThemeStyles
import com.fridgefinish.app.theme.previewColorScheme
import com.fridgefinish.app.theme.previewSemanticColors
import com.fridgefinish.app.ui.BarcodeLookupState
import com.fridgefinish.app.ui.BarcodeScannerScreen
import com.fridgefinish.app.ui.FridgeFinishUiState
import com.fridgefinish.app.ui.FridgeFinishViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FridgeFinishApp() }
    }
}

private enum class Screen(val label: String) {
    TODAY("Today"),
    FRIDGE("Storage"),
    FREEZER("Storage"),
    PANTRY("Storage"),
    GARAGE_FREEZER("Storage"),
    MINI_FRIDGE("Storage"),
    OTHER("Storage"),
    RECIPES("Recipes"),
    RESTOCK("Shop"),
    SETTINGS("Info"),
    PLUS("Plus"),
    SCAN("Scan"),
    RECEIPT_SCAN("Receipt")
}

private val storageScreens = listOf(
    Screen.FRIDGE,
    Screen.FREEZER,
    Screen.PANTRY,
    Screen.GARAGE_FREEZER,
    Screen.MINI_FRIDGE,
    Screen.OTHER
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun FridgeFinishApp(viewModel: FridgeFinishViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val barcodeLookup by viewModel.barcodeLookup.collectAsState()
    val selectedThemeStyle by ThemePreferences.selectedThemeStyle(context).collectAsState(AppThemeStyle.OriginalFresh)
    val selectedAppearanceMode by ThemePreferences.selectedAppearanceMode(context).collectAsState(AppearanceMode.FollowSystem)
    var previewThemeStyle by rememberSaveable { mutableStateOf<AppThemeStyle?>(null) }
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var editingFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var addingFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var useItFirstFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var upgradePrompt by remember { mutableStateOf<FeatureGateResult.UpgradeRequired?>(null) }
    var shopMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val activeThemeStyle = previewThemeStyle ?: selectedThemeStyle

    fun showGate(result: FeatureGateResult): Boolean {
        return when (result) {
            FeatureGateResult.Allowed -> true
            is FeatureGateResult.UpgradeRequired -> {
                upgradePrompt = result
                false
            }
        }
    }

    fun addFoodIfAllowed(item: FoodItemEntity) {
        if (showGate(FridgeFinishFeatureGate.gateAddItem(uiState.subscription))) {
            addingFood = item
        }
    }

    fun finishFoodWithUndo(item: FoodItemEntity, addToRestock: Boolean = true) {
        viewModel.markFinished(item, addToRestock = addToRestock)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "${item.name.ifBlank { "Food item" }.cleanShoppingName()} marked finished",
                actionLabel = "Undo",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.saveFood(item.copy(isFinished = false, finishedDate = null))
            }
        }
    }

    fun showSavedMessage(name: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "${name.ifBlank { "Food item" }.cleanShoppingName()} saved",
                withDismissAction = true
            )
        }
    }

    val motionTarget = when {
        editingFood != null || addingFood != null -> 100
        useItFirstFood != null -> 90
        else -> screen.ordinal
    }

    FridgeFinishTheme(style = activeThemeStyle, appearanceMode = selectedAppearanceMode) {
        upgradePrompt?.let { prompt ->
            UpgradePromptDialog(
                prompt = prompt,
                onDismiss = { upgradePrompt = null },
                onOpenPlus = {
                    upgradePrompt = null
                    screen = Screen.PLUS
                }
            )
        }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (editingFood != null || addingFood != null) {
                            Text("Food item")
                        } else if (useItFirstFood != null) {
                            Text("Use this first")
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fridge Finish")
                                if (screen !in storageScreens) {
                                    PlusStatusChip(uiState.subscription)
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (editingFood == null && addingFood == null && useItFirstFood == null && screen !in listOf(Screen.RESTOCK, Screen.SETTINGS, Screen.PLUS, Screen.SCAN, Screen.RECEIPT_SCAN)) {
                    FloatingActionButton(onClick = { addFoodIfAllowed(viewModel.presetFood(FoodCategory.OTHER)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add food")
                    }
                }
            },
            bottomBar = {
                if (editingFood == null && addingFood == null && useItFirstFood == null) {
                    NavigationBar(containerColor = FridgeFinishColors.current.bottomNavContainer) {
                        listOf(Screen.TODAY, Screen.FRIDGE, Screen.RECIPES, Screen.RESTOCK, Screen.SETTINGS).forEach { item ->
                            NavigationBarItem(
                                selected = screen == item || (item == Screen.FRIDGE && screen in storageScreens),
                                onClick = { screen = item },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FridgeFinishColors.current.onBottomNavSelected,
                                    selectedTextColor = FridgeFinishColors.current.onBottomNavSelected,
                                    indicatorColor = FridgeFinishColors.current.bottomNavSelected,
                                    unselectedIconColor = FridgeFinishColors.current.bottomNavUnselected,
                                    unselectedTextColor = FridgeFinishColors.current.bottomNavUnselected
                                ),
                                icon = {
                        Icon(
                            when (item) {
                                Screen.TODAY -> Icons.Default.Home
                                Screen.RESTOCK -> Icons.Default.ShoppingCart
                                Screen.SETTINGS -> Icons.Default.Info
                                Screen.RECIPES -> Icons.Default.Restaurant
                                Screen.SCAN -> Icons.Default.Search
                                else -> Icons.Default.Kitchen
                            },
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = motionTarget,
                    transitionSpec = { FridgeFinishMotion.screenTransform(targetState >= initialState) },
                    label = "screen-transition"
                ) { animatedTarget ->
                    androidx.compose.runtime.key(animatedTarget) {
                    when {
                        useItFirstFood != null -> UseItFirstScreen(
                        item = useItFirstFood!!,
                        uiState = uiState,
                        onBack = { useItFirstFood = null },
                        onCookRecipe = { idea ->
                            shopMessage = "Opened ${idea.title}. Review dates before cooking."
                        },
                        onSaveRecipe = { idea ->
                            shopMessage = "Saved ${idea.title} for later."
                        },
                        onMarkItemUsed = {
                            viewModel.markFinished(useItFirstFood!!, addToRestock = false)
                            useItFirstFood = null
                        },
                        onMarkRecipeUsed = { idea ->
                            uiState.activeFoods
                                .filter { it.id in idea.usedFoodIds }
                                .forEach { viewModel.markFinished(it, addToRestock = false) }
                            useItFirstFood = null
                        },
                        onFreezeInstead = {
                            val item = useItFirstFood!!
                            viewModel.saveFood(
                                item.copy(
                                    location = FoodLocation.FREEZER,
                                    expirationDate = maxOf(item.expirationDate, LocalDate.now().plusMonths(3)),
                                    notes = listOfNotNull(item.notes, "Moved to freezer from Use It First. Check before eating.").joinToString("\n")
                                )
                            )
                            useItFirstFood = null
                        },
                        onAddMissingToRestock = { idea ->
                            missingItemsNotAlreadyInShop(
                                missingItems = idea.missing,
                                openShopItems = uiState.restock.filterNot { it.isPurchased }.map { it.name }
                            ).itemsToAdd.forEach { missing ->
                                viewModel.saveRestock(
                                    RestockItemEntity(
                                        name = missing,
                                        category = missing.inferShoppingCategory(),
                                        note = "For ${idea.title}. ${idea.servings}.",
                                        quantity = "Recipe item"
                                    )
                                )
                            }
                            useItFirstFood = null
                            screen = Screen.RESTOCK
                        }
                        )
                        editingFood != null || addingFood != null -> FoodEditorScreen(
                        initial = editingFood ?: addingFood!!,
                        subscriptionState = uiState.subscription,
                        onOpenPlus = {
                            editingFood = null
                            addingFood = null
                            screen = Screen.PLUS
                        },
                        onSave = {
                            viewModel.saveFood(it)
                            showSavedMessage(it.name)
                            editingFood = null
                            addingFood = null
                            screen = Screen.TODAY
                        },
                        onCancel = {
                            editingFood = null
                            addingFood = null
                        },
                        onScanBarcode = {
                            viewModel.clearBarcodeLookup()
                            editingFood = null
                            addingFood = null
                            screen = Screen.SCAN
                        }
                        )
                        screen == Screen.TODAY -> TodayScreen(
                        uiState = uiState,
                        onAddFood = { addFoodIfAllowed(viewModel.presetFood(FoodCategory.OTHER)) },
                        onAddLeftovers = { addFoodIfAllowed(viewModel.presetFood(FoodCategory.LEFTOVERS)) },
                        onAddRestock = {
                            if (showGate(FridgeFinishFeatureGate.gateFeature(PlusFeature.SmartGroceryList, uiState.subscription))) {
                                screen = Screen.RESTOCK
                            }
                        },
                        onScanBarcode = {
                            viewModel.clearBarcodeLookup()
                            screen = Screen.SCAN
                        },
                        onEdit = { editingFood = it },
                        onFinish = { finishFoodWithUndo(it) },
                        onMarkLeftoverUsed = { finishFoodWithUndo(it, addToRestock = false) },
                        onUseFirst = { useItFirstFood = it }
                        )
                        screen in storageScreens -> FoodListScreen(
                        location = screen.toLocation(),
                        uiState = uiState,
                        onLocationSelected = { location ->
                            if (location == FoodLocation.FRIDGE || showGate(FridgeFinishFeatureGate.gateFeature(PlusFeature.MultipleStorageLocations, uiState.subscription))) {
                                screen = location.toScreen()
                            }
                        },
                        onOpenPlus = { screen = Screen.PLUS },
                        onFinish = { finishFoodWithUndo(it) },
                        onDelete = viewModel::deleteFood,
                        onUseFirst = { useItFirstFood = it }
                        ) { editingFood = it }
                        screen == Screen.RECIPES -> RecipeIdeasScreen(
                        uiState = uiState,
                        onOpenPlus = { screen = Screen.PLUS },
                        onUseItFirst = { useItFirstFood = it },
                        onSaveRecipeFeedback = viewModel::saveRecipeFeedback,
                        onMarkIngredientsUsed = { idea ->
                            uiState.activeFoods
                                .filter { it.id in idea.usedFoodIds }
                                .forEach { finishFoodWithUndo(it, addToRestock = false) }
                        },
                        onAddMissingToRestock = { idea ->
                            val mergeResult = missingItemsNotAlreadyInShop(
                                missingItems = idea.missing,
                                openShopItems = uiState.restock
                                .filterNot { it.isPurchased }
                                    .map { it.name }
                            )
                            val newItems = mergeResult.itemsToAdd
                            newItems.forEach { missing ->
                                viewModel.saveRestock(
                                    RestockItemEntity(
                                        name = missing,
                                        category = missing.inferShoppingCategory(),
                                        note = "For ${idea.title}. ${idea.servings}.",
                                        quantity = "Recipe item"
                                    )
                                )
                            }
                            val skippedCount = mergeResult.skippedCount
                            shopMessage = when {
                                newItems.isNotEmpty() && skippedCount > 0 -> "Added ${newItems.size} item${if (newItems.size == 1) "" else "s"}. $skippedCount already on your list."
                                newItems.isNotEmpty() -> "Added ${newItems.size} item${if (newItems.size == 1) "" else "s"} for ${idea.title}."
                                else -> "Those items are already on your list."
                            }
                            screen = Screen.RESTOCK
                        }
                        )
                        screen == Screen.RESTOCK -> RestockScreen(
                        uiState = uiState,
                        message = shopMessage,
                        onMessageDismissed = { shopMessage = null },
                        onOpenPlus = { screen = Screen.PLUS },
                        onSave = {
                            shopMessage = null
                            viewModel.saveRestock(it)
                        },
                        onToggle = viewModel::toggleRestock,
                        onDelete = viewModel::deleteRestock
                        )
                        screen == Screen.SETTINGS -> SettingsScreen(
                        subscriptionState = uiState.subscription,
                        recipeFeedback = uiState.recipeFeedback,
                        hiddenRecipeTitles = uiState.hiddenRecipeTitles,
                        dislikedIngredients = uiState.dislikedIngredients,
                        selectedThemeStyle = selectedThemeStyle,
                        previewThemeStyle = previewThemeStyle,
                        selectedAppearanceMode = selectedAppearanceMode,
                        onApplyThemeStyle = { style ->
                            previewThemeStyle = null
                            coroutineScope.launch { ThemePreferences.setThemeStyle(context, style) }
                        },
                        onPreviewThemeStyle = { style -> previewThemeStyle = style },
                        onClearThemePreview = { previewThemeStyle = null },
                        onAppearanceModeSelected = { mode ->
                            coroutineScope.launch { ThemePreferences.setAppearanceMode(context, mode) }
                        },
                        onOpenPlus = { screen = Screen.PLUS },
                        onUnlockBetaPremium = viewModel::activateBetaPremium,
                        onRestorePurchases = viewModel::restorePlusPurchases,
                        onAddSamples = viewModel::addSampleData,
                        onResetRecipeFeedback = viewModel::clearRecipeFeedback,
                        onUnhideRecipe = viewModel::unhideRecipe,
                        onClearDislikedIngredient = { ingredient ->
                            uiState.recipeFeedback
                                .filter { feedback ->
                                    feedback.action == RecipeFeedbackAction.NOT_MY_TASTE &&
                                        feedback.ingredients.split(",").any { it.trim().equals(ingredient, ignoreCase = true) }
                                }
                                .forEach(viewModel::deleteRecipeFeedback)
                        }
                        )
                        screen == Screen.PLUS -> FridgeFinishPlusScreen(
                        subscriptionState = uiState.subscription,
                        onUnlockBetaPremium = viewModel::activateBetaPremium,
                        onStartPurchase = viewModel::startPlusPurchase,
                        onBack = { screen = Screen.SETTINGS }
                        )
                        screen == Screen.SCAN -> BarcodeScannerScreen(
                        lookupState = barcodeLookup,
                        onBarcodeScanned = viewModel::lookupBarcode,
                        onUseProduct = { product ->
                            addFoodIfAllowed(viewModel.presetFood(product.category).copy(
                                name = product.name,
                                barcode = product.barcode,
                                imageUri = product.imageUrl,
                                notes = "Added from barcode scan. Check the package date before saving."
                            ))
                            viewModel.clearBarcodeLookup()
                        },
                        onUseBarcodeManually = { barcode ->
                            addFoodIfAllowed(viewModel.presetFood(FoodCategory.OTHER).copy(
                                barcode = barcode,
                                notes = "Barcode: $barcode. Check the package date before saving."
                            ))
                            viewModel.clearBarcodeLookup()
                        },
                        onScanAnother = viewModel::clearBarcodeLookup,
                        onScanReceipt = {
                            viewModel.clearBarcodeLookup()
                            screen = Screen.RECEIPT_SCAN
                        },
                        onCancel = {
                            viewModel.clearBarcodeLookup()
                            screen = Screen.TODAY
                        }
                        )
                        screen == Screen.RECEIPT_SCAN -> ReceiptImportScreen(
                        onImport = { candidates ->
                            candidates.forEach { candidate ->
                                viewModel.saveFood(
                                    FoodItemEntity(
                                        name = candidate.name,
                                        category = candidate.category,
                                        location = when (candidate.category) {
                                            FoodCategory.FROZEN -> FoodLocation.FREEZER
                                            FoodCategory.PANTRY -> FoodLocation.PANTRY
                                            else -> FoodLocation.FRIDGE
                                        },
                                        expirationDate = candidate.expirationDate,
                                        reminderDaysBefore = candidate.reminderDaysBefore,
                                        notes = "Imported from receipt scan. Confirm the date on the package."
                                    )
                                )
                            }
                            screen = Screen.FRIDGE
                        },
                        onCancel = { screen = Screen.SCAN }
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    uiState: FridgeFinishUiState,
    onAddFood: () -> Unit,
    onAddLeftovers: () -> Unit,
    onAddRestock: () -> Unit,
    onScanBarcode: () -> Unit,
    onEdit: (FoodItemEntity) -> Unit,
    onFinish: (FoodItemEntity) -> Unit,
    onMarkLeftoverUsed: (FoodItemEntity) -> Unit,
    onUseFirst: (FoodItemEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TodayHeroCard(uiState)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CountCard("Past date", uiState.expiredCount, FreshnessStatus.EXPIRED, Modifier.weight(1f))
                CountCard("Today", uiState.expiresTodayCount, FreshnessStatus.EXPIRES_TODAY, Modifier.weight(1f))
                CountCard("Eat soon", uiState.eatSoonCount, FreshnessStatus.EAT_SOON, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onScanBarcode, modifier = Modifier.weight(1f)) { Text("Scan barcode") }
                Button(onClick = onAddFood, modifier = Modifier.weight(1f)) { Text("Add food") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onAddLeftovers, modifier = Modifier.weight(1f)) { Text("Add leftovers") }
                OutlinedButton(onClick = onAddRestock, modifier = Modifier.weight(1f)) { Text("Restock") }
            }
        }
        item {
            LeftoverRescueCard(
                leftovers = uiState.leftoversExpiringSoon,
                uiState = uiState,
                onAddLeftovers = onAddLeftovers,
                onRescue = onUseFirst,
                onMarkUsed = onMarkLeftoverUsed
            )
        }
        section("Eat first", uiState.topPriority.filter { uiState.statusOf(it) in listOf(FreshnessStatus.EXPIRED, FreshnessStatus.EAT_SOON) }, uiState, onEdit, onFinish, onUseFirst)
        section("Expires today", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.EXPIRES_TODAY }, uiState, onEdit, onFinish, onUseFirst)
        section("Coming up", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.FRESH }.take(5), uiState, onEdit, onFinish, onUseFirst)
        item {
            RestockPreviewCard(uiState)
        }
    }
}

@Composable
private fun TodayHeroCard(uiState: FridgeFinishUiState) {
    val nextItem = uiState.topPriority.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Eat first today", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                nextItem?.name?.takeIf { it.isNotBlank() } ?: "Your fridge is quiet right now",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                nextItem?.let { "${FreshnessCalculator.daysRemainingText(it.expirationDate)} - ${it.location.label}" }
                    ?: "Add food or scan a barcode to start getting useful reminders.",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Dates are reminders, not safety guarantees. Check before eating.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeftoverRescueCard(
    leftovers: List<FoodItemEntity>,
    uiState: FridgeFinishUiState,
    onAddLeftovers: () -> Unit,
    onRescue: (FoodItemEntity) -> Unit,
    onMarkUsed: (FoodItemEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Rescue leftovers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        if (leftovers.isEmpty()) "Track cooked food here before it gets forgotten." else "${leftovers.size} leftover item${if (leftovers.size == 1) "" else "s"} to use soon.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(onClick = onAddLeftovers) { Text("Add") }
            }
            if (leftovers.isEmpty()) {
                Text(
                    "Save the date cooked, amount, and use-by date so Fridge Finish can suggest wraps, bowls, soups, scrambles, and other easy rescues.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                leftovers.take(3).forEach { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name.cleanShoppingName(), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${uiState.statusOf(item).label} - ${FreshnessCalculator.daysRemainingText(item.expirationDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                item.sourceMeal?.takeIf { it.isNotBlank() }?.let {
                                    Text("From $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = { onMarkUsed(item) }) { Text("Used") }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            leftoverRescueIdeaLabels(item).take(4).forEach { idea ->
                                AssistChip(onClick = { onRescue(item) }, label = { Text(idea, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestockPreviewCard(uiState: FridgeFinishUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Restock list", style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text("${uiState.restockOpen.size} open") })
            }
            if (uiState.restockOpen.isEmpty()) {
                Text("Nothing to restock right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                uiState.restockOpen.take(4).forEach { item ->
                    Text("${item.name}${item.quantity?.let { q -> " - $q" }.orEmpty()}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    foodItems: List<FoodItemEntity>,
    uiState: FridgeFinishUiState,
    onEdit: (FoodItemEntity) -> Unit,
    onFinish: (FoodItemEntity) -> Unit,
    onUseFirst: (FoodItemEntity) -> Unit
) {
    item {
        SectionHeader(title, foodItems.size)
        if (foodItems.isEmpty()) {
            EmptySectionCard(title)
        }
    }
    items(foodItems, key = { "$title-${it.id}" }) { item ->
        FoodCard(item, uiState.statusOf(item), onEdit, onFinish, onUseFirst, onDelete = null, modifier = Modifier.animateItem())
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        AnimatedContent(targetState = count, label = "section-count") { animatedCount ->
            Text("$animatedCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptySectionCard(title: String) {
    Text(
        when (title) {
            "Eat first" -> "No urgent food right now."
            "Expires today" -> "Nothing expires today."
            "Coming up" -> "Add more dates to see what is coming up."
            else -> "No items here."
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CountCard(label: String, count: Int, status: FreshnessStatus, modifier: Modifier = Modifier) {
    val countColor by animateColorAsState(statusContentColor(status), animationSpec = tween(FridgeFinishMotion.Standard), label = "count-color")
    val containerColor by animateColorAsState(statusContainerColor(status), animationSpec = tween(FridgeFinishMotion.Standard), label = "count-container")
    Card(modifier = modifier.animateContentSize(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AnimatedContent(targetState = count, label = "dashboard-count") { animatedCount ->
                Text(animatedCount.toString(), style = MaterialTheme.typography.headlineMedium, color = countColor)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = countColor)
        }
    }
}

@Composable
private fun FoodListScreen(
    location: FoodLocation,
    uiState: FridgeFinishUiState,
    onLocationSelected: (FoodLocation) -> Unit,
    onOpenPlus: () -> Unit,
    onFinish: (FoodItemEntity) -> Unit,
    onDelete: (FoodItemEntity) -> Unit,
    onUseFirst: (FoodItemEntity) -> Unit,
    onEdit: (FoodItemEntity) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var status by rememberSaveable { mutableStateOf("All") }
    val foods = uiState.activeFoods
        .filter { it.location == location }
        .filter { it.name.contains(search, ignoreCase = true) }
        .filter { category == "All" || it.category.label == category }
        .filter { status == "All" || uiState.statusOf(it).label == status }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Storage", style = MaterialTheme.typography.titleLarge)
                    Text(
                        location.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PlusStatusChip(uiState.subscription)
            }
            if (uiState.subscription.isPlus) {
                StorageAccessRow(uiState.subscription)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FoodLocation.entries.forEach { option ->
                    val locked = option != FoodLocation.FRIDGE && !uiState.subscription.isPlus
                    FilterChip(
                        selected = location == option,
                        onClick = { onLocationSelected(option) },
                        modifier = Modifier.widthIn(min = if (option == FoodLocation.GARAGE_FREEZER) 132.dp else 92.dp),
                        label = {
                            Text(
                                if (locked) "${option.label} Plus" else option.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            if (!uiState.subscription.isPlus) {
                Text("Free keeps one storage location. Plus adds freezer, pantry, garage freezer, mini fridge, and other.")
                TextButton(onClick = onOpenPlus) { Text("View Plus storage") }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search food") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleMenu("Category", category, listOf("All") + FoodCategory.entries.map { it.label }) { category = it }
                SimpleMenu("Status", status, listOf("All") + FreshnessStatus.entries.map { it.label }) { status = it }
            }
        }
        item {
            AnimatedVisibility(
                visible = foods.isEmpty(),
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                EmptyStorageCard(location, uiState.subscription)
            }
        }
        items(foods, key = { it.id }) { item ->
            FoodCard(item, uiState.statusOf(item), onEdit, onFinish, onUseFirst, onDelete, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun PlusStatusChip(subscriptionState: FridgeFinishSubscriptionState) {
    val isPremium = subscriptionState.isPlus
    AssistChip(
        onClick = {},
        leadingIcon = if (isPremium) {
            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        label = { Text(if (isPremium) subscriptionState.planLabel else "Free") },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (isPremium) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (isPremium) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconContentColor = if (isPremium) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun StorageAccessRow(subscriptionState: FridgeFinishSubscriptionState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            subscriptionState.premiumAccessLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "All storage locations unlocked",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStorageCard(location: FoodLocation, subscriptionState: FridgeFinishSubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nothing in ${location.label} yet", style = MaterialTheme.typography.titleMedium)
            Text(
                if (subscriptionState.isPlus) {
                    "Use + to add an item here, or scan a barcode from the Scan tab."
                } else {
                    "Use + to add your first fridge item."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodCard(
    item: FoodItemEntity,
    status: FreshnessStatus,
    onEdit: (FoodItemEntity) -> Unit,
    onFinish: (FoodItemEntity) -> Unit,
    onUseFirst: (FoodItemEntity) -> Unit,
    onDelete: ((FoodItemEntity) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var finishing by remember(item.id) { mutableStateOf(false) }
    val finishCheckScale by animateFloatAsState(
        targetValue = if (finishing) 1.18f else 1f,
        animationSpec = tween(FridgeFinishMotion.Quick),
        label = "finish-check-scale"
    )
    Card(modifier = modifier.fillMaxWidth().animateContentSize(animationSpec = tween(FridgeFinishMotion.Standard))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                item.imageUri?.takeIf { it.isNotBlank() }?.let { imageUri ->
                    AsyncImage(
                        model = imageUri,
                        contentDescription = item.name,
                        modifier = Modifier.size(56.dp).padding(end = 10.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.name.ifBlank { "Unnamed food" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${item.location.label} - ${item.category.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status)
            }
            AnimatedVisibility(
                visible = item.itemState != FoodItemState.FRESH || item.location == FoodLocation.FREEZER,
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(item.effectiveItemState().label) })
                    if (item.itemState == FoodItemState.SPOILED) {
                        AssistChip(onClick = {}, label = { Text("Dispose") })
                    } else if (item.itemState == FoodItemState.QUESTIONABLE) {
                        AssistChip(onClick = {}, label = { Text("Use only if still safe") })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Expires ${item.expirationDate}", style = MaterialTheme.typography.bodyMedium)
                    Text(FreshnessCalculator.daysRemainingText(item.expirationDate), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                item.quantity?.takeIf { it.isNotBlank() }?.let {
                    Text("Qty $it ${item.unit.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (status in listOf(FreshnessStatus.EXPIRED, FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON)) {
                    Button(onClick = { onUseFirst(item) }, modifier = Modifier.weight(1f)) {
                        Text("Use first")
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (!finishing) {
                            finishing = true
                            onFinish(item)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size((18 * finishCheckScale).dp))
                    Text("Finished")
                }
                OutlinedButton(onClick = { onEdit(item) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Edit")
                }
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
        }
    }
}

private fun FoodLocation.toScreen(): Screen = when (this) {
    FoodLocation.FRIDGE -> Screen.FRIDGE
    FoodLocation.FREEZER -> Screen.FREEZER
    FoodLocation.PANTRY -> Screen.PANTRY
    FoodLocation.GARAGE_FREEZER -> Screen.GARAGE_FREEZER
    FoodLocation.MINI_FRIDGE -> Screen.MINI_FRIDGE
    FoodLocation.OTHER -> Screen.OTHER
}

private fun Screen.toLocation(): FoodLocation = when (this) {
    Screen.FREEZER -> FoodLocation.FREEZER
    Screen.PANTRY -> FoodLocation.PANTRY
    Screen.GARAGE_FREEZER -> FoodLocation.GARAGE_FREEZER
    Screen.MINI_FRIDGE -> FoodLocation.MINI_FRIDGE
    Screen.OTHER -> FoodLocation.OTHER
    else -> FoodLocation.FRIDGE
}

private data class RecipeIdea(
    val title: String,
    val minutes: Int,
    val servings: String,
    val portions: List<String>,
    val have: List<String>,
    val expiringSoon: List<String>,
    val optional: List<String>,
    val missing: List<String>,
    val urgentCount: Int,
    val matchScore: Int,
    val scoreReasons: List<String>,
    val whySuggested: String,
    val difficulty: String,
    val filters: Set<RecipeFilter>,
    val tools: Set<CookingTool>,
    val safetyNote: String,
    val usedFoodIds: List<Long>,
    val note: String,
    val steps: String
)

private enum class RecipeSortOption(val label: String) {
    BEST_MATCH("Best match"),
    EXPIRING_SOON("Expiring soon"),
    FEWEST_MISSING("Fewest missing ingredients"),
    FASTEST("Fastest"),
    MOST_INGREDIENTS_USED("Most ingredients used")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UseItFirstScreen(
    item: FoodItemEntity,
    uiState: FridgeFinishUiState,
    onBack: () -> Unit,
    onCookRecipe: (RecipeIdea) -> Unit,
    onSaveRecipe: (RecipeIdea) -> Unit,
    onMarkItemUsed: () -> Unit,
    onMarkRecipeUsed: (RecipeIdea) -> Unit,
    onFreezeInstead: () -> Unit,
    onAddMissingToRestock: (RecipeIdea) -> Unit
) {
    val status = uiState.statusOf(item)
    val ideas = remember(item, uiState.activeFoods, uiState.recipes, uiState.recipeIngredients) {
        buildUseItFirstRecipeIdeas(item, uiState)
    }
    val quickIdeas = remember(item) { quickUseItFirstIdeas(item) }
    var selectedRecipe by remember { mutableStateOf<RecipeIdea?>(null) }
    var savedTitles by remember { mutableStateOf(emptySet<String>()) }
    var hiddenTitles by remember { mutableStateOf(emptySet<String>()) }
    var hiddenMissingIngredients by remember { mutableStateOf(emptySet<String>()) }
    var message by remember { mutableStateOf<String?>(null) }
    val visibleIdeas = ideas
        .filterNot { it.title in hiddenTitles }
        .filterNot { idea -> idea.missing.any { normalizeIngredientName(it) in hiddenMissingIngredients } }

    selectedRecipe?.let { idea ->
        RecipeInfoDialog(
            idea = idea,
            onDismiss = { selectedRecipe = null },
            onAddMissingToRestock = {
                selectedRecipe = null
                onAddMissingToRestock(idea)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.name.cleanShoppingName(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(status.label) })
                        AssistChip(onClick = {}, label = { Text(FreshnessCalculator.daysRemainingText(item.expirationDate)) })
                        item.quantity?.takeIf { it.isNotBlank() }?.let { quantity ->
                            AssistChip(onClick = {}, label = { Text("Qty $quantity ${item.unit.orEmpty()}".trim()) })
                        }
                    }
                    Text(
                        "Recipes below are centered on this item first, then ranked by how many other foods you already have.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Dates are reminders, not safety guarantees. Check before eating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        message?.let { current ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(current, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick ideas", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickIdeas.forEach { idea ->
                            AssistChip(onClick = {}, label = { Text(idea) })
                        }
                    }
                    if (canSuggestFreezing(item)) {
                        Text("If you cannot use it today, freezing can buy time for many foods. Label it and check it later.", style = MaterialTheme.typography.bodySmall)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onMarkItemUsed) { Text("Mark item used") }
                        if (canSuggestFreezing(item)) {
                            OutlinedButton(onClick = onFreezeInstead) { Text("Freeze it instead") }
                        }
                        OutlinedButton(onClick = onBack) { Text("Dismiss") }
                    }
                }
            }
        }
        item {
            SectionHeader("Suggested meals", visibleIdeas.size)
            if (visibleIdeas.isEmpty()) {
                EmptyRecipeCard("No matching meal template yet. Use a quick idea above, or add a pantry staple like rice, tortillas, pasta, eggs, or broth.")
            }
        }
        items(visibleIdeas, key = { "use-first-${item.id}-${it.title}" }) { idea ->
            AnimatedVisibility(
                visible = true,
                enter = FridgeFinishMotion.listItemEnter(),
                exit = FridgeFinishMotion.listItemExit(),
                modifier = Modifier.animateItem()
            ) {
            RecipeIdeaCard(
                idea = idea,
                isSaved = idea.title in savedTitles,
                onCookThis = {
                    selectedRecipe = idea
                    onCookRecipe(idea)
                },
                onSaveRecipe = {
                    savedTitles = savedTitles + idea.title
                    message = "Saved ${idea.title} for later."
                    onSaveRecipe(idea)
                },
                onHide = {
                    hiddenTitles = hiddenTitles + idea.title
                    message = "Dismissed ${idea.title}."
                },
                onFeedback = { action ->
                    message = idea.feedbackMessage(action)
                },
                onMarkIngredientsUsed = { onMarkRecipeUsed(idea) },
                onAddMissingToRestock = onAddMissingToRestock,
                onReplaceMissingIngredient = { _, missing ->
                    message = "Try replacing ${missing.cleanShoppingName()} with a similar item you already have, then review the recipe."
                },
                onHideMissingIngredient = { missing ->
                    hiddenMissingIngredients = hiddenMissingIngredients + normalizeIngredientName(missing)
                    message = "Hid recipes missing ${missing.cleanShoppingName()}."
                },
                onMoreInfo = { selectedRecipe = idea }
            )
            }
        }
    }
}

@Composable
private fun RecipeIdeasScreen(
    uiState: FridgeFinishUiState,
    onOpenPlus: () -> Unit,
    onUseItFirst: (FoodItemEntity) -> Unit,
    onSaveRecipeFeedback: (RecipeFeedbackEntity) -> Unit,
    onMarkIngredientsUsed: (RecipeIdea) -> Unit,
    onAddMissingToRestock: (RecipeIdea) -> Unit
) {
    if (!uiState.subscription.isPlus) {
        PlusLockedScreen(
            title = "Recipe ideas are in Plus",
            body = "Fridge Finish Plus suggests recipe ideas from ingredients that are expiring soon. Your basic expiration list stays free.",
            onOpenPlus = onOpenPlus
        )
        return
    }
    var selectedFilters by remember { mutableStateOf(emptySet<RecipeFilter>()) }
    var selectedTools by remember { mutableStateOf(emptySet<CookingTool>()) }
    var maxMinutes by remember { mutableStateOf<Int?>(null) }
    var hiddenTitles by remember { mutableStateOf(emptySet<String>()) }
    var hiddenMissingIngredients by remember { mutableStateOf(emptySet<String>()) }
    var savedTitles by remember { mutableStateOf(emptySet<String>()) }
    var selectedSort by remember { mutableStateOf(RecipeSortOption.BEST_MATCH) }
    var assumePantryStaples by rememberSaveable { mutableStateOf(true) }
    var includeExpiredItems by rememberSaveable { mutableStateOf(false) }
    var avoidIngredientsText by rememberSaveable { mutableStateOf("") }
    var favoriteIngredientsText by rememberSaveable { mutableStateOf("") }
    var dietaryStyle by rememberSaveable { mutableStateOf(DietaryStyle.NO_PREFERENCE) }
    var allergensToAvoid by remember { mutableStateOf(emptySet<RecipeAllergen>()) }
    var spiceLevel by rememberSaveable { mutableStateOf(SpiceLevel.MILD) }
    var cookingConfidence by rememberSaveable { mutableStateOf(CookingConfidence.COMFORTABLE) }
    var recipeMessage by remember { mutableStateOf<String?>(null) }
    val persistedHiddenTitles = uiState.hiddenRecipeTitles.toSet()
    val allHiddenTitles = hiddenTitles + persistedHiddenTitles
    val preferences = RecipeSuggestionPreferences(
        filters = selectedFilters,
        maxMinutes = maxMinutes,
        tools = selectedTools,
        blockedIngredients = avoidIngredientsText.toPreferenceTerms(),
        favoriteIngredients = favoriteIngredientsText.toPreferenceTerms(),
        dietaryStyle = dietaryStyle,
        allergensToAvoid = allergensToAvoid,
        spiceLevel = spiceLevel,
        cookingConfidence = cookingConfidence,
        recentlyHiddenTitles = allHiddenTitles,
        assumePantryStaples = assumePantryStaples,
        includeExpiredItems = includeExpiredItems
    )
    val ideas = remember(uiState.activeFoods, uiState.recipes, uiState.recipeIngredients, uiState.recipeFeedback, selectedFilters, selectedTools, maxMinutes, allHiddenTitles, hiddenMissingIngredients, selectedSort, assumePantryStaples, includeExpiredItems, avoidIngredientsText, favoriteIngredientsText, dietaryStyle, allergensToAvoid, spiceLevel, cookingConfidence) {
        buildRecipeIdeas(uiState, preferences)
            .filterNot { it.title in allHiddenTitles }
            .filterNot { idea -> idea.missing.any { normalizeIngredientName(it) in hiddenMissingIngredients } }
            .sortedByOption(selectedSort)
    }
    val expiringItems = uiState.activeFoods
        .filter { uiState.statusOf(it) in listOf(FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON, FreshnessStatus.EXPIRED) }
        .filterNot { it.itemState == FoodItemState.SPOILED }
        .take(12)
    val unsafeOnly = uiState.activeFoods.isNotEmpty() &&
        uiState.activeFoods.all { it.itemState == FoodItemState.SPOILED || it.effectiveItemState() == FoodItemState.EXPIRED }
    var selectedRecipe by remember { mutableStateOf<RecipeIdea?>(null) }
    selectedRecipe?.let { idea ->
        RecipeInfoDialog(
            idea = idea,
            onDismiss = { selectedRecipe = null },
            onAddMissingToRestock = {
                selectedRecipe = null
                onAddMissingToRestock(idea)
            },
            onMarkIngredientsUsed = {
                onMarkIngredientsUsed(idea)
                recipeMessage = "Marked matched ingredients used."
                selectedRecipe = null
            },
            onSaveRecipe = {
                savedTitles = savedTitles + idea.title
                onSaveRecipeFeedback(idea.toFeedback(RecipeFeedbackAction.SAVED))
                recipeMessage = "Saved ${idea.title} for later."
                selectedRecipe = null
            }
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            RecipeScreenHeader(
                ideaCount = ideas.size,
                expiringCount = expiringItems.size
            )
        }
        item {
            SmartRecipeFilters(
                selectedFilters = selectedFilters,
                onToggleFilter = { filter ->
                    selectedFilters = if (filter in selectedFilters) selectedFilters - filter else selectedFilters + filter
                },
                selectedSort = selectedSort,
                onUseExpiringFirst = {
                    selectedSort = if (selectedSort == RecipeSortOption.EXPIRING_SOON) RecipeSortOption.BEST_MATCH else RecipeSortOption.EXPIRING_SOON
                },
                onFewestMissing = {
                    selectedSort = if (selectedSort == RecipeSortOption.FEWEST_MISSING) RecipeSortOption.BEST_MATCH else RecipeSortOption.FEWEST_MISSING
                },
                quickMealSelected = maxMinutes == 15 && RecipeFilter.QUICK in selectedFilters,
                onToggleQuickMeal = {
                    if (maxMinutes == 15 && RecipeFilter.QUICK in selectedFilters) {
                        maxMinutes = null
                        selectedFilters = selectedFilters - RecipeFilter.QUICK
                    } else {
                        maxMinutes = 15
                        selectedFilters = selectedFilters + RecipeFilter.QUICK
                    }
                }
            )
        }
        item {
            ExpiringIngredientStrip(
                items = expiringItems,
                uiState = uiState,
                onUseItFirst = onUseItFirst
            )
        }
        recipeMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item {
            SectionHeader("Recipe suggestions", ideas.size)
            when {
                uiState.activeFoods.isEmpty() -> EmptyRecipeCard("Add groceries to get recipe ideas.")
                unsafeOnly -> EmptyRecipeCard("No safe recipe suggestions from current items.")
                ideas.isEmpty() -> EmptyRecipeCard("Try allowing pantry staples or adding one missing ingredient.")
            }
        }
        items(ideas, key = { "recipe-${it.title}" }) { idea ->
            AnimatedVisibility(
                visible = true,
                enter = FridgeFinishMotion.listItemEnter(),
                exit = FridgeFinishMotion.listItemExit(),
                modifier = Modifier.animateItem()
            ) {
            RecipeIdeaCard(
                idea = idea,
                isSaved = idea.title in savedTitles,
                onCookThis = {
                    selectedRecipe = idea
                    recipeMessage = "Opened ${idea.title}. Review dates before cooking."
                },
                onSaveRecipe = {
                    savedTitles = savedTitles + idea.title
                    onSaveRecipeFeedback(idea.toFeedback(RecipeFeedbackAction.SAVED))
                    recipeMessage = "Saved ${idea.title} for later."
                },
                onHide = {
                    hiddenTitles = hiddenTitles + idea.title
                    onSaveRecipeFeedback(idea.toFeedback(RecipeFeedbackAction.HIDDEN))
                    recipeMessage = "Hid ${idea.title}."
                },
                onFeedback = { action ->
                    onSaveRecipeFeedback(idea.toFeedback(action))
                    recipeMessage = idea.feedbackMessage(action)
                },
                onMarkIngredientsUsed = {
                    onMarkIngredientsUsed(idea)
                    recipeMessage = "Marked matched ingredients used."
                },
                onAddMissingToRestock = onAddMissingToRestock,
                onReplaceMissingIngredient = { _, missing ->
                    recipeMessage = "Try replacing ${missing.cleanShoppingName()} with a similar item you already have."
                },
                onHideMissingIngredient = { missing ->
                    hiddenMissingIngredients = hiddenMissingIngredients + normalizeIngredientName(missing)
                    recipeMessage = "Hid recipes missing ${missing.cleanShoppingName()}."
                },
                onMoreInfo = { selectedRecipe = idea }
            )
            }
        }
    }
}

@Composable
private fun RecipeScreenHeader(ideaCount: Int, expiringCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("What can I make?", style = MaterialTheme.typography.headlineMedium)
        Text(
            when {
                ideaCount > 0 && expiringCount > 0 -> "$ideaCount ideas, with $expiringCount eat-soon item${if (expiringCount == 1) "" else "s"} to prioritize."
                ideaCount > 0 -> "$ideaCount ideas from what you already track."
                else -> "Recipe ideas get better as you add groceries."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartRecipeFilters(
    selectedFilters: Set<RecipeFilter>,
    onToggleFilter: (RecipeFilter) -> Unit,
    selectedSort: RecipeSortOption,
    onUseExpiringFirst: () -> Unit,
    onFewestMissing: () -> Unit,
    quickMealSelected: Boolean,
    onToggleQuickMeal: () -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedSort == RecipeSortOption.EXPIRING_SOON,
            onClick = onUseExpiringFirst,
            label = { Text("Use expiring first") }
        )
        FilterChip(
            selected = quickMealSelected,
            onClick = onToggleQuickMeal,
            label = { Text("Quick meal") }
        )
        FilterChip(
            selected = RecipeFilter.DINNER in selectedFilters,
            onClick = { onToggleFilter(RecipeFilter.DINNER) },
            label = { Text("Dinner") }
        )
        FilterChip(
            selected = RecipeFilter.NO_COOK in selectedFilters,
            onClick = { onToggleFilter(RecipeFilter.NO_COOK) },
            label = { Text("No-cook") }
        )
        FilterChip(
            selected = selectedSort == RecipeSortOption.FEWEST_MISSING,
            onClick = onFewestMissing,
            label = { Text("Fewest missing items") }
        )
        FilterChip(
            selected = RecipeFilter.LEFTOVER_RESCUE in selectedFilters,
            onClick = { onToggleFilter(RecipeFilter.LEFTOVER_RESCUE) },
            label = { Text("Leftovers") }
        )
    }
}

@Composable
private fun ExpiringIngredientStrip(
    items: List<FoodItemEntity>,
    uiState: FridgeFinishUiState,
    onUseItFirst: (FoodItemEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Use soon", style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            EmptyRecipeCard("No eat-soon ingredients right now.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { "expiring-${it.id}" }) { item ->
                    ExpiringIngredientChip(
                        item = item,
                        status = uiState.statusOf(item),
                        onClick = { onUseItFirst(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiringIngredientChip(
    item: FoodItemEntity,
    status: FreshnessStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(min = 132.dp, max = 190.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name.cleanShoppingName(), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(status.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            item.quantity?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipePreferenceCard(
    selectedFilters: Set<RecipeFilter>,
    onToggleFilter: (RecipeFilter) -> Unit,
    maxMinutes: Int?,
    onMaxMinutesSelected: (Int?) -> Unit,
    selectedTools: Set<CookingTool>,
    onToggleTool: (CookingTool) -> Unit,
    selectedSort: RecipeSortOption,
    onSortSelected: (RecipeSortOption) -> Unit,
    assumePantryStaples: Boolean,
    onAssumePantryStaplesChanged: (Boolean) -> Unit,
    includeExpiredItems: Boolean,
    onIncludeExpiredItemsChanged: (Boolean) -> Unit,
    avoidIngredientsText: String,
    onAvoidIngredientsTextChanged: (String) -> Unit,
    favoriteIngredientsText: String,
    onFavoriteIngredientsTextChanged: (String) -> Unit,
    dietaryStyle: DietaryStyle,
    onDietaryStyleSelected: (DietaryStyle) -> Unit,
    allergensToAvoid: Set<RecipeAllergen>,
    onToggleAllergen: (RecipeAllergen) -> Unit,
    spiceLevel: SpiceLevel,
    onSpiceLevelSelected: (SpiceLevel) -> Unit,
    cookingConfidence: CookingConfidence,
    onCookingConfidenceSelected: (CookingConfidence) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recipe controls", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onClear) { Text("Clear") }
            }
            Text("Meal type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = filter in selectedFilters,
                        onClick = { onToggleFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
            Text("Available time", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Any", 5 to "5 min", 15 to "15 min", 30 to "30 min", 60 to "1 hour").forEach { (minutes, label) ->
                    FilterChip(
                        selected = maxMinutes == minutes,
                        onClick = { onMaxMinutesSelected(minutes) },
                        label = { Text(label) }
                    )
                }
            }
            Text("Cooking tools", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CookingTool.entries.forEach { tool ->
                    FilterChip(
                        selected = tool in selectedTools,
                        onClick = { onToggleTool(tool) },
                        label = { Text(tool.label) }
                    )
                }
            }
            Text("Sort by", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeSortOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedSort == option,
                        onClick = { onSortSelected(option) },
                        label = { Text(option.label) }
                    )
                }
            }
            Text("Taste preferences", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = avoidIngredientsText,
                onValueChange = onAvoidIngredientsTextChanged,
                label = { Text("Avoid ingredients") },
                placeholder = { Text("mushrooms, olives") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = favoriteIngredientsText,
                onValueChange = onFavoriteIngredientsTextChanged,
                label = { Text("Favorite ingredients") },
                placeholder = { Text("chicken, rice, cheddar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Dietary style", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DietaryStyle.entries.forEach { style ->
                    FilterChip(
                        selected = dietaryStyle == style,
                        onClick = { onDietaryStyleSelected(style) },
                        label = { Text(style.label) }
                    )
                }
            }
            Text("Allergens to avoid", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeAllergen.entries.forEach { allergen ->
                    FilterChip(
                        selected = allergen in allergensToAvoid,
                        onClick = { onToggleAllergen(allergen) },
                        label = { Text(allergen.label) }
                    )
                }
            }
            Text("Spice level", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiceLevel.entries.forEach { level ->
                    FilterChip(
                        selected = spiceLevel == level,
                        onClick = { onSpiceLevelSelected(level) },
                        label = { Text(level.label) }
                    )
                }
            }
            Text("Cooking confidence", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CookingConfidence.entries.forEach { confidence ->
                    FilterChip(
                        selected = cookingConfidence == confidence,
                        onClick = { onCookingConfidenceSelected(confidence) },
                        label = { Text(confidence.label) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAssumePantryStaplesChanged(!assumePantryStaples) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = assumePantryStaples,
                    onCheckedChange = onAssumePantryStaplesChanged
                )
                Column(Modifier.weight(1f)) {
                    Text("Assume basic pantry staples", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Salt, pepper, oil, butter, sugar, flour, and common spices will not count as missing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onIncludeExpiredItemsChanged(!includeExpiredItems) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = includeExpiredItems,
                    onCheckedChange = onIncludeExpiredItemsChanged
                )
                Column(Modifier.weight(1f)) {
                    Text("Include expired items in suggestions", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Off by default. Past-date foods only appear with caution copy when this is on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeHeroCard(
    ideas: List<RecipeIdea>,
    uiState: FridgeFinishUiState,
    onOpenRecipe: (RecipeIdea) -> Unit
) {
    val best = ideas.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cook from your fridge", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (best == null) {
                Text(
                    "Add food to unlock recipe ideas",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text("${uiState.activeFoods.size} tracked items available for matching.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                        .clickable { onOpenRecipe(best) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Best match", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(best.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${best.matchScore}% match - ${best.minutes} min - ${best.difficulty}")
                            Text(best.whySuggested, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onOpenRecipe(best) }) {
                            Text("View")
                        }
                    }
                }
            }
            Text(
                "Local recipe ideas. Check dates and use your judgment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RecipeMetricCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyRecipeCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MealPlanCard(ideas: List<RecipeIdea>, onOpenRecipe: (RecipeIdea) -> Unit) {
    val plan = ideas.take(3)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Use-up meal plan", style = MaterialTheme.typography.titleMedium)
            if (plan.isEmpty()) {
                Text("Add or scan more food to build a simple plan from what is already in the fridge.")
            } else {
                Text("A quick plan based on the best matches in your fridge right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                plan.forEachIndexed { index, idea ->
                    val slot = when (index) {
                        0 -> "Next meal"
                        1 -> "Later today"
                        else -> "Backup"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenRecipe(idea) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        AssistChip(onClick = {}, label = { Text(slot) })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(idea.title, style = MaterialTheme.typography.titleSmall)
                            Text("${idea.servings} - ${idea.minutes} min - ${if (idea.missing.isEmpty()) "ready now" else "${idea.missing.size} to buy"}")
                        }
                        TextButton(onClick = { onOpenRecipe(idea) }) {
                            Text("View")
                        }
                    }
                }
                Text("Use More info for portions and family-size notes.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeIdeaCard(
    idea: RecipeIdea,
    isSaved: Boolean,
    onCookThis: () -> Unit,
    onSaveRecipe: () -> Unit,
    onHide: () -> Unit,
    onFeedback: (RecipeFeedbackAction) -> Unit,
    onMarkIngredientsUsed: () -> Unit,
    onAddMissingToRestock: (RecipeIdea) -> Unit,
    onReplaceMissingIngredient: (RecipeIdea, String) -> Unit,
    onHideMissingIngredient: (String) -> Unit,
    onMoreInfo: () -> Unit
) {
    Card(Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(FridgeFinishMotion.Standard))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(idea.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(idea.whySuggested, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (idea.matchScore >= 80) FridgeFinishColors.current.success else FridgeFinishColors.current.chip
                ) {
                    Text(
                        "${idea.matchScore}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (idea.matchScore >= 80) FridgeFinishColors.current.onSuccess else FridgeFinishColors.current.onChip
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RecipeInfoPill(Icons.Default.Timer, "${idea.minutes} min")
                RecipeInfoPill(Icons.Default.Restaurant, idea.difficulty)
                RecipeInfoPill(Icons.Default.Check, "Uses ${idea.have.size + idea.optional.size}")
                RecipeInfoPill(Icons.Default.ShoppingCart, if (idea.missing.isEmpty()) "Nothing missing" else "Missing ${idea.missing.size}")
            }

            if (idea.expiringSoon.isNotEmpty()) {
                Text(
                    "Saves ${idea.expiringSoon.first().cleanShoppingName()} ${if (idea.urgentCount > 1) "and ${idea.urgentCount - 1} more" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(idea.missingSummary(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            RecipeBadgeRow(idea)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCookThis) { Text("Cook this") }
                OutlinedButton(onClick = onSaveRecipe, enabled = !isSaved) { Text(if (isSaved) "Saved" else "Save recipe") }
                TextButton(onClick = onMoreInfo) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Details")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onFeedback(RecipeFeedbackAction.COOKED) }) { Text("Cooked it") }
                TextButton(onClick = onHide) { Text("Hide") }
                TextButton(onClick = { onFeedback(RecipeFeedbackAction.BAD_SUGGESTION) }) { Text("Bad suggestion") }
            }
        }
    }
}

@Composable
private fun RecipeInfoPill(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeBadgeRow(idea: RecipeIdea) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (idea.minutes <= 15 || RecipeFilter.QUICK in idea.filters) {
            AssistChip(onClick = {}, label = { Text("Quick") })
        }
        if (RecipeFilter.NO_COOK in idea.filters || CookingTool.NO_TOOLS in idea.tools) {
            AssistChip(onClick = {}, label = { Text("No-cook") })
        }
        if (RecipeFilter.LEFTOVER_RESCUE in idea.filters || idea.have.any { it.contains("leftover", ignoreCase = true) }) {
            AssistChip(onClick = {}, label = { Text("Uses leftovers") })
        }
        if (RecipeFilter.ONE_PAN in idea.filters) {
            AssistChip(onClick = {}, label = { Text("One-pan") })
        }
        if (idea.matchScore >= 80) {
            AssistChip(onClick = {}, label = { Text("High match") })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeInfoDialog(
    idea: RecipeIdea,
    onDismiss: () -> Unit,
    onAddMissingToRestock: () -> Unit,
    onMarkIngredientsUsed: () -> Unit = {},
    onSaveRecipe: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSaveRecipe) { Text("Save recipe") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(idea.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${idea.matchScore}% match - ${idea.minutes} minutes - ${idea.difficulty} - ${idea.servings}")
                Text(idea.whySuggested)
                if (idea.scoreReasons.isNotEmpty()) {
                    Text("Why this score", style = MaterialTheme.typography.labelLarge)
                    idea.scoreReasons.forEach { reason -> Text(reason) }
                }
                Text(idea.note)
                RecipeIngredientGroup("You have", idea.have)
                RecipeIngredientGroup("Use soon", idea.expiringSoon)
                if (idea.optional.isNotEmpty()) {
                    RecipeIngredientGroup("Optional", idea.optional)
                }
                if (idea.missing.isNotEmpty()) {
                    RecipeIngredientGroup("Missing", idea.missing)
                }
                Text("Steps", style = MaterialTheme.typography.labelLarge)
                idea.detailSteps().forEachIndexed { index, step ->
                    Text("${index + 1}. $step")
                }
                Text("Substitutions", style = MaterialTheme.typography.labelLarge)
                idea.substitutionIdeas().forEach { substitution ->
                    Text(substitution)
                }
                Text("Portions", style = MaterialTheme.typography.labelLarge)
                idea.portions.forEach { portion ->
                    Text(portion)
                }
                Text("For a family: double the portions for 4+ servings, or add a simple side if you are short on one ingredient.", style = MaterialTheme.typography.bodySmall)
                Text(idea.safetyNote, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onMarkIngredientsUsed,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = idea.usedFoodIds.isNotEmpty()
                ) {
                    Text("Mark ingredients used")
                }
                if (idea.missing.isNotEmpty()) {
                    OutlinedButton(onClick = onAddMissingToRestock, modifier = Modifier.fillMaxWidth()) {
                        Text(idea.addMissingButtonLabel())
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeIngredientGroup(title: String, items: List<String>) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    if (items.isEmpty()) {
        Text("None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.distinct().forEach { item ->
                AssistChip(onClick = {}, label = { Text(item.cleanShoppingName(), maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
    }
}

private fun buildRecipeIdeas(
    uiState: FridgeFinishUiState,
    preferences: RecipeSuggestionPreferences = RecipeSuggestionPreferences()
): List<RecipeIdea> {
    val expiringItems = uiState.activeFoods.filter { item ->
        uiState.statusOf(item) in listOf(FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON, FreshnessStatus.EXPIRED)
    }
    val suggestions = LocalTemplateRecipeGenerator().generate(
        RecipeGeneratorInput(
            inventoryItems = uiState.activeFoods,
            expiringItems = expiringItems,
            userPreferences = preferences,
            availableTimeMinutes = preferences.maxMinutes,
            mealTypes = preferences.filters,
            cookingTools = preferences.tools,
            assumePantryStaples = preferences.assumePantryStaples,
            dietaryRestrictions = preferences.dietaryRestrictions,
            feedbackHistory = uiState.recipeFeedback,
            recipes = uiState.recipes,
            recipeIngredients = uiState.recipeIngredients
        )
    )
    return suggestions.map { suggestion ->
        RecipeIdea(
            title = suggestion.title,
            minutes = suggestion.estimatedTimeMinutes,
            servings = suggestion.estimatedServings(),
            portions = suggestion.portionGuidance(),
            have = suggestion.ingredientsOwned,
            expiringSoon = suggestion.ingredientsExpiringSoon,
            optional = suggestion.ingredientsOptional,
            missing = suggestion.ingredientsMissing,
            urgentCount = suggestion.ingredientsExpiringSoon.size,
            matchScore = suggestion.matchScore,
            scoreReasons = suggestion.scoreReasons,
            whySuggested = suggestion.explanation,
            difficulty = suggestion.difficulty,
            filters = suggestion.filters,
            tools = suggestion.tools,
            safetyNote = suggestion.safetyWarnings.firstOrNull() ?: suggestion.safetyNote,
            usedFoodIds = suggestion.usedFoods.map { it.id },
            note = suggestion.summary,
            steps = suggestion.steps.joinToString(". ")
        )
    }
        .map { it.applyRecipeFeedback(uiState.recipeFeedback) }
        .sortedByOption(RecipeSortOption.BEST_MATCH)
}

private val negativeRecipeFeedbackActions = setOf(
    RecipeFeedbackAction.HIDDEN,
    RecipeFeedbackAction.TOO_MANY_MISSING,
    RecipeFeedbackAction.NOT_MY_TASTE,
    RecipeFeedbackAction.TOO_MUCH_WORK,
    RecipeFeedbackAction.BAD_SUGGESTION
)

private fun RecipeIdea.applyRecipeFeedback(feedback: List<RecipeFeedbackEntity>): RecipeIdea {
    if (feedback.isEmpty()) return this
    val format = mealFormat()
    val exactFeedback = feedback.filter { it.recipeTitle.equals(title, ignoreCase = true) }
    val positiveExact = exactFeedback.count { it.action == RecipeFeedbackAction.COOKED || it.action == RecipeFeedbackAction.SAVED }
    val negativeExact = exactFeedback.count { it.action in negativeRecipeFeedbackActions }
    val cookedFormatCount = feedback.count { it.mealFormat == format && it.action == RecipeFeedbackAction.COOKED }
    val rejectedFormatCount = feedback.count { it.mealFormat == format && it.action in negativeRecipeFeedbackActions }
    val hiddenSimilarCount = feedback.count {
        it.action == RecipeFeedbackAction.HIDDEN &&
            !it.recipeTitle.equals(title, ignoreCase = true) &&
            it.recipeTitle.isSimilarRecipeTitle(title)
    }

    var delta = 0
    val learningReasons = mutableListOf<String>()
    if (positiveExact > 0) {
        delta += (positiveExact * 10).coerceAtMost(20)
        learningReasons += "Boosted because you saved or cooked this before"
    }
    if (cookedFormatCount > 0) {
        delta += (cookedFormatCount * 4).coerceAtMost(12)
        learningReasons += "Matches meal formats you cook often"
    }
    if (negativeExact > 0) {
        delta -= (negativeExact * 15).coerceAtMost(35)
        learningReasons += "Lowered because you rejected this suggestion before"
    }
    if (rejectedFormatCount >= 2) {
        delta -= 20
        learningReasons += "Lowered because similar meal formats were rejected"
    }
    if (hiddenSimilarCount > 0) {
        delta -= 15
        learningReasons += "Lowered because it is similar to a hidden recipe"
    }

    return copy(
        matchScore = (matchScore + delta).coerceIn(0, 100),
        scoreReasons = (learningReasons + scoreReasons).distinct()
    )
}

private fun RecipeIdea.toFeedback(action: RecipeFeedbackAction): RecipeFeedbackEntity =
    RecipeFeedbackEntity(
        recipeTitle = title,
        mealFormat = mealFormat(),
        action = action,
        ingredients = (have + expiringSoon + optional + missing)
            .map { it.cleanShoppingName() }
            .distinct()
            .joinToString(",")
    )

private fun RecipeIdea.feedbackMessage(action: RecipeFeedbackAction): String =
    when (action) {
        RecipeFeedbackAction.COOKED -> "Got it. Similar ${mealFormat()} ideas will rank higher."
        RecipeFeedbackAction.SAVED -> "Saved ${title} for later."
        RecipeFeedbackAction.HIDDEN -> "Hid ${title}."
        RecipeFeedbackAction.TOO_MANY_MISSING -> "Got it. Recipes with too many missing items will rank lower."
        RecipeFeedbackAction.NOT_MY_TASTE -> "Got it. Similar ingredients and recipes will rank lower."
        RecipeFeedbackAction.TOO_MUCH_WORK -> "Got it. Easier recipes will get priority."
        RecipeFeedbackAction.BAD_SUGGESTION -> "Got it. This kind of suggestion will rank lower."
    }

private fun RecipeIdea.mealFormat(): String {
    val text = title.lowercase()
    return when {
        "bowl" in text -> "bowls"
        listOf("wrap", "quesadilla", "taco", "burrito").any { it in text } -> "wraps"
        listOf("pasta", "noodle").any { it in text } -> "pasta"
        "soup" in text -> "soup"
        "salad" in text -> "salad"
        listOf("omelet", "scramble", "egg", "breakfast", "smoothie", "yogurt").any { it in text } -> "breakfast"
        listOf("snack", "plate").any { it in text } -> "snacks"
        RecipeFilter.ONE_PAN in filters -> "one-pan meals"
        else -> "other"
    }
}

private fun String.isSimilarRecipeTitle(other: String): Boolean {
    val left = lowercase().split(" ", "-", "/").filter { it.length > 3 }.toSet()
    val right = other.lowercase().split(" ", "-", "/").filter { it.length > 3 }.toSet()
    return left.intersect(right).size >= 2
}

private fun buildUseItFirstRecipeIdeas(
    item: FoodItemEntity,
    uiState: FridgeFinishUiState
): List<RecipeIdea> {
    val focusedName = normalizeIngredientName(item.name)
    val focusedSuggestions = buildRecipeIdeas(uiState)
        .filter { idea ->
            item.id in idea.usedFoodIds ||
                (idea.have + idea.expiringSoon + idea.optional).any { label ->
                    normalizeIngredientName(label).containsWholeFoodTerm(focusedName) ||
                        focusedName.containsWholeFoodTerm(label)
                }
        }
        .map { idea ->
            idea.copy(
                whySuggested = "Built around ${item.name.cleanShoppingName()}. ${idea.whySuggested}",
                scoreReasons = listOf("Uses ${item.name.cleanShoppingName()} first") + idea.scoreReasons
            )
        }
    val fallbackIdeas = fallbackUseItFirstRecipes(item, uiState)
    return (focusedSuggestions + fallbackIdeas)
        .distinctBy { it.title.lowercase() }
        .sortedWith(
            compareByDescending<RecipeIdea> { item.id in it.usedFoodIds }
                .thenByDescending { it.have.count { have -> !normalizeIngredientName(have).containsWholeFoodTerm(focusedName) } + it.optional.size }
                .thenBy { it.missing.size }
                .thenByDescending { it.matchScore }
                .thenBy { it.minutes }
        )
}

private fun fallbackUseItFirstRecipes(item: FoodItemEntity, uiState: FridgeFinishUiState): List<RecipeIdea> {
    val normalized = normalizeIngredientName(item.name)
    val ownedNames = uiState.activeFoods
        .filterNot { it.id == item.id }
        .map { it.name.cleanShoppingName() }
    val itemName = item.name.cleanShoppingName()
    val baseSafetyNote = recipeSafetyNote(listOf(item))

    fun idea(
        title: String,
        minutes: Int,
        missing: List<String>,
        steps: String,
        filters: Set<RecipeFilter>,
        tools: Set<CookingTool>,
        note: String = "A flexible use-first idea centered on $itemName."
    ): RecipeIdea {
        val ownedHelpers = missing.mapNotNull { missingItem ->
            ownedNames.firstOrNull { owned -> normalizeIngredientName(owned).containsWholeFoodTerm(missingItem) }
        }
        val score = (72 + ownedHelpers.size * 6 - missing.size * 8).coerceIn(0, 100)
        return RecipeIdea(
            title = title,
            minutes = minutes,
            servings = if (title.contains("Soup") || title.contains("Pasta") || title.contains("Bowl")) "Serves 2-4" else "Serves 1-2",
            portions = fallbackPortions(title),
            have = (listOf(itemName) + ownedHelpers).distinct(),
            expiringSoon = listOf(itemName),
            optional = ownedNames.take(2).filterNot { it in ownedHelpers },
            missing = missing.filterNot { missingItem ->
                ownedHelpers.any { owned -> normalizeIngredientName(owned).containsWholeFoodTerm(missingItem) }
            },
            urgentCount = 1,
            matchScore = score,
            scoreReasons = listOf(
                "Uses $itemName as the main ingredient",
                "Ranked by other foods you already have"
            ),
            whySuggested = note,
            difficulty = if (minutes <= 15) "Easy" else "Moderate",
            filters = filters,
            tools = tools,
            safetyNote = baseSafetyNote,
            usedFoodIds = listOf(item.id),
            note = note,
            steps = steps
        )
    }

    return when {
        normalized.containsWholeFoodTerm("chicken") -> listOf(
            idea("Chicken Rice Bowl", 15, listOf("rice", "vegetable", "sauce"), "Warm chicken with rice. Add vegetables. Finish with sauce or seasoning.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK), setOf(CookingTool.MICROWAVE, CookingTool.STOVE)),
            idea("Chicken Quesadilla", 10, listOf("tortilla", "cheese"), "Add chicken and cheese to a tortilla. Heat until crisp. Serve with sauce if available.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK), setOf(CookingTool.STOVE, CookingTool.MICROWAVE)),
            idea("Chicken Pasta", 25, listOf("pasta", "sauce"), "Cook pasta. Warm chicken with sauce. Toss together and add cheese if available.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE)),
            idea("Chicken Caesar Wrap", 10, listOf("greens or vegetables", "tortilla or bread", "dressing"), "Chop chicken. Add greens or vegetables. Wrap or serve over salad with dressing.", setOf(RecipeFilter.LUNCH, RecipeFilter.NO_COOK, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.NO_TOOLS)),
            idea("Chicken Noodle Soup", 30, listOf("broth", "vegetable", "noodles or rice"), "Simmer broth. Add chicken and vegetables. Add noodles or rice if available.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE))
        )
        normalized.containsWholeFoodTerm("rice") -> listOf(
            idea("Leftover Fried Rice", 15, listOf("egg", "vegetable", "soy sauce"), "Stir-fry rice with vegetables. Add egg or leftover protein if available. Finish with sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK, RecipeFilter.ONE_PAN, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE)),
            idea("Leftover Rice Bowl", 10, listOf("protein", "vegetable", "sauce"), "Warm rice, add protein and vegetables, then finish with sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.MICROWAVE, CookingTool.STOVE)),
            idea("Stuffed Pepper Rice Filling", 30, listOf("bell pepper", "cheese or sauce"), "Mix rice with sauce or cheese, stuff into peppers, and bake or microwave until hot.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.OVEN, CookingTool.MICROWAVE)),
            idea("Rice Soup Add-in", 20, listOf("broth", "vegetable"), "Stir rice into broth with vegetables or leftovers and simmer until hot.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE))
        )
        normalized.containsWholeFoodTerm("banana") -> listOf(
            idea("Banana Smoothie", 5, listOf("milk or yogurt", "frozen fruit"), "Blend banana with milk or yogurt. Add frozen fruit or ice if available.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.SNACK, RecipeFilter.QUICK), setOf(CookingTool.BLENDER)),
            idea("Banana Pancakes", 15, listOf("egg", "flour or pancake mix"), "Mash banana. Mix with egg and flour or mix. Cook small pancakes until set.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.COMFORT), setOf(CookingTool.STOVE)),
            idea("Banana Yogurt Bowl", 5, listOf("yogurt", "granola or nuts"), "Slice banana over yogurt. Add granola, nuts, or cereal if available.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.SNACK, RecipeFilter.NO_COOK), setOf(CookingTool.NO_TOOLS))
        )
        normalized.containsWholeFoodTerm("spinach") -> listOf(
            idea("Spinach Omelet", 10, listOf("egg", "cheese"), "Cook spinach briefly. Add eggs. Finish with cheese if available.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.QUICK, RecipeFilter.ONE_PAN), setOf(CookingTool.STOVE)),
            idea("Spinach Salad", 10, listOf("protein", "dressing"), "Use spinach as the base. Add protein, cheese, or dressing if available.", setOf(RecipeFilter.LUNCH, RecipeFilter.NO_COOK, RecipeFilter.HEALTHY), setOf(CookingTool.NO_TOOLS)),
            idea("Spinach Pasta Add-in", 20, listOf("pasta", "sauce"), "Cook pasta. Stir spinach into warm sauce until wilted. Toss together.", setOf(RecipeFilter.DINNER), setOf(CookingTool.STOVE)),
            idea("Spinach Smoothie", 5, listOf("fruit", "milk or yogurt"), "Blend spinach with fruit and milk or yogurt until smooth.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.SNACK, RecipeFilter.HEALTHY), setOf(CookingTool.BLENDER)),
            idea("Spinach Soup Add-in", 30, listOf("broth", "protein or beans"), "Simmer broth and protein or beans. Stir spinach in at the end.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT), setOf(CookingTool.STOVE))
        )
        normalized.containsWholeFoodTerm("vegetable") || normalized.containsWholeFoodTerm("veggie") -> listOf(
            idea("Leftover Vegetable Omelet", 10, listOf("egg", "cheese"), "Warm vegetables briefly, add eggs, and cook until set.", setOf(RecipeFilter.BREAKFAST, RecipeFilter.QUICK, RecipeFilter.ONE_PAN, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE)),
            idea("Vegetable Pasta Add-in", 20, listOf("pasta", "sauce"), "Warm vegetables in sauce, then toss with cooked pasta.", setOf(RecipeFilter.DINNER, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE)),
            idea("Leftover Vegetable Soup", 25, listOf("broth", "protein or beans"), "Simmer vegetables with broth and add beans or protein if available.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE)),
            idea("Vegetable Stir Fry", 15, listOf("rice or noodles", "sauce"), "Stir-fry vegetables quickly and serve with rice, noodles, or sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK, RecipeFilter.ONE_PAN, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE))
        )
        item.category == FoodCategory.LEFTOVERS || item.isLeftover -> listOf(
            idea("${itemName} Wrap", 10, listOf("tortilla or bread", "cheese or sauce"), "Warm the leftover if needed, wrap it with cheese, greens, or sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.QUICK, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.NO_TOOLS, CookingTool.MICROWAVE)),
            idea("${itemName} Bowl", 12, listOf("rice or grain", "vegetable"), "Serve the leftover over rice, pasta, potatoes, or greens with a quick sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.MICROWAVE, CookingTool.STOVE)),
            idea("${itemName} Sandwich or Melt", 10, listOf("bread", "cheese or sauce"), "Layer the leftover on bread. Toast with cheese or serve cold with sauce if appropriate.", setOf(RecipeFilter.LUNCH, RecipeFilter.QUICK, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.NO_TOOLS, CookingTool.STOVE)),
            idea("${itemName} Quesadilla", 10, listOf("tortilla", "cheese"), "Add the leftover and cheese to a tortilla, then toast until hot.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER, RecipeFilter.QUICK, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.STOVE, CookingTool.MICROWAVE)),
            idea("${itemName} Casserole", 35, listOf("rice, pasta, or potatoes", "cheese or sauce"), "Combine leftovers with a base and sauce, then bake until hot throughout.", setOf(RecipeFilter.DINNER, RecipeFilter.COMFORT, RecipeFilter.LEFTOVER_RESCUE), setOf(CookingTool.OVEN))
        )
        item.category == FoodCategory.MEAT -> listOf(
            idea("${itemName} Rice Bowl", 15, listOf("rice", "vegetable"), "Warm the protein. Serve over rice with vegetables and sauce.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER), setOf(CookingTool.MICROWAVE, CookingTool.STOVE)),
            idea("${itemName} Wrap", 10, listOf("tortilla or bread", "vegetable"), "Layer the protein with vegetables in a wrap or sandwich.", setOf(RecipeFilter.LUNCH, RecipeFilter.QUICK), setOf(CookingTool.NO_TOOLS, CookingTool.MICROWAVE))
        )
        item.category == FoodCategory.PRODUCE -> listOf(
            idea("${itemName} Salad", 10, listOf("protein", "dressing"), "Use it as the main fresh ingredient with protein or dressing.", setOf(RecipeFilter.LUNCH, RecipeFilter.NO_COOK), setOf(CookingTool.NO_TOOLS)),
            idea("${itemName} Pasta Add-in", 20, listOf("pasta", "sauce"), "Cook pasta and fold this item into the sauce or topping.", setOf(RecipeFilter.DINNER), setOf(CookingTool.STOVE))
        )
        else -> listOf(
            idea("${itemName} Bowl", 15, listOf("rice or pasta", "vegetable"), "Use this item as the main topping over a simple base.", setOf(RecipeFilter.LUNCH, RecipeFilter.DINNER), setOf(CookingTool.MICROWAVE, CookingTool.STOVE))
        )
    }
}

private fun fallbackPortions(title: String): List<String> =
    when {
        title.contains("Smoothie") -> listOf("1 serving: 1 banana or 1 cup fruit plus 1 cup liquid.", "2 servings: double fruit and liquid.")
        title.contains("Pancake") -> listOf("1 serving: 1 banana with 1 egg or enough mix for 2 to 3 small pancakes.", "Family size: make multiple small batches.")
        title.contains("Soup") -> listOf("1 serving: about 1 1/2 cups soup.", "Family size: add extra broth, vegetables, rice, or noodles.")
        title.contains("Wrap") || title.contains("Quesadilla") -> listOf("1 serving: 1 wrap or quesadilla.", "Family size: make one per person.")
        else -> listOf("1 serving: use one normal portion of the main ingredient.", "Family size: double the base and add a side.")
    }

private fun quickUseItFirstIdeas(item: FoodItemEntity): List<String> {
    val normalized = normalizeIngredientName(item.name)
    return when {
        normalized.containsWholeFoodTerm("chicken") -> listOf("Chicken rice bowl", "Chicken quesadilla", "Chicken pasta", "Chicken Caesar wrap", "Chicken noodle soup")
        normalized.containsWholeFoodTerm("banana") -> listOf("Smoothie", "Banana pancakes", "Yogurt bowl", "Freeze for later")
        normalized.containsWholeFoodTerm("spinach") -> listOf("Omelet", "Salad", "Pasta add-in", "Smoothie", "Soup add-in")
        normalized.containsWholeFoodTerm("rice") -> listOf("Fried rice", "Rice bowl", "Stuffed peppers", "Soup add-in", "Freeze for later")
        item.category == FoodCategory.LEFTOVERS || item.isLeftover -> leftoverRescueIdeaLabels(item)
        item.category == FoodCategory.MEAT -> listOf("Rice bowl", "Wrap", "Pasta", "Soup", "Freeze for later")
        item.category == FoodCategory.PRODUCE -> listOf("Salad", "Omelet add-in", "Pasta add-in", "Soup add-in", "Freeze for later")
        else -> listOf("Bowl", "Wrap", "Soup add-in", "Snack plate", "Freeze for later")
    }
}

private fun leftoverRescueIdeaLabels(item: FoodItemEntity): List<String> {
    val normalized = normalizeIngredientName(item.name)
    return when {
        normalized.containsWholeFoodTerm("chicken") -> listOf("Quesadilla", "Rice bowl", "Caesar wrap", "Noodle soup")
        normalized.containsWholeFoodTerm("rice") -> listOf("Fried rice", "Rice bowl", "Stuffed peppers", "Soup add-in")
        normalized.containsWholeFoodTerm("vegetable") || normalized.containsWholeFoodTerm("veggie") -> listOf("Omelet", "Pasta add-in", "Soup", "Stir fry")
        normalized.containsWholeFoodTerm("pasta") || normalized.containsWholeFoodTerm("noodle") -> listOf("Pasta bake", "Soup add-in", "Skillet", "Casserole")
        else -> listOf("Wrap", "Bowl", "Sandwich", "Quesadilla", "Salad", "Casserole")
    }
}

private fun canSuggestFreezing(item: FoodItemEntity): Boolean =
    item.location != FoodLocation.FREEZER &&
        item.category !in setOf(FoodCategory.PANTRY, FoodCategory.CONDIMENTS, FoodCategory.DRINKS, FoodCategory.SNACKS)

private fun FoodItemEntity.effectiveItemState(): FoodItemState =
    when {
        itemState == FoodItemState.SPOILED || itemState == FoodItemState.QUESTIONABLE -> itemState
        location == FoodLocation.FREEZER || itemState == FoodItemState.FROZEN -> FoodItemState.FROZEN
        itemState != FoodItemState.FRESH -> itemState
        expirationDate.isBefore(LocalDate.now()) -> FoodItemState.EXPIRED
        FreshnessCalculator.status(expirationDate, reminderDaysBefore, isFinished) == FreshnessStatus.EAT_SOON -> FoodItemState.USE_SOON
        else -> FoodItemState.FRESH
    }

private fun recipeSafetyNote(foods: List<FoodItemEntity>): String {
    val today = LocalDate.now()
    return when {
        foods.any { it.itemState == FoodItemState.SPOILED } -> "Spoiled items are not recipe ingredients. Dispose of them instead."
        foods.any { it.itemState == FoodItemState.QUESTIONABLE } -> "Use only if still safe. Check smell, texture, and package guidance before using."
        foods.any { it.expirationDate.isBefore(today) || it.itemState == FoodItemState.EXPIRED } -> "Includes a past-date item. Check smell, texture, and package guidance before using."
        foods.any { it.isAgedLeftover(today) } -> "Leftovers can become risky after several days. Check smell, texture, date, and reheat thoroughly."
        foods.any { it.isHighRiskFood() } -> "High-risk foods need extra caution. Check smell, texture, and package guidance before using."
        else -> "Dates are reminders, not safety guarantees. Check food before eating."
    }
}

private fun List<RecipeIdea>.sortedByOption(option: RecipeSortOption): List<RecipeIdea> =
    when (option) {
        RecipeSortOption.BEST_MATCH -> sortedWith(
            compareByDescending<RecipeIdea> { it.matchScore }
                .thenBy { it.missing.size }
                .thenBy { it.minutes }
        )
        RecipeSortOption.EXPIRING_SOON -> sortedWith(
            compareByDescending<RecipeIdea> { it.urgentCount }
                .thenByDescending { it.matchScore }
                .thenBy { it.missing.size }
        )
        RecipeSortOption.FEWEST_MISSING -> sortedWith(
            compareBy<RecipeIdea> { it.missing.size }
                .thenByDescending { it.matchScore }
                .thenBy { it.minutes }
        )
        RecipeSortOption.FASTEST -> sortedWith(
            compareBy<RecipeIdea> { it.minutes }
                .thenBy { it.missing.size }
                .thenByDescending { it.matchScore }
        )
        RecipeSortOption.MOST_INGREDIENTS_USED -> sortedWith(
            compareByDescending<RecipeIdea> { it.have.size + it.optional.size }
                .thenByDescending { it.matchScore }
                .thenBy { it.missing.size }
        )
    }

private fun RecipeEntity.estimatedServings(): String {
    val text = title.lowercase()
    return when {
        listOf("smoothie", "yogurt", "snack").any { text.contains(it) } -> "Serves 1-2"
        listOf("soup", "pasta", "fried rice", "grain bowl", "salad").any { text.contains(it) } -> "Serves 3-4"
        listOf("omelet", "quesadilla").any { text.contains(it) } -> "Serves 2"
        else -> "Serves 2-3"
    }
}

private fun RecipeSuggestion.estimatedServings(): String {
    val text = title.lowercase()
    return when {
        listOf("smoothie", "yogurt", "snack", "scramble", "omelet").any { text.contains(it) } -> "Serves 1-2"
        listOf("soup", "pasta", "fried rice", "rice bowl", "grain bowl", "stir fry", "salad").any { text.contains(it) } -> "Serves 3-4"
        listOf("wrap", "quesadilla").any { text.contains(it) } -> "Serves 2"
        else -> "Serves 2-3"
    }
}

private fun RecipeSuggestion.portionGuidance(): List<String> {
    val text = title.lowercase()
    return when {
        text.contains("smoothie") -> listOf(
            "1 serving: about 1 cup fruit plus 1 cup milk or yogurt if available.",
            "2 servings: double the fruit and liquid, then blend in batches if needed."
        )
        text.contains("scramble") || text.contains("omelet") -> listOf(
            "1 serving: 2 eggs plus a small handful of vegetables or cheese.",
            "Family size: use 2 eggs per person and cook in batches."
        )
        text.contains("rice bowl") || text.contains("stir fry") || text.contains("fried rice") -> listOf(
            "1 bowl: about 1 cup cooked rice or noodles, 1/2 cup protein, and 1 cup vegetables.",
            "Family size: set the base, protein, and toppings out separately."
        )
        text.contains("wrap") || text.contains("quesadilla") -> listOf(
            "1 serving: 1 large tortilla with 1/2 cup filling.",
            "Family size: build one wrap or quesadilla per person."
        )
        text.contains("soup") -> listOf(
            "1 serving: about 1 1/2 cups soup.",
            "Family size: add more broth, vegetables, or grains to stretch it."
        )
        else -> recipe.portionGuidance()
    }
}

private fun TemplateRecipeSuggestion.estimatedServings(): String {
    val text = title.lowercase()
    return when {
        listOf("smoothie", "salad", "scramble", "omelet").any { text.contains(it) } -> "Serves 1-2"
        listOf("soup", "pasta", "rice bowl", "stir fry").any { text.contains(it) } -> "Serves 3-4"
        listOf("wrap", "quesadilla").any { text.contains(it) } -> "Serves 2"
        else -> "Serves 2-3"
    }
}

private fun TemplateRecipeSuggestion.portionGuidance(): List<String> {
    val text = title.lowercase()
    return when {
        text.contains("smoothie") -> listOf(
            "1 serving: about 1 cup fruit plus 1 cup milk or yogurt if available.",
            "2 servings: double the fruit and liquid, then blend in batches if needed."
        )
        text.contains("scramble") || text.contains("omelet") -> listOf(
            "1 serving: 2 eggs plus a small handful of vegetables or cheese.",
            "Family size: use 2 eggs per person and cook in batches."
        )
        text.contains("rice bowl") || text.contains("stir fry") -> listOf(
            "1 bowl: about 1 cup cooked rice or noodles, 1/2 cup protein, and 1 cup vegetables.",
            "Family size: set the base, protein, and toppings out separately."
        )
        text.contains("wrap") || text.contains("quesadilla") -> listOf(
            "1 serving: 1 large tortilla with 1/2 cup filling.",
            "Family size: build one wrap or quesadilla per person."
        )
        text.contains("soup") -> listOf(
            "1 serving: about 1 1/2 cups soup.",
            "Family size: add more broth, vegetables, or grains to stretch it."
        )
        else -> listOf("Use the listed ingredients as flexible portions and scale up for more servings.")
    }
}

private fun RecipeEntity.portionGuidance(): List<String> {
    val text = title.lowercase()
    return when {
        text.contains("smoothie") -> listOf(
            "1 serving: about 1 cup milk or yogurt, 1 cup fruit, and 1/2 cup frozen fruit or ice.",
            "2 servings: double those amounts and blend in batches if needed."
        )
        text.contains("yogurt") -> listOf(
            "1 serving: about 3/4 cup yogurt, 1/2 to 1 cup fruit, and 2 tablespoons granola or nuts.",
            "2 servings: use about 1 1/2 cups yogurt and split toppings across two bowls."
        )
        text.contains("snack") -> listOf(
            "1 plate: 1 handful fruit or vegetables, 1 small protein or dairy item, and 1 handful crackers or nuts.",
            "Family plate: make one section per person so everyone can grab what they want."
        )
        text.contains("omelet") -> listOf(
            "1 serving: 2 eggs, 1/4 cup chopped vegetables, and a small handful of cheese if available.",
            "2 servings: use 4 eggs and cook as one large omelet or two smaller ones."
        )
        text.contains("quesadilla") -> listOf(
            "1 serving: 1 tortilla, 1/3 cup cheese, and 1/4 to 1/2 cup beans, peppers, or leftovers.",
            "2 servings: make two tortillas or one large quesadilla cut in half."
        )
        text.contains("pasta") -> listOf(
            "2 servings: about 4 ounces dry pasta or 2 cups prepared pasta-style food, plus 1 cup vegetables or leftovers.",
            "4 servings: double the pasta and add extra sauce or vegetables."
        )
        text.contains("soup") -> listOf(
            "2 servings: about 2 cups broth or water, 1 cup vegetables, and 1 cup beans, grains, or leftovers.",
            "4 servings: double the liquid first, then add more solids until it looks balanced."
        )
        text.contains("fried rice") -> listOf(
            "2 servings: about 2 cups cooked rice, 1 egg or protein, and 1 cup vegetables.",
            "4 servings: use 4 cups cooked rice and cook in batches so it does not steam too much."
        )
        text.contains("grain bowl") -> listOf(
            "1 bowl: about 1 cup cooked rice, pasta, or grain, 1/2 cup leftovers, and 1/2 cup vegetables.",
            "Family bowls: set ingredients out separately and build one bowl per person."
        )
        text.contains("salad") -> listOf(
            "1 serving: 2 cups greens, 1/2 cup protein or leftovers, and 1 to 2 tablespoons dressing.",
            "Family salad: use a large bowl of greens and keep dressing on the side."
        )
        else -> listOf(
            "1 serving: use one normal portion of each listed item.",
            "Family size: double the main items for 4+ servings and add a side if needed."
        )
    }
}

private fun RecipeIdea.detailSteps(): List<String> {
    val baseSteps = steps.split(".")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val prep = when {
        missing.isEmpty() -> "Pull out the listed fridge items first so the oldest food gets used."
        else -> "Gather what you have, then add the missing item${if (missing.size == 1) "" else "s"} before cooking."
    }
    val scale = "For more servings, double the main ingredients and keep seasoning flexible."
    return (listOf(prep) + baseSteps + scale).distinct()
}

private fun RecipeIdea.substitutionIdeas(): List<String> {
    val ideas = mutableListOf<String>()
    if (missing.isNotEmpty()) {
        ideas += "Missing ${missing.first().cleanShoppingName()}: use a similar item you already like, or add it to the shopping list."
    }
    when (mealFormat()) {
        "bowls" -> ideas += "Bowls can swap rice, pasta, potatoes, or salad greens as the base."
        "wraps" -> ideas += "Wraps can use tortillas, bread, lettuce cups, or a bowl format if you are out of wraps."
        "pasta" -> ideas += "Pasta ideas can use noodles, rice, or cooked potatoes if that is what you have."
        "soup" -> ideas += "Soup can use broth, water with seasoning, or a canned soup base."
        "salad" -> ideas += "Salad can use lettuce, spinach, chopped vegetables, or a grain base."
        "breakfast" -> ideas += "Breakfast ideas can swap milk, yogurt, cheese, or eggs depending on the recipe."
        "snacks" -> ideas += "Snack plates work with crackers, toast, fruit, vegetables, cheese, hummus, or leftovers."
        else -> ideas += "Keep the main item, then swap sides and seasonings based on what is already open."
    }
    return ideas.distinct()
}

private fun RecipeIdea.missingSummary(): String =
    when {
        missing.isEmpty() && scoreReasons.any { it.contains("pantry basics assumed", ignoreCase = true) } -> "Pantry basics assumed"
        missing.isEmpty() -> "You have everything needed"
        missing.size == 1 && have.size >= 2 -> "You have almost everything. Only missing 1 item"
        missing.size == 1 -> "Only missing 1 item"
        else -> "Missing ${missing.size} items"
    }

private fun RecipeIdea.addMissingButtonLabel(): String =
    if (missing.size == 1) {
        "Add ${missing.first().cleanShoppingName()} to shopping list"
    } else {
        "Add missing items to shopping list"
    }

private fun String.toPreferenceTerms(): Set<String> =
    split(",", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()

private fun RecipeIngredientEntity.shoppingLabel(): String {
    val clean = label.trim()
    return when (clean.lowercase()) {
        "produce", "vegetables" -> "spinach, peppers, or carrots"
        "greens or vegetables" -> "spinach or peppers"
        "fruit" -> "berries or bananas"
        "dairy or protein" -> "cheese, yogurt, or hummus"
        "protein or leftovers" -> "chicken, eggs, or tofu"
        "base" -> "broth or beans"
        "filling" -> "beans, peppers, or leftovers"
        "frozen item" -> "frozen fruit"
        "crunch" -> "granola or nuts"
        else -> clean
    }.cleanShoppingName()
}

private fun String.inferShoppingCategory(): FoodCategory {
    val text = lowercase()
    return when {
        listOf("spinach", "pepper", "carrot", "lettuce", "berries", "banana", "apple", "celery", "tomato", "peas").any { text.contains(it) } -> FoodCategory.PRODUCE
        listOf("cheese", "yogurt", "milk").any { text.contains(it) } -> FoodCategory.DAIRY
        listOf("chicken", "egg", "tofu", "protein").any { text.contains(it) } -> FoodCategory.MEAT
        listOf("rice", "pasta", "broth", "beans", "tortilla", "granola").any { text.contains(it) } -> FoodCategory.PANTRY
        listOf("cracker", "pretzel", "nuts").any { text.contains(it) } -> FoodCategory.SNACKS
        text.contains("sauce") || text.contains("dressing") -> FoodCategory.CONDIMENTS
        text.contains("frozen") -> FoodCategory.FROZEN
        else -> FoodCategory.OTHER
    }
}

private fun RecipeIngredientEntity.matches(food: FoodItemEntity): Boolean {
    return recipeIngredientMatchesFood(label, keywords, category, food.name, food.category)
}

private fun String.hasAny(vararg terms: String): Boolean =
    terms.any { contains(it, ignoreCase = true) }

@Composable
private fun StatusChip(status: FreshnessStatus) {
    val containerColor by animateColorAsState(statusContainerColor(status), animationSpec = tween(FridgeFinishMotion.Standard), label = "status-chip-container")
    val labelColor by animateColorAsState(statusContentColor(status), animationSpec = tween(FridgeFinishMotion.Standard), label = "status-chip-label")
    AssistChip(
        onClick = {},
        label = {
            AnimatedContent(targetState = status.label, label = "status-chip-label") { label ->
                Text(label)
            }
        },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor
        )
    )
}

@Composable
private fun statusContainerColor(status: FreshnessStatus) = when (status) {
    FreshnessStatus.EXPIRED -> FridgeFinishColors.current.expired
    FreshnessStatus.EXPIRES_TODAY -> FridgeFinishColors.current.expiring
    FreshnessStatus.EAT_SOON -> FridgeFinishColors.current.useSoon
    FreshnessStatus.FRESH -> FridgeFinishColors.current.fresh
    FreshnessStatus.FINISHED -> FridgeFinishColors.current.success
}

@Composable
private fun statusContentColor(status: FreshnessStatus) = when (status) {
    FreshnessStatus.EXPIRED -> FridgeFinishColors.current.onExpired
    FreshnessStatus.EXPIRES_TODAY -> FridgeFinishColors.current.onExpiring
    FreshnessStatus.EAT_SOON -> FridgeFinishColors.current.onUseSoon
    FreshnessStatus.FRESH -> FridgeFinishColors.current.onFresh
    FreshnessStatus.FINISHED -> FridgeFinishColors.current.onSuccess
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodEditorScreen(
    initial: FoodItemEntity,
    subscriptionState: FridgeFinishSubscriptionState,
    onOpenPlus: () -> Unit,
    onSave: (FoodItemEntity) -> Unit,
    onCancel: () -> Unit,
    onScanBarcode: () -> Unit
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var location by rememberSaveable(initial.id) { mutableStateOf(initial.location) }
    var category by rememberSaveable(initial.id) { mutableStateOf(initial.category) }
    var quantity by rememberSaveable(initial.id) { mutableStateOf(initial.quantity.orEmpty()) }
    var unit by rememberSaveable(initial.id) { mutableStateOf(initial.unit.orEmpty()) }
    var purchaseDate by rememberSaveable(initial.id) { mutableStateOf(initial.purchaseDate?.toString().orEmpty()) }
    var openedDate by rememberSaveable(initial.id) { mutableStateOf(initial.openedDate?.toString().orEmpty()) }
    var dateCooked by rememberSaveable(initial.id) { mutableStateOf(initial.dateCooked?.toString().orEmpty()) }
    var expirationDate by rememberSaveable(initial.id) { mutableStateOf(initial.expirationDate.toString()) }
    var reminderDays by rememberSaveable(initial.id) { mutableStateOf(initial.reminderDaysBefore.toString()) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes.orEmpty()) }
    var sourceMeal by rememberSaveable(initial.id) { mutableStateOf(initial.sourceMeal.orEmpty()) }
    var imageUri by rememberSaveable(initial.id) { mutableStateOf(initial.imageUri.orEmpty()) }
    var barcode by rememberSaveable(initial.id) { mutableStateOf(initial.barcode.orEmpty()) }
    var itemState by rememberSaveable(initial.id) { mutableStateOf(initial.effectiveItemState()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable(initial.id) { mutableStateOf(false) }
    var showDetails by rememberSaveable(initial.id) {
        mutableStateOf(
            quantity.isNotBlank() ||
                unit.isNotBlank() ||
                purchaseDate.isNotBlank() ||
                openedDate.isNotBlank() ||
                dateCooked.isNotBlank() ||
                sourceMeal.isNotBlank() ||
                notes.isNotBlank()
        )
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }
    val availableLocations = if (subscriptionState.isPlus) {
        FoodLocation.entries
    } else {
        (listOf(FoodLocation.FRIDGE) + initial.location).distinct()
    }
    val hasScannedProduct = barcode.isNotBlank() || imageUri.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AddFoodHeroCard(subscriptionState)
            AnimatedVisibility(
                visible = error != null,
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item {
            AnimatedVisibility(
                visible = hasScannedProduct,
                enter = FridgeFinishMotion.popIn(),
                exit = FridgeFinishMotion.popOut()
            ) {
                ScannedProductReviewCard(
                    name = name,
                    imageUri = imageUri,
                    barcode = barcode,
                    category = category,
                    location = location
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedVisibility(
                            visible = imageUri.isNotBlank(),
                            enter = FridgeFinishMotion.popIn(),
                            exit = FridgeFinishMotion.popOut()
                        ) {
                            imageUri.takeIf { it.isNotBlank() }?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Product image preview",
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(name, { name = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedButton(onClick = onScanBarcode, modifier = Modifier.fillMaxWidth()) { Text("Scan barcode") }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FoodCategory.entries.take(6).forEach { preset ->
                            FilterChip(
                                selected = category == preset,
                                onClick = {
                                    category = preset
                                    reminderDays = FreshnessCalculator.defaultReminderDays(category).toString()
                                    if (preset == FoodCategory.LEFTOVERS && dateCooked.isBlank()) {
                                        dateCooked = LocalDate.now().toString()
                                        expirationDate = LocalDate.now().plusDays(3).toString()
                                    }
                                },
                                label = { Text(preset.label, maxLines = 1) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimpleMenu("Location", location.label, availableLocations.map { it.label }) { label -> location = availableLocations.first { it.label == label } }
                        SimpleMenu("Category", category.label, FoodCategory.entries.map { it.label }) { label ->
                            category = FoodCategory.fromLabel(label)
                            reminderDays = FreshnessCalculator.defaultReminderDays(category).toString()
                            if (category == FoodCategory.LEFTOVERS && dateCooked.isBlank()) {
                                dateCooked = LocalDate.now().toString()
                                expirationDate = LocalDate.now().plusDays(3).toString()
                            }
                        }
                        SimpleMenu("State", itemState.label, FoodItemState.entries.map { it.label }) { label ->
                            itemState = FoodItemState.entries.first { it.label == label }
                            if (itemState == FoodItemState.FROZEN) location = FoodLocation.FREEZER
                        }
                    }
                    if (!subscriptionState.isPlus) {
                        Text("Free uses Main Fridge and standard reminder timing.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenPlus) { Text("View Plus organization") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Date reminder", style = MaterialTheme.typography.titleMedium)
                    DateInput("Expiration date", expirationDate) { expirationDate = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpirationPresetChip("2 days") { expirationDate = LocalDate.now().plusDays(2).toString() }
                        ExpirationPresetChip("3 days") { expirationDate = LocalDate.now().plusDays(3).toString() }
                        ExpirationPresetChip("1 week") { expirationDate = LocalDate.now().plusDays(7).toString() }
                        ExpirationPresetChip("1 month") { expirationDate = LocalDate.now().plusMonths(1).toString() }
                        ExpirationPresetChip("3 months") { expirationDate = LocalDate.now().plusMonths(3).toString() }
                        ExpirationPresetChip("6 months") { expirationDate = LocalDate.now().plusMonths(6).toString() }
                    }
                    Text("Dates are reminders, not safety guarantees. Check before eating.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            OutlinedButton(onClick = { showDetails = !showDetails }, modifier = Modifier.fillMaxWidth()) {
                val rotation by animateFloatAsState(if (showDetails) 180f else 0f, animationSpec = tween(FridgeFinishMotion.Standard), label = "details-rotation")
                Text(if (showDetails) "Hide details" else "More details")
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(rotation))
            }
        }
        item {
            AnimatedVisibility(
                visible = showDetails,
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        DateInput("Purchase date", purchaseDate) { purchaseDate = it }
                        DateInput("Opened date", openedDate) { openedDate = it }
                        if (category == FoodCategory.LEFTOVERS || initial.isLeftover) {
                            DateInput("Date cooked", dateCooked) { dateCooked = it }
                            OutlinedTextField(
                                sourceMeal,
                                { sourceMeal = it },
                                label = { Text("Source meal") },
                                placeholder = { Text("Sunday dinner, rotisserie chicken") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        if (subscriptionState.isPlus) {
                            OutlinedTextField(reminderDays, { reminderDays = it }, label = { Text("Reminder days before expiration") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(imageUri, { imageUri = it }, label = { Text("Product image URL") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (imageUri.isBlank()) "Attach photo" else "Change photo")
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (isSaving) return@Button
                    val parsedExpiration = parseDate(expirationDate)
                    if (name.isBlank() || parsedExpiration == null) {
                        error = "Add a food name and a valid expiration date."
                        return@Button
                    }
                    isSaving = true
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            location = location,
                            category = category,
                            quantity = quantity.takeIf { it.isNotBlank() },
                            unit = unit.takeIf { it.isNotBlank() },
                            purchaseDate = parseDate(purchaseDate),
                            openedDate = parseDate(openedDate),
                            dateCooked = parseDate(dateCooked),
                            expirationDate = parsedExpiration,
                            reminderDaysBefore = if (subscriptionState.isPlus) {
                                reminderDays.toIntOrNull()?.coerceAtLeast(0) ?: FreshnessCalculator.defaultReminderDays(category)
                            } else {
                                FreshnessCalculator.defaultReminderDays(category)
                            },
                            notes = notes.takeIf { it.isNotBlank() },
                            sourceMeal = sourceMeal.takeIf { it.isNotBlank() },
                            imageUri = imageUri.takeIf { it.isNotBlank() },
                            barcode = barcode.takeIf { it.isNotBlank() },
                            itemState = itemState
                        )
                    )
                }, modifier = Modifier.weight(1f), enabled = !isSaving) {
                    AnimatedContent(targetState = isSaving, label = "save-food-label") { saving ->
                        Text(if (saving) "Saving..." else "Save food")
                    }
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !isSaving) { Text("Cancel") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannedProductReviewCard(
    name: String,
    imageUri: String,
    barcode: String,
    category: FoodCategory,
    location: FoodLocation
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Review scanned product",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                imageUri.takeIf { it.isNotBlank() }?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = name.ifBlank { "Product image" },
                        modifier = Modifier.size(84.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        name.ifBlank { "Unnamed product" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (barcode.isNotBlank()) {
                        Text(
                            "Barcode $barcode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(location.label) })
                AssistChip(onClick = {}, label = { Text(category.label) })
            }
            Text(
                "Next: confirm the date, then save.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AddFoodHeroCard(subscriptionState: FridgeFinishSubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add food fast", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                PlusStatusChip(subscriptionState)
            }
            Text("Start with name and expiration date. Details can wait.", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ExpirationPresetChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

@Composable
private fun RestockScreen(
    uiState: FridgeFinishUiState,
    message: String?,
    onMessageDismissed: () -> Unit,
    onOpenPlus: () -> Unit,
    onSave: (RestockItemEntity) -> Unit,
    onToggle: (RestockItemEntity) -> Unit,
    onDelete: (RestockItemEntity) -> Unit
) {
    if (!uiState.subscription.isPlus) {
        PlusLockedScreen(
            title = "Smart grocery list is in Plus",
            body = "Plus turns finished and expiring food into a smarter shopping list. You can still track fridge items for free.",
            onOpenPlus = onOpenPlus
        )
        return
    }
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val openItems = uiState.restock.filterNot { it.isPurchased }
    val purchasedItems = uiState.restock.filter { it.isPurchased }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ShopHeroCard(openItems.size, purchasedItems.size)
        }
        message?.let {
            item {
                ShopMessageCard(message = it, onDismiss = onMessageDismissed)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add to shop", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(name, { name = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(
                        note,
                        { note = it },
                        label = { Text("Private note") },
                        placeholder = { Text("Brand, size, recipe, or store note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                RestockItemEntity(
                                    name = name.trim(),
                                    quantity = quantity.takeIf { it.isNotBlank() },
                                    note = note.takeIf { it.isNotBlank() }
                                )
                            )
                            name = ""
                            quantity = ""
                            note = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add item") }
                }
            }
        }
        item {
            SectionHeader("Need to buy", openItems.size)
            AnimatedVisibility(
                visible = openItems.isEmpty(),
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                EmptySectionCard("Restock")
            }
        }
        items(openItems, key = { "open-${it.id}" }) { item ->
            RestockCard(item, onToggle, onDelete, modifier = Modifier.animateItem())
        }
        if (purchasedItems.isNotEmpty()) {
            item { SectionHeader("Purchased", purchasedItems.size) }
            items(purchasedItems, key = { "done-${it.id}" }) { item ->
                RestockCard(item, onToggle, onDelete, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ShopMessageCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ShopHeroCard(openCount: Int, purchasedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart shop list", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                "$openCount item${if (openCount == 1) "" else "s"} to buy",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                if (purchasedCount > 0) "$purchasedCount marked purchased. Recipe missing items can land here." else "Add items manually or from almost-ready recipes.",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RestockCard(
    item: RestockItemEntity,
    onToggle: (RestockItemEntity) -> Unit,
    onDelete: (RestockItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (item.isPurchased) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = tween(FridgeFinishMotion.Standard),
        label = "restock-container"
    )
    Card(modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isPurchased, onCheckedChange = { onToggle(item) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name.cleanShoppingName(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.quantity?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.note?.takeIf { it.isNotBlank() }?.let {
                    Text("Note: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.category?.let {
                    AssistChip(onClick = {}, label = { Text(it.label) })
                }
            }
            IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    subscriptionState: FridgeFinishSubscriptionState,
    recipeFeedback: List<RecipeFeedbackEntity>,
    hiddenRecipeTitles: List<String>,
    dislikedIngredients: List<String>,
    selectedThemeStyle: AppThemeStyle,
    previewThemeStyle: AppThemeStyle?,
    selectedAppearanceMode: AppearanceMode,
    onApplyThemeStyle: (AppThemeStyle) -> Unit,
    onPreviewThemeStyle: (AppThemeStyle) -> Unit,
    onClearThemePreview: () -> Unit,
    onAppearanceModeSelected: (AppearanceMode) -> Unit,
    onOpenPlus: () -> Unit,
    onUnlockBetaPremium: () -> Unit,
    onRestorePurchases: () -> Unit,
    onAddSamples: () -> Unit,
    onResetRecipeFeedback: () -> Unit,
    onUnhideRecipe: (String) -> Unit,
    onClearDislikedIngredient: (String) -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val favoriteFormats = remember(recipeFeedback) {
        recipeFeedback
            .filter { it.action == RecipeFeedbackAction.COOKED || it.action == RecipeFeedbackAction.SAVED }
            .groupingBy { it.mealFormat }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SettingsHeroCard(subscriptionState, onUnlockBetaPremium)
        }
        item {
            AppearanceSettingsCard(
                subscriptionState = subscriptionState,
                selectedThemeStyle = selectedThemeStyle,
                previewThemeStyle = previewThemeStyle,
                selectedAppearanceMode = selectedAppearanceMode,
                onApplyThemeStyle = onApplyThemeStyle,
                onPreviewThemeStyle = onPreviewThemeStyle,
                onClearThemePreview = onClearThemePreview,
                onAppearanceModeSelected = onAppearanceModeSelected,
                onOpenPlus = onOpenPlus
            )
        }
        item {
            SettingsCard("Plan") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(subscriptionState.planLabel, style = MaterialTheme.typography.titleLarge)
                        Text("Items tracked: ${subscriptionState.activeItemCount}/${FridgeFinishSubscriptionState.FREE_ITEM_LIMIT}")
                    }
                    PlusStatusChip(subscriptionState)
                }
                BetaPremiumAccessCard(subscriptionState, onUnlockBetaPremium)
                if (!subscriptionState.isPlus && subscriptionState.freeSlotsRemaining == 0) {
                    Text("You reached the free item limit. Plus unlocks unlimited items.")
                }
                subscriptionState.billingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenPlus, modifier = Modifier.weight(1f)) { Text("View Plus") }
                    OutlinedButton(onClick = onRestorePurchases, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }
        }
        item {
            SettingsCard("Local-first") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null)
                    Text("Your food list is stored on this device. No account is required, and there is no cloud sync.")
                }
            }
        }
        item {
            SettingsCard("Notifications") {
                Text("Local reminders can warn you before food dates arrive. If notifications are disabled, reminders will not appear.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                }, modifier = Modifier.fillMaxWidth()) { Text("Ask for notification permission") }
            }
        }
        item {
            SettingsCard("Testing") {
                Text("Sample data helps check dashboard, recipes, storage, and shopping flows.")
                OutlinedButton(onClick = onAddSamples, modifier = Modifier.fillMaxWidth()) { Text("Add sample data") }
            }
        }
        item {
            SettingsCard("Better suggestions") {
                Text("Recipe feedback stays on this device and helps rank future suggestions.")
                if (favoriteFormats.isEmpty()) {
                    Text("No favorite meal formats yet. Cook or save recipes to train suggestions.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Favorite meal formats", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        favoriteFormats.forEach { entry ->
                            AssistChip(onClick = {}, label = { Text("${entry.key} ${entry.value}") })
                        }
                    }
                }
                Text("Hidden recipes", style = MaterialTheme.typography.labelLarge)
                if (hiddenRecipeTitles.isEmpty()) {
                    Text("No hidden recipes.", style = MaterialTheme.typography.bodySmall)
                } else {
                    hiddenRecipeTitles.forEach { title ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onUnhideRecipe(title) }) { Text("Unhide") }
                        }
                    }
                }
                Text("Disliked ingredients", style = MaterialTheme.typography.labelLarge)
                if (dislikedIngredients.isEmpty()) {
                    Text("No disliked ingredients yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dislikedIngredients.forEach { ingredient ->
                            AssistChip(
                                onClick = { onClearDislikedIngredient(ingredient) },
                                label = { Text(ingredient.cleanShoppingName()) }
                            )
                        }
                    }
                    Text("Tap an ingredient to stop lowering recipes that use it.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onResetRecipeFeedback,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipeFeedback.isNotEmpty()
                ) {
                    Text("Reset recipe feedback")
                }
            }
        }
        item {
            SettingsCard("Food date note") {
                Text("Fridge Finish helps you organize food dates and reminders. It does not determine whether food is safe to eat.")
                Text("Use your judgment and check before eating, especially when something is past date.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettingsCard(
    subscriptionState: FridgeFinishSubscriptionState,
    selectedThemeStyle: AppThemeStyle,
    previewThemeStyle: AppThemeStyle?,
    selectedAppearanceMode: AppearanceMode,
    onApplyThemeStyle: (AppThemeStyle) -> Unit,
    onPreviewThemeStyle: (AppThemeStyle) -> Unit,
    onClearThemePreview: () -> Unit,
    onAppearanceModeSelected: (AppearanceMode) -> Unit,
    onOpenPlus: () -> Unit
) {
    var lockedThemePrompt by remember { mutableStateOf<AppThemeStyle?>(null) }

    lockedThemePrompt?.let { style ->
        ModalBottomSheet(onDismissRequest = { lockedThemePrompt = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Unlock Style Packs", style = MaterialTheme.typography.headlineSmall)
                Text("Premium unlocks extra Fridge Finish themes, advanced recipe tools, smarter waste-saving features, and personalization.")
                Button(
                    onClick = {
                        onPreviewThemeStyle(style)
                        lockedThemePrompt = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preview theme")
                }
                OutlinedButton(
                    onClick = {
                        lockedThemePrompt = null
                        onOpenPlus()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock Premium")
                }
                TextButton(
                    onClick = { lockedThemePrompt = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not now")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    SettingsCard("Appearance") {
        Text("Original Fresh stays the free default. Premium style packs are optional.")
        if (previewThemeStyle != null) {
            Card(colors = CardDefaults.cardColors(containerColor = FridgeFinishColors.current.warning)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Previewing ${previewThemeStyle.displayName}", color = FridgeFinishColors.current.onWarning, style = MaterialTheme.typography.titleSmall)
                        Text("Apply requires Premium.", color = FridgeFinishColors.current.onWarning, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onClearThemePreview) { Text("Stop") }
                }
            }
        }
        Text("Mode", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppearanceMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedAppearanceMode == mode,
                    onClick = { onAppearanceModeSelected(mode) },
                    label = { Text(mode.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FridgeFinishColors.current.selectedChip,
                        selectedLabelColor = FridgeFinishColors.current.onSelectedChip
                    )
                )
            }
        }
        Text("Theme style", style = MaterialTheme.typography.labelLarge)
        appThemeStyles().forEach { style ->
            val locked = style.isPremium && !subscriptionState.isPlus
            val selected = selectedThemeStyle == style
            val previewing = previewThemeStyle == style
            ThemeStylePreviewCard(
                style = style,
                appearanceMode = selectedAppearanceMode,
                selected = selected,
                previewing = previewing,
                locked = locked,
                onClick = {
                    when {
                        locked -> lockedThemePrompt = style
                        else -> onApplyThemeStyle(style)
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeStylePreviewCard(
    style: AppThemeStyle,
    appearanceMode: AppearanceMode,
    selected: Boolean,
    previewing: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val scheme = previewColorScheme(style, appearanceMode)
    val semantic = previewSemanticColors(style, appearanceMode)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected || previewing) 2.dp else 1.dp,
            color = if (selected || previewing) scheme.primary else scheme.outline
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(style.displayName, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    Text(
                        when {
                            selected -> "Applied"
                            previewing -> "Previewing"
                            locked -> "Premium style pack"
                            else -> "Free default"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (locked) Icon(Icons.Default.Lock, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(scheme.primary))
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(scheme.tertiary))
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(semantic.expiring))
                }
            }
            MiniThemePreview(scheme = scheme, semantic = semantic)
        }
    }
}

@Composable
private fun MiniThemePreview(
    scheme: androidx.compose.material3.ColorScheme,
    semantic: com.fridgefinish.app.theme.FridgeFinishSemanticColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.background)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(semantic.card)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Lettuce", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text("Fridge - Produce", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(semantic.useSoon)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Eat soon", style = MaterialTheme.typography.labelSmall, color = semantic.onUseSoon)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(semantic.recipe)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recipe idea", style = MaterialTheme.typography.labelMedium, color = semantic.onRecipe)
                    Text("Salad wrap", style = MaterialTheme.typography.bodySmall, color = semantic.onRecipe)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(semantic.primaryButton)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Add food", style = MaterialTheme.typography.labelMedium, color = semantic.onPrimaryButton)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(semantic.bottomNavContainer)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Today", "Storage", "Recipes").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (index == 0) semantic.bottomNavSelected else semantic.bottomNavContainer)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == 0) semantic.onBottomNavSelected else semantic.bottomNavUnselected
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeroCard(subscriptionState: FridgeFinishSubscriptionState, onUnlockBetaPremium: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Info", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                "${subscriptionState.planLabel} is active",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                if (subscriptionState.isPlus) {
                    "Premium tools are unlocked for storage, recipes, and smart shopping."
                } else {
                    "Beta testers can unlock Premium on this device before billing is connected."
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (!subscriptionState.isPlus) {
                Button(
                    onClick = onUnlockBetaPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Unlock beta Premium")
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PlusLockedScreen(title: String, body: String, onOpenPlus: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body)
            Button(onClick = onOpenPlus) { Text("View Fridge Finish Plus") }
        }
    }
}

@Composable
private fun UpgradePromptDialog(
    prompt: FeatureGateResult.UpgradeRequired,
    onDismiss: () -> Unit,
    onOpenPlus: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(prompt.title) },
        text = { Text(prompt.message) },
        confirmButton = { Button(onClick = onOpenPlus) { Text("View Plus") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

@Composable
private fun FridgeFinishPlusScreen(
    subscriptionState: FridgeFinishSubscriptionState,
    onUnlockBetaPremium: () -> Unit,
    onStartPurchase: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PlusHeroCard(subscriptionState)
        }
        item {
            BetaPremiumAccessCard(subscriptionState, onUnlockBetaPremium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PlusFeatureTile("Storage", "More places", Modifier.weight(1f))
                PlusFeatureTile("Recipes", "Use food first", Modifier.weight(1f))
                PlusFeatureTile("Shop", "Smarter list", Modifier.weight(1f))
            }
        }
        item {
            PlanCard("Free", FridgeFinishPlans.freeBenefits, highlighted = false)
        }
        item {
            PlanCard("Plus", FridgeFinishPlans.plusBenefits, highlighted = true)
        }
        item {
            SettingsCard("Billing status") {
                Text(
                    if (subscriptionState.isPlus) {
                        "${subscriptionState.premiumAccessLabel}. Premium features are available on this device. Google Play Billing is still not connected for public purchases."
                    } else {
                        "Google Play Billing is not connected in this build. Beta testers can unlock Premium locally with the button above."
                    }
                )
                subscriptionState.billingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = onStartPurchase, modifier = Modifier.fillMaxWidth()) {
                    Text("Check upgrade availability")
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Maybe later")
                }
            }
        }
    }
}

@Composable
private fun PlusHeroCard(subscriptionState: FridgeFinishSubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fridge Finish Plus", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                if (subscriptionState.isPlus) subscriptionState.premiumAccessLabel else "Upgrade from tracking to finishing",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Multiple storage locations, recipe ideas, smart shopping, and deeper planning without ads or accounts.",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PlusFeatureTile(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val FridgeFinishSubscriptionState.planLabel: String
    get() = when {
        hasAdminAccess -> "Admin Plus"
        hasBetaTesterAccess -> "Beta Plus"
        isPlus -> "Plus"
        else -> "Free"
    }

private val FridgeFinishSubscriptionState.premiumAccessLabel: String
    get() = when {
        hasAdminAccess -> "Admin Plus active"
        hasBetaTesterAccess -> "Beta Premium active"
        isPlus -> "Plus active"
        else -> "Premium locked"
    }

@Composable
private fun BetaPremiumAccessCard(
    subscriptionState: FridgeFinishSubscriptionState,
    onUnlockBetaPremium: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (subscriptionState.isPlus) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                FridgeFinishColors.current.success
            }
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (subscriptionState.isPlus) "Premium access is active" else "Beta tester Premium access",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (subscriptionState.isPlus) MaterialTheme.colorScheme.onPrimaryContainer else FridgeFinishColors.current.onSuccess
                    )
                    Text(
                        if (subscriptionState.isPlus) {
                            "Storage locations, recipes, smart shopping, and style packs are unlocked on this device."
                        } else {
                            "Testing Fridge Finish? Tap once to unlock Plus features locally. No payment or account needed."
                        },
                        color = if (subscriptionState.isPlus) MaterialTheme.colorScheme.onPrimaryContainer else FridgeFinishColors.current.onSuccess
                    )
                }
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (subscriptionState.isPlus) MaterialTheme.colorScheme.onPrimaryContainer else FridgeFinishColors.current.onSuccess,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (!subscriptionState.hasBetaTesterAccess) {
                Button(
                    onClick = onUnlockBetaPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (subscriptionState.isPlus) "Save beta Premium access" else "Unlock beta Premium")
                }
            } else {
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Beta Premium saved on this device") }
                )
            }
        }
    }
}

@Composable
private fun PlanCard(title: String, benefits: List<String>, highlighted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (highlighted) AssistChip(onClick = {}, label = { Text("Best") })
            }
        benefits.forEach { benefit ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        if (highlighted) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(benefit, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SimpleMenu(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }) { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = {
                    onSelected(it)
                    expanded = false
                })
            }
        }
    }
}

private fun parseDate(value: String): LocalDate? =
    if (value.isBlank()) null else try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptImportScreen(
    onImport: (List<ReceiptImportCandidate>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    var rawText by rememberSaveable { mutableStateOf("") }
    var candidates by remember { mutableStateOf(emptyList<ReceiptImportCandidate>()) }
    var selectedNames by remember { mutableStateOf(emptySet<String>()) }
    var scannerPaused by rememberSaveable { mutableStateOf(false) }
    var importMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scannerPaused = true
            importMessage = "Reading receipt photo..."
            recognizeReceiptFromImage(context, uri) { text, detected ->
                rawText = text.replace('\n', ' ').trim()
                candidates = detected
                selectedNames = detected.map { it.name }.toSet()
                importMessage = if (detected.isEmpty()) {
                    "No grocery items found in that photo. Try a clearer screenshot or crop closer to the item list."
                } else {
                    "Found ${detected.size} possible item${if (detected.size == 1) "" else "s"} from the photo."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Import receipt", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Scan grocery receipt text or import a digital receipt photo, review the matches, then add selected items to your inventory.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Receipt dates are not expiration dates. Fridge Finish uses default reminders until you edit each item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Scan the item list", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fill the camera with receipt item rows, or pick a saved receipt screenshot/photo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pick receipt photo")
                    }
                    AnimatedVisibility(
                        visible = importMessage != null,
                        enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                        exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
                    ) {
                        importMessage?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        }
                    }
                    if (hasPermission) {
                        if (scannerPaused) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    "Scanner paused. Review the detected items below or scan again.",
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            ReceiptOcrCamera { text, detected ->
                                rawText = text.replace('\n', ' ').trim()
                                if (detected.isNotEmpty()) {
                                    candidates = detected
                                    selectedNames = detected.map { it.name }.toSet()
                                }
                            }
                        }
                    } else {
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow camera")
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scannerPaused = !scannerPaused
                        }) {
                            Text(if (scannerPaused) "Scan again" else "Pause scanner")
                        }
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                    if (rawText.isNotBlank()) {
                        Text("Read: ${rawText.take(140)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            SectionHeader("Review items", candidates.size)
            AnimatedVisibility(
                visible = candidates.isEmpty(),
                enter = FridgeFinishMotion.fadeInStandard() + expandVertically(animationSpec = tween(FridgeFinishMotion.Standard)),
                exit = FridgeFinishMotion.fadeOutQuick() + shrinkVertically(animationSpec = tween(FridgeFinishMotion.Quick))
            ) {
                EmptyRecipeCard("No receipt items detected yet. Move closer, improve lighting, or try a flatter receipt.")
            }
        }

        items(candidates, key = { it.name }) { candidate ->
            ReceiptCandidateCard(
                candidate = candidate,
                selected = candidate.name in selectedNames,
                onSelectedChange = { selected ->
                    selectedNames = if (selected) selectedNames + candidate.name else selectedNames - candidate.name
                },
                modifier = Modifier.animateItem()
            )
        }

        if (candidates.isNotEmpty()) {
            item {
                val selected = candidates.filter { it.name in selectedNames }
                Button(
                    onClick = { onImport(selected) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Import ${selected.size} item${if (selected.size == 1) "" else "s"}")
                }
                Text(
                    "After import, open any item to adjust location, quantity, or exact expiration date.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptCandidateCard(
    candidate: ReceiptImportCandidate,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = onSelectedChange)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(candidate.category.label) })
                    AssistChip(onClick = {}, label = { Text("Default date ${candidate.expirationDate}") })
                }
            }
        }
    }
}

@Composable
private fun ReceiptOcrCamera(onResult: (rawText: String, candidates: List<ReceiptImportCandidate>) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastScanAt by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 380.dp)
            .height(320.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener(
                    {
                        val cameraProvider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastScanAt < 1400L) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    lastScanAt = now
                                    scanReceiptImageProxy(imageProxy, onResult)
                                }
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                    },
                    ContextCompat.getMainExecutor(context)
                )
                previewView
            }
        )
        ScanFrameOverlay(
            label = "Receipt items",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun scanReceiptImageProxy(
    imageProxy: ImageProxy,
    onResult: (rawText: String, candidates: List<ReceiptImportCandidate>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { result ->
            onResult(result.text, ReceiptTextParser.extractItems(result.text))
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun recognizeReceiptFromImage(
    context: Context,
    uri: Uri,
    onComplete: (rawText: String, candidates: List<ReceiptImportCandidate>) -> Unit
) {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    if (bitmap == null) {
        onComplete("", emptyList())
        return
    }
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result ->
            onComplete(result.text, ReceiptTextParser.extractItems(result.text))
        }
        .addOnFailureListener {
            onComplete("", emptyList())
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DateInput(label: String, value: String, onValueChange: (String) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showLiveScanner by rememberSaveable { mutableStateOf(false) }
    var scanMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedText by rememberSaveable { mutableStateOf("") }
    var detectedDates by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var dateLocked by rememberSaveable { mutableStateOf(false) }
    val scannerBringIntoViewRequester = remember { BringIntoViewRequester() }
    val selectedMillis = parseDate(value)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)

    LaunchedEffect(showLiveScanner) {
        if (showLiveScanner) {
            delay(150)
            scannerBringIntoViewRequester.bringIntoView()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showLiveScanner) {
            DateOcrLiveScanner(
                modifier = Modifier.bringIntoViewRequester(scannerBringIntoViewRequester),
                statusMessage = scanMessage ?: "Reading text. Fill the frame with the printed date.",
                onResult = { rawText, candidates ->
                    if (dateLocked) return@DateOcrLiveScanner
                    scannedText = rawText.replace('\n', ' ').trim()
                    val foundDates = candidates.map { it.toString() }
                    if (foundDates.isNotEmpty()) {
                        val firstDate = foundDates.first()
                        dateLocked = true
                        detectedDates = foundDates
                        onValueChange(firstDate)
                        scanMessage = "Found $firstDate. Scanner paused so you can review it."
                        showLiveScanner = false
                    } else if (scannedText.isNotBlank()) {
                        scanMessage = "Reading text, but no date matched yet. Move closer and reduce background text."
                    }
                },
                onClose = {
                    showLiveScanner = false
                    if (scanMessage == null) {
                        scanMessage = "Scanner closed. You can pick a date or scan again."
                    }
                }
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("$label (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showPicker = true }) { Text("Pick $label") }
            if (!showLiveScanner) {
                OutlinedButton(
                    onClick = {
                        showLiveScanner = true
                        scanMessage = "Fill the camera with the printed date only."
                        scannedText = ""
                        detectedDates = emptyList()
                        dateLocked = false
                    }
                ) { Text("Scan $label") }
            }
            if (value.isNotBlank()) {
                TextButton(onClick = {
                    onValueChange("")
                    detectedDates = emptyList()
                    scannedText = ""
                    scanMessage = null
                    dateLocked = false
                }) { Text("Clear") }
            }
        }
        if (!showLiveScanner) scanMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (detectedDates.isNotEmpty()) {
            Text("Detected dates", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detectedDates.take(3).forEach { candidate ->
                    AssistChip(
                        onClick = {
                            onValueChange(candidate)
                            scanMessage = "Using $candidate."
                            showLiveScanner = false
                            dateLocked = true
                        },
                        label = { Text(candidate) }
                    )
                }
            }
        }
        if (scannedText.isNotBlank()) {
            Text(
                "Read: ${scannedText.take(120)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onValueChange(
                                Instant.ofEpochMilli(it)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .toString()
                            )
                        }
                        showPicker = false
                    }
                ) {
                    Text("Use date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateOcrLiveScanner(
    modifier: Modifier = Modifier,
    statusMessage: String,
    onResult: (rawText: String, candidates: List<LocalDate>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Scan the printed date",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Move close, keep it well lit, and avoid the nutrition label or long ingredient text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (hasPermission) {
                LiveDateCamera(onResult)
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }

            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose) { Text("Done scanning") }
                TextButton(onClick = onClose) { Text("Close scanner") }
            }
        }
    }
}

@Composable
private fun LiveDateCamera(onResult: (rawText: String, candidates: List<LocalDate>) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastScanAt by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 320.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener(
                    {
                        val cameraProvider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val now = System.currentTimeMillis()
                                    if (now - lastScanAt < 900L) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    lastScanAt = now
                                    scanDateImageProxy(imageProxy, onResult)
                                }
                            }
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                        camera.cameraControl.setZoomRatio(2.0f)
                    },
                    ContextCompat.getMainExecutor(context)
                )
                previewView
            }
        )
        ScanFrameOverlay(
            label = "Printed date only",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ScanFrameOverlay(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(0.82f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
        )
        Text(
            label,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun scanDateImageProxy(
    imageProxy: ImageProxy,
    onResult: (rawText: String, candidates: List<LocalDate>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(image)
        .addOnSuccessListener { result ->
            val candidates = DateTextParser.extractCandidates(result.text)
            onResult(result.text, candidates)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun createDateScanUri(context: Context): Uri {
    val directory = File(context.cacheDir, "date_scans").apply { mkdirs() }
    val file = File.createTempFile("date_scan_", ".jpg", directory)
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun recognizeDateFromImage(
    context: Context,
    uri: Uri,
    onComplete: (rawText: String, candidates: List<LocalDate>) -> Unit
) {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    if (bitmap == null) {
        onComplete("", emptyList())
        return
    }
    val variants = dateScanBitmapVariants(bitmap)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val rawTexts = mutableListOf<String>()
    val candidates = mutableListOf<LocalDate>()

    fun run(index: Int) {
        if (index >= variants.size) {
            onComplete(rawTexts.joinToString(" | "), candidates.distinct().sorted())
            return
        }
        recognizer.process(InputImage.fromBitmap(variants[index], 0))
            .addOnSuccessListener { result ->
                if (result.text.isNotBlank()) {
                    rawTexts += result.text
                    candidates += DateTextParser.extractCandidates(result.text)
                }
            }
            .addOnCompleteListener { run(index + 1) }
    }

    run(0)
}

private fun dateScanBitmapVariants(source: Bitmap): List<Bitmap> {
    val width = source.width
    val height = source.height
    fun crop(leftFraction: Float, topFraction: Float, widthFraction: Float, heightFraction: Float): Bitmap {
        val x = (width * leftFraction).toInt().coerceIn(0, width - 1)
        val y = (height * topFraction).toInt().coerceIn(0, height - 1)
        val cropWidth = (width * widthFraction).toInt().coerceIn(1, width - x)
        val cropHeight = (height * heightFraction).toInt().coerceIn(1, height - y)
        return Bitmap.createBitmap(source, x, y, cropWidth, cropHeight)
    }

    val crops = listOf(
        source,
        crop(0.0f, 0.0f, 1.0f, 0.60f),
        crop(0.0f, 0.25f, 1.0f, 0.35f),
        crop(0.0f, 0.35f, 1.0f, 0.28f),
        crop(0.0f, 0.42f, 1.0f, 0.18f),
        crop(0.0f, 0.48f, 1.0f, 0.16f),
        crop(0.0f, 0.54f, 1.0f, 0.14f),
        crop(0.0f, 0.35f, 0.70f, 0.30f),
        crop(0.20f, 0.35f, 0.65f, 0.30f)
    )
    val scaled = crops.map { upscaleForOcr(it) }
    return crops + scaled + scaled.map { enhanceForDotMatrixText(it) }
}

private fun upscaleForOcr(source: Bitmap): Bitmap =
    Bitmap.createScaledBitmap(source, source.width * 2, source.height * 2, true)

private fun enhanceForDotMatrixText(source: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix().apply {
        setSaturation(0f)
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    1.8f, 0f, 0f, 0f, -70f,
                    0f, 1.8f, 0f, 0f, -70f,
                    0f, 0f, 1.8f, 0f, -70f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
    Canvas(output).drawBitmap(source, 0f, 0f, paint)
    return output
}
