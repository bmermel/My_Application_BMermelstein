package com.example.myapplicationbmermelstein

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument


/* ------------------ Routes ------------------ */
sealed class Route(val route: String) {
    data object Wallet : Route("wallet")
    data object Receipt : Route("receipt/{amount}") {
        fun withAmount(amount: String) = "receipt/${Uri.encode(amount)}"
    }
}

/* ------------------ ViewModel ------------------ */
class WalletViewModel : ViewModel() {
    var balance by mutableStateOf(10_000.00)
        private set

    fun withdraw(amount: Double): Boolean {
        if (amount <= 0) return false
        if (amount > balance) return false
        balance -= amount
        return true
    }
}

/* ------------------ Activity ------------------ */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                AppNav()
            }
        }
    }
}

/* ------------------ Navigation Host ------------------ */
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: WalletViewModel = viewModel()

    NavHost(navController = nav, startDestination = Route.Wallet.route) {
        composable(Route.Wallet.route) {
            WalletScreen(
                balance = vm.balance,
                onWithdraw = { amount ->
                    val ok = vm.withdraw(amount)
                    ok
                },
                onWithdrawSuccess = { amount ->
                    nav.navigate(Route.Receipt.withAmount("%.2f".format(amount)))
                }
            )
        }
        composable(
            route = Route.Receipt.route,
            arguments = listOf(navArgument("amount") { type = NavType.StringType })
        ) { backStack ->
            val amountArg = backStack.arguments?.getString("amount") ?: "0.00"
            ReceiptScreen(
                withdrawnText = amountArg,
                onBackToWallet = { nav.popBackStack() }
            )
        }
    }
}

/* ------------------ Pantalla 1: Wallet ------------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    balance: Double,
    onWithdraw: (Double) -> Boolean,
    onWithdrawSuccess: (Double) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun tryWithdraw() {
        val amount = input.replace(",", ".").toDoubleOrNull()
        if (amount == null) {
            error = "Ingresá un número válido."
            // Para cerrar el teclado
            focusManager.clearFocus()
            return
        }
        val ok = onWithdraw(amount)
        if (ok) {
            error = null
            onWithdrawSuccess(amount)
            input = ""
            focusManager.clearFocus()
        } else {
            error = if (amount <= 0) "El monto debe ser mayor a 0."
            else "Saldo insuficiente."
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Billetera Virtual - Brian Mermelstein") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo de la billetera",
                    modifier = Modifier
                        .size(250.dp)
                        .padding(bottom = 16.dp)
                )

                Text("Saldo disponible", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$ " + "%,.2f".format(balance),
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; if (error != null) error = null },
                    label = { Text("Monto a retirar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            tryWithdraw()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { tryWithdraw() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = input.isNotBlank()
                ) {
                    Text("Retirar")
                }
            }
        }
    }
}


/* ------------------ Pantalla 2: Comprobante ------------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    withdrawnText: String,
    onBackToWallet: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Comprobante") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Retiro exitoso", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("Monto retirado: $ $withdrawnText", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBackToWallet) {
                Text("Volver a la billetera")
            }
        }
    }
}
