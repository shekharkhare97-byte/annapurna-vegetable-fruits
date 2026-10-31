package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OrderEntity
import com.example.data.repository.CartLineItem
import com.example.ui.viewmodel.CustomerFormState
import com.example.util.MarathiFormatters

@Composable
fun CartAndOrderScreen(
    cartItems: List<CartLineItem>,
    formState: CustomerFormState,
    recentOrders: List<OrderEntity>,
    confirmedOrder: OrderEntity?,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    onDismissReceipt: () -> Unit,
    onReorder: (OrderEntity) -> Unit,
    onBackToBazaar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToBazaar)

    val context = LocalContext.current
    val grandTotal = remember(cartItems) { cartItems.sumOf { it.lineTotalRupees } }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_screen_list")
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cart_header_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (cartItems.isNotEmpty()) {
                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.testTag("clear_cart_button")
                    ) {
                        Text(
                            text = stringResource(R.string.cart_clear_all),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                EmptyCartCard(onBrowseBazaar = onBackToBazaar)
            }
        } else {
            items(cartItems, key = { it.produce.id }) { line ->
                CartLineCard(
                    line = line,
                    onIncrease = { onIncreaseQty(line.produce.id) },
                    onDecrease = { onDecreaseQty(line.produce.id) },
                    onRemove = { onRemoveItem(line.produce.id) }
                )
            }

            item {
                BillSummaryCard(grandTotal = grandTotal)
            }
        }

        // Customer Name, Mobile Number & Place Order Section
        item {
            CustomerOrderFormCard(
                formState = formState,
                grandTotal = grandTotal,
                isCartEmpty = cartItems.isEmpty(),
                onCustomerNameChange = onCustomerNameChange,
                onMobileNumberChange = onMobileNumberChange,
                onAddressChange = onAddressChange,
                onSubmitOrder = onSubmitOrder
            )
        }

        if (recentOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.recent_orders_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(recentOrders, key = { it.orderId }) { order ->
                PastOrderCard(
                    order = order,
                    onShare = {
                        MarathiFormatters.shareOrderReceipt(
                            context = context,
                            receiptText = formatOrderShareReceipt(order)
                        )
                    },
                    onReorder = { onReorder(order) }
                )
            }
        }
    }

    if (confirmedOrder != null) {
        OrderConfirmedDialog(
            order = confirmedOrder,
            onShareReceipt = {
                MarathiFormatters.shareOrderReceipt(
                    context = context,
                    receiptText = formatOrderShareReceipt(confirmedOrder)
                )
            },
            onDismiss = onDismissReceipt
        )
    }
}

@Composable
private fun EmptyCartCard(onBrowseBazaar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.cart_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.cart_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBrowseBazaar,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("browse_bazaar_button")
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_browse_bazaar))
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLineItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_line_${line.produce.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = line.produce.imageRes),
                contentDescription = line.produce.marathiName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.produce.marathiName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "₹${MarathiFormatters.toMarathiNumber(line.produce.priceRupees)} / ${line.produce.unitMarathi}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "एकूण: ₹${MarathiFormatters.toMarathiNumber(line.lineTotalRupees)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.testTag("cart_minus_${line.produce.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.cd_decrease_qty)
                    )
                }
                Text(
                    text = MarathiFormatters.toMarathiNumber(line.quantity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.testTag("cart_plus_${line.produce.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_increase_qty)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("cart_remove_${line.produce.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_remove_item),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BillSummaryCard(grandTotal: Int) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.bill_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bill_items_total))
                Text("₹${MarathiFormatters.toMarathiNumber(grandTotal)}")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bill_delivery_label))
                Text(
                    text = stringResource(R.string.bill_delivery_free),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.bill_grand_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${MarathiFormatters.toMarathiNumber(grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomerOrderFormCard(
    formState: CustomerFormState,
    grandTotal: Int,
    isCartEmpty: Boolean,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_form_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.customer_section_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.customer_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (formState.cartErrorMarathi != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formState.cartErrorMarathi,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = formState.customerName,
                onValueChange = onCustomerNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.label_customer_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_customer_name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = formState.nameErrorMarathi != null,
                supportingText = formState.nameErrorMarathi?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            OutlinedTextField(
                value = formState.mobileNumber,
                onValueChange = onMobileNumberChange,
                singleLine = true,
                label = { Text(stringResource(R.string.label_mobile_number)) },
                placeholder = { Text(stringResource(R.string.placeholder_mobile_number)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = formState.mobileErrorMarathi != null,
                supportingText = formState.mobileErrorMarathi?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mobile_number_input")
            )

            OutlinedTextField(
                value = formState.deliveryAddress,
                onValueChange = onAddressChange,
                maxLines = 2,
                label = { Text(stringResource(R.string.label_delivery_address)) },
                placeholder = { Text(stringResource(R.string.placeholder_delivery_address)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_address_input")
            )

            val orderButtonLabel = if (!isCartEmpty && grandTotal > 0) {
                "${stringResource(R.string.btn_place_order)} • ₹${MarathiFormatters.toMarathiNumber(grandTotal)}"
            } else {
                stringResource(R.string.btn_place_order)
            }

            Button(
                onClick = onSubmitOrder,
                enabled = !formState.isSubmitting,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("place_order_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = orderButtonLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PastOrderCard(
    order: OrderEntity,
    onShare: () -> Unit,
    onReorder: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_order_card_${order.orderId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        R.string.order_id_format,
                        MarathiFormatters.toMarathiNumber(order.orderId.toInt())
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = stringResource(R.string.order_status_confirmed),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${order.customerName} • ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (order.deliveryAddress.isNotBlank()) {
                Text(
                    text = "पत्ता: ${order.deliveryAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = MarathiFormatters.formatTimestampMarathi(order.createdAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = order.itemsSummaryMarathi,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "एकूण: ₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.btn_share_receipt),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(
                        onClick = onReorder,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_reorder))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderConfirmedDialog(
    order: OrderEntity,
    onShareReceipt: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.order_success_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.order_success_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = stringResource(
                        R.string.order_id_format,
                        MarathiFormatters.toMarathiNumber(order.orderId.toInt())
                    ),
                    fontWeight = FontWeight.Bold
                )
                Text("ग्राहक: ${order.customerName}")
                Text("मोबाईल: ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}")
                if (order.deliveryAddress.isNotBlank()) {
                    Text("पत्ता: ${order.deliveryAddress}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.itemsSummaryMarathi,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "एकूण बिल: ₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onShareReceipt,
                modifier = Modifier.testTag("share_receipt_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_share_receipt))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("order_receipt_done_button")
            ) {
                Text(stringResource(R.string.btn_done))
            }
        }
    )
}

private fun formatOrderShareReceipt(order: OrderEntity): String {
    val addressLine = if (order.deliveryAddress.isNotBlank()) {
        "\nपत्ता: ${order.deliveryAddress}"
    } else ""
    return """
        🌿 अन्नपूर्णा भाजीपाला आणि फळे 🌿
        ऑर्डर पावती (#${MarathiFormatters.toMarathiNumber(order.orderId.toInt())})
        ---------------------------
        ग्राहक: ${order.customerName}
        मोबाईल: ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}$addressLine
        ---------------------------
        ${order.itemsSummaryMarathi}
        ---------------------------
        एकूण देय रक्कम: ₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}
        धन्यवाद! ताजा शेतमाल लवकरच घरपोच केला जाईल.
    """.trimIndent()
}
