package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderEntity
import com.example.ui.viewmodel.CustomerFormState

@Composable
fun CustomerProfileScreen(
    formState: CustomerFormState,
    recentOrders: List<OrderEntity>,
    trackingStatus: String,
    trackingLocation: String,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(formState.customerName, formState.mobileNumber, formState.deliveryAddress) {
        if (formState.customerName.isNotBlank() && formState.mobileNumber.isNotBlank()) editing = false
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp))
                }
                Column {
                    Text(formState.customerName.ifBlank { "अन्नपूर्णा ग्राहक" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(formState.mobileNumber.ifBlank { "मोबाईल नंबर जोडा" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  माझी माहिती", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { editing = !editing }) {
                            Text(if (editing) "बंद करा" else "बदला")
                        }
                    }
                    if (editing || formState.customerName.isBlank()) {
                        OutlinedTextField(value = formState.customerName, onValueChange = onNameChange, label = { Text("पूर्ण नाव") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.mobileNumber, onValueChange = onMobileChange, label = { Text("मोबाईल नंबर") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.deliveryAddress, onValueChange = onAddressChange, label = { Text("डिलिव्हरी पत्ता") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(onClick = onSaveProfile, enabled = !formState.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                            Text(if (formState.isSubmitting) "जतन होत आहे..." else "माहिती जतन करा")
                        }
                        formState.cartErrorMarathi?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    } else {
                        Text(formState.customerName)
                        Text(formState.mobileNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            Text("  " + formState.deliveryAddress.ifBlank { "पत्ता जोडलेला नाही" })
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  लाईव्ह ऑर्डर ट्रॅकिंग", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        when (trackingStatus) {
                            "new" -> "ऑर्डर मिळाली आहे. दुकानाकडून पुष्टीची प्रतीक्षा आहे."
                            "accepted" -> "दुकानाने ऑर्डर स्वीकारली आहे."
                            "out_for_delivery" -> "तुमची ऑर्डर डिलिव्हरीसाठी निघाली आहे."
                            "completed" -> "ऑर्डर यशस्वीपणे पोहोचवली आहे."
                            "cancelled" -> "ही ऑर्डर रद्द झाली आहे."
                            "unavailable" -> "ट्रॅकिंग सध्या उपलब्ध नाही. इंटरनेट तपासा."
                            else -> "ऑर्डर केल्यानंतर तिचे अपडेट येथे दिसतील."
                        }
                    )
                    if (trackingLocation.isNotBlank()) {
                        Text("डिलिव्हरी पार्टनरचे लोकेशन: $trackingLocation", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ReceiptLong, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                Text("  माझ्या ऑर्डर्स", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onOpenOrders) { Text("सर्व पहा") }
            }
        }
        if (recentOrders.isEmpty()) {
            item { Text("अजून कोणतीही ऑर्डर नाही. तुमची पहिली खरेदी सुरू करा!") }
        } else {
            items(recentOrders.take(5)) { order ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ऑर्डर #${order.orderId}", fontWeight = FontWeight.Bold)
                        Text(order.itemsSummaryMarathi, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${order.totalAmountRupeesDecimal}  •  ${java.text.DateFormat.getDateInstance().format(java.util.Date(order.createdAtMillis))}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileShortcut(Icons.Outlined.AddLocationAlt, "पत्ते व्यवस्थापन", "डिलिव्हरीसाठी तुमचा मुख्य पत्ता")
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.FavoriteBorder, "आवडती उत्पादने", "आवडते पदार्थ लवकर शोधा")
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.CardGiftcard, "कूपन आणि ऑफर्स", "उपलब्ध सवलती आणि ऑफर्स")
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.HelpOutline, "मदत आणि ग्राहक सेवा", "ऑर्डर किंवा पेमेंटसाठी मदत")
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProfileShortcut(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
