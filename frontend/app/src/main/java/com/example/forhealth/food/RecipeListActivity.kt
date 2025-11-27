package com.example.forhealth.food

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RecipeListActivity : AppCompatActivity() {

    private lateinit var rvRecipes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: RecipeAdapter

    private val gson = Gson()
    private var recipeMap = mutableMapOf<String, String>() // recipeId -> JSON

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_list)

        rvRecipes = findViewById(R.id.rvRecipes)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvRecipes.layoutManager = LinearLayoutManager(this)

        loadRecipes()
    }

    private fun loadRecipes() {
        val prefs = getSharedPreferences("recipes", Context.MODE_PRIVATE)
        recipeMap = prefs.all.mapNotNull {
            val key = it.key
            val value = it.value as? String ?: return@mapNotNull null
            key to value
        }.toMap().toMutableMap()

        if (recipeMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvRecipes.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvRecipes.visibility = View.VISIBLE

            adapter = RecipeAdapter(recipeMap.keys.toList(),
                onItemClick = { id -> showRecipeDetail(id) },
                onDeleteClick = { id -> deleteRecipe(id) }
            )
            rvRecipes.adapter = adapter
        }
    }

    private fun deleteRecipe(id: String) {
        getSharedPreferences("recipes", Context.MODE_PRIVATE)
            .edit()
            .remove(id)
            .apply()

        Toast.makeText(this, "已删除: $id", Toast.LENGTH_SHORT).show()
        loadRecipes()
    }

    private fun showRecipeDetail(id: String) {
        val json = recipeMap[id] ?: return
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val recipeList = gson.fromJson<List<Map<String, Any>>>(json, type)

        val msg = recipeList.joinToString("\n") {
            val foodName = it["foodName"]
            val servingAmount = it["servingAmount"]
            val calories = it["calories"] ?: "未知"
            "$foodName x $servingAmount  卡路里: $calories kcal"
        }


        AlertDialog.Builder(this)
            .setTitle(id)
            .setMessage(msg)
            .setPositiveButton("关闭", null)
            .show()
    }
}
