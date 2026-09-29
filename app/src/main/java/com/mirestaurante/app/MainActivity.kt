package com.mirestaurante.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class Producto(val nombre:String,val unidad:String,val stock:Double,val minimo:Double,val precio:Double)
data class Plato(val nombre:String,val precio:Double,val consumo:Map<String,Double>)
data class Datos(val productos:List<Producto>,val menu:List<Plato>,val ventas:Int,val ingresos:Double,val costos:Double)

private const val PREF="mi_restaurante"
private const val KEY="datos"

private fun inicial()=Datos(
 listOf(Producto("CARNE","KG",20.0,2.0,28000.0),Producto("CERDO","KG",15.0,2.0,22000.0),Producto("POLLO","KG",12.0,2.0,18000.0),Producto("CHICHARRÓN","KG",10.0,2.0,25000.0)),
 listOf(Plato("CARNE ASADA",25000.0,mapOf("CARNE" to .30)),Plato("CARNE Y CERDO",28000.0,mapOf("CARNE" to .20,"CERDO" to .20)),Plato("POLLO",20000.0,mapOf("POLLO" to .30)),Plato("CHICHARRÓN",22000.0,mapOf("CHICHARRÓN" to .25))),0,0.0,0.0)

private fun guardar(c:Context,d:Datos){
 val r=JSONObject();val ps=JSONArray();d.productos.forEach{p->ps.put(JSONObject().apply{put("n",p.nombre);put("u",p.unidad);put("s",p.stock);put("m",p.minimo);put("p",p.precio)})}
 val ms=JSONArray();d.menu.forEach{m->val co=JSONObject();m.consumo.forEach{(n,v)->co.put(n,v)};ms.put(JSONObject().apply{put("n",m.nombre);put("p",m.precio);put("c",co)})}
 r.put("ps",ps);r.put("ms",ms);r.put("v",d.ventas);r.put("i",d.ingresos);r.put("k",d.costos)
 c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,r.toString()).apply()
}

private fun cargar(c:Context):Datos{
 val raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,null)?:return inicial()
 return try{
  val r=JSONObject(raw);val a=r.getJSONArray("ps");val ps=buildList{for(i in 0 until a.length()){val x=a.getJSONObject(i);add(Producto(x.getString("n"),x.getString("u"),x.getDouble("s"),x.optDouble("m",2.0),x.getDouble("p")))}}
  val b=r.getJSONArray("ms");val ms=buildList{for(i in 0 until b.length()){val x=b.getJSONObject(i);val co=x.getJSONObject("c");val map=mutableMapOf<String,Double>();co.keys().forEach{map[it]=co.getDouble(it)};add(Plato(x.getString("n"),x.getDouble("p"),map))}}
  Datos(ps,ms,r.optInt("v"),r.optDouble("i"),r.optDouble("k"))
 }catch(_:Exception){inicial()}
}

