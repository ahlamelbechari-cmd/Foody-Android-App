package com.imadev.foody.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.imadev.foody.R
import com.imadev.foody.databinding.ItemRowOrderLayoutBinding
import com.imadev.foody.model.Meal
import com.imadev.foody.utils.CounterView
import com.imadev.foody.utils.loadFromUrl


private const val TAG = "CartAdapter"

class CartAdapter(
    private var meals: MutableList<Meal> = mutableListOf(),
    private val isEditable: Boolean = true
) : RecyclerView.Adapter<CartAdapter.ViewHolder>(),
    CounterView.OnCountChangeListener {


    private var listener: ((Int) -> Unit)? = null


    class ViewHolder(private val binding: ItemRowOrderLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context: Context = binding.root.context
        val counterView = binding.counter

        fun bind(meal: Meal, isEditable: Boolean) {

            with(binding) {
                foodImg.loadFromUrl(context,meal.image)
                foodTitle.text = meal.name
                foodPrice.text = context.resources.getString(R.string.price, meal.price.toString())
                
                if(meal.quantity == 0) meal.quantity = 1
                
                counter.setFoodModel(meal)
                counter.setQuantity(meal.quantity)
                
                if (!isEditable) {
                    // In read-only mode, we hide the plus/minus buttons if possible 
                    // or just disable them. For now, let's just make it non-clickable if we can.
                    counter.findViewById<View>(R.id.plus).visibility = View.GONE
                    counter.findViewById<View>(R.id.minus).visibility = View.GONE
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRowOrderLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = meals[position]
        holder.bind(meal, isEditable)
        
        if (isEditable) {
            holder.counterView.addOnCountChangeListener(object : CounterView.OnCountChangeListener {
                override fun onCountChange(count: Int) {
                    meal.quantity = count
                    listener?.invoke(count)
                }
            })
        }
    }

    override fun getItemCount(): Int = meals.size


    fun addOnCountChanged(listener: (Int) -> Unit) {
        this.listener = listener
    }


    override fun onCountChange(count: Int) {
    }
}