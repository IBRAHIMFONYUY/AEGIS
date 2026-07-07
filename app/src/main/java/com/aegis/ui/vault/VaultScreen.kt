package com.aegis.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.ui.components.GradientTopBar
import com.aegis.ui.components.SectionHeader
import com.aegis.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Vault", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(Icons.Filled.Lock, "Lock")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientTopBar()
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    VaultContent()
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = WarningOrange.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Vault is Encrypted",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your passwords, IDs, and secure documents are protected with AES-256 hardware encryption.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { 
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    viewModel.unlock(activity)
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Fingerprint, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Biometrics")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader("Secure Items")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your secure storage is empty.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* Add Item */ }) {
            Icon(Icons.Filled.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Secure Item")
        }
    }
}
