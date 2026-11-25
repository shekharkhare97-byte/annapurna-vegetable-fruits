package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.example.data.local.OrderEntity
import java.text.DateFormat
import java.util.Date

@Composable
fun CustomerOrdersScreen(
    orders: List<OrderEntity>,
    modifier: Modifier = Modifier
) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"
    fun tr(mr: String, hi: String, en: String) = when (language) { "hi" -> hi; "en" -> en; else -> mr }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5E9)) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF23833B), modifier = Modifier.padding(14.dp))
                }
                Column {
                    Text(tr("माझ्या ऑर्डर्स", "मेरे ऑर्डर", "My Orders"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(tr("तुमच्या खरेदीचा संपूर्ण तपशील", "आपकी खरीदारी का पूरा विवरण", "Your complete purchase history"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F1))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🛍️", style = MaterialTheme.typography.displaySmall)
                        Text(tr("अजून ऑर्डर नाही", "अभी कोई ऑर्डर नहीं है", "No orders yet"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(tr("तुमची पहिली खरेदी सुरू करा. केलेल्या ऑर्डर्सचा तपशील येथे दिसेल.", "अपनी पहली खरीदारी शुरू करें। आपके ऑर्डर यहाँ दिखाई देंगे।", "Start your first purchase. Your orders will appear here."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(orders, key = { it.orderId }) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("ऑर्डर #", "ऑर्डर #", "Order #") + order.orderId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Text(tr("नोंदवली", "दर्ज हुआ", "Placed"), color = Color(0xFF23833B), fontWeight = FontWeight.SemiBold)
                        }
                        Text(localizedOrderItemsSummary(order, language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${order.totalItemsCount} " + tr("वस्तू", "वस्तुएँ", "items"))
                            Spacer(Modifier.weight(1f))
                            Text("₹${order.totalAmountRupeesDecimal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(order.createdAtMillis)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.deliveryAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
