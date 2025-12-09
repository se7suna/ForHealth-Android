package com.example.forhealth.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.decode.SvgDecoder
import com.example.forhealth.R
import com.example.forhealth.models.ExerciseItem
import com.example.forhealth.models.SelectedExerciseItem
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ExerciseListAdapter(
    private val exerciseList: List<ExerciseItem>,
    private val selectedItems: List<SelectedExerciseItem>,
    private val onAddClick: (ExerciseItem) -> Unit
) : RecyclerView.Adapter<ExerciseListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivExerciseImage: ImageView = itemView.findViewById(R.id.ivExerciseImage)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseInfo: TextView = itemView.findViewById(R.id.tvExerciseInfo)
        val fabAdd: FloatingActionButton = itemView.findViewById(R.id.fabAdd)
        val layoutDuration: ViewGroup = itemView.findViewById(R.id.layoutDuration)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val fabAddMore: FloatingActionButton = itemView.findViewById(R.id.fabAddMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = exerciseList[position]
        val selected = selectedItems.find { it.exerciseItem.id == exercise.id }

        // 加载图片
        var image_url = exercise.image.replace("127.0.0.1", "10.0.2.2")
        holder.ivExerciseImage.load(image_url) {
            decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
            placeholder(R.color.slate_100)
            error(R.color.slate_100)
        }

        // 设置名称和信息
        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseInfo.text = "~${exercise.caloriesPerUnit.toInt()} kcal/min"

        // 根据是否已选择显示不同的UI
        if (selected != null) {
            holder.fabAdd.visibility = View.GONE
            holder.layoutDuration.visibility = View.VISIBLE
            holder.tvDuration.text = "${selected.count.toInt()} min"
            holder.fabAddMore.setOnClickListener { onAddClick(exercise) }
        } else {
            holder.fabAdd.visibility = View.VISIBLE
            holder.layoutDuration.visibility = View.GONE
            holder.fabAdd.setOnClickListener { onAddClick(exercise) }
        }
    }

    override fun getItemCount() = exerciseList.size
}

