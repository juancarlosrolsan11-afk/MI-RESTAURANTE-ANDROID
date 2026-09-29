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

data class Producto(val nombre: String, val unidad: String, val stock: Double, val precio: Double)
data class Plato(val nombre: String, val precio: Double, val consumo: Map<String, Double>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiRestauranteApp() }
    }
}

@Composable
fun MiRestauranteApp() {
    var productos by remember { mutableStateOf(listOf(
        Producto("CARNE", "KG", 20.0, 28000.0),
        Producto("CERDO", "KG", 15.0, 22000.0),
        Producto("POLLO", "KG", 12.0, 18000.0),
        Producto("CHICHARRÓN", "KG", 10.0, 25000.0)
    ))}
    val menu = listOf(
        Plato("CARNE ASADA", 25000.0, mapOf("CARNE" to 0.30)),
        Plato("CARNE Y CERDO", 28000.0, mapOf("CARNE" to 0.20, "CERDO" to 0.20)),
        Plato("POLLO", 20000.0, mapOf("POLLO" to 0.30)),
        Plato("CHICHARRÓN", 22000.0, mapOf("CHICHARRÓN" to 0.25))
    )
    var ventas by remember { mutableStateOf(0) }
    var ingresos by remember { mutableStateOf(0.0) }
    var pantalla by remember { mutableStateOf("INVENTARIO") }

    fun vender(plato: Plato) {
        if (!plato.consumo.all { (n, c) -> productos.firstOrNull { it.nombre == n }?.stock?.let { it >= c } == true }) return
        productos = productos.map { p -> p.copy(stock = p.stock - (plato.consumo[p.nombre] ?: 0.0)) }
        ventas++
        ingresos += plato.precio
    }

    Scaffold(topBar = { TopAppBar(title = { Text("MI RESTAURANTE") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { pantalla = "INVENTARIO" }) { Text("INVENTARIO") }
                Button(onClick = { pantalla = "MENU" }) { Text("MENÚ") }
                Button(onClick = { pantalla = "VENTAS" }) { Text("VENTAS") }
            }
            Spacer(Modifier.height(12.dp))
            when (pantalla) {
                "INVENTARIO" -> {
                    Text("INVENTARIO", style = MaterialTheme.typography.headlineSmall)
                    Text("PESO, EXISTENCIA Y PRECIO DE COMPRA")
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(productos, key = { it.nombre }) { p ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("EXISTENCIA: %.2f %s".format(p.stock, p.unidad))
                                    Text("PRECIO: $%,.0f / %s".format(p.precio, p.unidad))
                                    if (p.stock <= 2) Text("STOCK BAJO")
                                }
                            }
                        }
                    }
                }
                "MENU" -> {
                    Text("MENÚ", style = MaterialTheme.typography.headlineSmall)
                    Text("LA VENTA DESCUENTA EL PESO DEL INVENTARIO.")
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(menu, key = { it.nombre }) { plato ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(plato.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("PRECIO: $%,.0f".format(plato.precio))
                                    Text("CONSUMO: " + plato.consumo.entries.joinToString { it.key + " %.2f KG".format(it.value) })
                                    Spacer(Modifier.height(6.dp))
                                    Button(onClick = { vender(plato) }) { Text("REGISTRAR VENTA") }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("VENTAS", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text("PLATOS VENDIDOS: $ventas")
                    Text("INGRESOS: $%,.0f".format(ingresos))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { ventas = 0; ingresos = 0.0 }) { Text("NUEVO TURNO") }
                }
            }
        }
    }
}
