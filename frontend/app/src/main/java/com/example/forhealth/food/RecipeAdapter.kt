package com.example.forhealth.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R

class RecipeAdapter(
    private val recipeIds: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        val btnDeleteRecipe: Button = itemView.findViewById(R.id.btnDeleteRecipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun getItemCount(): Int = recipeIds.size

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val id = recipeIds[position]
        holder.tvRecipeName.text = id

        holder.itemView.setOnClickListener { onItemClick(id) }
        holder.btnDeleteRecipe.setOnClickListener { onDeleteClick(id) }
    }
}
