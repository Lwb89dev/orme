package it.orma.cammino.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.orma.cammino.R

@Composable
fun IllustratedBanner(
    @DrawableRes artworkRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
) {
    val shape = RoundedCornerShape(14.dp)
    Image(
        painter = painterResource(artworkRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .border(BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant), shape),
    )
}

@DrawableRes
fun chapterArtworkRes(chapterId: String): Int = when (chapterId) {
    "story.mantello.001" -> R.drawable.art_chapter_mantello
    "story.rinuncia.001" -> R.drawable.art_chapter_rinuncia
    "story.lebbrosi.001" -> R.drawable.art_chapter_lebbrosi
    "story.lupo.001" -> R.drawable.art_chapter_lupo
    "story.tempesta.001" -> R.drawable.art_chapter_tempesta
    else -> R.drawable.art_mode_viandante
}