class MainActivity:ComponentActivity(){override fun onCreate(s:Bundle?){super.onCreate(s);setContent{App(this)}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(context:Context){
 var d by remember{mutableStateOf(cargar(context))};var page by remember{mutableStateOf("INICIO")}
 var editP by remember{mutableStateOf<Producto?>(null)};var addP by remember{mutableStateOf(false)}
 var editM by remember{mutableStateOf<Plato?>(null)};var addM by remember{mutableStateOf(false)};var msg by remember{mutableStateOf<String?>(null)}
 fun save(x:Datos){d=x;guardar(context,x)}
 fun vender(m:Plato,q:Int){
  val falta=m.consumo.entries.firstOrNull{(n,v)->d.productos.firstOrNull{it.nombre.equals(n,true)}?.stock?.let{s->s>=v*q}!=true}
  if(falta!=null){msg="NO HAY EXISTENCIA SUFICIENTE DE \${falta.key}";return}
  val np=d.productos.map{p->p.copy(stock=p.stock-(m.consumo[p.nombre]?:0.0)*q)}
  val costo=m.consumo.entries.sumOf{(n,v)->v*q*d.productos.first{it.nombre.equals(n,true)}.precio}
  save(d.copy(productos=np,ventas=d.ventas+q,ingresos=d.ingresos+m.precio*q,costos=d.costos+costo));msg="VENTA REGISTRADA: \$q x \${m.nombre}"
 }
 Scaffold(topBar={TopAppBar(title={Text("MI RESTAURANTE")})}){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)){
   when(page){
    "INICIO"->Inicio(d){page=it}
    "INVENTARIO"->Inventario(d.productos,{addP=true},{editP=it},{p->save(d.copy(productos=d.productos.filterNot{it.nombre==p.nombre}))})
    "MENU"->Menu(d.productos,d.menu,{addM=true},{editM=it},{m->save(d.copy(menu=d.menu.filterNot{it.nombre==m.nombre}))},{m,q->vender(m,q)})
    else->Ventas(d){save(d.copy(ventas=0,ingresos=0.0,costos=0.0))}
   }
   Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
    Button({page="INICIO"},Modifier.weight(1f)){Text("INICIO")};Button({page="INVENTARIO"},Modifier.weight(1f)){Text("INVENTARIO")}
    Button({page="MENU"},Modifier.weight(1f)){Text("MENÚ")};Button({page="VENTAS"},Modifier.weight(1f)){Text("VENTAS")}
   }
  }
 }
 if(addP||editP!=null)ProductoDialog(editP,{addP=false;editP=null}){p->
  val list=if(editP!=null)d.productos.map{if(it.nombre==editP!!.nombre)p else it}else d.productos.filterNot{it.nombre.equals(p.nombre,true)}+p
  save(d.copy(productos=list));addP=false;editP=null
 }
 if(addM||editM!=null)PlatoDialog(editM,d.productos,{addM=false;editM=null}){m->
  val list=if(editM!=null)d.menu.map{if(it.nombre==editM!!.nombre)m else it}else d.menu.filterNot{it.nombre.equals(m.nombre,true)}+m
  save(d.copy(menu=list));addM=false;editM=null
 }
 msg?.let{AlertDialog(onDismissRequest={msg=null},confirmButton={TextButton({msg=null}){Text("OK")}},title={Text("MI RESTAURANTE")},text={Text(it)})}
}

@Composable
fun Inicio(d:Datos,go:(String)->Unit){
 Text("PANEL PRINCIPAL",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp))
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){
  Text("PRODUCTOS: \${d.productos.size}");Text("PLATOS: \${d.menu.size}");Text("VENTAS: \${d.ventas}")
  Text("INGRESOS: \$%,.0f".format(d.ingresos));Text("COSTOS: \$%,.0f".format(d.costos));Text("GANANCIA BRUTA: \$%,.0f".format(d.ingresos-d.costos))
 }}
 Spacer(Modifier.height(10.dp));Button({go("INVENTARIO")},Modifier.fillMaxWidth()){Text("ADMINISTRAR INVENTARIO")};Button({go("MENU")},Modifier.fillMaxWidth()){Text("ADMINISTRAR MENÚ / VENDER")}
}

@Composable
fun Inventario(ps:List<Producto>,add:()->Unit,edit:(Producto)->Unit,del:(Producto)->Unit){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("INVENTARIO",style=MaterialTheme.typography.headlineSmall);Button(add){Text("+ PRODUCTO")}}
 Text("PESO/CANTIDAD • STOCK MÍNIMO • PRECIO DE COMPRA")
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(ps,key={it.nombre}){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
  Text(p.nombre,style=MaterialTheme.typography.titleMedium);Text("EXISTENCIA: %.3f %s".format(p.stock,p.unidad));Text("MÍNIMO: %.3f %s".format(p.minimo,p.unidad));Text("COMPRA: \$%,.0f / %s".format(p.precio,p.unidad))
  if(p.stock<=p.minimo)Text("⚠ STOCK BAJO");Row{TextButton({edit(p)}){Text("EDITAR")};TextButton({del(p)}){Text("ELIMINAR")}}
 }}}}
}

@Composable
fun Menu(ps:List<Producto>,ms:List<Plato>,add:()->Unit,edit:(Plato)->Unit,del:(Plato)->Unit,sell:(Plato,Int)->Unit){
 var qty by remember{mutableStateOf(mapOf<String,Int>())}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("MENÚ",style=MaterialTheme.typography.headlineSmall);Button(add){Text("+ PLATO / CAJA")}}
 Text("Cada venta descuenta automáticamente los ingredientes.")
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(ms,key={it.nombre}){m->
  val cost=m.consumo.entries.sumOf{(n,v)->v*(ps.firstOrNull{it.nombre.equals(n,true)}?.precio?:0.0)}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(m.nombre,style=MaterialTheme.typography.titleMedium);Text("VENTA: \$%,.0f".format(m.precio))
   Text("COSTO: \$%,.0f • GANANCIA: \$%,.0f".format(cost,m.precio-cost));Text("CONSUMO: "+m.consumo.entries.joinToString{it.key+" %.3f".format(it.value)})
   Row{TextButton({edit(m)}){Text("EDITAR")};TextButton({del(m)}){Text("ELIMINAR")}}
   val q=qty[m.nombre]?:1;Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton({qty=qty+(m.nombre to maxOf(1,q-1))}){Text("-")};Text("  \$q  ");OutlinedButton({qty=qty+(m.nombre to q+1)}){Text("+")};Spacer(Modifier.width(8.dp));Button({sell(m,q)}){Text("VENDER")}}
  }}
 }}
}

