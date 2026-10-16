# Fase 2: Capa de datos, inyección de dependencias y red

**Fecha:** 2026-10-16
**Proyecto:** UserAndOrdersApp (`com.example.userandordersapp`)
**Fase anterior:** [fase-1-estructura-y-gitignore.md](fase-1-estructura-y-gitignore.md)
**Guía detallada de frameworks:** [dagger-hilt-retrofit.md](../guias/dagger-hilt-retrofit.md)
**Estado:** Completada a nivel de código. Compila (`:app:compileDebugKotlin`), pero aún no se ha ejecutado contra una API real ni contra un dispositivo.

---

## 1. Resumen

En esta fase se pasó de una estructura vacía a una capa de datos completa, conectada por inyección de dependencias:

- **Dominio:** modelos (`User`, `Product`, `Order`, `OrderStatus`), interfaces de repositorio y casos de uso.
- **Datos remotos:** servicios de Retrofit y data sources que los envuelven.
- **Datos locales:** entidades, DAOs de Room y data sources que los envuelven.
- **Repositorios:** implementaciones que combinan remoto y local.
- **Inyección de dependencias:** módulos de Hilt que construyen y conectan todo lo anterior.

El flujo completo, de la pantalla hacia la red o la base de datos, es:

```
Pantalla (ViewModel, pendiente)
   │
   ▼
Caso de uso             domain/usecases/...        (lógica de negocio, validaciones)
   │
   ▼
Interfaz de repositorio  domain/repositories/...   (contrato, no sabe de Room ni de Retrofit)
   │
   ▼
RepositoryImpl           data/repositories/...     (decide: red primero, caché si falla)
   │                 ┌────────────────┐
   ▼                 ▼                ▼
Remote data source   Local data source
(Retrofit)           (Room / DAO)
   │                 │
   ▼                 ▼
API HTTP             Base de datos SQLite
```

Cada capa solo conoce a la de abajo. Por eso la pantalla no sabe si los datos vienen de la red o de la caché, y los casos de uso no saben si hay Room o Retrofit.

---

## 2. Dependencias

Definidas en `gradle/libs.versions.toml` y usadas en `app/build.gradle.kts`:

| Librería | Versión | Para qué sirve |
|---|---|---|
| `retrofit` | 3.0.0 | Convierte interfaces Kotlin en llamadas HTTP. |
| `okhttp` | 5.5.0 | Cliente HTTP por debajo de Retrofit. Aquí se agrega el logging. |
| `okhttp-logging-interceptor` | 5.5.0 | Imprime las peticiones y respuestas en el Logcat. |
| `retrofit-converter-kotlinx-serialization` | 3.0.0 | Convierte JSON a DTOs de Kotlin y viceversa. |
| `kotlinx-serialization-json` | 1.11.0 | Motor de serialización JSON, usado por el converter y por los `TypeConverter` de Room. |
| `room-runtime` / `room-ktx` | 2.8.5 | Base de datos SQLite con DAOs tipados. `room-ktx` agrega soporte para corrutinas. |
| `room-compiler` (vía `ksp`) | 2.8.5 | Genera el código de los DAOs y de la base de datos en tiempo de compilación. |
| `hilt-android` | 2.60.1 | Inyección de dependencias para Android, sobre Dagger. |
| `hilt-compiler` (vía `ksp`) | 2.60.1 | Genera el código de Hilt en tiempo de compilación. |
| `kotlinx-coroutines-core` / `android` | 1.11.0 | Corrutinas. Las funciones `suspend` de todo el proyecto las usan. |

Plugins aplicados en `app/build.gradle.kts`:

| Plugin | Qué hace |
|---|---|
| `com.google.dagger.hilt.android` | Activa el procesamiento de anotaciones de Hilt. |
| `com.google.devtools.ksp` | Kotlin Symbol Processing: permite que Room y Hilt generen código a partir de tus anotaciones. |
| `org.jetbrains.kotlin.plugin.serialization` | Genera los serializadores para las clases `@Serializable`. |

