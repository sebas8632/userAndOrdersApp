# Guía de frameworks: Dagger, Hilt, Room, Retrofit, OkHttp, kotlinx.serialization, Coroutines y KSP

**Proyecto:** UserAndOrdersApp (`com.example.userandordersapp`)
**Relacionado:** [fase-2-capa-de-datos.md](../fases/fase-2-capa-de-datos.md)

Esta guía explica cada framework con la misma estructura:

1. **Qué es** y qué problema resuelve.
2. **Cómo funciona** por dentro.
3. **Anotaciones y piezas**, con qué hace cada una.
4. **Uso en el proyecto**, con los archivos reales.
5. **Errores comunes** y cómo leerlos.

Las anotaciones marcadas como **pendiente** todavía no están en el código. Aparecerán en la capa de presentación.

---

## Índice

- [0. Mapa de frameworks](#0-mapa-de-frameworks)
- [1. KSP (Kotlin Symbol Processing)](#1-ksp-kotlin-symbol-processing)
- [2. Dagger](#2-dagger)
- [3. Hilt](#3-hilt)
- [4. Room](#4-room)
- [5. Retrofit](#5-retrofit)
- [6. OkHttp](#6-okhttp)
- [7. kotlinx.serialization](#7-kotlinxserialization)
- [8. Kotlin Coroutines](#8-kotlin-coroutines)
- [9. Librerías declaradas y aún no usadas](#9-librerías-declaradas-y-aún-no-usadas)
- [10. Cómo se conectan todas las piezas](#10-cómo-se-conectan-todas-las-piezas)
- [11. Glosario](#11-glosario)

---

## 0. Mapa de frameworks

| Framework | Rol en el proyecto | Versión | Dónde se usa |
|---|---|---|---|
| KSP | Procesa las anotaciones y genera código en compilación | 2.3.12 | Plugin en `build.gradle.kts` |
| Dagger | Inyección de dependencias (grafo y proveedores) | Vía Hilt 2.60.1 | `@Inject`, `@Module`, `@Provides`, `@Binds` |
| Hilt | Dagger con componentes para Android | 2.60.1 | `@HiltAndroidApp`, `@InstallIn`, módulos en `di/` |
| Room | Base de datos SQLite con DAOs tipados | 2.8.5 | `data/database/`, `data/datasources/local/` |
| Retrofit | Cliente HTTP declarativo | 3.0.0 | `data/datasources/remote/api/` |
| OkHttp | Cliente HTTP de bajo nivel debajo de Retrofit | 5.5.0 | `NetworkModule` |
| kotlinx.serialization | JSON a clases Kotlin y viceversa | 1.11.0 | DTOs, `Converters`, `NetworkModule` |
| Coroutines | Concurrencia con `suspend` | 1.11.0 | Casos de uso, repositorios, data sources |

---

## 1. KSP (Kotlin Symbol Processing)

### 1.1 Qué es

KSP es una herramienta del compilador de Kotlin que **lee tu código y las anotaciones**, y genera código Kotlin o Java adicional antes de que termine la compilación.

Room y Hilt (Dagger) necesitan generar clases que tú no escribes: implementaciones de DAOs, la base de datos, las factories de Dagger, los componentes. KSP es el mecanismo que lo hace.

### 1.2 Cómo funciona

```
Tu código (anotaciones)
   │
   ▼
KSP lee los símbolos (clases, funciones, anotaciones)
   │
   ├── Room compiler  → UserDao_Impl, AppDatabase_Impl
   └── Hilt/Dagger    → factories, componentes, clases Hilt_*
   │
   ▼
Compilación normal de Kotlin con el código generado incluido
```

El código generado queda en `build/generated/ksp/`. Revisarlo es la mejor forma de entender qué hace un framework por dentro.

### 1.3 Piezas

| Elemento | Qué es |
|---|---|
| `plugins { alias(libs.plugins.ksp) }` | Activa KSP en el módulo. |
| `ksp(libs.androidx.room.compiler)` | Dice a KSP qué procesador ejecutar. Cada framework tiene el suyo. |
| `ksp(libs.hilt.compiler)` | Procesador de Hilt. |
| `build/generated/ksp/` | Carpeta donde queda el código generado. |

Diferencia con `implementation`: `ksp(...)` solo se usa en compilación. La librería no se incluye en el APK, solo su procesador.

### 1.4 En el proyecto

- `room-compiler` genera la implementación de los tres DAOs y de `AppDatabase`.
- `hilt-compiler` genera el componente de Dagger y las clases `Hilt_*`.
- Dagger también genera una factory por cada `@Inject constructor` y cada `@Provides`.

### 1.5 Errores comunes

- **El código generado no existe:** falta el plugin o el procesador (`ksp(...)`). Verifica que la línea `ksp(...)` exista para cada framework.
- **Errores que apuntan a código generado:** el mensaje menciona `_Impl` o `_Factory`. Casi siempre la causa está en tu código fuente, no en el archivo generado.
- **Versiones desalineadas:** KSP debe ser compatible con la versión de Kotlin. Si el proyecto deja de compilar tras cambiar una de las dos, revisa esa compatibilidad primero.

---

## 2. Dagger

### 2.1 Qué es

Dagger es un framework de **inyección de dependencias**. Su trabajo es construir objetos y entregarlos a las clases que los necesitan, sin que esas clases los creen.

### 2.2 El problema

Sin inyección, una clase crea sus dependencias:

```kotlin
class GetUsersUseCase {
    private val repo = UserRepositoryImpl(UserRemoteDataSource(...), UserLocalDataSource(...))
}
```

Problemas:

- **Acoplamiento:** `GetUsersUseCase` conoce las clases concretas.
- **Imposible de probar:** no puedes cambiar `repo` por una versión falsa.
- **Duplicación:** si dos clases necesitan la misma instancia, cada una crea la suya.

Con inyección:

```kotlin
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
)
```

La clase declara lo que necesita. Otro componente decide qué implementación entregar.

### 2.3 Cómo funciona por dentro

**Dagger lee las anotaciones y genera código Java o Kotlin**. Ese código contiene:

- Una **factory** por cada clase con `@Inject constructor`. Por ejemplo, para `GetUsersUseCase` genera algo como `GetUsersUseCase_Factory`, que sabe construirla pidiendo sus dependencias.
- Una **factory** por cada `@Provides`. Por ejemplo, `NetworkModule_ProvideRetrofitFactory`.
- Un **componente**: una clase que, al pedirle un tipo, recorre el grafo, construye cada nodo y devuelve el resultado.

No usa reflexión en tiempo de ejecución. Todo está resuelto en compilación.

#### Grafo de dependencias

Cada tipo es un nodo. Cada dependencia es una arista. Ejemplo del proyecto:

```
GetUsersUseCase
   └── UserRepository (interfaz)
          └── UserRepositoryImpl
                 ├── UserRemoteDataSource
                 │      └── UserApiService
                 │             └── Retrofit
                 │                    ├── OkHttpClient
                 │                    └── Json
                 └── UserLocalDataSource
                        └── UserDao
                               └── AppDatabase
                                      └── Context (aplicación)
```

Para construir `GetUsersUseCase`, Dagger recorre el grafo de abajo hacia arriba. Si un nodo no tiene proveedor, la compilación falla.

#### Ciclo de vida de una instancia

Cuando una clase pide un tipo, Dagger decide si crea uno nuevo o reutiliza uno existente. Eso depende del **scope**:

| Proveedor | Comportamiento |
|---|---|
| Sin scope | Una instancia **nueva** en cada inyección. |
| `@Singleton` | Una instancia **compartida** en todo el componente. |

Ejemplo: `UserRemoteDataSource` no tiene scope, así que cada repositorio que lo pida recibe una instancia nueva. `OkHttpClient` tiene `@Singleton`, así que todos los servicios comparten el mismo cliente, que es lo recomendado porque el cliente administra conexiones y un pool.

### 2.4 Anotaciones de Dagger

#### `@Inject`

Le dice a Dagger cómo construir una clase o qué campo rellenar.

**Constructor** (lo que usamos):

```kotlin
class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao
)
```

**Campo** (no usado en el proyecto):

```kotlin
class Example {
    @Inject lateinit var userDao: UserDao
}
```

Con campo, Dagger rellena la propiedad después de crear el objeto. Por eso necesita `lateinit` o un valor por defecto. El constructor es preferible porque la dependencia es obligatoria desde el inicio.

**Regla:** solo puede haber un constructor con `@Inject` por clase.

**Dónde se usa:** casos de uso, repositorios implementados, data sources.

#### `@Module`

Marca una clase u objeto que contiene **proveedores**. Se usa cuando no puedes poner `@Inject` en un constructor, por ejemplo porque la clase viene de una librería externa (Room, Retrofit, OkHttp, Json).

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule { ... }
```

**Dónde se usa:** `RepositoryModule`, `DatabaseModule`, `NetworkModule`.

#### `@Provides`

Dentro de un módulo tipo `object`, marca una función que **construye** un objeto.

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "user_and_orders.db").build()
```

- El **tipo de retorno** es la clave. Cualquier clase que pida un `AppDatabase` recibe el que devuelve esta función.
- Los **parámetros** son dependencias que Dagger resuelve antes de llamar la función.
- Se usa cuando hay lógica de construcción (builders, configuración).

**Dónde se usa:** `DatabaseModule`, `NetworkModule`.

#### `@Binds`

Dentro de una `abstract class`, enlaza una **interfaz** con una implementación que ya tiene `@Inject constructor`.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

| | `@Provides` | `@Binds` |
|---|---|---|
| Vive en | `object` | `abstract class` |
| Ejecuta código | Sí | No, solo declara el enlace |
| Código generado | Una llamada a función | Ninguna llamada extra |
| Cuándo usarlo | Hay lógica de construcción | La implementación ya tiene `@Inject constructor` |

Regla: `impl` debe ser subtipo de la interfaz de retorno.

**Dónde se usa:** `RepositoryModule`.

#### `@Singleton`

Scope de `javax.inject`. Dagger crea **una sola instancia** dentro del componente donde se declara.

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(...): OkHttpClient { ... }
```

Reglas:

- El scope debe coincidir con el componente. `@Singleton` solo funciona en `SingletonComponent` (o en componentes que lo tengan como padre). Usarlo en un componente con otro scope produce el error `IncompatiblyScopedBindings`.
- En `SearchProductsUseCase` es necesario: sin `@Singleton`, cada inyección tiene su propia caché.

**Dónde se usa:** módulos y `SearchProductsUseCase`.

#### `@Qualifier` y `@ApplicationContext`

Un **calificador** distingue dos instancias del mismo tipo. Por ejemplo, `Context` puede ser el de la aplicación o el de una activity.

`@ApplicationContext` es un calificador de Hilt que pide el contexto de la aplicación:

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase
```

Usar el contexto de una activity en un singleton causa fugas de memoria: el singleton mantiene viva la activity.

**Dónde se usa:** `DatabaseModule`, `NetworkModule`.

#### `@Named` (de `javax.inject`)

Calificador con nombre de texto. No se usa en el proyecto, porque `@ApplicationContext` cubre el caso.

```kotlin
@Provides @Named("base") fun provideBaseUrl(): String = "..."
```

#### Anotaciones de Dagger no usadas

| Anotación | Para qué sirve |
|---|---|
| `@Component` | Contenedor manual. Hilt lo genera por nosotros. |
| `@Subcomponent` | Contenedor hijo que hereda del padre. Hilt lo usa internamente. |
| `@Reusable` | Scope liviano: reutiliza la instancia sin garantizar que sea única. |
| `@Lazy<T>` (`dagger.Lazy`) | Inyecta un proveedor perezoso: el objeto se crea la primera vez que se pide `get()`. |
| `@Provider<T>` | Inyecta una función que devuelve una instancia nueva cada vez que se llama `get()`. |
| `@IntoSet`, `@IntoMap`, `@Multibinds` | Junta varias implementaciones del mismo tipo en un conjunto o un mapa. |
| `@BindsOptionalOf` | Dependencia opcional: si nadie la provee, no falla. |

### 2.5 Uso en el proyecto

| Clase | Anotaciones | Motivo |
|---|---|---|
| `GetUsersUseCase` y demás casos de uso | `@Inject constructor` | Dagger los construye solos. |
| `SearchProductsUseCase` | `@Inject constructor` + `@Singleton` | La caché debe ser única. |
| `UserRepositoryImpl` y demás | `@Inject constructor` | Dagger los construye solos. |
| `UserRemoteDataSource` y demás | `@Inject constructor` | Dagger los construye solos. |
| `AppDatabase`, `Json`, `OkHttpClient`, `Retrofit` | `@Provides` | Los construyen funciones, no constructores propios. |
| `UserRepository` → `UserRepositoryImpl` | `@Binds` | Enlace interfaz → implementación. |

### 2.6 Errores comunes

| Error | Causa | Solución |
|---|---|---|
| `[Dagger/MissingBinding] X cannot be provided` | Falta un `@Provides` o `@Binds` para `X`, o el módulo no está instalado. | Agregar el proveedor en un módulo con `@InstallIn`. |
| `[Dagger/DuplicateBindings]` | Dos proveedores para el mismo tipo. | Dejar uno, o usar calificadores para distinguirlos. |
| `[Dagger/IncompatiblyScopedBindings]` | Un scope no coincide con el componente. | Usar el scope del componente donde vive el módulo. |

Consejo: el primer error del log es el que importa. Los siguientes suelen ser consecuencias del mismo problema.

---

## 3. Hilt

### 3.1 Qué es

Hilt es Dagger con **componentes predefinidos para Android**. Dagger no sabe cuándo nace o muere una `Activity`. Hilt sí, y crea los componentes por ti.

Sin Hilt tendrías que escribir un componente, conectarlo al ciclo de vida de la `Application` y recrearlo en cada activity. Hilt hace eso.

### 3.2 Cómo funciona

Cuando la app arranca:

1. Android crea la clase `UserAndOrdersApplication`.
2. Hilt, mediante la clase generada `Hilt_UserAndOrdersApplication` (su clase base), crea el **`SingletonComponent`**.
3. El componente recorre los módulos instalados en él y registra sus proveedores.
4. Cuando alguien pide una dependencia, el componente la construye o la reutiliza según su scope.

Hilt también genera clases para cada `@AndroidEntryPoint`, `@HiltViewModel` y otros puntos de entrada, con su propio componente hijo.

### 3.3 Componentes y scopes

Cada componente vive mientras viva un ciclo de vida de Android:

```
SingletonComponent               vive mientras viva la app          @Singleton
 ├── ActivityRetainedComponent    sobrevive rotaciones               @ActivityRetainedScoped
 │    └── ViewModelComponent      vive mientras viva el ViewModel    @ViewModelScoped
 └── ActivityComponent            vive mientras viva la activity     @ActivityScoped
      ├── FragmentComponent       @FragmentScoped
      ├── ViewComponent           @ViewScoped
      └── ServiceComponent        @ServiceScoped
```

Un módulo elige su componente con `@InstallIn`:

| Valor de `@InstallIn` | Ciclo de vida | Ejemplo |
|---|---|---|
| `SingletonComponent::class` | Toda la app | Base de datos, red, repositorios |
| `ViewModelComponent::class` | Un ViewModel | Dependencias de una pantalla |
| `ActivityComponent::class` | Una activity | Helpers de navegación |

En este proyecto usamos `SingletonComponent` en todos los módulos, porque la base de datos, la red y los repositorios deben vivir mientras viva la app.

### 3.4 Anotaciones de Hilt

#### `@HiltAndroidApp`

Va en la clase `Application`. Activa Hilt y genera el componente raíz.

```kotlin
@HiltAndroidApp
class UserAndOrdersApplication : Application()
```

Debe estar declarada en el manifest:

```xml
<application android:name=".UserAndOrdersApplication" ... >
```

Sin ella, ninguna clase con `@Inject` del proyecto funciona.

**Estado:** usada.

#### `@InstallIn`

Va en cada módulo. Indica el componente donde se instala.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule { ... }
```

Un módulo sin `@InstallIn` no entra al grafo: sus proveedores no existen y las clases que los necesitan fallan al compilar.

**Estado:** usada en los tres módulos.

#### `@AndroidEntryPoint` (pendiente)

Va en `Activity`, `Fragment`, `Service` o `BroadcastReceiver`. Permite que Hilt rellene campos `@Inject` de esa clase.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

Sin ella, un `@Inject` dentro de la activity queda vacío y la app falla al usarlo.

#### `@HiltViewModel` (pendiente)

Va en un ViewModel con `@Inject constructor`. Hilt lo construye con sus dependencias.

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProducts: GetProductsUseCase
) : ViewModel()
```

En Compose se obtiene con `hiltViewModel()`. Sin `@HiltViewModel`, la fábrica por defecto no sabe construirlo con dependencias.

#### `@ApplicationContext` y `@ActivityContext`

Calificadores de `Context`:

- `@ApplicationContext`: vive lo mismo que la app. Lo usamos.
- `@ActivityContext`: solo existe en componentes de activity. Pedirlo en `SingletonComponent` produce error de compilación.

#### `@ViewModelScoped`, `@ActivityScoped`, etc.

Scopes para componentes de vida corta. Solo se pueden usar en el componente que corresponde.

```kotlin
@ViewModelScoped
class SomeHelper @Inject constructor() { ... }
```

No usadas aún.

#### `@EntryPoint`

Permite obtener dependencias de Hilt desde una clase que no puede usar `@Inject`, como un `ContentProvider`. No usada.

#### `@DefineComponent`

Permite definir un componente propio de Hilt. No usada.

#### `@HiltAndroidTest` y `@TestInstallIn`

Para pruebas instrumentadas. La clase de test se marca con `@HiltAndroidTest` y declara una regla:

```kotlin
@HiltAndroidTest
class HomeScreenTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }
}
```

`@TestInstallIn` reemplaza un módulo real por uno de prueba:

```kotlin
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object FakeNetworkModule {
    @Provides
    fun provideUserApiService(): UserApiService = FakeUserApiService()
}
```

Así un test puede usar datos falsos sin tocar la API real. El runner `HiltTestRunner.kt` ya está configurado en `build.gradle.kts`. `HomeScreenTest` aún es un placeholder.

### 3.5 Uso en el proyecto

```
UserAndOrdersApplication   @HiltAndroidApp
   │ genera Hilt_UserAndOrdersApplication
   ▼
SingletonComponent
   ├── DatabaseModule       @Provides  → AppDatabase, UserDao, ProductDao, OrderDao
   ├── NetworkModule        @Provides  → Json, OkHttpClient, Retrofit, ApiServices
   └── RepositoryModule     @Binds    → UserRepository, ProductRepository, OrderRepository
```

### 3.6 Errores comunes

| Error | Causa | Solución |
|---|---|---|
| Error de Hilt que pide `@HiltAndroidApp` | Falta la anotación o el manifest no declara la clase. | Revisar `@HiltAndroidApp` y `android:name`. |
| `@ActivityContext` no disponible en `SingletonComponent` | Se pidió un contexto de activity en un singleton. | Usar `@ApplicationContext`. |
| La activity tiene campos `@Inject` vacíos | Falta `@AndroidEntryPoint`. | Agregar la anotación. |
| `[Dagger/MissingBinding]` al compilar los tests | Un test quita un módulo (`@UninstallModules`) o reemplaza uno (`@TestInstallIn`) y deja sin proveedor a una dependencia. | Reemplazar también los proveedores que se pierden, o no quitar el módulo. |

---

## 4. Room

### 4.1 Qué es

Room es una capa sobre **SQLite**, la base de datos que Android trae integrada. Sin Room, tendrías que:

- Escribir SQL como texto, sin revisión del compilador.
- Convertir cada fila del cursor a un objeto a mano.
- Cuidar que las consultas no bloqueen el hilo principal.

Room hace eso por ti, y además **valida el SQL al compilar**. Si una consulta tiene un error de sintaxis o una columna que no existe, la compilación falla.

### 4.2 Cómo funciona

Room tiene tres piezas que se conectan:

```
Entidades (@Entity)  → definen las tablas y sus columnas
DAOs (@Dao)          → definen las consultas sobre esas tablas
Database (@Database) → junta las tablas y los DAOs, y abre el archivo SQLite
```

Al compilar, el `room-compiler` (vía KSP) genera:

- `UserDao_Impl`, `ProductDao_Impl`, `OrderDao_Impl`: implementan los DAOs con SQL y conversiones.
- `AppDatabase_Impl`: abre la base, crea las tablas y entrega los DAOs.

Puedes verlas en `build/generated/ksp/`. Ahí se ve el SQL exacto que usa cada consulta.

### 4.3 Entidades: `@Entity`

Una entidad es una clase que representa una tabla. Cada propiedad es una columna.

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatar: String?,
    val createdAt: Long
)
```

Parámetros de `@Entity`:

| Parámetro | Qué hace | Ejemplo |
|---|---|---|
| `tableName` | Nombre de la tabla. Por defecto, el nombre de la clase. | `tableName = "users"` |
| `primaryKeys` | Clave primaria compuesta, con varios campos. | `primaryKeys = ["userId", "productId"]` |
| `indices` | Índices para acelerar búsquedas. | `indices = [Index("email")]` |
| `foreignKeys` | Relación con otra tabla. | Ver `@ForeignKey` abajo. |
| `ignoredColumns` | Propiedades que no se guardan. | `ignoredColumns = ["tmp"]` |

En el proyecto usamos solo `tableName`.

### 4.4 Columnas y claves

| Anotación | Qué hace | Ejemplo |
|---|---|---|
| `@PrimaryKey` | Marca la clave primaria. | `@PrimaryKey val id: String` |
| `@PrimaryKey(autoGenerate = true)` | SQLite asigna el número automáticamente. Solo para `Int` o `Long`. | `@PrimaryKey(autoGenerate = true) val id: Long` |
| `@ColumnInfo(name = "...")` | Cambia el nombre de la columna. | `@ColumnInfo(name = "created_at") val createdAt: Long` |
| `@ColumnInfo(defaultValue = "...")` | Valor por defecto en SQL. | `@ColumnInfo(defaultValue = "0") val stock: Int` |
| `@Ignore` | Propiedad que no se guarda. | `@Ignore val cache: String? = null` |
| `@Embedded` | Agrega los campos de otra clase como columnas de esta tabla. | `@Embedded val address: Address` |

En el proyecto, `id` es `String` y tiene `@PrimaryKey` sin `autoGenerate`, porque el id viene de la API.

### 4.5 Relaciones

Room puede modelar relaciones entre tablas, aunque en el proyecto las dejamos como listas JSON.

| Anotación | Qué hace |
|---|---|
| `@ForeignKey(entity, parentColumns, childColumns, onDelete)` | Exige que el valor exista en la tabla padre. |
| `@Relation(...)` | Carga objetos relacionados en un solo objeto. Requiere `@Transaction`. |
| `@Index` | Índice sobre una columna que aparece en `WHERE` o `JOIN`. |

Ejemplo con clave foránea (no usado en el proyecto):

```kotlin
@Entity(
    tableName = "orders",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class OrderEntity(...)
```

Con `CASCADE`, borrar un usuario borra sus órdenes.

#### Por qué `OrderEntity` no tiene clave foránea

`OrderEntity.products` es una lista de `ProductEntity`. SQLite no guarda listas, así que usamos un `TypeConverter` que la convierte a JSON. Eso impide relacionar productos con su tabla, pero simplifica la caché. La decisión está en la fase 2.

### 4.6 TypeConverters

SQLite solo guarda `Int`, `Long`, `Double`, `String`, `ByteArray` y `null`. Cualquier otro tipo necesita un convertidor.

```kotlin
class Converters {
    @TypeConverter
    fun fromProducts(products: List<ProductEntity>): String = Json.encodeToString(products)

    @TypeConverter
    fun toProducts(value: String): List<ProductEntity> = Json.decodeFromString(value)
}
```

| Anotación | Dónde | Qué hace |
|---|---|---|
| `@TypeConverter` | Función | Marca una conversión. Necesitas una para guardar y otra para leer. |
| `@TypeConverters(Converters::class)` | Entidad, DAO o base | Indica qué clase de convertidores usar. |

Room no necesita convertidor para los enums: los guarda por nombre. Por eso `OrderStatus` no tiene uno.

### 4.7 DAOs: `@Dao`, `@Query`, `@Insert`, `@Update`, `@Delete`, `@Upsert`

Un DAO es una interfaz con las operaciones sobre una tabla.

```kotlin
@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
}
```

| Anotación | Qué hace | Ejemplo en el proyecto |
|---|---|---|
| `@Dao` | Marca una interfaz de acceso a datos. | `UserDao`, `ProductDao`, `OrderDao` |
| `@Query("SQL")` | Ejecuta SQL. Los parámetros se enlazan con `:nombre`. | `search(query)` |
| `@Insert` | Inserta una o varias filas. Devuelve el `rowId` si se declara `Long`. | `insertAll` |
| `@Update` | Actualiza filas por clave primaria. Devuelve el número de filas afectadas. | `UserDao.update` |
| `@Delete` | Elimina por clave primaria. Devuelve el número de filas. | No usada (usamos `@Query("DELETE ...")`) |
| `@Upsert` | Inserta, o actualiza si la clave ya existe. | No usada |
| `@Transaction` | Ejecuta el método como una transacción. Necesario para relaciones. | No usada |
| `@RawQuery` | Consulta SQL construida en tiempo de ejecución. | No usada |

#### Estrategias de conflicto

`OnConflictStrategy` decide qué pasa si la clave ya existe:

| Estrategia | Efecto |
|---|---|
| `ABORT` (por defecto) | Cancela la operación y lanza error. |
| `REPLACE` | Borra la fila existente y guarda la nueva. |
| `IGNORE` | Deja la fila existente y no inserta. |
| `ROLLBACK` | Cancela la transacción completa. |
| `FAIL` | Lanza error, sin revertir la transacción. |

En el proyecto usamos `REPLACE`. Así, guardar un producto que ya está en caché lo actualiza.

#### Consultas que devuelven `Flow`

Con `room-ktx`, una consulta puede devolver `Flow<T>`. El Flow emite de nuevo cuando la tabla cambia. No la usamos todavía, pero es la base para que la UI se actualice sola cuando cambian los datos.

```kotlin
@Query("SELECT * FROM products")
fun observeAll(): Flow<List<ProductEntity>>
```

### 4.8 Base de datos: `@Database`

```kotlin
@Database(
    entities = [UserEntity::class, ProductEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
}
```

| Parámetro | Qué hace |
|---|---|
| `entities` | Lista de tablas. |
| `version` | Versión del esquema. Debe subir si cambian las tablas. |
| `exportSchema` | Si es `true`, Room guarda un JSON con el esquema de cada versión, útil para migraciones. Aquí es `false`. |

La base se construye en `DatabaseModule`:

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "user_and_orders.db").build()
```

El archivo `user_and_orders.db` queda en el almacenamiento interno de la app.

#### Migraciones

Si cambias una tabla (agregas una columna, por ejemplo), debes:

1. Subir `version` de `1` a `2`.
2. Escribir una **migración**: el SQL que transforma la base vieja en la nueva.
3. Registrarla en el builder con `addMigrations(...)`.

Si no lo haces, Room lanza un error al abrir la base. Una alternativa es `fallbackToDestructiveMigration()`, que borra la base y la vuelve a crear. Pierdes los datos locales, así que no sirve para producción sin cuidado.

En esta fase la versión es `1`. Por eso no hay migraciones todavía.

### 4.9 Hilos y corrutinas

- Las funciones `suspend` del DAO se ejecutan fuera del hilo principal. Room las lanza en su propio ejecutor.
- Una función **no** `suspend` que consulte la base en el hilo principal lanza error (`Cannot access database on the main thread`). Por eso todos los DAOs del proyecto son `suspend`.
- Si la corrutina se cancela, Room cancela la consulta.

### 4.10 Uso en el proyecto

```
DatabaseModule (@Provides)
   └── AppDatabase  (@Database)
          ├── userDao()  → UserDao    (@Dao)  → UserEntity    (@Entity "users")
          ├── productDao() → ProductDao (@Dao)  → ProductEntity (@Entity "products")
          └── orderDao()  → OrderDao   (@Dao)  → OrderEntity   (@Entity "orders")
                                                    └── @TypeConverters(Converters)
```

Los data sources locales (`UserLocalDataSource`, etc.) envuelven los DAOs. `UserRepositoryImpl` y los demás usan los data sources.

### 4.11 Errores comunes

| Error | Causa | Solución |
|---|---|---|
| `Cannot figure out how to save this field into database` | Un campo no es un tipo que Room sepa guardar. | Agregar un `@TypeConverter`. |
| `There is a problem with the query` (en tiempo de compilación) | El SQL de `@Query` tiene un error. | Revisar la sintaxis y el nombre de tabla y columnas. |
| `A migration from 1 to 2 was required but not found` | Cambió el esquema sin migración. | Subir `version` y escribir la migración. |
| `Cannot access database on the main thread` | Consulta no `suspend` en el hilo principal. | Hacer el método `suspend` o usar Flow. |
| `The columns returned by the query do not match` | La consulta no devuelve las columnas que espera la entidad o el objeto. | Ajustar el SELECT o el tipo de retorno. |

---

## 5. Retrofit

### 5.1 Qué es

Retrofit convierte una **interfaz Kotlin anotada** en un cliente HTTP. Describes el contrato y Retrofit arma la petición, la envía, convierte la respuesta y devuelve el resultado.

```kotlin
interface UserApiService {
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserDto
}
```

Retrofit no hace la conexión por sí mismo. Delega en **OkHttp** para eso y en un **converter** para el JSON.

### 5.2 Cómo funciona

Cuando llamas a `retrofit.create(UserApiService::class.java)`:

1. **Proxy dinámico:** Retrofit crea un objeto con `java.lang.reflect.Proxy` que implementa la interfaz. Cada llamada a un método pasa por ese proxy.
2. **Lectura de anotaciones:** la primera vez que se llama un método, Retrofit lee sus anotaciones y las guarda en caché. Ese paso produce un `ServiceMethod` con el verbo, la ruta y los parámetros.
3. **Armado de la petición:** reemplaza `{id}` con el valor de `@Path`, agrega los `@Query`, y serializa el `@Body`.
4. **Envío:** OkHttp ejecuta la petición.
5. **Conversión:** el converter convierte el JSON al tipo de retorno (`UserDto`).
6. **Resultado:** devuelve el cuerpo o lanza excepción.

#### Ejemplo de la petición que genera

Para `getUserById("123")` con base `https://api.example.com/`, la petición HTTP aproximada es:

```
GET /users/123 HTTP/1.1
Host: api.example.com
Accept-Encoding: gzip
User-Agent: okhttp/5.5.0
```

Y para `createUser(user)`:

```
POST /users HTTP/1.1
Host: api.example.com
Content-Type: application/json

{"id":"1","name":"Ana","email":"ana@example.com","avatar":null,"createdAt":1700000000000}
```

#### Soporte de `suspend`

Kotlin agrega un parámetro extra de tipo `Continuation` a las funciones `suspend`. Retrofit lo detecta y ejecuta la petición dentro de una corrutina. Si la corrutina se cancela, Retrofit cancela la petición de OkHttp.

Por eso una función `suspend fun getUsers(): List<UserDto>` devuelve la lista directamente, sin `Call` ni callbacks.

### 5.3 Configuración

```kotlin
Retrofit.Builder()
    .baseUrl(Constants.BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
```

| Método | Qué hace |
|---|---|
| `baseUrl(...)` | URL base. Debe terminar en `/`. Si no, lanza `IllegalArgumentException`. |
| `client(...)` | Cliente OkHttp que ejecuta las peticiones. Si no lo pasas, Retrofit crea uno propio sin configuración. |
| `addConverterFactory(...)` | Registra el convertidor de JSON. Sin él, Retrofit no sabe convertir DTOs. |
| `json.asConverterFactory(mediaType)` | Función de extensión de `retrofit2.converter.kotlinx.serialization`. Crea el converter con tu instancia de `Json`. |
| `"application/json".toMediaType()` | Convierte el texto a `MediaType` de OkHttp. |

Puedes agregar varios converters. Retrofit usa el primero que acepte el tipo.

### 5.4 Anotaciones de método

| Anotación | Verbo | Uso típico | En el proyecto |
|---|---|---|---|
| `@GET("ruta")` | GET | Leer | `getUsers`, `getUserById`, `getProducts`, `searchProducts`, `getOrders`, `getOrderById` |
| `@POST("ruta")` | POST | Crear | `createUser`, `createOrder` |
| `@PUT("ruta")` | PUT | Reemplazar un recurso | `updateUser` |
| `@PATCH("ruta")` | PATCH | Actualizar parcialmente | No usada |
| `@DELETE("ruta")` | DELETE | Eliminar | `deleteUser` |
| `@HEAD("ruta")` | HEAD | Solo headers, sin cuerpo | No usada |
| `@OPTIONS("ruta")` | OPTIONS | Consultar opciones del servidor | No usada |
| `@HTTP(method, path, hasBody)` | Cualquiera | Verbo personalizado | No usada |
| `@Headers("Nombre: valor")` | Fija headers | Agrega headers fijos al método | No usada |

La ruta es **relativa a la base**:

- `@GET("users")` con base `https://api.example.com/` → `https://api.example.com/users`.
- `@GET("/users")` (con `/` inicial) → Retrofit descarta la ruta de la base. Por eso no usamos `/` al inicio.

### 5.5 Anotaciones de parámetro

| Anotación | Qué hace | Ejemplo en el proyecto |
|---|---|---|
| `@Path("nombre")` | Reemplaza `{nombre}` en la ruta. | `@Path("id") id: String` → `users/123` |
| `@Query("nombre")` | Agrega `?nombre=valor`. | `@Query("q") query: String` → `products/search?q=mesa` |
| `@QueryMap` | Varios query params desde un `Map`. | No usada |
| `@Body` | Envía el objeto como JSON. | `@Body user: UserDto` |
| `@Header("nombre")` | Header que cambia por llamada. | No usada |
| `@Url` | URL completa en vez de la ruta. | No usada |

Reglas:

- `@Path` con valor `null` lanza error. Por eso los parámetros de ruta no son nulos.
- `@Query` con valor `null` omite el parámetro. Útil para filtros opcionales.

#### Formularios y multipart (no usados)

| Anotación | Para qué sirve |
|---|---|
| `@FormUrlEncoded` + `@Field("x")` | Formulario `application/x-www-form-urlencoded`. |
| `@FieldMap` | Varios campos de formulario desde un `Map`. |
| `@Multipart` + `@Part` | Subir archivos o datos multiparte. |
| `@PartMap` | Varias partes desde un `Map`. |

Las APIs REST con JSON normalmente no las necesitan.

### 5.6 Tipos de retorno

| Tipo | Comportamiento |
|---|---|
| `T` (por ejemplo `UserDto`) | Devuelve el cuerpo. Lanza `HttpException` si hay error HTTP. |
| `Response<T>` | Devuelve cuerpo **y** código de estado. No lanza por errores HTTP. |
| `Unit` | Descarta el cuerpo. |

`Response<Unit>` se usa en `deleteUser` para revisar solo el código:

```kotlin
@DELETE("users/{id}")
suspend fun deleteUser(@Path("id") id: String): Response<Unit>
```

```kotlin
suspend fun deleteUser(id: String): Boolean = userApiService.deleteUser(id).isSuccessful
```

### 5.7 Uso en el proyecto

| Archivo | Responsabilidad |
|---|---|
| `api/UserApiService.kt` | Endpoints de usuarios. |
| `api/ProductApiService.kt` | Endpoints de productos. |
| `api/OrderApiService.kt` | Endpoints de órdenes. |
| `UserRemoteDataSource.kt` | Envuelve al servicio para los repositorios. |
| `NetworkModule.kt` | Crea `Retrofit` y los servicios. |

### 5.8 Errores comunes

| Error | Causa | Solución |
|---|---|---|
| `baseUrl must end in /` | Falta la barra final en la URL base. | Agregar `/`. |
| Error de Retrofit que indica que el método no tiene anotación HTTP | Falta `@GET`, `@POST`, etc. | Agregar el verbo. |
| `IllegalArgumentException` por `@Path` con `null` | Parámetro de ruta nulo. | Revisar el valor. |
| `HttpException` (404, 500) | El servidor respondió con error. | Revisar la URL y el endpoint. |
| `IOException` / `UnknownHostException` | Sin conexión, o URL inexistente (como `api.example.com`). | Revisar la conexión y `BASE_URL`. |
| `MissingFieldException` / `JsonDecodingException` | El JSON no coincide con el DTO. | Revisar el DTO contra la respuesta real. |

---

## 6. OkHttp

### 6.1 Qué es

OkHttp es el **cliente HTTP** que hace las peticiones de verdad. Abre conexiones, las reutiliza, comprime, reintenta según la configuración y maneja HTTP/2. Retrofit no hace nada de eso: solo describe la petición y entrega la respuesta.

Puedes usar OkHttp sin Retrofit, pero Retrofit siempre usa OkHttp por debajo (o cualquier otro `okhttp3.Call.Factory`).

### 6.2 Cómo funciona

```
Retrofit (describe la petición)
   │
   ▼
OkHttpClient (configuración: interceptores, timeouts, caché)
   │
   ▼
Interceptores de aplicación  →  (lógica tuya, por cada llamada)
   │
   ▼
Conexión, pool de conexiones, HTTP/2, reintentos
   │
   ▼
Interceptores de red        →  (lógica tuya, por cada conexión real)
   │
   ▼
Servidor
```

Un `OkHttpClient` es costoso de crear y guarda un pool de conexiones. Por eso debe ser `@Singleton` y compartirse entre servicios.

### 6.3 Piezas y configuración

#### `OkHttpClient.Builder`

```kotlin
OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
    .build()
```

| Método | Qué hace | Por defecto |
|---|---|---|
| `addInterceptor(...)` | Agrega un interceptor de aplicación. | Ninguno |
| `addNetworkInterceptor(...)` | Agrega un interceptor de red. | Ninguno |
| `connectTimeout(duration)` | Tiempo máximo para establecer la conexión. | 10 s |
| `readTimeout(duration)` | Tiempo máximo esperando datos del servidor. | 10 s |
| `writeTimeout(duration)` | Tiempo máximo para enviar datos. | 10 s |
| `callTimeout(duration)` | Tiempo máximo total de una llamada. | Sin límite (0) |
| `cache(Cache(...))` | Caché HTTP en disco. | Sin caché |
| `authenticator(...)` | Lógica para responder a un 401. | Ninguna |

#### Interceptores

Un interceptor es un bloque que recibe la petición, puede modificarla, la envía con `chain.proceed(...)` y puede modificar la respuesta.

```kotlin
val authInterceptor = Interceptor { chain ->
    val request = chain.request().newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
    chain.proceed(request)
}
```

| Tipo | Cuándo se ejecuta | Caso de uso |
|---|---|---|
| Aplicación (`addInterceptor`) | Una vez por llamada, aunque haya reintentos o redirecciones. | Agregar headers, registrar, reintentar. |
| Red (`addNetworkInterceptor`) | Por cada conexión real. | Ver las redirecciones y el tráfico real. |

#### `HttpLoggingInterceptor`

Imprime las peticiones. Tiene cuatro niveles:

| Nivel | Qué imprime |
|---|---|
| `NONE` | Nada. |
| `BASIC` | Método, URL y código de respuesta. |
| `HEADERS` | Lo anterior y los headers. |
| `BODY` | Lo anterior y el cuerpo. |

En el proyecto, `BODY` solo se usa en builds depurables:

```kotlin
val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
```

`FLAG_DEBUGGABLE` es un bit de los flags de la app. La operación `and` aísla ese bit, y `!= 0` indica si está activo.

**Por qué importa:** `BODY` imprime datos del usuario, como emails y órdenes. En release debe estar en `NONE`.

#### Caché HTTP (no usada)

```kotlin
val cache = Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024)
OkHttpClient.Builder().cache(cache).build()
```

Guarda respuestas en disco y respeta los headers `Cache-Control`. Es distinto de la caché de Room: la de OkHttp la maneja el protocolo HTTP, la de Room la manejamos nosotros.

#### Authenticator (no usado)

Se ejecuta cuando el servidor responde 401. Puede pedir un token nuevo y reintentar la petición:

```kotlin
OkHttpClient.Builder().authenticator(object : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = ...
}).build()
```

Útil cuando la API usa tokens que caducan.

### 6.4 Conexiones, pool y HTTP/2

- **Pool de conexiones:** OkHttp guarda conexiones abiertas y las reutiliza para peticiones al mismo host. Por eso el cliente debe ser singleton.
- **HTTP/2:** se negocia automáticamente con TLS. Varias peticiones comparten una conexión.
- **Compresión:** OkHttp envía `Accept-Encoding: gzip` y descomprime la respuesta sin que lo pidas.

### 6.5 Uso en el proyecto

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val loggingLevel = if (isDebuggable) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
        .build()
}
```

Solo tenemos el logging. Timeouts, caché y autenticación usan los valores por defecto.

### 6.6 Errores comunes

| Excepción | Causa | Efecto en la app |
|---|---|---|
| `UnknownHostException` | No hay DNS para el host (sin conexión, o URL inexistente). | Es `IOException`: `UserRepositoryImpl` usa la caché. |
| `SocketTimeoutException` | El servidor tardó más que el timeout. | Es `IOException`: se usa la caché. |
| `SSLHandshakeException` | Certificado inválido o TLS no compatible. | Es `IOException`: se usa la caché. |
| `ConnectException` | No se pudo conectar. | Es `IOException`. |

Todas son subclases de `IOException`, así que el `catch (e: IOException)` de los repositorios las captura. Los errores HTTP (`HttpException`) no lo son y se propagan.

---

## 7. kotlinx.serialization

### 7.1 Qué es

kotlinx.serialization es la librería oficial de JetBrains para convertir objetos Kotlin a formatos como **JSON** y viceversa. Funciona en compilación: el plugin genera un **serializador** para cada clase `@Serializable`, sin reflexión.

### 7.2 Cómo funciona

El plugin `org.jetbrains.kotlin.plugin.serialization` genera, para cada clase `@Serializable`, un objeto `serializer()` que describe sus propiedades. El formato (`Json`) usa ese descriptor para leer y escribir.

```kotlin
val json = Json { ignoreUnknownKeys = true }

val dto: UserDto = json.decodeFromString("""{"id":"1","name":"Ana","email":"a@b.com","createdAt":0}""")
val texto: String = json.encodeToString(dto)
```

### 7.3 Anotaciones

| Anotación | Dónde | Qué hace | En el proyecto |
|---|---|---|---|
| `@Serializable` | Clase, enum, objeto | Genera el serializador. Sin ella, la conversión falla. | DTOs, `ProductEntity` |
| `@SerialName("nombre")` | Propiedad o entrada de enum | Usa otro nombre en el JSON. Útil para `snake_case`. | No usada |
| `@Transient` | Propiedad | Excluye la propiedad de la conversión. Debe tener valor por defecto. | No usada |
| `@Contextual` | Propiedad | Delega la conversión a un serializador registrado en el `SerializersModule`. | No usada |
| `@Polymorphic` | Clase base | Para jerarquías, con un campo que indica el tipo. | No usada |

Ejemplo de `@SerialName`:

```kotlin
@Serializable
data class UserDto(
    val id: String,
    @SerialName("created_at") val createdAt: Long
)
```

### 7.4 Opciones de `Json`

```kotlin
Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
```

| Opción | Por defecto | Efecto |
|---|---|---|
| `ignoreUnknownKeys` | `false` | Ignora campos del JSON que no están en la clase. Con `true`, un campo nuevo en la API no rompe la app. **En el proyecto: `true`.** |
| `explicitNulls` | `true` | Si `false`, los campos `null` pueden omitirse al decodificar. |
| `encodeDefaults` | `false` | Si `false`, las propiedades con valor por defecto no se escriben en el JSON. Por ejemplo, `avatar = null` no aparece al enviar. |
| `coerceInputValues` | `false` | Si `true`, usa el valor por defecto cuando el JSON trae un valor inválido (por ejemplo, un enum desconocido). |
| `isLenient` | `false` | Acepta JSON menos estricto (comillas faltantes, etc.). |
| `prettyPrint` | `false` | Formatea el JSON con sangrías. Útil para depurar. |

**Importante:** con `encodeDefaults = false` (el valor por defecto), si un DTO tiene `avatar: String? = null`, el campo no se envía. Si la API espera el campo siempre, cambia la opción a `true`.

### 7.5 Tipos soportados

| Tipo | Cómo se convierte |
|---|---|
| `String`, `Int`, `Long`, `Double`, `Boolean` | Directo. |
| Enums | Por nombre (`"PENDING"`). `@SerialName` en una entrada cambia el nombre. |
| `List<T>`, `Map<K, V>` | Recursivo, si `T` es serializable. |
| `T?` | `null` o el valor. |
| Clases `@Serializable` anidadas | Recursivo. |

Un tipo no serializable (por ejemplo, una clase de Android) requiere un serializador manual o `@Contextual`.

### 7.6 Errores comunes

| Excepción | Causa | Solución |
|---|---|---|
| `MissingFieldException` | El JSON no trae un campo que no tiene valor por defecto. | Agregar `= null` o un valor por defecto. |
| `JsonDecodingException` | Un valor tiene un tipo distinto al esperado (texto donde va un número). | Revisar la API o el DTO. |
| `SerializationException` (clase no serializable) | Falta `@Serializable` en una clase usada en un DTO. | Agregar la anotación. |
| Error de compilación: "Serializer for class X is not found" | Falta el plugin de serialización o la clase no es `@Serializable`. | Revisar el plugin y la anotación. |

Estas excepciones no son `IOException`, así que los repositorios las dejan pasar. Un JSON mal formado no se resuelve con caché: es un error de contrato con la API.

---

## 8. Kotlin Coroutines

### 8.1 Qué es

Las corrutinas son un mecanismo de Kotlin para **pausar** una función sin bloquear el hilo. Una función `suspend` puede esperar una respuesta de red o de disco mientras el hilo atiende otras tareas.

### 8.2 Cómo funciona

```kotlin
suspend fun getUsers(): List<User> = userRepository.getUsers()
```

- `suspend` indica que la función puede pausarse. Solo puede llamarse desde otra función `suspend` o desde una corrutina.
- Cuando la función espera (red, disco), libera el hilo. Cuando el resultado está listo, la corrutina continúa, a veces en otro hilo.
- Una corrutina se lanza con un **scope**, por ejemplo `viewModelScope.launch { ... }` (pendiente).

### 8.3 Piezas

| Elemento | Qué hace |
|---|---|
| `suspend` | Marca una función que puede pausarse. |
| `Dispatchers.Main` | Hilo principal. Para actualizar la UI. |
| `Dispatchers.IO` | Hilos para red y disco. |
| `Dispatchers.Default` | Hilos para cálculo intensivo. |
| `withContext(dispatcher)` | Cambia de hilo dentro de una función `suspend`. |
| `CancellationException` | Excepción especial que indica que la corrutina se canceló. No debe tragarse. |
| `runCatching { }` | Captura cualquier excepción, incluida la de cancelación. Úsalo con cuidado dentro de corrutinas. |

### 8.4 Uso en el proyecto

| Capa | Cómo usa las corrutinas |
|---|---|
| Casos de uso | `suspend operator fun invoke(...)`. |
| Repositorios | `suspend` en la interfaz y en la implementación. |
| Data sources remotos | Retrofit ejecuta la petición en la corrutina y la cancela si se cancela la corrutina. |
| DAOs de Room | Room ejecuta la consulta en su ejecutor y respeta la cancelación. |

El proyecto no lanza corrutinas propias todavía. Cuando llegue la capa de presentación, los ViewModels las lanzarán con `viewModelScope`.

### 8.5 Errores comunes

| Problema | Causa | Solución |
|---|---|---|
| `Suspend function should be called only from a coroutine or another suspend function` | Llamaste una función `suspend` desde código normal. | Llamarla desde una corrutina o marcar el método como `suspend`. |
| Una tarea que no se cancela | Se capturó `CancellationException` con `catch (e: Exception)`. | Dejar pasar `CancellationException` o capturar solo lo necesario. |
| UI congelada | Trabajo pesado en `Dispatchers.Main`. | Usar `withContext(Dispatchers.IO)`. |

---

## 9. Librerías declaradas y aún no usadas

Estas librerías están en `libs.versions.toml` o en `build.gradle.kts`, pero el código aún no las usa. Se explican aquí para que sepas para qué sirven cuando lleguemos a esas capas.

| Librería | Para qué sirve | Cuándo la usaremos |
|---|---|---|
| `androidx-datastore-preferences` | Almacenamiento clave-valor, reemplazo moderno de SharedPreferences. | Guardar el usuario logueado o preferencias. |
| `coil-compose` / `coil-network-okhttp` | Carga de imágenes desde URL, con caché y OkHttp. | Mostrar `imageUrl` de los productos. |
| `androidx-navigation-compose` | Navegación entre pantallas de Compose. | Ir de Home a Detalle. |
| `androidx-hilt-navigation-compose` | Obtener ViewModels de Hilt en Compose (`hiltViewModel()`). | Capa de presentación. |
| `androidx-lifecycle-viewmodel-compose` | ViewModel integrado con Compose. | Capa de presentación. |
| `androidx-lifecycle-runtime-compose` | Observar estado del ciclo de vida desde Compose. | Capa de presentación. |
| `androidx-compose-*` | UI declarativa. | Pantallas y componentes. |
| `kotlinx-coroutines-test` | Controlar corrutinas en pruebas. | Tests de ViewModels y repositorios. |
| `turbine` | Probar `Flow` de forma sencilla. | Tests cuando haya `Flow`. |
| `mockito-core` / `mockito-kotlin` | Crear objetos falsos en pruebas unitarias. | Tests de casos de uso y repositorios. |
| `androidx-espresso-core` / `androidx-compose-ui-test-junit4` | Pruebas de UI. | Tests de pantallas. |

---

## 10. Cómo se conectan todas las piezas

Recorrido completo, desde la app hasta la red y la base de datos:

```
1.  @HiltAndroidApp            Hilt arranca y crea SingletonComponent
2.  DatabaseModule (@Provides) Hilt sabe construir AppDatabase y los DAOs
3.  NetworkModule  (@Provides) Hilt sabe construir Json, OkHttp, Retrofit y servicios
4.  Room compiler (KSP)        genera UserDao_Impl y AppDatabase_Impl
5.  Retrofit.create() (proxy)  genera la implementación de UserApiService
6.  UserRemoteDataSource       @Inject constructor: recibe UserApiService
7.  UserLocalDataSource        @Inject constructor: recibe UserDao
8.  UserRepositoryImpl         @Inject constructor: recibe los dos data sources
9.  RepositoryModule (@Binds)  Hilt entrega UserRepositoryImpl cuando piden UserRepository
10. GetUsersUseCase            @Inject constructor: recibe UserRepository
11. ViewModel (pendiente)      @HiltViewModel: recibe el caso de uso
```

Ejecución de una llamada, `getUsers()`:

```
ViewModel → GetUsersUseCase → UserRepository → UserRepositoryImpl
   ├── remote.getUsers() → Retrofit (proxy) → OkHttp → servidor → JSON → kotlinx → UserDto
   │       └── onEach { local.insert(...) }  → Room → SQLite
   └── si IOException → local.getAll() → Room → SQLite
```

---

## 11. Glosario

| Término | Significado |
|---|---|
| Inyección de dependencias | Recibir las dependencias desde afuera. |
| Grafo de dependencias | Mapa de qué clase necesita qué otra. |
| Scope | Cuánto dura una instancia y cuántas veces se crea. |
| Componente | Contenedor que guarda las dependencias de un ciclo de vida. |
| Proveedor | Regla (`@Provides` o `@Binds`) que dice cómo obtener un tipo. |
| Factory | Clase generada que sabe construir otra clase. |
| Proxy dinámico | Objeto que implementa una interfaz en tiempo de ejecución. Así funciona `retrofit.create`. |
| Converter | Pieza que traduce entre JSON y objetos. |
| Interceptor | Código que se ejecuta en cada petición HTTP. |
| Pool de conexiones | Conexiones abiertas que OkHttp reutiliza. |
| Entidad | Clase que representa una tabla de Room. |
| DAO | Interfaz con las consultas sobre una tabla. |
| Migración | Cambio del esquema de la base entre versiones. |
| Serializador | Código que convierte una clase a JSON y viceversa. |
| Suspend | Función que puede pausarse sin bloquear el hilo. |
| Dispatcher | Decide en qué hilo corre una corrutina. |
| KSP | Procesador de anotaciones que genera código en compilación. |
