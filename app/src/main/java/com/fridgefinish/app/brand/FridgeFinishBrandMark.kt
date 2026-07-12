package com.fridgefinish.app.brand

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.fridgefinish.app.R

@Composable
fun FridgeFinishBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Fridge Finish"
) {
    Image(
        painter = painterResource(id = R.drawable.ic_fridge_finish_brand),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
