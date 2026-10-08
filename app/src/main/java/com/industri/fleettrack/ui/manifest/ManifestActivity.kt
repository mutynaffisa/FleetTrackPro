package com.industri.fleettrack.ui.manifest

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.industri.fleettrack.R
import com.industri.fleettrack.data.local.AppDatabase
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import com.industri.fleettrack.data.remote.api.CourierApiService
import com.industri.fleettrack.data.repository.DeliveryRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManifestActivity : AppCompatActivity() {

    private lateinit var viewModel: ManifestViewModel
    private lateinit var adapter: DeliveryListAdapter
    private var allOrders: List<DeliveryOrderEntity> = emptyList()

    private lateinit var rvManifest: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etSearchManifest: EditText
    private lateinit var pbMainLoading: ProgressBar
    private lateinit var tvStatsSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manifest)

        rvManifest = findViewById(R.id.rvManifest)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        etSearchManifest = findViewById(R.id.etSearchManifest)
        pbMainLoading = findViewById(R.id.pbMainLoading)
        tvStatsSummary = findViewById(R.id.tvStatsSummary)

        val database = AppDatabase.getInstance(this)
        val apiService = CourierApiService.create()
        val repository = DeliveryRepository(database.deliveryDao(), apiService)
        val factory = ManifestViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[ManifestViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchBar()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = DeliveryListAdapter { order ->
            viewModel.markAsDelivered(order.orderId)
            Toast.makeText(this, "Paket ${order.trackingNumber} berhasil diserahkan!", Toast.LENGTH_SHORT).show()
        }
        rvManifest.layoutManager = LinearLayoutManager(this)
        rvManifest.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupSearchBar() {
        etSearchManifest.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterOrders(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterOrders(query: String) {
        val filtered = if (query.isBlank()) {
            allOrders
        } else {
            allOrders.filter { order ->
                order.trackingNumber.contains(query, ignoreCase = true) ||
                        order.recipientName.contains(query, ignoreCase = true) ||
                        order.destinationAddress.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered.toMutableList())
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                swipeRefresh.isRefreshing = (state is ManifestUiState.Loading)
                when (state) {
                    is ManifestUiState.Loading -> {
                        pbMainLoading.visibility = View.VISIBLE
                    }
                    is ManifestUiState.Success -> {
                        pbMainLoading.visibility = View.GONE
                        allOrders = state.orders

                        val deliveredCount = allOrders.count { it.deliveryStatus == "DELIVERED" }
                        tvStatsSummary.text = "Tugas Hari Ini: ${allOrders.size} Paket | Terkirim: $deliveredCount"

                        val currentSearch = etSearchManifest.text.toString()
                        filterOrders(currentSearch)
                    }
                    is ManifestUiState.Error -> {
                        pbMainLoading.visibility = View.GONE
                        Toast.makeText(this@ManifestActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}