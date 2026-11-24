package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.SubscriptionPlanConfig
import com.example.data.repository.AdminRepository
import com.example.data.repository.KombaRepository
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOutUser
import com.example.ui.components.SuspendedAccountScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoyaltyScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PostStudioScreen
import com.example.ui.screens.PosterStudioScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.KombaBoostTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.KombaViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KombaBoostTheme {
                AppNavigation()
            }
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val PRIMARY_TABS = listOf(
    NavTab("dashboard", "Accueil", Icons.Default.Dashboard),
    NavTab("posters", "Affiches", Icons.Default.Brush),
    NavTab("posts", "Textes", Icons.AutoMirrored.Filled.Send),
    NavTab("catalog", "Catalogue", Icons.Default.Inventory2),
    NavTab("subscription", "Offres", Icons.Default.CreditCard)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: ""
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    defaultDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: KombaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY manquante" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                KombaViewModel(
                    repo = KombaRepository(db),
                    adminRepo = AdminRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val postersState by viewModel.postersState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val plansState by viewModel.plansState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.userPaymentsState.collectAsStateWithLifecycle()
    val adminMerchantsState by viewModel.adminMerchantsState.collectAsStateWithLifecycle()
    val adminPaymentsState by viewModel.adminPaymentsState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val onSignOut: () -> Unit = {
        signOutUser(credentialManager, scope)
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        is UiState.Success -> {
            val profile = pState.data
            if (profile == null) {
                OnboardingScreen(
                    defaultDisplayName = defaultDisplayName,
                    isBusy = isBusy,
                    onSubmit = viewModel::createProfile,
                    onSignOut = onSignOut
                )
            } else if (profile.accountStatus == "suspended" && !isAdmin) {
                SuspendedAccountScreen(
                    businessName = profile.businessName,
                    onSignOut = onSignOut
                )
            } else {
                var currentRoute by rememberSaveable { mutableStateOf("dashboard") }
                val products = (productsState as? UiState.Success)?.data ?: emptyList()
                val posters = (postersState as? UiState.Success)?.data ?: emptyList()
                val posts = (postsState as? UiState.Success)?.data ?: emptyList()
                val events = (calendarState as? UiState.Success)?.data ?: emptyList()
                val customers = (customersState as? UiState.Success)?.data ?: emptyList()
                val plans = (plansState as? UiState.Success)?.data ?: SubscriptionPlanConfig.defaults()
                val userPayments = (paymentsState as? UiState.Success)?.data ?: emptyList()
                val activePlan = plans.firstOrNull { it.id == profile.plan } ?: plans.first()

                if (currentRoute != "dashboard") {
                    BackHandler { currentRoute = "dashboard" }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
                        containerColor = ObsidianBg,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!useRail && currentRoute in PRIMARY_TABS.map { it.route }) {
                                NavigationBar(
                                    containerColor = ObsidianSurface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    PRIMARY_TABS.forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentRoute == tab.route,
                                            onClick = { currentRoute = tab.route },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, maxLines = 1) },
                                            modifier = Modifier.testTag("nav_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    containerColor = ObsidianSurface,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    PRIMARY_TABS.forEach { tab ->
                                        NavigationRailItem(
                                            selected = currentRoute == tab.route,
                                            onClick = { currentRoute = tab.route },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 720.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                when (currentRoute) {
                                    "dashboard" -> DashboardScreen(
                                        profile = profile,
                                        activePlan = activePlan,
                                        productsCount = products.size,
                                        postersCount = posters.size,
                                        postsCount = posts.size,
                                        customersCount = customers.size,
                                        isAdmin = isAdmin,
                                        onNavigate = { currentRoute = it },
                                        onSignOut = onSignOut
                                    )

                                    "posters" -> PosterStudioScreen(
                                        profile = profile,
                                        activePlan = activePlan,
                                        products = products,
                                        posters = posters,
                                        isBusy = isBusy,
                                        onSavePoster = viewModel::savePoster,
                                        onShareRecorded = viewModel::recordShare,
                                        onMessage = viewModel::showMessage
                                    )

                                    "posts" -> PostStudioScreen(
                                        profile = profile,
                                        activePlan = activePlan,
                                        products = products,
                                        posts = posts,
                                        isBusy = isBusy,
                                        onSavePost = viewModel::savePost,
                                        onShareRecorded = viewModel::recordShare,
                                        onMessage = viewModel::showMessage
                                    )

                                    "catalog" -> CatalogScreen(
                                        profile = profile,
                                        activePlan = activePlan,
                                        products = products,
                                        isBusy = isBusy,
                                        onSaveProduct = viewModel::saveProduct,
                                        onDeleteProduct = viewModel::deleteProduct,
                                        onShareRecorded = viewModel::recordShare
                                    )

                                    "calendar" -> CalendarScreen(
                                        events = events,
                                        isBusy = isBusy,
                                        onAddEvent = viewModel::addCalendarEvent,
                                        onMarkStatus = viewModel::markCalendarEventStatus,
                                        onDeleteEvent = viewModel::deleteCalendarEvent
                                    )

                                    "loyalty" -> LoyaltyScreen(
                                        profile = profile,
                                        activePlan = activePlan,
                                        customers = customers,
                                        isBusy = isBusy,
                                        onAddCustomer = viewModel::addCustomer,
                                        onRecordOfferSent = viewModel::updateCustomerOffer,
                                        onDeleteCustomer = viewModel::deleteCustomer
                                    )

                                    "subscription" -> SubscriptionScreen(
                                        profile = profile,
                                        plans = plans,
                                        userPayments = userPayments,
                                        isBusy = isBusy,
                                        onSubmitPayment = viewModel::submitPayment,
                                        onMessage = viewModel::showMessage
                                    )

                                    "admin" -> AdminScreen(
                                        isAdmin = isAdmin,
                                        merchants = (adminMerchantsState as? UiState.Success)?.data ?: emptyList(),
                                        payments = (adminPaymentsState as? UiState.Success)?.data ?: emptyList(),
                                        plans = plans,
                                        isBusy = isBusy,
                                        onApprovePayment = viewModel::approvePayment,
                                        onRejectPayment = viewModel::rejectPayment,
                                        onToggleSuspension = viewModel::toggleMerchantSuspension,
                                        onSavePlan = viewModel::savePlanConfig,
                                        onBack = { currentRoute = "dashboard" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
