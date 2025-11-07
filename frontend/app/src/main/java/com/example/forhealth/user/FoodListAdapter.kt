package com.example.forhealth.user

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.forhealth.R
import com.example.forhealth.model.FoodItem

class FoodListAdapter(
    private val context: Context    ,
    private var foodList: List<FoodItem>
) : BaseAdapter() {

    override fun getCount(): Int = foodList.size

    override fun getItem(position: Int): FoodItem = foodList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.food_list_item, parent, false)
        val foodItem = foodList[position]

        val foodName = view.findViewById<TextView>(R.id.foodName)
        foodName.text = foodItem.name

        return view
    }

    fun updateData(newFoodList: List<FoodItem>) {
        foodList = newFoodList
        notifyDataSetChanged()
    }
}
