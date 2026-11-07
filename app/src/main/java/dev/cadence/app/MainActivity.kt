package dev.cadence.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.cadence.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Implement permission checking flow in Phase 7
        binding.buttonCheckPermissions.setOnClickListener {
            // No-op for Phase 1
        }
    }
}
