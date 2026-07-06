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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fridgefinish.app.data.FoodItemEntity
import com.fridgefinish.app.data.RecipeEntity
import com.fridgefinish.app.data.RecipeIngredientEntity
import com.fridgefinish.app.data.RestockItemEntity
import com.fridgefinish.app.domain.FoodCategory
import com.fridgefinish.app.domain.FoodLocation
import com.fridgefinish.app.domain.FreshnessCalculator
import com.fridgefinish.app.domain.FreshnessStatus
import com.fridgefinish.app.domain.DateTextParser
import com.fridgefinish.app.domain.cleanShoppingName
import com.fridgefinish.app.domain.missingItemsNotAlreadyInShop
import com.fridgefinish.app.domain.recipeIngredientMatchesFood
import com.fridgefinish.app.subscription.FeatureGateResult
import com.fridgefinish.app.subscription.FridgeFinishFeatureGate
import com.fridgefinish.app.subscription.FridgeFinishPlans
import com.fridgefinish.app.subscription.FridgeFinishSubscriptionState
import com.fridgefinish.app.subscription.PlusFeature
import com.fridgefinish.app.ui.BarcodeLookupState
import com.fridgefinish.app.ui.BarcodeScannerScreen
import com.fridgefinish.app.ui.FridgeFinishUiState
import com.fridgefinish.app.ui.FridgeFinishViewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
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
    SCAN("Scan")
}