**Permiso agregado:** `<uses-permission android:name="android.permission.INTERNET" />` en `AndroidManifest.xml`. Sin él, cualquier petición de red falla.

---

## 3. Modelos de dominio

Carpeta: `domain/models/`. Son clases de Kotlin puras, sin anotaciones de framework. Así el dominio no depende de Room, Retrofit ni Android.

- **`User`**, **`Product`** y **`Order`** son `data class`. Ver la sección de glosario para qué significa cada etiqueta.
- **`OrderStatus`** es un `enum class` con `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED` y `CANCELLED`. Este estado lo definimos nosotros, porque tu lista no traía los valores.

---

## 4. Interfaces de repositorio

Carpeta: `domain/repositories/`. Son contratos (`interface`), no implementaciones.

- `UserRepository`: `getUsers`, `getUserById`, `createUser`, `updateUser`, `deleteUser`.
- `ProductRepository`: `getProducts`, `getProductById`, `searchProducts`.
- `OrderRepository`: `getOrders`, `createOrder`, `getOrderById`.

Todas las funciones son `suspend`. Esto permite llamarlas desde corrutinas sin bloquear el hilo principal.

---

## 5. Casos de uso

Carpeta: `domain/usecases/` (subcarpetas `user/`, `product/`, `order/`).

Un caso de uso representa una acción de la app. Cada uno tiene esta forma:

```kotlin
class GetUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<User> = userRepository.getUsers()
}
```

Puntos clave:

- **`@Inject constructor`:** Hilt sabe construir esta clase solo, sin módulo. Recibe `UserRepository` y Hilt le entrega la implementación que enlazamos en `RepositoryModule`.
- **`operator fun invoke`:** permite llamar al caso de uso como si fuera una función: `getUsers()` en vez de `getUsers.execute()`.
- **Validaciones con `require`:** `CreateUserUseCase` valida que `name` y `email` no estén en blanco. `CreateOrderUseCase` valida que haya productos y que `totalAmount > 0`. Si no se cumple, lanza `IllegalArgumentException`.
- **`SearchProductsUseCase` con caché en memoria:** guarda los resultados en un `ConcurrentHashMap` usando el término de búsqueda (sin espacios y en minúsculas) como clave. Está marcado `@Singleton` para que la caché viva mientras viva la app. Sin esa anotación, cada inyección crearía una caché nueva y no serviría de nada.

Decisión tomada: no existe un wrapper `Result`. Los errores se lanzan como excepciones. Si en el futuro quieres un tipo `Result<T>` para manejarlos en la UI, se puede agregar sin cambiar las capas de datos.

---

## 6. Capa remota (Retrofit)

Carpetas: `data/datasources/remote/` y `data/datasources/remote/api/`. Archivos principales:

- `api/UserApiService.kt`, `api/ProductApiService.kt`, `api/OrderApiService.kt`: interfaces con los endpoints.
- `UserRemoteDataSource.kt`, `ProductRemoteDataSource.kt`, `OrderRemoteDataSource.kt`: envuelven a los servicios.
- `data/models/UserDto.kt`, `ProductDto.kt`, `OrderDto.kt`: formas de los JSON de la API.

### 6.1 Cómo funciona Retrofit

Declaras una interfaz con anotaciones que describen cada petición, y Retrofit genera la implementación:

```kotlin
interface UserApiService {
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): UserDto
}
```

Retrofit toma `"users/{id}"`, reemplaza `{id}` por el valor del parámetro marcado con `@Path("id")`, hace un `GET` contra `BASE_URL + "users/..."`, convierte el JSON de la respuesta en un `UserDto` y lo devuelve.

### 6.2 Endpoints y su significado

