package com.example.forhealth.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.NumberPicker
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.DialogFragment
import com.example.forhealth.R
import java.util.Locale

/**
 * 通用数字选择对话框
 * 使用三个NumberPicker选择数值（可用于体重、周数等）
 */
class NumberPickerDialogFragment : DialogFragment() {
    
    private var minValue: Int = 0
    private var maxValue: Int = 100
    private var currentValue: Int = 50
    private var unit: String = ""
    private var promptText: String = "请选择"
    
    private var onValueSelectedListener: ((value: Int) -> Unit)? = null
    
    companion object {
        private const val ARG_MIN = "min"
        private const val ARG_MAX = "max"
        private const val ARG_CURRENT = "current"
        private const val ARG_UNIT = "unit"
        private const val ARG_PROMPT = "prompt"
        
        fun newInstance(
            min: Int,
            max: Int,
            current: Int? = null,
            unit: String = "",
            prompt: String = "请选择"
        ): NumberPickerDialogFragment {
            val fragment = NumberPickerDialogFragment()
            val args = Bundle()
            args.putInt(ARG_MIN, min)
            args.putInt(ARG_MAX, max)
            current?.let { args.putInt(ARG_CURRENT, it) }
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_PROMPT, prompt)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            minValue = it.getInt(ARG_MIN, 0)
            maxValue = it.getInt(ARG_MAX, 100)
            currentValue = it.getInt(ARG_CURRENT, (minValue + maxValue) / 2)
            unit = it.getString(ARG_UNIT, "") ?: ""
            promptText = it.getString(ARG_PROMPT, "请选择") ?: "请选择"
        }
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
        val view = inflater.inflate(R.layout.dialog_number_picker, container, false)
        
        val tvPrompt = view.findViewById<TextView>(R.id.tvPrompt)
        val npValue = view.findViewById<NumberPicker>(R.id.npValue)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        
        tvPrompt.text = promptText
        
        // 设置NumberPicker
        npValue.minValue = minValue
        npValue.maxValue = maxValue
        npValue.wrapSelectorWheel = false
        
        // 设置显示格式（如果有单位）
        if (unit.isNotEmpty()) {
            // 使用displayedValues确保所有值（包括当前选中的）都显示单位
            val displayedValues = (minValue..maxValue).map { "$it $unit" }.toTypedArray()
            npValue.displayedValues = displayedValues
        }
        
        // 设置当前值（必须在设置displayedValues之后）
        npValue.value = currentValue.coerceIn(minValue, maxValue)
        
        btnConfirm.setOnClickListener {
            onValueSelectedListener?.invoke(npValue.value)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        return view
    }
    
    fun setOnValueSelectedListener(listener: (value: Int) -> Unit) {
        onValueSelectedListener = listener
    }
}

