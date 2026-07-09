package com.fridgefinish.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeStyle(val displayName: String, val isPremium: Boolean) {
    OriginalFresh("Original Fresh", false),
    FarmersMarket("Farmers Market", true),
    CitrusClean("Citrus Clean", true),
    PantryPro("Pantry Pro", true),
    BerryBasket("Berry Basket", true),
    FreezerMode("Freezer Mode", true),
    TomatoTimer("Tomato Timer", true),
    MinimalGrocery("Minimal Grocery", true),
    MidnightKitchen("Midnight Kitchen", true)
}

enum class AppearanceMode(val displayName: String) {
    Light("Light"),
    Dark("Dark"),
    FollowSystem("Follow system")
}

@Immutable
data class FridgeFinishSemanticColors(
    val fresh: Color,
    val onFresh: Color,
    val useSoon: Color,
    val onUseSoon: Color,
    val expiring: Color,
    val onExpiring: Color,
    val expired: Color,
    val onExpired: Color,
    val frozen: Color,
    val onFrozen: Color,
    val pantry: Color,
    val onPantry: Color,
    val recipe: Color,
    val onRecipe: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val danger: Color,
    val onDanger: Color,
    val bottomNavContainer: Color,
    val bottomNavSelected: Color,
    val onBottomNavSelected: Color,
    val bottomNavUnselected: Color,
    val card: Color,
    val elevatedCard: Color,
    val chip: Color,
    val onChip: Color,
    val selectedChip: Color,
    val onSelectedChip: Color,
    val primaryButton: Color,
    val onPrimaryButton: Color
)

private data class ThemePack(
    val style: AppThemeStyle,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val lightSemanticColors: FridgeFinishSemanticColors,
    val darkSemanticColors: FridgeFinishSemanticColors
)

private val LocalFridgeFinishSemanticColors = compositionLocalOf { originalFreshLightSemantic }

object FridgeFinishColors {
    val current: FridgeFinishSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFridgeFinishSemanticColors.current
}

@Composable
fun FridgeFinishTheme(
    style: AppThemeStyle = AppThemeStyle.OriginalFresh,
    appearanceMode: AppearanceMode = AppearanceMode.FollowSystem,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
        AppearanceMode.FollowSystem -> isSystemInDarkTheme()
    }
    val pack = themePack(style)
    val scheme = if (darkTheme) pack.darkColorScheme else pack.lightColorScheme
    val semantic = if (darkTheme) pack.darkSemanticColors else pack.lightSemanticColors

    androidx.compose.runtime.CompositionLocalProvider(LocalFridgeFinishSemanticColors provides semantic) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

fun appThemeStyles(): List<AppThemeStyle> = AppThemeStyle.entries

@Composable
fun previewColorScheme(style: AppThemeStyle, appearanceMode: AppearanceMode): ColorScheme {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
        AppearanceMode.FollowSystem -> isSystemInDarkTheme()
    }
    val pack = themePack(style)
    return if (darkTheme) pack.darkColorScheme else pack.lightColorScheme
}

@Composable
fun previewSemanticColors(style: AppThemeStyle, appearanceMode: AppearanceMode): FridgeFinishSemanticColors {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
        AppearanceMode.FollowSystem -> isSystemInDarkTheme()
    }
    val pack = themePack(style)
    return if (darkTheme) pack.darkSemanticColors else pack.lightSemanticColors
}

private fun themePack(style: AppThemeStyle): ThemePack = themePacks.getValue(style)

private fun color(hex: Long): Color = Color(hex)

private fun lightScheme(
    background: Long,
    surface: Long,
    surfaceVariant: Long,
    primary: Long,
    primaryContainer: Long,
    secondary: Long,
    tertiary: Long,
    onBackground: Long,
    onSurface: Long,
    onSurfaceVariant: Long,
    outline: Long
): ColorScheme = lightColorScheme(
    primary = color(primary),
    onPrimary = Color.White,
    primaryContainer = color(primaryContainer),
    onPrimaryContainer = color(onSurface),
    secondary = color(secondary),
    onSecondary = Color.White,
    secondaryContainer = color(surfaceVariant),
    onSecondaryContainer = color(onSurface),
    tertiary = color(tertiary),
    onTertiary = Color.White,
    tertiaryContainer = color(surfaceVariant),
    onTertiaryContainer = color(onSurface),
    background = color(background),
    onBackground = color(onBackground),
    surface = color(surface),
    onSurface = color(onSurface),
    surfaceVariant = color(surfaceVariant),
    onSurfaceVariant = color(onSurfaceVariant),
    outline = color(outline),
    error = color(0xFFB3261E),
    onError = Color.White,
    errorContainer = color(0xFFFFDAD6),
    onErrorContainer = color(0xFF410002)
)