| Servicio | Función | Anotación | Ruta | Qué hace |
|---|---|---|---|---|
| `UserApiService` | `getUsers` | `@GET` | `users` | Lista de usuarios. |
| | `getUserById` | `@GET` | `users/{id}` | Un usuario. |
| | `createUser` | `@POST` | `users` | Crea un usuario. El cuerpo va en `@Body`. |
| | `updateUser` | `@PUT` | `users/{id}` | Reemplaza un usuario. |
| | `deleteUser` | `@DELETE` | `users/{id}` | Elimina. Devuelve `Response<Unit>` para revisar el código de estado. |
| `ProductApiService` | `getProducts` | `@GET` | `products` | Lista de productos. |
| | `getProductById` | `@GET` | `products/{id}` | Un producto. |
| | `searchProducts` | `@GET` | `products/search?q=...` | Búsqueda. `@Query("q")` agrega `?q=valor` a la URL. |
| `OrderApiService` | `getOrders` | `@GET` | `users/{userId}/orders` | Órdenes de un usuario. |
| | `createOrder` | `@POST` | `orders` | Crea una orden. |
| | `getOrderById` | `@GET` | `orders/{id}` | Una orden. |

### 6.3 Por qué `deleteUser` devuelve `Response<Unit>`

Si la función devolviera `Unit` directamente, Retrofit lanzaría excepción ante un error HTTP, pero no nos diría si el borrado fue exitoso. Con `Response<Unit>` obtenemos el código de estado, y el data source devuelve `isSuccessful` como `Boolean`.

### 6.4 Data sources remotos

Son wrappers finos. Su única responsabilidad es llamar al servicio y devolver el DTO:

```kotlin
class UserRemoteDataSource @Inject constructor(
    private val userApiService: UserApiService
) {
    suspend fun getUsers(): List<UserDto> = userApiService.getUsers()
    suspend fun deleteUser(id: String): Boolean = userApiService.deleteUser(id).isSuccessful
}
```

Separarlos del servicio permite cambiar el origen de los datos sin tocar los repositorios.

---

## 7. Capa local (Room)

Carpetas: `data/database/`, `data/database/entities/`, `data/datasources/local/` y `data/datasources/local/dao/`.

Room es la capa de SQLite de Jetpack. Escribes clases y interfaces con anotaciones, y Room genera el SQL y el código que lo ejecuta.

### 7.1 Entidades

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

- **`@Entity(tableName = "users")`:** esta clase es una tabla. El nombre de la tabla en SQLite es `users`.
- **`@PrimaryKey`:** marca `id` como clave primaria. Dos filas no pueden tener el mismo `id`.
- Las propiedades sin anotación se convierten en columnas con el mismo nombre.
- `avatar: String?` se guarda como columna que admite `NULL`.

Las tres entidades son `UserEntity` (tabla `users`), `ProductEntity` (tabla `products`) y `OrderEntity` (tabla `orders`).

**`@Serializable` en `ProductEntity`:** permite guardar la lista de productos de una orden como JSON. Ver la sección de `TypeConverter`.

### 7.2 Lista de productos dentro de una orden

SQLite no tiene columna para una lista. `OrderEntity` tiene `products: List<ProductEntity>`, así que necesitamos convertir esa lista a texto:

```kotlin
@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val products: List<ProductEntity>,
    ...
)
```

- **`@TypeConverters(Converters::class)`:** le dice a Room que, para los tipos que no sabe guardar, use la clase `Converters`.
- **`Converters.kt`:**

```kotlin
class Converters {
    @TypeConverter
    fun fromProducts(products: List<ProductEntity>): String = Json.encodeToString(products)

    @TypeConverter
    fun toProducts(value: String): List<ProductEntity> = Json.decodeFromString(value)
}
```

- **`@TypeConverter`:** marca una función que convierte de un tipo a otro. Room necesita dos: una para guardar (objeto → texto) y otra para leer (texto → objeto).
- `Json.encodeToString` y `Json.decodeFromString` de kotlinx.serialization hacen la conversión. Requieren que `ProductEntity` sea `@Serializable`.

El enum `OrderStatus` no necesita converter: Room guarda los enums como texto automáticamente.

### 7.3 DAOs

Un DAO (Data Access Object) es una interfaz con las operaciones sobre una tabla:

```kotlin
@Dao
interface ProductDao {

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
}
```