private val storageScreens = listOf(
    Screen.FRIDGE,
    Screen.FREEZER,
    Screen.PANTRY,
    Screen.GARAGE_FREEZER,
    Screen.MINI_FRIDGE,
    Screen.OTHER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeFinishApp(viewModel: FridgeFinishViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val barcodeLookup by viewModel.barcodeLookup.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var editingFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var addingFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var upgradePrompt by remember { mutableStateOf<FeatureGateResult.UpgradeRequired?>(null) }
    var shopMessage by rememberSaveable { mutableStateOf<String?>(null) }

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

    FridgeFinishTheme {
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
            topBar = {
                TopAppBar(
                    title = {
                        if (editingFood != null || addingFood != null) {
                            Text("Food item")
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
                if (editingFood == null && addingFood == null && screen !in listOf(Screen.RESTOCK, Screen.SETTINGS, Screen.PLUS, Screen.SCAN)) {
                    FloatingActionButton(onClick = { addFoodIfAllowed(viewModel.presetFood(FoodCategory.OTHER)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add food")
                    }
                }
            },
            bottomBar = {
                if (editingFood == null && addingFood == null) {
                    NavigationBar {
                        listOf(Screen.TODAY, Screen.FRIDGE, Screen.RECIPES, Screen.RESTOCK, Screen.SETTINGS).forEach { item ->
                            NavigationBarItem(
                                selected = screen == item || (item == Screen.FRIDGE && screen in storageScreens),
                                onClick = { screen = item },
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
                when {
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
                        onFinish = { viewModel.markFinished(it) }
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
                        onFinish = { viewModel.markFinished(it) },
                        onDelete = viewModel::deleteFood
                    ) { editingFood = it }
                    screen == Screen.RECIPES -> RecipeIdeasScreen(
                        uiState = uiState,
                        onOpenPlus = { screen = Screen.PLUS },
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
                                        quantity = "For ${idea.title}"
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
                        onOpenPlus = { screen = Screen.PLUS },
                        onRestorePurchases = viewModel::restorePlusPurchases,
                        onAddSamples = viewModel::addSampleData
                    )
                    screen == Screen.PLUS -> FridgeFinishPlusScreen(
                        subscriptionState = uiState.subscription,
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
                        onCancel = {
                            viewModel.clearBarcodeLookup()
                            screen = Screen.TODAY
                        }
                    )
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
    onFinish: (FoodItemEntity) -> Unit
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
        section("Eat first", uiState.topPriority.filter { uiState.statusOf(it) in listOf(FreshnessStatus.EXPIRED, FreshnessStatus.EAT_SOON) }, uiState, onEdit, onFinish)
        section("Expires today", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.EXPIRES_TODAY }, uiState, onEdit, onFinish)
        section("Coming up", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.FRESH }.take(5), uiState, onEdit, onFinish)
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
    onFinish: (FoodItemEntity) -> Unit
) {
    item {
        SectionHeader(title, foodItems.size)
        if (foodItems.isEmpty()) {
            EmptySectionCard(title)
        }
    }
    items(foodItems, key = { "$title-${it.id}" }) { item ->
        FoodCard(item, uiState.statusOf(item), onEdit, onFinish, onDelete = null)
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
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
    val countColor = when (status) {
        FreshnessStatus.EXPIRED -> MaterialTheme.colorScheme.error
        FreshnessStatus.EXPIRES_TODAY -> MaterialTheme.colorScheme.secondary
        FreshnessStatus.EAT_SOON -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = countColor)
            Text(label, style = MaterialTheme.typography.labelMedium)
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
        if (foods.isEmpty()) {
            item {
                EmptyStorageCard(location, uiState.subscription)
            }
        }
        items(foods, key = { it.id }) { item ->
            FoodCard(item, uiState.statusOf(item), onEdit, onFinish, onDelete)
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
            if (subscriptionState.hasAdminAccess) "Admin Plus active" else "Plus active",
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

@Composable
private fun FoodCard(
    item: FoodItemEntity,
    status: FreshnessStatus,
    onEdit: (FoodItemEntity) -> Unit,
    onFinish: (FoodItemEntity) -> Unit,
    onDelete: ((FoodItemEntity) -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                OutlinedButton(onClick = { onFinish(item) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
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
    val missing: List<String>,
    val urgentCount: Int,
    val note: String,
    val steps: String
)

@Composable
private fun RecipeIdeasScreen(
    uiState: FridgeFinishUiState,
    onOpenPlus: () -> Unit,
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
    val ideas = remember(uiState.activeFoods, uiState.recipes, uiState.recipeIngredients) { buildRecipeIdeas(uiState) }
    val readyIdeas = ideas.filter { it.missing.isEmpty() }
    val almostIdeas = ideas.filter { it.missing.isNotEmpty() }
    var selectedRecipe by remember { mutableStateOf<RecipeIdea?>(null) }
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
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RecipeHeroCard(
                ideas = ideas,
                uiState = uiState,
                onOpenRecipe = { selectedRecipe = it }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                RecipeMetricCard("Ready", readyIdeas.size, Modifier.weight(1f))
                RecipeMetricCard("Almost", almostIdeas.size, Modifier.weight(1f))
                RecipeMetricCard("Eat soon", ideas.count { it.urgentCount > 0 }, Modifier.weight(1f))
            }
        }
        item {
            MealPlanCard(
                ideas = ideas,
                onOpenRecipe = { selectedRecipe = it }
            )
        }
        item {
            SectionHeader("Ready now", readyIdeas.size)
            if (readyIdeas.isEmpty()) EmptyRecipeCard("No ready recipes yet. Add a few staple items or scan food you already have.")
        }
        items(readyIdeas, key = { "ready-${it.title}" }) { idea ->
            RecipeIdeaCard(idea, onAddMissingToRestock, onMoreInfo = { selectedRecipe = idea })
        }
        item {
            SectionHeader("Almost there", almostIdeas.size)
            if (almostIdeas.isEmpty()) EmptyRecipeCard("No almost-ready ideas right now.")
        }
        items(almostIdeas, key = { "almost-${it.title}" }) { idea ->
            RecipeIdeaCard(idea, onAddMissingToRestock, onMoreInfo = { selectedRecipe = idea })
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
                            Text(if (best.missing.isEmpty()) "You can make this with what you have." else "You only need ${best.missing.size} more item${if (best.missing.size == 1) "" else "s"}.")
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
    onAddMissingToRestock: (RecipeIdea) -> Unit,
    onMoreInfo: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(idea.title, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("${idea.minutes} min")
                        Text(idea.servings)
                        Text(if (idea.missing.isEmpty()) "Ready now" else "Almost there", color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (idea.urgentCount > 0) {
                    AssistChip(onClick = {}, label = { Text("Uses eat soon") })
                }
            }
            Text(idea.note, style = MaterialTheme.typography.bodyMedium)
            Text(idea.steps, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("You have", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                idea.have.take(4).forEach {
                    AssistChip(onClick = {}, label = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
            if (idea.missing.isNotEmpty()) {
                Text("Add to make it", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    idea.missing.forEach {
                        AssistChip(onClick = {}, label = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }
                }
                Button(onClick = { onAddMissingToRestock(idea) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add missing to Shop")
                }
            }
            OutlinedButton(onClick = onMoreInfo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("More info")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeInfoDialog(
    idea: RecipeIdea,
    onDismiss: () -> Unit,
    onAddMissingToRestock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (idea.missing.isNotEmpty()) {
                TextButton(onClick = onAddMissingToRestock) { Text("Add missing") }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
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
                Text("${idea.minutes} minutes - ${idea.servings}")
                Text(idea.note)
                Text("Portions", style = MaterialTheme.typography.labelLarge)
                idea.portions.forEach { portion ->
                    Text(portion)
                }
                Text("For a family: double the portions for 4+ servings, or add a simple side if you are short on one ingredient.", style = MaterialTheme.typography.bodySmall)
                Text("Use first", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    idea.have.take(6).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                }
                if (idea.missing.isNotEmpty()) {
                    Text("Need to buy", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        idea.missing.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                    }
                }
                Text("Steps", style = MaterialTheme.typography.labelLarge)
                idea.detailSteps().forEachIndexed { index, step ->
                    Text("${index + 1}. $step")
                }
                Text("Dates are reminders, not safety guarantees. Check before eating.", style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}

private fun buildRecipeIdeas(uiState: FridgeFinishUiState): List<RecipeIdea> {
    val foods = uiState.activeFoods
    val ingredientsByRecipe = uiState.recipeIngredients.groupBy { it.recipeId }

    return uiState.recipes.mapNotNull { recipe ->
        val ingredients = ingredientsByRecipe[recipe.id].orEmpty()
        val matchedFoods = ingredients.mapNotNull { ingredient -> foods.firstOrNull { food -> ingredient.matches(food) } }
        val haveItems = matchedFoods.map { it.name.ifBlank { it.category.label }.cleanShoppingName() }
        val missing = ingredients
            .filter { ingredient -> foods.none { food -> ingredient.matches(food) } && ingredient.required }
            .map { it.shoppingLabel() }
            .distinct()
        if (haveItems.isEmpty() || missing.size > 2) return@mapNotNull null
        val urgent = matchedFoods.count { food -> uiState.statusOf(food) in listOf(FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON, FreshnessStatus.EXPIRED) }
        RecipeIdea(
            recipe.title,
            recipe.minutes,
            recipe.estimatedServings(),
            recipe.portionGuidance(),
            haveItems.distinct(),
            missing,
            urgent,
            recipe.description,
            recipe.steps
        )
    }.sortedWith(
        compareBy<RecipeIdea> { it.missing.size }
            .thenByDescending { it.urgentCount }
            .thenBy { it.minutes }
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
    val color = when (status) {
        FreshnessStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
        FreshnessStatus.EXPIRES_TODAY -> MaterialTheme.colorScheme.secondaryContainer
        FreshnessStatus.EAT_SOON -> MaterialTheme.colorScheme.primaryContainer
        FreshnessStatus.FRESH -> MaterialTheme.colorScheme.surfaceVariant
        FreshnessStatus.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    AssistChip(onClick = {}, label = { Text(status.label) }, colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = color))
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
    var expirationDate by rememberSaveable(initial.id) { mutableStateOf(initial.expirationDate.toString()) }
    var reminderDays by rememberSaveable(initial.id) { mutableStateOf(initial.reminderDaysBefore.toString()) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes.orEmpty()) }
    var imageUri by rememberSaveable(initial.id) { mutableStateOf(initial.imageUri.orEmpty()) }
    var barcode by rememberSaveable(initial.id) { mutableStateOf(initial.barcode.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showDetails by rememberSaveable(initial.id) {
        mutableStateOf(
            quantity.isNotBlank() ||
                unit.isNotBlank() ||
                purchaseDate.isNotBlank() ||
                openedDate.isNotBlank() ||
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
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (hasScannedProduct) {
            item {
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
                        imageUri.takeIf { it.isNotBlank() }?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Product image preview",
                                modifier = Modifier.size(72.dp),
                                contentScale = ContentScale.Crop
                            )
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
                Text(if (showDetails) "Hide details" else "More details")
            }
        }
        if (showDetails) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        DateInput("Purchase date", purchaseDate) { purchaseDate = it }
                        DateInput("Opened date", openedDate) { openedDate = it }
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
                    val parsedExpiration = parseDate(expirationDate)
                    if (name.isBlank() || parsedExpiration == null) {
                        error = "Add a food name and a valid expiration date."
                        return@Button
                    }
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            location = location,
                            category = category,
                            quantity = quantity.takeIf { it.isNotBlank() },
                            unit = unit.takeIf { it.isNotBlank() },
                            purchaseDate = parseDate(purchaseDate),
                            openedDate = parseDate(openedDate),
                            expirationDate = parsedExpiration,
                            reminderDaysBefore = if (subscriptionState.isPlus) {
                                reminderDays.toIntOrNull()?.coerceAtLeast(0) ?: FreshnessCalculator.defaultReminderDays(category)
                            } else {
                                FreshnessCalculator.defaultReminderDays(category)
                            },
                            notes = notes.takeIf { it.isNotBlank() },
                            imageUri = imageUri.takeIf { it.isNotBlank() },
                            barcode = barcode.takeIf { it.isNotBlank() }
                        )
                    )
                }, modifier = Modifier.weight(1f)) { Text("Save food") }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
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
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity or note") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            onSave(RestockItemEntity(name = name.trim(), quantity = quantity.takeIf { it.isNotBlank() }))
                            name = ""
                            quantity = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add item") }
                }
            }
        }
        item {
            SectionHeader("Need to buy", openItems.size)
            if (openItems.isEmpty()) EmptySectionCard("Restock")
        }
        items(openItems, key = { "open-${it.id}" }) { item ->
            RestockCard(item, onToggle, onDelete)
        }
        if (purchasedItems.isNotEmpty()) {
            item { SectionHeader("Purchased", purchasedItems.size) }
            items(purchasedItems, key = { "done-${it.id}" }) { item ->
                RestockCard(item, onToggle, onDelete)
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
    onDelete: (RestockItemEntity) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isPurchased, onCheckedChange = { onToggle(item) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name.cleanShoppingName(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.quantity?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.category?.let {
                    AssistChip(onClick = {}, label = { Text(it.label) })
                }
            }
            IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun SettingsScreen(
    subscriptionState: FridgeFinishSubscriptionState,
    onOpenPlus: () -> Unit,
    onRestorePurchases: () -> Unit,
    onAddSamples: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SettingsHeroCard(subscriptionState)
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
                if (subscriptionState.hasAdminAccess) {
                    Text("Admin build access is on. Plus features are unlocked for testing.")
                }
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
            SettingsCard("Food date note") {
                Text("Fridge Finish helps you organize food dates and reminders. It does not determine whether food is safe to eat.")
                Text("Use your judgment and check before eating, especially when something is past date.")
            }
        }
    }
}

@Composable
private fun SettingsHeroCard(subscriptionState: FridgeFinishSubscriptionState) {
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
                    "Basic fridge tracking is free. Plus unlocks deeper organization."
                },
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
    onStartPurchase: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PlusHeroCard(subscriptionState)
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
                    if (subscriptionState.hasAdminAccess) {
                        "Admin build access is on. Plus features are unlocked for testing. Google Play Billing is still not connected for public purchases."
                    } else {
                        "Google Play Billing is not connected in this build. The app will not start a real purchase or fake an upgrade until billing is implemented."
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
                if (subscriptionState.isPlus) "Plus is active" else "Upgrade from tracking to finishing",
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
        isPlus -> "Plus"
        else -> "Free"
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

@Composable
private fun FridgeFinishTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF149A66),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDF8EA),
            onPrimaryContainer = Color(0xFF083D2A),
            secondary = Color(0xFF6FBF45),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8F7D8),
            onSecondaryContainer = Color(0xFF21390B),
            tertiary = Color(0xFF4A8F78),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFD9F1E8),
            onTertiaryContainer = Color(0xFF0A3528),
            background = Color(0xFFFCFFFD),
            onBackground = Color(0xFF17211C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF17211C),
            surfaceVariant = Color(0xFFF0F7F2),
            onSurfaceVariant = Color(0xFF4D5B53),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFFFE1DD),
            onErrorContainer = Color(0xFF410E0B)
        ),
        content = content
    )
}
