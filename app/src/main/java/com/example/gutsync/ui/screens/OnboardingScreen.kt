package com.example.gutsync.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gutsync.GutSyncViewModel
import com.example.gutsync.ui.theme.SurfaceContainerLowest

@Composable
fun OnboardingScreen(viewModel: GutSyncViewModel, onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    // Step 0 Data
    val dietTypes = listOf(
        "Balanced", "Vegan", "Vegetarian", "Keto", "Paleo", 
        "Low Carb", "High Protein", "Mediterranean", "Carnivore", "Pescatarian"
    )
    var selectedDiet by remember { mutableStateOf("Balanced") }
    
    // Step 1 Data
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    // Step 2 Data
    val healthIssues = listOf(
        "Lactose Intolerance",
        "Gluten Sensitivity",
        "IBS",
        "Acid Reflux",
        "Bloating",
        "Diabetes",
        "High Cholesterol"
    )
    var selectedIssues by remember { mutableStateOf(setOf<String>()) }
    var otherIssue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Crossfade(targetState = currentStep, modifier = Modifier.weight(1f)) { step ->
            when (step) {
                0 -> DietSelectionStep(
                    dietTypes = dietTypes,
                    selectedDiet = selectedDiet,
                    onSelect = { selectedDiet = it }
                )
                1 -> MetricsStep(
                    age = age,
                    height = height,
                    weight = weight,
                    onAgeChange = { age = it },
                    onHeightChange = { height = it },
                    onWeightChange = { weight = it }
                )
                2 -> HealthConditionsStep(
                    issues = healthIssues,
                    selectedIssues = selectedIssues,
                    otherIssue = otherIssue,
                    onToggleIssue = { issue ->
                        selectedIssues = if (selectedIssues.contains(issue)) {
                            selectedIssues - issue
                        } else {
                            selectedIssues + issue
                        }
                    },
                    onOtherChange = { otherIssue = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val buttonText = when {
            currentStep < 2 -> "Continue"
            selectedIssues.isEmpty() && otherIssue.isBlank() -> "I have no issues"
            else -> "Finish Setup"
        }

        Button(
            onClick = {
                if (currentStep < 2) {
                    currentStep++
                } else {
                    val finalConditions = selectedIssues.toMutableList()
                    if (otherIssue.isNotBlank()) finalConditions.add(otherIssue)
                    
                    viewModel.completeOnboarding(
                        dietType = selectedDiet,
                        age = age.toIntOrNull() ?: 25,
                        height = height.toFloatOrNull() ?: 170f,
                        weight = weight.toFloatOrNull() ?: 70f,
                        conditions = finalConditions
                    )
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        if (currentStep > 0) {
            TextButton(
                onClick = { currentStep-- },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Back", color = Color.White.copy(alpha = 0.6f))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DietSelectionStep(
    dietTypes: List<String>,
    selectedDiet: String,
    onSelect: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Your Diet Style",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "What best describes your current eating pattern?",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dietTypes) { diet ->
                val isSelected = diet == selectedDiet
                OnboardingCard(
                    text = diet,
                    isSelected = isSelected,
                    onClick = { onSelect(diet) }
                )
            }
        }
    }
}

@Composable
fun MetricsStep(
    age: String,
    height: String,
    weight: String,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "About You",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "These details help us calculate your BMI and personalized microbiome goals.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OnboardingTextField(label = "Age", value = age, onValueChange = onAgeChange)
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingTextField(label = "Height (cm)", value = height, onValueChange = onHeightChange)
        Spacer(modifier = Modifier.height(16.dp))
        OnboardingTextField(label = "Weight (kg)", value = weight, onValueChange = onWeightChange)
    }
}

@Composable
fun HealthConditionsStep(
    issues: List<String>,
    selectedIssues: Set<String>,
    otherIssue: String,
    onToggleIssue: (String) -> Unit,
    onOtherChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Health Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Select any conditions to help Maya give you better advice.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(issues) { issue ->
                val isSelected = selectedIssues.contains(issue)
                OnboardingCard(
                    text = issue,
                    isSelected = isSelected,
                    onClick = { onToggleIssue(issue) }
                )
            }
            item {
                OutlinedTextField(
                    value = otherIssue,
                    onValueChange = onOtherChange,
                    label = { Text("Other / Additional Notes", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun OnboardingCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White.copy(alpha = 0.15f) else SurfaceContainerLowest
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (isSelected) Color.White else Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun OnboardingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color.White
        )
    )
}