- **`@Dao`:** marca la interfaz como DAO. Room genera la implementación.
- **`@Query("...")`:** ejecuta SQL que escribes tú. `:query` es un parámetro enlazado: el valor de la función se pasa de forma segura y no se concatena como texto.
- **`LIKE '%' || :query || '%'`:** búsqueda por subcadena. `%` es un comodín.
- **`@Insert`:** inserta filas. Recibe una entidad o una lista.
- **`onConflict = OnConflictStrategy.REPLACE`:** si ya existe una fila con la misma clave primaria, la reemplaza. Esto hace que volver a guardar un producto actualice su versión en caché.
- **`@Update`:** actualiza una fila existente. Devuelve el número de filas afectadas (`Int`).
- **`@Query("DELETE ...")`:** en `UserDao` se usa para borrar. Devuelve el número de filas eliminadas, y así sabemos si el borrado tuvo efecto.
- **`suspend`:** la consulta corre en una corrutina, fuera del hilo principal. Room no permite consultas en el hilo principal por defecto, así que `suspend` es lo que hace que estas funciones se puedan llamar desde un ViewModel.

Los DAOs `UserDao`, `ProductDao` y `OrderDao` están en `data/datasources/local/dao/`.

### 7.4 Base de datos

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

- **`@Database(entities = [...])`:** lista las tablas de la base de datos.
- **`version = 1`:** número de versión del esquema. Si cambias las entidades en el futuro, debes subirlo y definir una migración; de lo contrario Room lanza error al abrir la base.
- **`exportSchema = false`:** Room puede guardar un JSON con el esquema en cada versión. Lo desactivamos para no tener que configurar una carpeta para esos archivos. Si el proyecto va a tener migraciones complejas, conviene activarlo.
- **`RoomDatabase`:** clase base que Room extiende. Es `abstract` porque Room genera la implementación concreta.
- Las funciones `abstract fun userDao(): UserDao` le dan acceso a los DAOs desde la base.

La base se crea en `DatabaseModule` con `Room.databaseBuilder(context, AppDatabase::class.java, "user_and_orders.db").build()`. El archivo `user_and_orders.db` queda en el almacenamiento interno de la app.

### 7.5 Data sources locales

```kotlin
class ProductLocalDataSource @Inject constructor(
    private val productDao: ProductDao
) {
    suspend fun getAll(): List<ProductEntity> = productDao.getAll()
    suspend fun insertAll(products: List<ProductEntity>) = productDao.insertAll(products)
}
```

Igual que los remotos, son wrappers. Solo llaman al DAO.

---

## 8. Mappers

Carpeta: `data/mappers/`. Convierten entre las tres representaciones de cada objeto:

| Origen | Destino | Dónde se usa |
|---|---|---|
| `DTO` (red) | `Entity` (Room) | Guardar en caché lo que llegó de la API. |
| `DTO` (red) | `Domain` | Devolver datos al dominio. |
| `Entity` (Room) | `Domain` | Devolver datos de la caché al dominio. |
| `Domain` | `DTO` | Enviar datos a la API al crear o actualizar. |

Son funciones de extensión:

```kotlin
fun UserDto.toDomain(): User = User(id = id, name = name, email = email, avatar = avatar, createdAt = createdAt)
```

- **Funciones de extensión (`fun Tipo.nombre()`):** agregan una función a una clase existente sin tocarla. `UserDto.toDomain()` se lee como "convierte este DTO a dominio".
- Usamos argumentos con nombre (`id = id`) para que, si alguien cambia el orden de los campos, el compilador lo detecte.

Por qué tres modelos separados: si la API cambia un nombre de campo, solo cambia el DTO. Si Room cambia una columna, solo cambia la entidad. El dominio no se entera de ninguno de los dos.

---

## 9. Repositorios

Carpetas: `data/repositories/` (implementaciones) y `domain/repositories/` (contratos).