private fun darkScheme(
    background: Long,
    surface: Long,
    surfaceVariant: Long,
    primary: Long,
    primaryContainer: Long,
    secondary: Long,
    tertiary: Long,
    onBackground: Long,
    onSurface: Long,
    onSurfaceVariant: Long,
    outline: Long
): ColorScheme = darkColorScheme(
    primary = color(primary),
    onPrimary = color(0xFF102015),
    primaryContainer = color(primaryContainer),
    onPrimaryContainer = color(onSurface),
    secondary = color(secondary),
    onSecondary = color(0xFF24180D),
    secondaryContainer = color(surfaceVariant),
    onSecondaryContainer = color(onSurface),
    tertiary = color(tertiary),
    onTertiary = color(0xFF241A00),
    tertiaryContainer = color(surfaceVariant),
    onTertiaryContainer = color(onSurface),
    background = color(background),
    onBackground = color(onBackground),
    surface = color(surface),
    onSurface = color(onSurface),
    surfaceVariant = color(surfaceVariant),
    onSurfaceVariant = color(onSurfaceVariant),
    outline = color(outline),
    error = color(0xFFFFB4AB),
    onError = color(0xFF690005),
    errorContainer = color(0xFF93000A),
    onErrorContainer = color(0xFFFFDAD6)
)

private fun semantic(
    scheme: ColorScheme,
    dark: Boolean,
    frozen: Color = if (dark) color(0xFF7ED6D1) else color(0xFFCDEFEF),
    pantry: Color = if (dark) color(0xFFE0B56C) else color(0xFFE9E1D5)
): FridgeFinishSemanticColors {
    val fresh = if (dark) color(0xFF8FD19E) else color(0xFFDCEBDD)
    val useSoon = if (dark) color(0xFFFFD166) else color(0xFFFFF0B8)
    val expiring = if (dark) color(0xFFFFB15C) else color(0xFFFFDDB8)
    val expired = if (dark) color(0xFFFFB4AB) else color(0xFFFFDAD6)
    val darkText = color(0xFF17211C)
    return FridgeFinishSemanticColors(
        fresh = fresh,
        onFresh = if (dark) darkText else color(0xFF12351C),
        useSoon = useSoon,
        onUseSoon = color(0xFF3A2A00),
        expiring = expiring,
        onExpiring = color(0xFF432000),
        expired = expired,
        onExpired = color(0xFF410002),
        frozen = frozen,
        onFrozen = if (dark) darkText else color(0xFF08363A),
        pantry = pantry,
        onPantry = if (dark) color(0xFF2B1B00) else color(0xFF3B2A17),
        recipe = scheme.primaryContainer,
        onRecipe = scheme.onPrimaryContainer,
        success = fresh,
        onSuccess = if (dark) darkText else color(0xFF12351C),
        warning = useSoon,
        onWarning = color(0xFF3A2A00),
        danger = expired,
        onDanger = color(0xFF410002),
        bottomNavContainer = scheme.surface,
        bottomNavSelected = scheme.primaryContainer,
        onBottomNavSelected = scheme.onPrimaryContainer,
        bottomNavUnselected = scheme.onSurfaceVariant,
        card = scheme.surface,
        elevatedCard = scheme.surfaceVariant,
        chip = scheme.surfaceVariant,
        onChip = scheme.onSurfaceVariant,
        selectedChip = scheme.primaryContainer,
        onSelectedChip = scheme.onPrimaryContainer,
        primaryButton = scheme.primary,
        onPrimaryButton = scheme.onPrimary
    )
}

