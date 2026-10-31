package com.example.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.OwnerInventoryEntity
import com.example.data.model.ProduceCatalog
import com.example.ui.viewmodel.OwnerViewModel
@Composable
fun OwnerStockScreen(viewModel:OwnerViewModel,modifier:Modifier=Modifier) {
 val inventory by viewModel.inventory.collectAsStateWithLifecycle()
 val settings by viewModel.settings.collectAsStateWithLifecycle()
 var selected by remember { mutableStateOf<OwnerInventoryEntity?>(null) }
 var priceText by remember { mutableStateOf("") }
 var quantityText by remember { mutableStateOf("1") }
 var ownerName by remember { mutableStateOf("") }
 var phone by remember { mutableStateOf("") }
 var showSettings by remember { mutableStateOf(false) }
 LaunchedEffect(settings) { ownerName=settings?.ownerName.orEmpty(); phone=settings?.phoneNumber.orEmpty() }
 Column(modifier.fillMaxSize().padding(16.dp)) {
  Text("दुकान व्यवस्थापन",style=MaterialTheme.typography.headlineSmall)
  Text("स्टॉक, बाजार खरेदी आणि आजचे दर")
  Card(Modifier.fillMaxWidth()) {
   Row(Modifier.padding(14.dp), horizontalArrangement=Arrangement.spacedBy(12.dp), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
    Surface(shape=androidx.compose.foundation.shape.CircleShape, color=MaterialTheme.colorScheme.primaryContainer, modifier=Modifier.size(52.dp)) {
     Box(contentAlignment=androidx.compose.ui.Alignment.Center) { Text("👤", style=MaterialTheme.typography.headlineSmall) }
    }
    Column(Modifier.weight(1f)) {
     Text("मालकाची प्रोफाइल", style=MaterialTheme.typography.titleMedium)
     Text(ownerName.ifBlank { "मालकाचे नाव सेट करा" }, style=MaterialTheme.typography.bodyLarge)
     Text(phone.ifBlank { "मोबाईल नंबर जोडा" }, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick={showSettings=true}) { Text("बदला") }
   }
  }
  Spacer(Modifier.height(10.dp))
  OutlinedButton(onClick={showSettings=true}) { Text("दुकान सेटिंग्स") }
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
   items(inventory,key={it.produceId}) { row ->
    val product=ProduceCatalog.findById(row.produceId)
    val countBased=viewModel.isCountBased(row.produceId)
    val unit=if(countBased) "जुडी / नग" else "किलो"
    val stock=if(countBased) "${row.stockUnits} $unit" else "${row.stockGrams} ग्रॅम"
    val price=if(countBased) row.sellingPricePaisePerUnit/100 else row.sellingPricePaisePerKg/100
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
     Text(product?.marathiName ?: row.produceId,style=MaterialTheme.typography.titleMedium)
     Text("सध्याचा स्टॉक: $stock")
     Text("आजचा दर: ₹$price / $unit")
     Button(onClick={selected=row; priceText=price.toString(); quantityText=if(countBased) "1" else "1000"}) { Text("दर / स्टॉक बदला") }
    } }
   }
  }
 }
 selected?.let { item ->
  val countBased=viewModel.isCountBased(item.produceId)
  val unit=if(countBased) "जुडी / नग" else "ग्रॅम"
  AlertDialog(onDismissRequest={selected=null},title={Text(ProduceCatalog.findById(item.produceId)?.marathiName ?: "माल")},text={
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(priceText,{priceText=it.filter(Char::isDigit)},label={Text(if(countBased) "आजचा दर (₹ प्रति जुडी)" else "आजचा दर (₹ प्रति किलो)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
    OutlinedTextField(quantityText,{quantityText=it.filter(Char::isDigit)},label={Text("प्रमाण ($unit)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Button(onClick={viewModel.recordPurchase(item.produceId,quantityText.toIntOrNull()?:0);selected=null}) { Text("खरेदी जोडा") }
     OutlinedButton(onClick={viewModel.recordWastage(item.produceId,quantityText.toIntOrNull()?:0);selected=null}) { Text("नासाडी") }
    }
   }
  },confirmButton={TextButton(onClick={viewModel.updatePrice(item.produceId,priceText.toIntOrNull()?:0);selected=null}) { Text("दर जतन करा") }},dismissButton={TextButton(onClick={selected=null}) { Text("रद्द") }})
 }
 if(showSettings) AlertDialog(onDismissRequest={showSettings=false},title={Text("दुकानाची माहिती")},text={
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(ownerName,{ownerName=it},label={Text("मालकाचे नाव")})
   OutlinedTextField(phone,{phone=it},label={Text("मोबाईल नंबर")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
  }
 },confirmButton={TextButton(onClick={viewModel.saveSettings(ownerName,phone);showSettings=false}) { Text("जतन करा") }},dismissButton={TextButton(onClick={showSettings=false}) { Text("रद्द") }})
}