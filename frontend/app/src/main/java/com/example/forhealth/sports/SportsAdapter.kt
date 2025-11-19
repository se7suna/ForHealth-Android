package com.example.forhealth.sports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.forhealth.R
import com.example.forhealth.model.SearchSportsResponse

class SportsAdapter(
    private val onSelectClick: (SearchSportsResponse) -> Unit
) : RecyclerView.Adapter<SportsAdapter.SportsViewHolder>() {

    private var sportsList = listOf<SearchSportsResponse>()

    fun submitList(list: List<SearchSportsResponse>) {
        sportsList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SportsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sports, parent, false)
        return SportsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SportsViewHolder, position: Int) {
        holder.bind(sportsList[position])
    }

    override fun getItemCount(): Int = sportsList.size

    inner class SportsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSportType: TextView = itemView.findViewById(R.id.tvSportType)
        private val tvSportDesc: TextView = itemView.findViewById(R.id.tvSportDesc)
        private val btnSelect: Button = itemView.findViewById(R.id.btnSelect)

        fun bind(sport: SearchSportsResponse) {
            tvSportType.text = sport.sportType ?: "未知运动"
            
            val descParts = mutableListOf<String>()
            if (sport.mets != null) {
                descParts.add("METs: ${sport.mets}")
            }
            if (!sport.describe.isNullOrBlank()) {
                descParts.add(sport.describe)
            }
            tvSportDesc.text = descParts.joinToString(" | ")

            btnSelect.setOnClickListener {
                onSelectClick(sport)
            }
        }
    }
}

