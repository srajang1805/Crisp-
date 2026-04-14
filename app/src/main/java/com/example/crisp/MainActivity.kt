package com.example.crisp
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ripple

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

import com.example.crisp.ui.theme.CrispTheme
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrispTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NewsHomeScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsHomeScreen() {
    var selectedCategory by remember { mutableStateOf("Top") }

    val categories = listOf("Top", "World", "Tech", "Sports")
    val feedMap = mapOf(
        "Top" to "https://timesofindia.indiatimes.com/rssfeeds/296589292.cms",
        "World" to "http://feeds.bbci.co.uk/news/world/rss.xml",
        "Tech" to "https://www.theverge.com/rss/index.xml",
        "Sports" to "https://www.espn.com/espn/rss/news"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Crisp News",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            CategorySelector(categories, selectedCategory) {
                selectedCategory = it
            }
            NewsScreen(feedMap[selectedCategory]!!)
        }
    }
}

@Composable
fun CategorySelector(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun NewsScreen(feedUrl: String) {
    val scope = rememberCoroutineScope()
    var newsList by remember { mutableStateOf(listOf<NewsItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchFeed() {
        scope.launch(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(feedUrl).get()
                val items = doc.select("item")
                val temp = mutableListOf<NewsItem>()
                for (item in items) {
                    val title = item.select("title").text()
                    val link = item.select("link").text()
                    val desc = item.select("description").text()
                    val imageTag = item.select("media|content, enclosure[url]")
                    val imageUrl = imageTag.attr("url")
                    temp.add(NewsItem(title, link, desc, imageUrl.ifEmpty { null }))
                }
                newsList = temp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(feedUrl) {
        isLoading = true
        fetchFeed()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        SwipeRefreshList(newsList) { fetchFeed() }
    }
}

@Composable
fun SwipeRefreshList(newsList: List<NewsItem>, onRefresh: () -> Unit) {
    var refreshing by remember { mutableStateOf(false) }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing),
        onRefresh = {
            refreshing = true
            onRefresh()
            refreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(newsList) { news ->
                NewsCard(news)
            }
        }
    }
}

@Composable

fun NewsCard(news: NewsItem) {
    val context = LocalContext.current
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFE9D6), Color(0xFFF6D9B8))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                    context.startActivity(intent)
                },
                indication = ripple(color = MaterialTheme.colorScheme.primary, bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(12.dp)
        ) {
            Column {
                if (news.imageUrl != null) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Text(
                    text = news.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = news.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