private val originalFreshLightScheme = lightColorScheme(
    primary = color(0xFF149A66),
    onPrimary = Color.White,
    primaryContainer = color(0xFFDDF8EA),
    onPrimaryContainer = color(0xFF083D2A),
    secondary = color(0xFF6FBF45),
    onSecondary = Color.White,
    secondaryContainer = color(0xFFE8F7D8),
    onSecondaryContainer = color(0xFF21390B),
    tertiary = color(0xFF4A8F78),
    onTertiary = Color.White,
    tertiaryContainer = color(0xFFD9F1E8),
    onTertiaryContainer = color(0xFF0A3528),
    background = color(0xFFFCFFFD),
    onBackground = color(0xFF17211C),
    surface = Color.White,
    onSurface = color(0xFF17211C),
    surfaceVariant = color(0xFFF0F7F2),
    onSurfaceVariant = color(0xFF4D5B53),
    outline = color(0xFFC6D2CA),
    error = color(0xFFB3261E),
    onError = Color.White,
    errorContainer = color(0xFFFFE1DD),
    onErrorContainer = color(0xFF410E0B)
)

private val originalFreshDarkScheme = darkColorScheme(
    primary = color(0xFF7ADBAF),
    onPrimary = color(0xFF003824),
    primaryContainer = color(0xFF005137),
    onPrimaryContainer = color(0xFFDDF8EA),
    secondary = color(0xFFA7D982),
    onSecondary = color(0xFF173800),
    secondaryContainer = color(0xFF285100),
    onSecondaryContainer = color(0xFFE8F7D8),
    tertiary = color(0xFF91D8BE),
    onTertiary = color(0xFF00382A),
    tertiaryContainer = color(0xFF0C503E),
    onTertiaryContainer = color(0xFFD9F1E8),
    background = color(0xFF0E1512),
    onBackground = color(0xFFEAF3ED),
    surface = color(0xFF18201C),
    onSurface = color(0xFFEAF3ED),
    surfaceVariant = color(0xFF25312B),
    onSurfaceVariant = color(0xFFC7D3CB),
    outline = color(0xFF43534A),
    error = color(0xFFFFB4AB),
    onError = color(0xFF690005),
    errorContainer = color(0xFF93000A),
    onErrorContainer = color(0xFFFFDAD6)
)

private val originalFreshLightSemantic = semantic(originalFreshLightScheme, dark = false)
private val originalFreshDarkSemantic = semantic(originalFreshDarkScheme, dark = true)

