package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.CustomerFormState

@Composable
fun CustomerProfileEntryScreen(
    formState: CustomerFormState,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4FAF4))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_launcher_logo),
                    contentDescription = "Annapurna Vegetables and Fruits",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(104.dp)
                        .fillMaxWidth(0.42f)
                        .clip(RoundedCornerShape(22.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.profile_brand_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF176B3A)
                )
                Text(
                    text = stringResource(R.string.profile_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = formState.customerName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth().testTag("entry_customer_name"),
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_name)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = formState.nameErrorMarathi != null,
                    supportingText = { formState.nameErrorMarathi?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = formState.mobileNumber,
                    onValueChange = onMobileChange,
                    modifier = Modifier.fillMaxWidth().testTag("entry_customer_mobile"),
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_mobile)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    isError = formState.mobileErrorMarathi != null,
                    supportingText = { formState.mobileErrorMarathi?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = formState.deliveryAddress,
                    onValueChange = onAddressChange,
                    modifier = Modifier.fillMaxWidth().testTag("entry_customer_address"),
                    minLines = 3,
                    label = { Text(stringResource(R.string.profile_address)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    isError = formState.addressErrorMarathi != null,
                    supportingText = { formState.addressErrorMarathi?.let { Text(it) } }
                )
                formState.cartErrorMarathi?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onContinue,
                    enabled = !formState.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("entry_continue_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754))
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(22.dp))
                    } else {
                        Text(stringResource(R.string.profile_continue), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
