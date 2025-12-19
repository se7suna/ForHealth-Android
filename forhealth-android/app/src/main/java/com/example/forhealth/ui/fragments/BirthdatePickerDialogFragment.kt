package com.example.forhealth.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.DialogFragment
import com.example.forhealth.R
import java.util.Calendar
import java.util.Locale

/**
 * 生日选择对话框
 * 使用三个NumberPicker选择年、月、日
 */
class BirthdatePickerDialogFragment : DialogFragment() {
    
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR) - 20
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    
    private var onDateSelectedListener: ((year: Int, month: Int, day: Int) -> Unit)? = null
    
    companion object {
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month"
        private const val ARG_DAY = "day"
        
        fun newInstance(year: Int? = null, month: Int? = null, day: Int? = null): BirthdatePickerDialogFragment {
            val fragment = BirthdatePickerDialogFragment()
            val args = Bundle()
            year?.let { args.putInt(ARG_YEAR, it) }
            month?.let { args.putInt(ARG_MONTH, it) }
            day?.let { args.putInt(ARG_DAY, it) }
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedYear = it.getInt(ARG_YEAR, Calendar.getInstance().get(Calendar.YEAR) - 20)
            selectedMonth = it.getInt(ARG_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1)
            selectedDay = it.getInt(ARG_DAY, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_birthdate_picker, container, false)
        
        val tvPrompt = view.findViewById<TextView>(R.id.tvPrompt)
        val npYear = view.findViewById<NumberPicker>(R.id.npYear)
        val npMonth = view.findViewById<NumberPicker>(R.id.npMonth)
        val npDay = view.findViewById<NumberPicker>(R.id.npDay)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        
        tvPrompt.text = "请选择出生日期"
        
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        // 年
        npYear.minValue = currentYear - 100
        npYear.maxValue = currentYear
        npYear.value = selectedYear
        npYear.wrapSelectorWheel = false
        
        // 月
        npMonth.minValue = 1
        npMonth.maxValue = 12
        npMonth.value = selectedMonth
        npMonth.wrapSelectorWheel = true
        
        // 日
        npDay.minValue = 1
        npDay.maxValue = getDaysInMonth(selectedMonth, selectedYear)
        npDay.value = selectedDay.coerceAtMost(npDay.maxValue)
        npDay.wrapSelectorWheel = true
        
        // 监听月份变化，更新日的最大值
        npMonth.setOnValueChangedListener { _, _, newMonth ->
            val daysInMonth = getDaysInMonth(newMonth, npYear.value)
            val currentDay = npDay.value
            npDay.maxValue = daysInMonth
            if (currentDay > daysInMonth) {
                npDay.value = daysInMonth
            }
        }
        
        // 监听年份变化，更新日的最大值（处理闰年）
        npYear.setOnValueChangedListener { _, _, _ ->
            val daysInMonth = getDaysInMonth(npMonth.value, npYear.value)
            val currentDay = npDay.value
            npDay.maxValue = daysInMonth
            if (currentDay > daysInMonth) {
                npDay.value = daysInMonth
            }
        }
        
        btnConfirm.setOnClickListener {
            onDateSelectedListener?.invoke(npYear.value, npMonth.value, npDay.value)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        return view
    }
    
    fun setOnDateSelectedListener(listener: (year: Int, month: Int, day: Int) -> Unit) {
        onDateSelectedListener = listener
    }
    
    private fun getDaysInMonth(month: Int, year: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 31
        }
    }
    
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}