```kotlin
class UserRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource
) : UserRepository {

    override suspend fun getUsers(): List<User> =
        try {
            remote.getUsers().onEach { local.insert(it.toEntity()) }.map { it.toDomain() }
        } catch (e: IOException) {
            local.getAll().takeIf { it.isNotEmpty() }?.map { it.toDomain() } ?: throw e
        }
    ...
}
```

Estrategia que implementamos:

- **Lecturas:** primero la API. Lo que llega se guarda en Room. Si la red falla (`IOException`), se devuelve la caché.
- **Caché vacía:** si no hay nada en caché, el error se propaga. Así la UI no muestra una lista vacía cuando en realidad no hubo conexión.
- **Escrituras** (`createUser`, `updateUser`, `deleteUser`, `createOrder`): no usan caché. Si fallan, el error sale a la capa superior.
- **Errores HTTP** (404, 500, etc.) no son `IOException`, así que no activan la caché: se propagan.

Lo que significan las piezas del código:

- **`try / catch (e: IOException)`:** captura solo errores de red o de conexión. Otros errores (bugs, por ejemplo) no se tragan.
- **`onEach { }`:** recorre la lista para guardar cada elemento en Room, y devuelve la misma lista para seguir mapeando.
- **`also { }`:** ejecuta un bloque con el objeto y devuelve el mismo objeto. Lo usamos para guardar en caché y seguir devolviendo el resultado.
- **`takeIf { it.isNotEmpty() }`:** devuelve el objeto si cumple la condición, o `null` si no.
- **`?: throw e`:** operador Elvis. Si lo de la izquierda es `null`, lanza la excepción original de red.

---

## 10. Inyección de dependencias: Dagger y Hilt

### 10.1 Qué problema resuelve

Sin inyección, cada clase crea a sus dependencias:

```kotlin
class GetUsersUseCase {
    private val repo = UserRepositoryImpl(UserRemoteDataSource(...), UserLocalDataSource(...))
}
```

Eso acopla las clases entre sí y hace imposible cambiar una implementación (por ejemplo, para pruebas). Con inyección, cada clase declara lo que necesita y alguien externo se lo entrega.

**Dagger** es el framework de inyección de dependencias de Google, que resuelve el grafo en tiempo de compilación. **Hilt** es una capa encima de Dagger que simplifica su configuración para Android: ya conoce el ciclo de vida de la app, las activities y los ViewModels.

### 10.2 Anotaciones usadas

- **`@HiltAndroidApp`** (en `UserAndOrdersApplication`, ya existía): activa Hilt en toda la app. Genera el contenedor de dependencias.
- **`@Inject constructor(...)`:** le dice a Dagger cómo construir la clase. Dagger busca cada parámetro en el grafo. Se usa en casos de uso, repositorios implementados y data sources.
- **`@Module`:** marca una clase o un objeto que le enseña a Dagger a construir cosas que no se pueden construir solo con `@Inject`.
- **`@InstallIn(SingletonComponent::class)`:** indica en qué contenedor viven las dependencias del módulo. `SingletonComponent` significa "mientras viva la app". Hay otros, como `ViewModelComponent` o `ActivityComponent`.
- **`@Provides`:** dentro de un `object`, marca una función que construye un objeto. Dagger llama a esa función cuando necesita ese tipo.
- **`@Binds`:** dentro de una `abstract class`, le dice a Dagger que cuando alguien pida una interfaz, entregue una implementación. Es más eficiente que `@Provides` porque no requiere una función que construya el objeto.
- **`@Singleton`** (de `javax.inject`): dentro de `SingletonComponent`, Dagger crea una sola instancia y la comparte. Sin esta anotación, se crea una nueva cada vez.
- **`@ApplicationContext`** (de `hilt-android`): calificador que indica que se quiere el `Context` de la aplicación, no el de una activity. Necesario para Room y para el logging de OkHttp.

### 10.3 Los módulos

