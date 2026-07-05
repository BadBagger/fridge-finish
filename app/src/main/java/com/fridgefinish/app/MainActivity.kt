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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
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
                                PlusStatusChip(uiState.subscription)
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
                    screen == Screen.RECIPES -> RecipeIdeasScreen(uiState, onOpenPlus = { screen = Screen.PLUS })
                    screen == Screen.RESTOCK -> RestockScreen(uiState, onOpenPlus = { screen = Screen.PLUS }, viewModel::saveRestock, viewModel::toggleRestock, viewModel::deleteRestock)
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
            Text("Know what to eat before it goes bad.", style = MaterialTheme.typography.titleLarge)
            Text("Expiration dates are reminders, not safety guarantees.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                CountCard("Past date", uiState.expiredCount, Modifier.weight(1f))
                CountCard("Today", uiState.expiresTodayCount, Modifier.weight(1f))
                CountCard("Eat soon", uiState.eatSoonCount, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddFood) { Text("Add food") }
                Button(onClick = onScanBarcode) { Text("Scan barcode") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddLeftovers) { Text("Add leftovers") }
                OutlinedButton(onClick = onAddRestock) { Text("Restock item") }
            }
        }
        section("Eat first", uiState.topPriority.filter { uiState.statusOf(it) in listOf(FreshnessStatus.EXPIRED, FreshnessStatus.EAT_SOON) }, uiState, onEdit, onFinish)
        section("Expires today", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.EXPIRES_TODAY }, uiState, onEdit, onFinish)
        section("Coming up", uiState.activeFoods.filter { uiState.statusOf(it) == FreshnessStatus.FRESH }.take(5), uiState, onEdit, onFinish)
        item {
            Text("Restock list", style = MaterialTheme.typography.titleMedium)
            if (uiState.restockOpen.isEmpty()) Text("Nothing to restock right now.")
        }
        items(uiState.restockOpen.take(5), key = { it.id }) { Text("- ${it.name}${it.quantity?.let { q -> " - $q" }.orEmpty()}") }
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
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (foodItems.isEmpty()) Text("No items here.")
    }
    items(foodItems, key = { "$title-${it.id}" }) { item ->
        FoodCard(item, uiState.statusOf(item), onEdit, onFinish, onDelete = null)
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium)
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
                PlusAccessCard(uiState.subscription)
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
                        leadingIcon = if (!locked && option != FoodLocation.FRIDGE && uiState.subscription.isPlus) {
                            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
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
private fun PlusAccessCard(subscriptionState: FridgeFinishSubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (subscriptionState.hasAdminAccess) "Admin Plus is active" else "Fridge Finish Plus is active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Multiple storage locations, recipes, and smart shopping tools are unlocked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
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
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                item.imageUri?.takeIf { it.isNotBlank() }?.let { imageUri ->
                    AsyncImage(
                        model = imageUri,
                        contentDescription = item.name,
                        modifier = Modifier.size(48.dp).padding(end = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.name.ifBlank { "Unnamed food" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${item.location.label} - ${item.category.label} - ${FreshnessCalculator.daysRemainingText(item.expirationDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Expires ${item.expirationDate}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                item.quantity?.takeIf { it.isNotBlank() }?.let {
                    Text("Qty $it ${item.unit.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { onFinish(item) }) { Icon(Icons.Default.Check, contentDescription = "Finished") }
                IconButton(onClick = { onEdit(item) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                if (onDelete != null) IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
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
    val have: List<String>,
    val missing: List<String>,
    val urgentCount: Int,
    val note: String,
    val steps: String,
    val sourceName: String
)

@Composable
private fun RecipeIdeasScreen(uiState: FridgeFinishUiState, onOpenPlus: () -> Unit) {
    if (!uiState.subscription.isPlus) {
        PlusLockedScreen(
            title = "Recipe ideas are in Plus",
            body = "Fridge Finish Plus suggests recipe ideas from ingredients that are expiring soon. Your basic expiration list stays free.",
            onOpenPlus = onOpenPlus
        )
        return
    }
    val ideas = remember(uiState.activeFoods) { buildRecipeIdeas(uiState) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Use what you have", style = MaterialTheme.typography.titleLarge)
            Text("AI-built local recipe ideas based on your current food. Check dates and use your judgment.")
        }
        item {
            Text("Ready or close", style = MaterialTheme.typography.titleMedium)
            if (ideas.isEmpty()) Text("Add a few foods to see ideas here.")
        }
        items(ideas, key = { it.title }) { idea ->
            RecipeIdeaCard(idea)
        }
    }
}

@Composable
private fun RecipeIdeaCard(idea: RecipeIdea) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(idea.title, style = MaterialTheme.typography.titleMedium)
                    Text("${idea.minutes} min - ${if (idea.missing.isEmpty()) "You can make this" else "Almost there"}")
                }
                if (idea.urgentCount > 0) {
                    AssistChip(onClick = {}, label = { Text("Uses eat soon") })
                }
            }
            Text(idea.note, style = MaterialTheme.typography.bodyMedium)
            Text(idea.steps, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                idea.have.take(4).forEach {
                    AssistChip(onClick = {}, label = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
            if (idea.missing.isNotEmpty()) {
                Text("Missing: ${idea.missing.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(idea.sourceName, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildRecipeIdeas(uiState: FridgeFinishUiState): List<RecipeIdea> {
    val foods = uiState.activeFoods
    val ingredientsByRecipe = uiState.recipeIngredients.groupBy { it.recipeId }

    return uiState.recipes.mapNotNull { recipe ->
        val ingredients = ingredientsByRecipe[recipe.id].orEmpty()
        val matchedFoods = ingredients.mapNotNull { ingredient -> foods.firstOrNull { food -> ingredient.matches(food) } }
        val haveItems = matchedFoods.map { it.name.ifBlank { it.category.label } }
        val missing = ingredients.filter { ingredient -> foods.none { food -> ingredient.matches(food) } && ingredient.required }.map { it.label }
        if (haveItems.isEmpty() || missing.size > 2) return@mapNotNull null
        val urgent = matchedFoods.count { food -> uiState.statusOf(food) in listOf(FreshnessStatus.EXPIRES_TODAY, FreshnessStatus.EAT_SOON, FreshnessStatus.EXPIRED) }
        RecipeIdea(
            recipe.title,
            recipe.minutes,
            haveItems.distinct(),
            missing,
            urgent,
            recipe.description,
            recipe.steps,
            recipe.sourceName
        )
    }.sortedWith(
        compareBy<RecipeIdea> { it.missing.size }
            .thenByDescending { it.urgentCount }
            .thenBy { it.minutes }
    )
}

private fun RecipeIngredientEntity.matches(food: FoodItemEntity): Boolean {
    val keywordMatch = keywords.split(",").any { keyword ->
        val trimmed = keyword.trim()
        trimmed.isNotBlank() && food.name.contains(trimmed, ignoreCase = true)
    }
    val categoryMatch = category?.let { food.category.name.equals(it, ignoreCase = true) } ?: false
    return keywordMatch || categoryMatch
}

private fun String.hasAny(vararg terms: String): Boolean =
    terms.any { contains(it, ignoreCase = true) }

@Composable
private fun StatusChip(status: FreshnessStatus) {
    val color = when (status) {
        FreshnessStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
        FreshnessStatus.EXPIRES_TODAY -> Color(0xFFFFE1A8)
        FreshnessStatus.EAT_SOON -> Color(0xFFD8EAD2)
        FreshnessStatus.FRESH -> MaterialTheme.colorScheme.surfaceVariant
        FreshnessStatus.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    AssistChip(onClick = {}, label = { Text(status.label) }, colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = color))
}

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
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }
    val availableLocations = if (subscriptionState.isPlus) {
        FoodLocation.entries
    } else {
        (listOf(FoodLocation.FRIDGE) + initial.location).distinct()
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Dates are reminders, not safety guarantees.", style = MaterialTheme.typography.bodyMedium)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(name, { name = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedButton(onClick = onScanBarcode) { Text("Scan barcode") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleMenu("Location", location.label, availableLocations.map { it.label }) { label -> location = availableLocations.first { it.label == label } }
                SimpleMenu("Category", category.label, FoodCategory.entries.map { it.label }) { label ->
                    category = FoodCategory.fromLabel(label)
                    reminderDays = FreshnessCalculator.defaultReminderDays(category).toString()
                }
            }
            if (!subscriptionState.isPlus) {
                Text("Free uses Main Fridge and standard reminder timing.")
                TextButton(onClick = onOpenPlus) { Text("View Plus organization") }
            }
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.fillMaxWidth())
            DateInput("Purchase date", purchaseDate) { purchaseDate = it }
            DateInput("Opened date", openedDate) { openedDate = it }
            DateInput("Expiration date", expirationDate) { expirationDate = it }
            if (subscriptionState.isPlus) {
                OutlinedTextField(reminderDays, { reminderDays = it }, label = { Text("Reminder days before expiration") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(imageUri, { imageUri = it }, label = { Text("Product image URL") }, modifier = Modifier.fillMaxWidth())
            imageUri.takeIf { it.isNotBlank() }?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Product image preview",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Fit
                )
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text(if (imageUri.isBlank()) "Attach photo" else "Photo attached") }
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
                }) { Text("Save") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun RestockScreen(
    uiState: FridgeFinishUiState,
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
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Shopping / Restock list", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(RestockItemEntity(name = name.trim(), quantity = quantity.takeIf { it.isNotBlank() }))
                    name = ""
                    quantity = ""
                }
            }) { Text("Add manually") }
        }
        items(uiState.restock, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.isPurchased, onCheckedChange = { onToggle(item) })
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.quantity.orEmpty())
                    }
                    IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
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
            Text("Settings / About", style = MaterialTheme.typography.titleLarge)
            Text("Fridge Finish stores your food list on this device. No account is required, and there is no cloud sync.")
            SettingsCard("Fridge Finish Plus") {
                Text("Current plan: ${subscriptionState.planLabel}")
                Text("Free slots used: ${subscriptionState.activeItemCount}/${FridgeFinishSubscriptionState.FREE_ITEM_LIMIT}")
                if (subscriptionState.hasAdminAccess) {
                    Text("Admin build access is on. Plus features are unlocked for testing.")
                }
                if (!subscriptionState.isPlus && subscriptionState.freeSlotsRemaining == 0) {
                    Text("You reached the free item limit. Plus unlocks unlimited items.")
                }
                subscriptionState.billingMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenPlus) { Text("View Plus") }
                    OutlinedButton(onClick = onRestorePurchases) { Text("Restore") }
                }
            }
            Text("Notifications are local reminders before food dates arrive. If notifications are disabled, reminders will not appear.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text("Ask for notification permission") }
            OutlinedButton(onClick = onAddSamples) { Text("Add sample data") }
            Text("Fridge Finish helps you organize food dates and reminders. It does not determine whether food is safe to eat.")
            Text("Use your judgment and check before eating, especially when something is past date.")
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
            Text("Fridge Finish Plus", style = MaterialTheme.typography.titleLarge)
            Text("A fair upgrade for deeper organization and planning. Basic fridge tracking stays free.")
        }
        item {
            PlanCard("Free", FridgeFinishPlans.freeBenefits)
        }
        item {
            PlanCard("Plus", FridgeFinishPlans.plusBenefits)
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

private val FridgeFinishSubscriptionState.planLabel: String
    get() = when {
        hasAdminAccess -> "Admin Plus"
        isPlus -> "Plus"
        else -> "Free"
    }

@Composable
private fun PlanCard(title: String, benefits: List<String>) {
    SettingsCard(title) {
        benefits.forEach { benefit ->
            Text("- $benefit")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInput(label: String, value: String, onValueChange: (String) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showLiveScanner by rememberSaveable { mutableStateOf(false) }
    var scanMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedText by rememberSaveable { mutableStateOf("") }
    var detectedDates by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val selectedMillis = parseDate(value)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("$label (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showPicker = true }) { Text("Pick $label") }
            OutlinedButton(
                onClick = {
                    showLiveScanner = !showLiveScanner
                    scanMessage = if (!showLiveScanner) "Hold the printed date inside the camera view." else null
                    scannedText = ""
                    detectedDates = emptyList()
                }
            ) { Text(if (showLiveScanner) "Close scanner" else "Scan $label") }
            if (value.isNotBlank()) {
                TextButton(onClick = { onValueChange("") }) { Text("Clear") }
            }
        }
        if (showLiveScanner) {
            DateOcrLiveScanner(
                onResult = { rawText, candidates ->
                    scannedText = rawText.replace('\n', ' ').trim()
                    detectedDates = candidates.map { it.toString() }
                    if (detectedDates.isNotEmpty()) {
                        onValueChange(detectedDates.first())
                        scanMessage = "Found ${detectedDates.first()}. Check it before saving."
                    } else if (scannedText.isNotBlank()) {
                        scanMessage = "Reading text, but no date matched yet."
                    }
                },
                onClose = { showLiveScanner = false }
            )
        }
        scanMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (detectedDates.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detectedDates.take(3).forEach { candidate ->
                    AssistChip(
                        onClick = { onValueChange(candidate) },
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

@Composable
private fun DateOcrLiveScanner(
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hold only the printed date in view. Avoid the nutrition label.", style = MaterialTheme.typography.bodySmall)
        if (hasPermission) {
            LiveDateCamera(onResult)
        } else {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Allow camera")
            }
        }
        OutlinedButton(onClick = onClose) { Text("Done scanning") }
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

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        factory = { viewContext ->
            val previewView = PreviewView(viewContext)
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
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFF9BD8C5),
                secondary = Color(0xFFF2C879),
                tertiary = Color(0xFFAFCBFF)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF245C4F),
                secondary = Color(0xFF8A5A16),
                tertiary = Color(0xFF405F91)
            )
        },
        content = content
    )
}
