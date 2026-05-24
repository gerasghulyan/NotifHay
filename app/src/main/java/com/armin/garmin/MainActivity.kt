package com.armin.garmin

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.armin.garmin.databinding.ActivityMainBinding
import com.armin.garmin.service.NotificationInterceptorService
import com.armin.garmin.settings.AppPreferences
import com.armin.garmin.transliteration.ArmenianTransliterator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)

        setupOptions()
        setupPermissionButton()
        setupBatteryOptimization()
        setupTestButton()
        updatePermissionStatus()
        updateBatteryStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateBatteryStatus()
    }

    private fun setupOptions() {
        binding.switchEnable.isChecked = prefs.transliterationEnabled
        binding.switchDebug.isChecked = prefs.debugMode

        binding.switchEnable.setOnCheckedChangeListener { _, checked ->
            prefs.transliterationEnabled = checked
        }
        binding.switchDebug.setOnCheckedChangeListener { _, checked ->
            prefs.debugMode = checked
        }
    }

    private fun setupPermissionButton() {
        binding.btnGrantPermission.setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_required_title)
                    .setMessage(R.string.permission_required_message)
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBatteryOptimization() {
        binding.btnDisableBatteryOpt.setOnClickListener {
            if (isBatteryOptimizationIgnored()) {
                Toast.makeText(this, R.string.battery_already_exempted, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
        }
    }

    private fun updateBatteryStatus() {
        val exempted = isBatteryOptimizationIgnored()
        binding.textBatteryStatus.text = if (exempted) {
            getString(R.string.battery_exempted)
        } else {
            getString(R.string.battery_restricted)
        }
        binding.textBatteryStatus.setTextColor(
            if (exempted) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_orange_dark)
        )
        binding.btnDisableBatteryOpt.isEnabled = !exempted
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun setupTestButton() {
        binding.btnTest.setOnClickListener {
            val input = binding.editTestInput.text.toString()
            if (input.isNotBlank()) {
                binding.textTestOutput.text = ArmenianTransliterator.transliterate(input)
            } else {
                val sample = "Բարև ոնց ես, John! 😄"
                binding.editTestInput.setText(sample)
                binding.textTestOutput.text = ArmenianTransliterator.transliterate(sample)
            }
        }
    }

    private fun updatePermissionStatus() {
        val enabled = isNotificationListenerEnabled()
        binding.textPermissionStatus.text = if (enabled) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_not_granted)
        }
        binding.textPermissionStatus.setTextColor(
            if (enabled) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
        binding.btnGrantPermission.text = if (enabled) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.grant_permission)
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            val myComponent = ComponentName(this, NotificationInterceptorService::class.java)
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn == myComponent) return true
            }
        }
        return false
    }
}
