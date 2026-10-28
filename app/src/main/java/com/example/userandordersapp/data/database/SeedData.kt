package com.example.userandordersapp.data.database

import com.example.userandordersapp.data.database.entities.OrderEntity
import com.example.userandordersapp.data.database.entities.ProductEntity
import com.example.userandordersapp.data.database.entities.UserEntity
import com.example.userandordersapp.domain.models.OrderStatus

/**
 * Datos de prueba para la base local. Se insertan una sola vez, cuando Room crea la base.
 *
 * Cada usuario tiene al menos una orden: el repositorio de órdenes lanza error si la caché
 * está vacía para ese usuario, así que un usuario sin órdenes mostraría un error en vez de "vacío".
 */
object SeedData {

    private val users = listOf(
        UserEntity("u1", "Ana Gómez", "ana@example.com", null, 1700000000000),
        UserEntity("u2", "Carlos Ruiz", "carlos@example.com", null, 1700100000000),
        UserEntity("u3", "Lucía Martínez", "lucia@example.com", null, 1700200000000)
    )

    private val products = listOf(
        ProductEntity("p1", "Teclado mecánico", "Teclado con switches táctiles", 89.9, "https://example.com/img/p1.jpg", 12),
        ProductEntity("p2", "Mouse inalámbrico", "Mouse ergonómico de 2.4 GHz", 25.5, "https://example.com/img/p2.jpg", 40),
        ProductEntity("p3", "Monitor 24 pulgadas", "Monitor Full HD con panel IPS", 199.99, "https://example.com/img/p3.jpg", 5),
        ProductEntity("p4", "Audífonos Bluetooth", "Audífonos con cancelación de ruido", 59.9, "https://example.com/img/p4.jpg", 8)
    )

    private val orders = listOf(
        OrderEntity("o1", "u1", listOf(products[0], products[1]), 115.4, OrderStatus.DELIVERED, 1710000000000),
        OrderEntity("o2", "u1", listOf(products[2]), 199.99, OrderStatus.SHIPPED, 1711000000000),
        OrderEntity("o3", "u2", listOf(products[3]), 59.9, OrderStatus.CONFIRMED, 1712000000000),
        OrderEntity("o4", "u3", listOf(products[1], products[3]), 85.4, OrderStatus.PENDING, 1713000000000)
    )

    /** Sentencias SQL que Room ejecuta en `onCreate`. */
    val statements: List<String> by lazy {
        val converters = Converters()
        buildList {
            users.forEach {
                add(
                    "INSERT INTO users (id, name, email, avatar, createdAt) VALUES " +
                        "('${it.id}', '${it.name}', '${it.email}', NULL, ${it.createdAt})"
                )
            }
            products.forEach {
                add(
                    "INSERT INTO products (id, title, description, price, imageUrl, stock) VALUES " +
                        "('${it.id}', '${it.title}', '${it.description}', ${it.price}, '${it.imageUrl}', ${it.stock})"
                )
            }
            orders.forEach {
                add(
                    "INSERT INTO orders (id, userId, products, totalAmount, status, createdAt) VALUES " +
                        "('${it.id}', '${it.userId}', '${converters.fromProducts(it.products)}', " +
                        "${it.totalAmount}, '${it.status.name}', ${it.createdAt})"
                )
            }
        }
    }
}
