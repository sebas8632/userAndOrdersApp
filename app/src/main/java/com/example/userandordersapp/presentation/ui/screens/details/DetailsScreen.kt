package com.example.userandordersapp.presentation.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.userandordersapp.presentation.ui.components.CustomButton
import com.example.userandordersapp.presentation.ui.components.CustomCard
import com.example.userandordersapp.presentation.ui.components.EmptyState
import com.example.userandordersapp.presentation.ui.components.LoadingIndicator

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Volver") }

        when {
            state.isLoading -> LoadingIndicator()

            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
                CustomButton(text = "Reintentar", onClick = viewModel::loadDetails)
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.user?.let { user ->
                    Text(text = user.name, style = MaterialTheme.typography.headlineMedium)
                    Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = "Órdenes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (state.orders.isEmpty()) {
                    EmptyState(message = "Este usuario no tiene órdenes")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.orders, key = { it.id }) { order ->
                            CustomCard(onClick = {}) {
                                Text(
                                    text = "Orden ${order.id}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Estado: ${order.status}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Total: $%.2f · %d producto(s)".format(
                                        order.totalAmount,
                                        order.products.size
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
