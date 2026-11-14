package fr.florianfish.mywhiskybeer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.florianfish.mywhiskybeer.ui.bottle.BottleEditScreen
import fr.florianfish.mywhiskybeer.ui.cellar.CellarScreen
import fr.florianfish.mywhiskybeer.ui.home.HomeScreen
import fr.florianfish.mywhiskybeer.ui.navigation.BottleEditRoute
import fr.florianfish.mywhiskybeer.ui.navigation.CellarRoute
import fr.florianfish.mywhiskybeer.ui.navigation.HomeRoute
import fr.florianfish.mywhiskybeer.ui.navigation.ProductDetailRoute
import fr.florianfish.mywhiskybeer.ui.navigation.ProductsRoute
import fr.florianfish.mywhiskybeer.ui.navigation.SettingsRoute
import fr.florianfish.mywhiskybeer.ui.navigation.TastingEditRoute
import fr.florianfish.mywhiskybeer.ui.product.ProductDetailScreen
import fr.florianfish.mywhiskybeer.ui.products.ProductsScreen
import fr.florianfish.mywhiskybeer.ui.settings.SettingsScreen
import fr.florianfish.mywhiskybeer.ui.tasting.TastingEditScreen
import fr.florianfish.mywhiskybeer.ui.navigation.StatsRoute
import fr.florianfish.mywhiskybeer.ui.stats.StatsScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(HomeRoute, HomeRoute::class, "Dégustations", Icons.Outlined.Home),
    TopLevel(ProductsRoute, ProductsRoute::class, "Carnet", Icons.AutoMirrored.Outlined.MenuBook),
    TopLevel(CellarRoute, CellarRoute::class, "Cave", Icons.Outlined.Inventory2),
    TopLevel(StatsRoute, StatsRoute::class, "Stats", Icons.Outlined.Insights),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWhiskyBeerAppUi() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val current = topLevels.firstOrNull { tl -> destination?.hasRoute(tl.routeClass) == true }

    Scaffold(
        topBar = {
            if (current != null) {
                TopAppBar(
                    title = { Text(current.label, style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        IconButton(onClick = { navController.navigate(SettingsRoute) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Réglages")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        },
        bottomBar = {
            if (current != null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    topLevels.forEach { tl ->
                        NavigationBarItem(
                            selected = current == tl,
                            onClick = {
                                navController.navigate(tl.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tl.icon, contentDescription = null) },
                            label = { Text(tl.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // La cave a son propre bouton (ajout de bouteilles)
            if (current != null && current.route != CellarRoute) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(TastingEditRoute()) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Déguster") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // Les écrans de premier niveau utilisent les marges du Scaffold ;
        // la saisie et la fiche produit gèrent leur propre Scaffold, en plein écran.
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onTastingClick = { navController.navigate(ProductDetailRoute(it)) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable<ProductsRoute> {
                ProductsScreen(
                    onProductClick = { navController.navigate(ProductDetailRoute(it)) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable<CellarRoute> {
                CellarScreen(
                    onProductClick = { navController.navigate(ProductDetailRoute(it)) },
                    onAddBottle = { wishlist -> navController.navigate(BottleEditRoute(wishlist = wishlist)) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable<StatsRoute> {
                StatsScreen(
                    onProductClick = { navController.navigate(ProductDetailRoute(it)) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable<TastingEditRoute> {
                TastingEditScreen(
                    onClose = { navController.popBackStack() },
                    onSaved = { productId ->
                        navController.navigate(ProductDetailRoute(productId)) {
                            popUpTo<TastingEditRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<ProductDetailRoute> {
                ProductDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTasteAgain = { navController.navigate(TastingEditRoute(it)) },
                    onEditStock = { navController.navigate(BottleEditRoute(productId = it)) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<BottleEditRoute> {
                BottleEditScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}
