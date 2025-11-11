package com.example.forhealth.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.forhealth.R
import com.example.forhealth.model.FoodItem

class FoodDetailFragment : DialogFragment() {

    companion object {
        private const val FOOD_ITEM_KEY = "food_item"

        fun newInstance(foodItem: FoodItem): FoodDetailFragment {
            val fragment = FoodDetailFragment()
            val args = Bundle()
            args.putParcelable(FOOD_ITEM_KEY, foodItem)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var foodItem: FoodItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_food_detail, container, false)

        foodItem = arguments?.getParcelable(FOOD_ITEM_KEY) ?: return view

        val foodNameTextView = view.findViewById<TextView>(R.id.foodNameTextView)
        val caloriesTextView = view.findViewById<TextView>(R.id.caloriesTextView)
        val proteinTextView = view.findViewById<TextView>(R.id.proteinTextView)
        val fatTextView = view.findViewById<TextView>(R.id.fatTextView)
        val carbsTextView = view.findViewById<TextView>(R.id.carbsTextView)

        foodNameTextView.text = foodItem.name
        caloriesTextView.text = "热量: ${foodItem.calories} kcal"
        proteinTextView.text = "蛋白质: ${foodItem.protein} g"
        fatTextView.text = "脂肪: ${foodItem.fat} g"
        carbsTextView.text = "碳水化合物: ${foodItem.carbs} g"

        return view
    }
}
