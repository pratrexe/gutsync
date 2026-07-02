package com.example.gutsync.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val GutsyncQuotes = listOf(
    "Consulting your gut...",
    "Counting bacteria...",
    "Negotiating with the broccoli...",
    "Convincing your microbiome not to riot...",
    "Sequencing imaginary DNA...",
    "Waking up the lab hamster...",
    "Feeding the AI coffee...",
    "Calibrating the flavor detector...",
    "Measuring avocado density...",
    "Asking Lactobacillus for its opinion...",
    "Summoning Bifidobacterium...",
    "Mixing questionable chemicals...",
    "Launching the GutSync satellite...",
    "Starting the Hadron Collider...",
    "Contacting NASA...",
    "Downloading more RAM...",
    "Asking the ancient food wizard...",
    "Convincing Linux to cooperate...",
    "Squashing bugs...",
    "Accidentally creating new bugs...",
    "Igniting the fusion reactor...",
    "Charging the flux capacitor...",
    "Borrowing a few neurons...",
    "Calculating donut curvature...",
    "Detecting taco interference...",
    "Sanitizing the bacteria...",
    "Teaching AI what broccoli looks like...",
    "Maximizing gut happiness...",
    "Rolling for critical nutrition...",
    "Asking Akkermansia nicely...",
    "Fermenting imaginary yogurt...",
    "Estimating banana potential...",
    "Compressing calories...",
    "Checking every supermarket on Earth...",
    "Waiting for cosmic rays...",
    "Pretending to be busy...",
    "Doing absolutely nothing...",
    "Distracting the ducks...",
    "Speedrunning nutrition science...",
    "Trying not to divide by zero...",
    "Unboxing your meal...",
    "Aligning the planets...",
    "Looking for unicorn ingredients...",
    "Running 4.2 million gut simulations...",
    "Consulting the Fiber Council...",
    "Interviewing your gut microbes...",
    "Checking for suspicious additives...",
    "Searching for hidden sugars...",
    "Counting polyphenols...",
    "Estimating prebiotic potential...",
    "Decoding ingredient hieroglyphics...",
    "Measuring microbiome happiness...",
    "Bribing the healthy bacteria...",
    "Negotiating with the probiotics...",
    "Inspecting every calorie...",
    "Calculating the crunch factor...",
    "Comparing with 12 million meals...",
    "Looking for ultra-processed ingredients...",
    "Asking your stomach for feedback...",
    "Waiting for the bacteria to vote...",
    "Running the Gut Intelligence Engine...",
    "Consulting the Nutrition Oracle...",
    "Reconstructing your meal atom by atom...",
    "Estimating tomorrow's cravings...",
    "Looking for fiber in all the right places...",
    "Checking if your broccoli is actually broccoli...",
    "Giving your microbiome a coffee break...",
    "Trying to pronounce the ingredient list...",
    "Reading the fine print on the nutrition label..."
)

@Composable
fun GutsyncLoadingAnimation(modifier: Modifier = Modifier) {
    // Create a shuffled copy of the quotes for this specific loading session
    val sessionQuotes = remember { GutsyncQuotes.shuffled() }
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (currentIndex < sessionQuotes.size - 1) {
            delay(5000)
            currentIndex++
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        Crossfade(targetState = sessionQuotes[currentIndex], animationSpec = tween(1000), label = "quote_fade") { quote ->
            Text(
                text = quote,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