**`RepositoryModule`** (`abstract class`, usa `@Binds`):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    ...
}
```

Cuando un caso de uso pide `UserRepository`, Dagger entrega una `UserRepositoryImpl`. Los tres repositorios se enlazan de la misma forma.

**`DatabaseModule`** (`object`, usa `@Provides`):

- `provideAppDatabase` construye la base de Room. Es `@Singleton` porque abrir una base cuesta.
- `provideUserDao`, `provideProductDao` y `provideOrderDao` extraen los DAOs de la base. No son `@Singleton` porque solo devuelven una referencia a la base, que ya es singleton.

**`NetworkModule`** (`object`, usa `@Provides`):

- `provideJson`: crea el `Json` con `ignoreUnknownKeys = true`, que ignora campos del JSON que no están en el DTO. Así un campo nuevo en la API no rompe la app.
- `provideOkHttpClient`: crea el cliente HTTP con el interceptor de logging. Usa `HttpLoggingInterceptor.Level.BODY` solo si la app es depurable (`FLAG_DEBUGGABLE`), y `NONE` en release, para no imprimir datos sensibles.
- `provideRetrofit`: crea Retrofit con `BASE_URL`, el cliente y el converter de kotlinx.serialization.
- `provideUserApiService`, `provideProductApiService`, `provideOrderApiService`: `retrofit.create(...)` genera la implementación de cada interfaz de servicio.

**`DataSourceModule`** no existe. Los data sources son clases concretas con `@Inject constructor`, así que Dagger los construye solos. Un módulo de este tipo solo sería necesario si creáramos interfaces para ellos. Se eliminó en la limpieza de archivos (sección 14).

### 10.4 Qué pasa al compilar

Cuando compilas, KSP ejecuta el procesador de Hilt. Ese procesador revisa el grafo: si un caso de uso necesita un `UserRepository` y no hay ningún `@Binds` ni `@Provides` que lo provea, la compilación falla con un error que indica la cadena de dependencias. Por eso `compileDebugKotlin` también valida la inyección.

---

## 11. Glosario de etiquetas

### Room

| Etiqueta | Dónde | Significado |
|---|---|---|
| `@Entity(tableName = "...")` | Entidades | Marca una clase como tabla. |
| `@PrimaryKey` | Entidades | Columna que identifica cada fila de forma única. |
| `@TypeConverters(...)` | `OrderEntity` | Indica qué clase convierte tipos que Room no maneja. |
| `@TypeConverter` | `Converters` | Función de conversión de un tipo a otro. |
| `@Database(...)` | `AppDatabase` | Declara la base: sus tablas y su versión. |
| `@Dao` | DAOs | Marca una interfaz de acceso a datos. |
| `@Query("...")` | DAOs | Ejecuta SQL escrito por ti. |
| `@Insert` | DAOs | Inserta filas. |
| `@Update` | DAOs | Actualiza filas. |
| `OnConflictStrategy.REPLACE` | `@Insert` | Reemplaza la fila si la clave ya existe. |

### Retrofit

| Etiqueta | Dónde | Significado |
|---|---|---|
| `@GET`, `@POST`, `@PUT`, `@DELETE` | Servicios | Método HTTP de la petición. |
| `@Path("id")` | Parámetros | Reemplaza `{id}` en la ruta. |
| `@Query("q")` | Parámetros | Agrega `?q=valor` a la URL. |
| `@Body` | Parámetros | Envía el objeto como cuerpo JSON. |
| `Response<T>` | Tipo de retorno | Permite leer el código de estado además del cuerpo. |

### Serialización

| Etiqueta | Dónde | Significado |
|---|---|---|
| `@Serializable` | DTOs, `ProductEntity` | Permite convertir la clase a JSON y viceversa. |
| `Json { ignoreUnknownKeys = true }` | `NetworkModule` | Ignora campos del JSON que no están en la clase. |

### Hilt / Dagger

| Etiqueta | Dónde | Significado |
|---|---|---|
| `@HiltAndroidApp` | `UserAndOrdersApplication` | Activa Hilt en la app. |
| `@Inject constructor` | Casos de uso, repositorios, data sources | Dagger puede construir la clase solo. |
| `@Module` | `RepositoryModule`, `DatabaseModule`, `NetworkModule` | Clase que enseña a construir dependencias. |
| `@InstallIn(SingletonComponent::class)` | Módulos | Las dependencias viven mientras viva la app. |
| `@Provides` | Funciones en `object` | Función que construye un objeto. |
| `@Binds` | Funciones en `abstract class` | Enlaza una interfaz con su implementación. |
| `@Singleton` | Clases y funciones | Una sola instancia compartida. |
| `@ApplicationContext` | Parámetros | Pide el `Context` de la aplicación. |

### Kotlin (usadas en el proyecto)

| Construcción | Significado |
|---|---|
| `suspend` | La función se puede pausar sin bloquear el hilo. |
| `operator fun invoke` | Permite llamar al objeto como si fuera una función. |
| `data class` | Clase con `equals`, `copy` y `toString` generados. |
| `enum class` | Conjunto cerrado de valores. |
| `object` | Instancia única. En los módulos de Hilt, `object` se usa con `@Provides`. |
| `abstract class` | No se instancia directamente. En `RepositoryModule` se usa con `@Binds`. |
| `require(...)` | Lanza `IllegalArgumentException` si la condición es falsa. |
| `?:` (Elvis) | Usa el valor de la derecha si el de la izquierda es `null`. |
| `takeIf { }` | Devuelve el objeto si cumple la condición, o `null`. |
| `onEach { }` / `also { }` | Ejecuta un bloque y devuelve el mismo objeto. |
| Función de extensión (`fun Tipo.f()`) | Agrega una función a una clase sin modificarla. |

---

## 12. Decisiones tomadas

| Decisión | Motivo |
|---|---|
| Lecturas con red primero y caché si falla | La app muestra datos frescos cuando hay conexión y algo útil cuando no. |
| Caché vacía propaga el error | Evita mostrar listas vacías que parecen datos reales. |
| Escrituras sin caché | Una escritura que no llegó a la API no debe fingir que sí. |
| Tres modelos (DTO, Entity, Domain) | Cada capa cambia por su propio motivo, sin arrastrar a las demás. |
| Errores como excepciones, sin `Result` | Menos código mientras no haya UI que los consuma. |
| `exportSchema = false` | Evita configurar una carpeta de esquemas que aún no necesitamos. |
| `ignoreUnknownKeys = true` | La API puede agregar campos sin romper la app. |
| Logging solo en depuración | Evita imprimir datos del cuerpo HTTP en builds de release. |
| Sin `DataSourceModule` | Los data sources no necesitan enlace: Dagger los construye solos. |

---

## 13. Pendientes y limitaciones

- **URL de la API:** `Constants.BASE_URL` apunta a `https://api.example.com/`, un placeholder. Hay que reemplazarlo por la URL real. Hasta entonces, las llamadas de red no devuelven datos.
- **Sin pruebas ejecutadas:** el código compila, pero no se ha ejecutado contra una API ni en un dispositivo. Los tests de `src/test` y `src/androidTest` siguen vacíos.
- **Caché de búsqueda sin límite:** `SearchProductsUseCase` guarda todas las búsquedas en memoria y nunca las borra. Con uso prolongado puede crecer. Una mejora sería limitar su tamaño.
- **Migraciones de Room:** `version = 1`. Si cambian las entidades, hay que subir la versión y definir una migración.
- **Capa de presentación:** las pantallas, ViewModels y componentes de UI siguen vacíos. Es la siguiente fase.

---

## 14. Archivos eliminados en esta fase

Por no ser necesarios con la implementación actual:

- `di/AppModule.kt`
- `di/UseCaseModule.kt`
- `di/DataSourceModule.kt`
- `data/datasources/remote/api/ApiClient.kt`
- `util/Result.kt`
- `util/Extensions.kt`

Los archivos `presentation/`, los tests vacíos y los de la plantilla (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) se conservan.

---

## 15. Próximo paso

Capa de presentación: ViewModels que usen los casos de uso y pantallas en Compose. Antes de eso, conviene reemplazar la URL de la API y ejecutar la app una vez para verificar la red y la base de datos.
