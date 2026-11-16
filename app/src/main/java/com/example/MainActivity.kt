package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.remote.KaamWalaFirestoreRepository
import com.example.firebase.configureAppCheck
import com.example.ui.KaamWalaApp
import com.example.ui.KaamWalaViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.theme.KaamWalaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install Firebase App Check before Compose / ViewModels initialize Firebase AI Logic
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            KaamWalaTheme {
                KaamWalaRoot(auth = Firebase.auth)
            }
        }
    }
}

@Composable
fun KaamWalaRoot(
    auth: FirebaseAuth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener transitions automatically */ })
    } else {
        key(signedInUser.uid) {
            val vmFactory = remember(signedInUser.uid) {
                viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        KaamWalaViewModel(
                            repository = KaamWalaFirestoreRepository(db, auth),
                            currentUserId = signedInUser.uid,
                            currentUserName = signedInUser.displayName,
                            currentUserEmail = signedInUser.email
                        )
                    }
                }
            }
            val kaamWalaViewModel: KaamWalaViewModel = viewModel(
                key = signedInUser.uid,
                factory = vmFactory
            )
            KaamWalaApp(
                viewModel = kaamWalaViewModel,
                onSignOut = {
                    com.example.ui.auth.signOutUser(
                        context = context,
                        scope = scope
                    )
                }
            )
        }
    }
}
