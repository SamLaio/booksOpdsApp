package com.example.booksopdsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun BookCard(
    book: OpdsBook,
    canNavigate: Boolean,
    canRead: Boolean,
    onOpen: () -> Unit
) {
    val canOpen = canNavigate || canRead
    val authorStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value + 2).sp
    )
    val summaryStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = (MaterialTheme.typography.bodySmall.fontSize.value + 2).sp
    )
    val hintStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = (MaterialTheme.typography.labelSmall.fontSize.value + 2).sp
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpen, onClick = onOpen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(book.title, style = MaterialTheme.typography.titleMedium)
            if (!canNavigate) {
                Text("作者: ${book.author}", style = authorStyle)
            }
            if (book.summary.isNotBlank()) {
                Text(
                    text = book.summary,
                    style = summaryStyle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canNavigate) {
                Text("點擊開啟子分類", style = hintStyle)
            } else if (canRead) {
                Text("點擊開啟書籍內容", style = hintStyle)
            }
        }
    }
}

@Composable
fun ProfileListScrollbar(
    modifier: Modifier,
    listState: LazyListState,
    totalItems: Int
) {
    if (totalItems <= 0) return
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    val firstIndex = visibleItems.first().index
    val visibleCount = visibleItems.size
    val proportion = visibleCount.toFloat() / max(totalItems, 1).toFloat()
    val thumbHeightFraction = proportion.coerceIn(0.1f, 1f)
    val scrollFraction = (firstIndex.toFloat() / max(totalItems - visibleCount, 1).toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier) {
        val trackHeight = this.maxHeight
        val thumbHeight = trackHeight * thumbHeightFraction
        val travel = trackHeight - thumbHeight
        val offsetY = travel * scrollFraction

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = offsetY)
                .height(thumbHeight)
                .width(5.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
    }
}