@Composable
fun Ventas(d:Datos,reset:()->Unit){
 Text("RESUMEN DE VENTAS",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Text("UNIDADES VENDIDAS: \${d.ventas}")
 Text("INGRESOS: \$%,.0f".format(d.ingresos));Text("COSTO INGREDIENTES: \$%,.0f".format(d.costos));Text("GANANCIA BRUTA: \$%,.0f".format(d.ingresos-d.costos))
 Spacer(Modifier.height(12.dp));Button(reset){Text("NUEVO TURNO / REINICIAR")}
}

@Composable
fun ProductoDialog(initial:Producto?,close:()->Unit,save:(Producto)->Unit){
 var n by remember{mutableStateOf(initial?.nombre?:"")};var u by remember{mutableStateOf(initial?.unidad?:"KG")};var s by remember{mutableStateOf(initial?.stock?.toString()?:"")}
 var min by remember{mutableStateOf(initial?.minimo?.toString()?:"2")};var p by remember{mutableStateOf(initial?.precio?.toString()?:"")}
 AlertDialog(onDismissRequest=close,title={Text(if(initial==null)"NUEVO PRODUCTO" else "EDITAR PRODUCTO")},text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
  OutlinedTextField(n,{n=it},label={Text("NOMBRE")});OutlinedTextField(u,{u=it.uppercase()},label={Text("UNIDAD: KG, G, UNIDAD, L...")})
  OutlinedTextField(s,{s=it},label={Text("EXISTENCIA")});OutlinedTextField(min,{min=it},label={Text("STOCK MÍNIMO")});OutlinedTextField(p,{p=it},label={Text("PRECIO DE COMPRA POR UNIDAD")})
 }},confirmButton={TextButton({
  val a=s.replace(",",".").toDoubleOrNull();val b=min.replace(",",".").toDoubleOrNull();val c=p.replace(",",".").toDoubleOrNull()
  if(n.isNotBlank()&&a!=null&&b!=null&&c!=null)save(Producto(n.trim().uppercase(),u.trim().uppercase(),a,b,c))
 }){Text("GUARDAR")}},dismissButton={TextButton(close){Text("CANCELAR")}})
}

@Composable
fun PlatoDialog(initial:Plato?,ps:List<Producto>,close:()->Unit,save:(Plato)->Unit){
 var n by remember{mutableStateOf(initial?.nombre?:"")};var p by remember{mutableStateOf(initial?.precio?.toString()?:"")}
 var r by remember{mutableStateOf(initial?.consumo?.entries?.joinToString(","){it.key+"="+it.value}?:"")}
 AlertDialog(onDismissRequest=close,title={Text(if(initial==null)"NUEVO PLATO / CAJA" else "EDITAR PLATO / CAJA")},text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
  OutlinedTextField(n,{n=it},label={Text("NOMBRE")});OutlinedTextField(p,{p=it},label={Text("PRECIO DE VENTA")})
  OutlinedTextField(r,{r=it},label={Text("CONSUMO: CARNE=0.250,CERDO=0.100")});Text("PRODUCTOS: "+ps.joinToString{it.nombre})
 }},confirmButton={TextButton({
  val price=p.replace(",",".").toDoubleOrNull();val map=mutableMapOf<String,Double>()
  r.split(",").forEach{z->val x=z.split("=");if(x.size==2){val v=x[1].trim().replace(",",".").toDoubleOrNull();if(!x[0].trim().isBlank()&&v!=null&&v>0)map[x[0].trim().uppercase()]=v}}
  if(n.isNotBlank()&&price!=null&&map.isNotEmpty())save(Plato(n.trim().uppercase(),price,map))
 }){Text("GUARDAR")}},dismissButton={TextButton(close){Text("CANCELAR")}})
}
