package dev.cadence.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

        binding.btnNext.setOnClickListener {
            when (step) {
                1 -> nextStep()
                2 -> {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    nextStep()
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

    private fun nextStep() {
        step = (step + 1).coerceAtMost(5)
        updateUI()
    }

    private fun updateUI() {
        when (step) {
            1 -> {
                binding.tvTitle.text = getString(R.string.onboard_step1_title)
                binding.tvBody.text = getString(R.string.onboard_step1_body)
                binding.btnNext.text = getString(R.string.onboard_next)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            2 -> {
                binding.tvTitle.text = getString(R.string.onboard_step2_title)
                binding.tvBody.text = getString(R.string.onboard_step2_body)
                binding.btnNext.text = getString(R.string.onboard_open_settings)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            3 -> {
                binding.tvTitle.text = getString(R.string.onboard_step3_title)
                binding.tvBody.text = getString(R.string.onboard_step3_body)
                binding.btnNext.text = getString(R.string.onboard_grant_mic)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            4 -> {
                binding.tvTitle.text = getString(R.string.onboard_step4_title)
                binding.tvBody.text = getString(R.string.onboard_step4_body)
                binding.btnNext.text = getString(R.string.onboard_grant_bt)
                binding.btnNext.visibility = View.VISIBLE
                binding.btnFinish.visibility = View.GONE
            }
            5 -> {
                binding.tvTitle.text = getString(R.string.onboard_done_title)
                binding.tvBody.text = getString(R.string.onboard_done_body)
                binding.btnNext.visibility = View.GONE
                binding.btnFinish.visibility = View.VISIBLE
            }
        }
    }
}
