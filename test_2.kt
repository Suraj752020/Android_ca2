
package com.example.sem7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sem7.ui.theme.Sem7Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Product Data Model
data class Product(
    val id: Int,
    val name: String,
    val price: Int
)

// Product Repository
class ProductRepository {

    private val products = listOf(
        Product(1, "iPhone 16", 70000),
        Product(2, "Samsung Galaxy", 45000),
        Product(3, "MacBook Air M4", 100000),
        Product(4, "Wireless Headphones", 2000),
        Product(5, "Bluetooth Speaker", 3000),
        Product(6, "Smart Watch", 5000)
    )

    suspend fun searchProducts(query: String): List<Product> =
        withContext(Dispatchers.IO) {
            delay(1000)

            if (query.isBlank()) {
                emptyList()
            } else {
                products.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }
}

// UI State
data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel
class SearchViewModel : ViewModel() {

    private val repository = ProductRepository()
    private val query = MutableStateFlow("")

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            query
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { searchQuery ->

                    if (searchQuery.isBlank()) {
                        _uiState.update {
                            it.copy(
                                products = emptyList(),
                                isLoading = false,
                                error = null
                            )
                        }
                        return@collectLatest
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            products = emptyList(),
                            error = null
                        )
                    }

                    try {
                        val results =
                            repository.searchProducts(searchQuery)

                        if (query.value == searchQuery) {
                            _uiState.update {
                                it.copy(
                                    products = results,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (query.value == searchQuery) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Something went wrong."
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery

        _uiState.update {
            it.copy(
                query = newQuery,
                products = emptyList(),
                isLoading = newQuery.isNotBlank(),
                error = null
            )
        }
    }
}

// Main Activity
class test_2 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Sem7Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SearchScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Search Screen
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "E-Commerce Search",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search products") },
            placeholder = { Text("Enter product name...") },
            singleLine = true,
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.onQueryChange("")
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(
                    Alignment.CenterHorizontally
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.products,
                key = { it.id }
            ) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = product.name,
                                style =
                                    MaterialTheme.typography.titleMedium
                            )

                            Text("₹${product.price}")
                        }

                        Button(onClick = {}) {
                            Text("View")
                        }
                    }
                }
            }

            if (
                !state.isLoading &&
                state.error == null &&
                state.query.isNotBlank() &&
                state.products.isEmpty()
            ) {
                item {
                    Text(
                        text = "No products found.",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    Sem7Theme {
        SearchScreen()
    }
}