private val themePacks: Map<AppThemeStyle, ThemePack> = buildMap {
    fun add(
        style: AppThemeStyle,
        light: ColorScheme,
        dark: ColorScheme,
        lightFrozen: Color = color(0xFFCDEFEF),
        darkFrozen: Color = color(0xFF7ED6D1),
        lightPantry: Color = light.surfaceVariant,
        darkPantry: Color = dark.tertiary
    ) {
        put(style, ThemePack(style, light, dark, semantic(light, false, lightFrozen, lightPantry), semantic(dark, true, darkFrozen, darkPantry)))
    }

    add(AppThemeStyle.OriginalFresh, originalFreshLightScheme, originalFreshDarkScheme)
    add(AppThemeStyle.FarmersMarket,
        lightScheme(0xFFFBF7ED, 0xFFFFFFFF, 0xFFEFE6D4, 0xFF4F7D3A, 0xFFDDEFCF, 0xFFA65F2B, 0xFFE6A93C, 0xFF252117, 0xFF252117, 0xFF5F584B, 0xFFD9CBB4),
        darkScheme(0xFF15130D, 0xFF211E15, 0xFF302A1C, 0xFFA8D98A, 0xFF26391D, 0xFFE2A06A, 0xFFF0C15A, 0xFFF8F0DC, 0xFFF8F0DC, 0xFFD8CCB6, 0xFF4A402E)
    )
    add(AppThemeStyle.CitrusClean,
        lightScheme(0xFFFCFFF3, 0xFFFFFFFF, 0xFFEFF6D8, 0xFF6E9F20, 0xFFE7F7C6, 0xFFE19B22, 0xFFF5D547, 0xFF1F2416, 0xFF1F2416, 0xFF5D6648, 0xFFD4DEC0),
        darkScheme(0xFF10140B, 0xFF1A2112, 0xFF26301A, 0xFFB9E96D, 0xFF2D3F15, 0xFFF2B94B, 0xFFFFE066, 0xFFF7FBEF, 0xFFF7FBEF, 0xFFD5E0C8, 0xFF3C4A2D)
    )
    add(AppThemeStyle.PantryPro,
        lightScheme(0xFFF7F4EE, 0xFFFFFFFF, 0xFFE9E1D5, 0xFF48634A, 0xFFDCE9DD, 0xFF6F5A42, 0xFFB9823B, 0xFF25231F, 0xFF25231F, 0xFF5F5A50, 0xFFD0C6B8),
        darkScheme(0xFF111310, 0xFF1D211C, 0xFF282E26, 0xFF9BC79F, 0xFF263829, 0xFFCBB99E, 0xFFE0A85A, 0xFFF1F3EE, 0xFFF1F3EE, 0xFFCBD2C7, 0xFF3C443A)
    )
    add(AppThemeStyle.BerryBasket,
        lightScheme(0xFFFFF7FA, 0xFFFFFFFF, 0xFFF3E3EA, 0xFF8A2F52, 0xFFF8D7E4, 0xFF557A3A, 0xFFE6A13D, 0xFF2A1F24, 0xFF2A1F24, 0xFF66565D, 0xFFDECBD3),
        darkScheme(0xFF151014, 0xFF21171D, 0xFF30212A, 0xFFF09ABD, 0xFF442333, 0xFFA6D47E, 0xFFF0B55B, 0xFFFFF1F6, 0xFFFFF1F6, 0xFFE2CAD4, 0xFF4A3540)
    )
    add(AppThemeStyle.FreezerMode,
        lightScheme(0xFFF3FAFA, 0xFFFFFFFF, 0xFFDDEEEF, 0xFF247C7A, 0xFFCDEFEF, 0xFF50666A, 0xFF8AC6D1, 0xFF172426, 0xFF172426, 0xFF526366, 0xFFC2D8DA),
        darkScheme(0xFF0D1416, 0xFF162123, 0xFF203033, 0xFF7ED6D1, 0xFF173A3A, 0xFFA8C4C9, 0xFF9DE3EF, 0xFFEEF9FA, 0xFFEEF9FA, 0xFFC5DADD, 0xFF34484B),
        lightFrozen = color(0xFFDDEEEF),
        darkFrozen = color(0xFF9DE3EF)
    )
    add(AppThemeStyle.TomatoTimer,
        lightScheme(0xFFFFF8F3, 0xFFFFFFFF, 0xFFF3E3D8, 0xFFB6422B, 0xFFFFD9CF, 0xFF5E7345, 0xFFE6B044, 0xFF2A211D, 0xFF2A211D, 0xFF665950, 0xFFDDCCC2),
        darkScheme(0xFF170F0D, 0xFF231916, 0xFF33241F, 0xFFFF9A84, 0xFF4A2018, 0xFFB5D28C, 0xFFF4C766, 0xFFFFF2EC, 0xFFFFF2EC, 0xFFE6D0C7, 0xFF4A3932)
    )
    add(AppThemeStyle.MinimalGrocery,
        lightScheme(0xFFFAFAF7, 0xFFFFFFFF, 0xFFEFEFEA, 0xFF2F5D3A, 0xFFDCEBDD, 0xFF70756C, 0xFFD49A3A, 0xFF1F211D, 0xFF1F211D, 0xFF5E625A, 0xFFD4D6CE),
        darkScheme(0xFF0E0F0D, 0xFF191B17, 0xFF252820, 0xFF8FD19E, 0xFF1F3524, 0xFFB8BDB3, 0xFFE8B45A, 0xFFF4F6F0, 0xFFF4F6F0, 0xFFCACFC4, 0xFF3A3E35)
    )
    add(AppThemeStyle.MidnightKitchen,
        lightScheme(0xFFF6F8F4, 0xFFFFFFFF, 0xFFE6EDE3, 0xFF3F7D48, 0xFFDAF0DD, 0xFF7A6648, 0xFFDDA64A, 0xFF1F241F, 0xFF1F241F, 0xFF5A6358, 0xFFCAD4C7),
        darkScheme(0xFF0F1110, 0xFF1A1D1A, 0xFF252A25, 0xFF7FD889, 0xFF1F3A24, 0xFFE0B56C, 0xFFFFCF70, 0xFFF3F7F1, 0xFFF3F7F1, 0xFFCAD4C8, 0xFF394139)
    )
}
