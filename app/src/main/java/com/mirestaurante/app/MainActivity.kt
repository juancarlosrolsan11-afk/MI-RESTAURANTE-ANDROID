package com.mirestaurante.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Producto(val nombre: String, val unidad: String, val stock: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiRestauranteApp() }
    }
}

@Composable
fun MiRestauranteApp() {
    var productos by remember {
        mutableStateOf(listOf(
            Producto("CARNE", "KG", 20),
            Producto("CERDO", "KG", 15),
            Producto("POLLO", "KG", 12),
            Producto("CHICHARRÓN", "KG", 10)
        ))
    }
    var ventas by remember { mutableStateOf(0) }
    var inventario by remember { mutableStateOf(true) }

    Scaffold(topBar = { TopAppBar(title = { Text("MI RESTAURANTE") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { inventario = true }) { Text("INVENTARIO") }
                OutlinedButton(onClick = { inventario = false }) { Text("VENTAS") }
            }
            Spacer(Modifier.height(12.dp))
            if (inventario) {
                Text("INVENTARIO ACTUAL", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productos, key = { it.nombre }) { producto ->
                        var cantidad by remember { mutableStateOf("1") }
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                                Text("UNIDAD: " + producto.unidad)
                                Text("STOCK: " + producto.stock)
                                OutlinedTextField(
                                    value = cantidad,
                                    onValueChange = { cantidad = it.filter(Char::isDigit) },
                                    label = { Text("CANTIDAD A DESCONTAR") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Button(onClick = {
                                    val n = cantidad.toIntOrNull() ?: 0
                                    if (n > 0 && n <= producto.stock) {
                                        productos = productos.map {
                                            if (it.nombre == producto.nombre) it.copy(stock = it.stock - n) else it
                                        }
                                        ventas += n
                                    }
                                }) { Text("REGISTRAR VENTA") }
                            }
                        }
                    }
                }
            } else {
                Text("RESUMEN DE VENTAS", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("UNIDADES DESCONTADAS: " + ventas)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { ventas = 0 }) { Text("NUEVO TURNO") }
            }
        }
    }
}
