package com.imadev.foody.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.databinding.ItemRowFoodHomBinding
import com.imadev.foody.model.Meal
import com.imadev.foody.utils.loadFromUrl

private const val TAG = "MealListHomeAdapter"

class MealListHomeAdapter(private val meals: ArrayList<Meal?>) :
    RecyclerView.Adapter<MealListHomeAdapter.ViewHolder>(), Filterable {


    private var onClick: ((Meal, Int) -> Unit)? = null

    // Utilisation d'une liste séparée pour le filtrage
    private var filteredMeals: ArrayList<Meal?> = ArrayList(meals)


    class ViewHolder(private val binding: ItemRowFoodHomBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(meal: Meal) {
            with(binding) {
                meal.image?.let { foodImg.loadFromUrl(context, it) }
                foodTitle.text = meal.name
                foodPrice.text = context.resources.getString(R.string.price, meal.price.toString())
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemRowFoodHomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = filteredMeals[position] ?: return
        holder.bind(meal)
        holder.itemView.setOnClickListener {
            onClick?.invoke(meal, position)
        }
    }

    override fun getItemCount(): Int = filteredMeals.size

    fun setItemClickListener(onClick: (Meal, Int) -> Unit) {
        this.onClick = onClick
    }

   

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase() ?: ""
                val filteredList = if (charString.isEmpty()) {
                    ArrayList(meals)
                } else {
                    val list = ArrayList<Meal?>()
                    meals.filter {
                        (it?.name?.lowercase()?.contains(charString) == true)
                    }.forEach {
                        list.add(it)
                    }
                    list
                }
                
                return FilterResults().apply {
                    values = filteredList
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(p0: CharSequence?, results: FilterResults?) {
                filteredMeals = if (results?.values == null) {
                    ArrayList()
                } else {
                    results.values as ArrayList<Meal?>
                }
                notifyDataSetChanged()
            }
        }
    }
}