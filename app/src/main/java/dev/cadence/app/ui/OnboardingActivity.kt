package dev.cadence.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import dev.cadence.app.MainActivity
import dev.cadence.app.R
import dev.cadence.app.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private var step = 1

    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) nextStep() else updateUI()
    }

    private val btPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        nextStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateUI()

        binding.btnOpenAppInfo.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        binding.btnNext.setOnClickListener {
            when (step) {
                1 -> nextStep()
                2 -> {
                    if (isNotificationAccessGranted()) {
                        nextStep()
                    } else {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
                3 -> micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                4 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    btPermLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    nextStep()
                }
            }
        }
        binding.btnFinish.setOnClickListener {
            getSharedPreferences("cadence_prefs", MODE_PRIVATE)
                .edit().putBoolean("onboarding_completed", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun isNotificationAccessGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun nextStep() {
        step = (step + 1).coerceAtMost(5)
        updateUI()
    }

    private fun updateUI() {
        binding.tvStepCounter.text = getString(R.string.onboard_step_counter, step, 5)

        when (step) {
            1 -> {
                binding.tvTitle.text = getString(R.string.onboard_step1_title)
                binding.tvBody.text = getString(R.string.onboard_step1_body)
                binding.cardRestrictedGuide.visibility = View.GONE
                binding.tvGrantedBadge.visibility = View.GONE
                binding.btnNext.text = getString(R.string.onboard_next)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            2 -> {
                val granted = isNotificationAccessGranted()
                binding.tvTitle.text = getString(R.string.onboard_step2_title)
                binding.tvBody.text = getString(R.string.onboard_step2_body)

                if (Build.VERSION.SDK_INT >= 33 && !granted) {
                    binding.cardRestrictedGuide.visibility = View.VISIBLE
                } else {
                    binding.cardRestrictedGuide.visibility = View.GONE
                }

                if (granted) {
                    binding.tvGrantedBadge.visibility = View.VISIBLE
                    binding.tvGrantedBadge.text = getString(R.string.onboard_notification_granted)
                    binding.btnNext.text = getString(R.string.onboard_next)
                } else {
                    binding.tvGrantedBadge.visibility = View.GONE
                    binding.btnNext.text = getString(R.string.onboard_btn_open_notification)
                }

                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            3 -> {
                binding.tvTitle.text = getString(R.string.onboard_step3_title)
                binding.tvBody.text = getString(R.string.onboard_step3_body)
                binding.cardRestrictedGuide.visibility = View.GONE
                binding.tvGrantedBadge.visibility = View.GONE
                binding.btnNext.text = getString(R.string.onboard_grant_mic)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            4 -> {
                binding.tvTitle.text = getString(R.string.onboard_step4_title)
                binding.tvBody.text = getString(R.string.onboard_step4_body)
                binding.cardRestrictedGuide.visibility = View.GONE
                binding.tvGrantedBadge.visibility = View.GONE
                binding.btnNext.text = getString(R.string.onboard_grant_bt)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            5 -> {
                binding.tvTitle.text = getString(R.string.onboard_done_title)
                binding.tvBody.text = getString(R.string.onboard_done_body)
                binding.cardRestrictedGuide.visibility = View.GONE
                binding.tvGrantedBadge.visibility = View.GONE
                binding.btnNext.visibility = View.GONE
                binding.btnFinish.visibility = View.VISIBLE
            }
        }
    }
}
