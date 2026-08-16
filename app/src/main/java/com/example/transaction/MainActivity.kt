package com.example.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.transaction.sms.SmsSyncManager
import com.example.transaction.ui.MainScreen
import com.example.transaction.ui.theme.TransactionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            syncMessages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        syncMessages()

        enableEdgeToEdge()
        setContent {
            TransactionTheme {
                MainScreen()
            }
        }
    }

    private fun syncMessages() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch {
                SmsSyncManager(this@MainActivity).syncMissedSms()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
