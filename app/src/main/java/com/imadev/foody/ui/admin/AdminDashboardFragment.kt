package com.imadev.foody.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentAdminDashboardBinding
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AdminDashboardFragment : BaseFragment<FragmentAdminDashboardBinding, AdminViewModel>() {

    override val viewModel: AdminViewModel by activityViewModels()

    private var isFirstLoad = true
    private var lastDataFingerprint: String = ""

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminDashboardBinding = FragmentAdminDashboardBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnManageOrders.setOnClickListener {
            viewModel.navigate(R.id.action_adminDashboardFragment_to_adminOrdersFragment)
        }
        binding.btnManageDrivers.setOnClickListener {
            viewModel.navigate(R.id.action_adminDashboardFragment_to_adminDriversFragment)
        }
        binding.btnManageUsers.setOnClickListener {
            viewModel.navigate(R.id.action_adminDashboardFragment_to_adminUsersFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.allOrders.collectFlow(viewLifecycleOwner) { orders ->
            if (orders.isEmpty()) return@collectFlow

            val currentFingerprint = orders.size.toString() + orders.joinToString("") { it.status.name }
            if (currentFingerprint == lastDataFingerprint) return@collectFlow
            lastDataFingerprint = currentFingerprint

            (activity as? HomeActivity)?.hideProgressBar()

            updateQuickStats(orders)
            updateLineChart(orders)
            updateRevenueBarChart(orders)
            updatePieChart(orders)
            updateTopMealsChart(orders)

            isFirstLoad = false
        }

        viewModel.allUsers.collectFlow(viewLifecycleOwner) { users ->
            binding.totalUsersCount.text = users.size.toString()
        }

        viewModel.allDrivers.collectFlow(viewLifecycleOwner) { drivers ->
            binding.activeDriversCount.text = drivers.size.toString()
            binding.onlineStatus.text = "● ${drivers.count { it.available }} Online Now"
        }
    }

    private fun updateQuickStats(orders: List<Order>) {
        binding.totalOrdersCount.text = orders.size.toString()
        binding.activeOrdersCount.text = orders.count {
            it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED
        }.toString()
        binding.deliveredOrdersCount.text = orders.count { it.status == OrderStatus.DELIVERED }.toString()

        val totalRevenue = orders.filter { it.status == OrderStatus.DELIVERED }
            .sumOf { order -> order.meals.sumOf { it.price * it.quantity } }

        binding.tvRevenue.text = String.format(Locale.getDefault(), "%.2f MAD", totalRevenue)
    }

    private fun setupCharts() {
        val charts = listOf(binding.orderChart, binding.revenueBarChart, binding.topMealsChart)
        charts.forEach {
            it.description.isEnabled = false
            it.legend.isEnabled = false
        }

        binding.statusPieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(20f, 0f, 20f, 0f)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 50f
            setDrawCenterText(false)
            setDrawEntryLabels(false)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 10f
            }
        }

        binding.topMealsChart.apply {
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 1f
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateLineChart(orders: List<Order>) {
        val (labels, counts) = getLast7DaysStats(orders) { 1f }
        val entries = labels.mapIndexed { index, label -> Entry(index.toFloat(), counts[label] ?: 0f) }

        val dataSet = LineDataSet(entries, "Orders").apply {
            color = "#FF4B3A".toColorInt()
            setCircleColor("#FF4B3A".toColorInt())
            lineWidth = 2.5f
            setDrawFilled(true)
            fillColor = "#FF4B3A".toColorInt()
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        binding.orderChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            data = LineData(dataSet)
            if (isFirstLoad) animateX(800)
            invalidate()
        }
    }

    private fun updateRevenueBarChart(orders: List<Order>) {
        val (labels, revenueMap) = getLast7DaysStats(orders.filter { it.status == OrderStatus.DELIVERED }) { order ->
            order.meals.sumOf { it.price * it.quantity }.toFloat()
        }
        val entries = labels.mapIndexed { index, label -> BarEntry(index.toFloat(), revenueMap[label] ?: 0f) }

        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = "#5E72E4".toColorInt()
            setDrawValues(true)
            valueTextSize = 9f
        }

        binding.revenueBarChart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            data = BarData(dataSet)
            if (isFirstLoad) animateY(800)
            invalidate()
        }
    }

    private fun updatePieChart(orders: List<Order>) {
        val statusCounts = orders.groupingBy { it.status }.eachCount()
        val entries = statusCounts.map { PieEntry(it.value.toFloat(), it.key.name) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                "#2DCE89".toColorInt(),
                "#FBAD17".toColorInt(),
                "#F5365C".toColorInt(),
                "#8965E0".toColorInt()
            )
            sliceSpace = 2f
            valueTextColor = Color.BLACK
            valueTextSize = 11f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLineColor = Color.GRAY
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.statusPieChart))
        }

        binding.statusPieChart.apply {
            data = pieData
            if (isFirstLoad) animateY(1000)
            invalidate()
        }
    }

    private fun updateTopMealsChart(orders: List<Order>) {
        val mealSales = mutableMapOf<String, Int>()
        orders.forEach { order ->
            order.meals.forEach { meal ->
                val name = meal.name.trim()
                if (name.isNotEmpty()) {
                    mealSales[name] = (mealSales[name] ?: 0) + meal.quantity
                }
            }
        }

        val topMeals = mealSales.entries.sortedByDescending { it.value }.take(10).reversed()
        if (topMeals.isEmpty()) return

        val labels = topMeals.map { it.key }
        val entries = topMeals.mapIndexed { index, entry -> BarEntry(index.toFloat(), entry.value.toFloat()) }

        val dataSet = BarDataSet(entries, "Top Selling").apply {
            color = "#2DCE89".toColorInt()
            valueTextSize = 10f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
        }

        binding.topMealsChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                setLabelCount(labels.size)
                granularity = 1f
                isGranularityEnabled = true
            }

            data = BarData(dataSet).apply {
                barWidth = 0.5f
            }

            setFitBars(true)
            if (isFirstLoad) animateY(1000)
            invalidate()
        }
    }

    private fun getLast7DaysStats(orders: List<Order>, valueSelector: (Order) -> Float): Pair<List<String>, Map<String, Float>> {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val labels = mutableListOf<String>()
        val dataMap = mutableMapOf<String, Float>()
        val calendar = Calendar.getInstance()
        for (i in 0..6) {
            val dateStr = sdf.format(calendar.time)
            labels.add(0, dateStr)
            dataMap[dateStr] = 0f
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        orders.forEach { order ->
            val dateStr = sdf.format(Date(order.date))
            if (dataMap.containsKey(dateStr)) dataMap[dateStr] = (dataMap[dateStr] ?: 0f) + valueSelector(order)
        }
        return Pair(labels, dataMap)
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Admin Dashboard")
    }
}