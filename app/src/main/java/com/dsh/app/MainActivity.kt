package com.dsh.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.dsh.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appPreferences: AppPreferences
    private lateinit var webChromeClient: DshWebChromeClient
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appPreferences = AppPreferences(this)

        setupInsets()
        setupBackNavigation()
        setupFileChooser()
        setupWebView()
        setupSwipeRefresh()
        setupSettingsButton()
        setupErrorView()

        if (savedInstanceState == null) {
            loadCurrentUrl()
        } else {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    private fun setupInsets() {
        // Keep all content inside the safe area: respect status bar, navigation
        // bar and display cutout margins instead of drawing under them.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupBackNavigation() {
        // User specified: back button always exits/minimizes app directly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    private fun setupFileChooser() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val uris = when {
                    data?.clipData != null -> {
                        val count = data.clipData!!.itemCount
                        Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                    }
                    data?.data != null -> arrayOf(data.data!!)
                    else -> null
                }
                webChromeClient.handleFileChooserResult(uris)
            } else {
                webChromeClient.handleFileChooserResult(null)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings

        // Enable rich modern web app capabilities
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Setup clients
        webChromeClient = DshWebChromeClient { intent ->
            fileChooserLauncher.launch(intent)
        }
        webView.webChromeClient = webChromeClient

        webView.webViewClient = DshWebViewClient(
            onPageStartedCallback = {
                // Page started loading
            },
            onPageFinishedCallback = {
                binding.swipeRefresh.isRefreshing = false
                showWebView()
            },
            onErrorCallback = { _, description ->
                binding.swipeRefresh.isRefreshing = false
                showError(description)
            }
        )
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface)
        binding.swipeRefresh.setOnRefreshListener {
            loadCurrentUrl()
        }
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupErrorView() {
        binding.btnRetry.setOnClickListener {
            loadCurrentUrl()
        }
        binding.btnChangeUrl.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun loadCurrentUrl() {
        val targetUrl = appPreferences.getServerUrl()
        binding.swipeRefresh.isRefreshing = true
        binding.webView.loadUrl(targetUrl)
    }

    private fun showWebView() {
        binding.layoutError.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
    }

    private fun showError(description: String) {
        binding.webView.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.txtErrorMessage.text = getString(R.string.error_message)
    }

    private fun showSettingsDialog() {
        val currentUrl = appPreferences.getServerUrl()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val editServerUrl = dialogView.findViewById<TextInputEditText>(R.id.editServerUrl)
        val inputLayoutUrl = dialogView.findViewById<TextInputLayout>(R.id.inputLayoutUrl)
        val btnResetDefault = dialogView.findViewById<TextView>(R.id.btnResetDefault)

        editServerUrl.setText(currentUrl)
        editServerUrl.setSelection(currentUrl.length)

        btnResetDefault.setOnClickListener {
            editServerUrl.setText(UrlHelper.DEFAULT_URL)
            editServerUrl.setSelection(UrlHelper.DEFAULT_URL.length)
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.settings_save, null)
            .setNegativeButton(R.string.settings_cancel, null)
            .create()
            .apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val input = editServerUrl.text?.toString()?.trim() ?: ""
                    if (UrlHelper.isValid(input)) {
                        val normalized = UrlHelper.normalize(input)
                        appPreferences.setServerUrl(normalized)
                        dismiss()
                        Toast.makeText(this@MainActivity, "Conectando a $normalized", Toast.LENGTH_SHORT).show()
                        loadCurrentUrl()
                    } else {
                        inputLayoutUrl.error = getString(R.string.url_invalid)
                    }
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
