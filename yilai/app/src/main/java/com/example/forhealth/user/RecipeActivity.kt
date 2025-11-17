package com.example.forhealth.user
import com.example.forhealth.R
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class RecipeActivity : AppCompatActivity() {

    private lateinit var etRecipeName: EditText
    private lateinit var etIngredientName: EditText
    private lateinit var etIngredientAmount: EditText
    private lateinit var btnAddIngredient: Button
    private lateinit var btnSaveRecipe: Button
    private lateinit var ingredientList: MutableList<Ingredient>
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        etRecipeName = findViewById(R.id.etRecipeName)
        etIngredientName = findViewById(R.id.etIngredientName)
        etIngredientAmount = findViewById(R.id.etIngredientAmount)
        btnAddIngredient = findViewById(R.id.btnAddIngredient)
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe)
        recyclerView = findViewById(R.id.recyclerViewIngredients)

        ingredientList = mutableListOf()
        ingredientAdapter = IngredientAdapter(ingredientList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ingredientAdapter
        val btnBackToFood = findViewById<Button>(R.id.btnBack)
        btnBackToFood.setOnClickListener {
            // 跳转回 FoodActivity
            val intent = Intent(this, FoodActivity::class.java)  // 返回到 FoodActivity
            startActivity(intent)
            finish()  // 结束当前页面
        }
        btnAddIngredient.setOnClickListener {
            val ingredientName = etIngredientName.text.toString()
            val ingredientAmount = etIngredientAmount.text.toString()

            if (ingredientName.isNotEmpty() && ingredientAmount.isNotEmpty()) {
                val ingredient = Ingredient(ingredientName, ingredientAmount)
                ingredientList.add(ingredient)
                ingredientAdapter.notifyDataSetChanged()

                // Reset input fields
                etIngredientName.text.clear()
                etIngredientAmount.text.clear()
            }
        }

        btnSaveRecipe.setOnClickListener {
            val recipeName = etRecipeName.text.toString()
            if (recipeName.isNotEmpty() && ingredientList.isNotEmpty()) {
                val recipe = Recipe(recipeName, ingredientList)
                saveRecipe(recipe)
                Toast.makeText(this, "食谱创建成功", Toast.LENGTH_SHORT).show()
                finish()  // Close the activity
            }
        }
    }

    private fun saveRecipe(recipe: Recipe) {
        // Save the recipe to the database or shared preferences
        // For simplicity, we will use a Toast to confirm the save
        Log.d("RecipeActivity", "Saved recipe: ${recipe.name}")
    }
}

data class Ingredient(val name: String, val amount: String)
data class Recipe(val name: String, val ingredients: List<Ingredient>)

class IngredientAdapter(private val ingredients: List<Ingredient>) :
    RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.bind(ingredient)
    }

    override fun getItemCount(): Int = ingredients.size

    inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIngredientName: TextView = itemView.findViewById(R.id.tvIngredientName)
        private val tvIngredientAmount: TextView = itemView.findViewById(R.id.tvIngredientAmount)

        fun bind(ingredient: Ingredient) {
            tvIngredientName.text = ingredient.name
            tvIngredientAmount.text = ingredient.amount
        }
    }
}
