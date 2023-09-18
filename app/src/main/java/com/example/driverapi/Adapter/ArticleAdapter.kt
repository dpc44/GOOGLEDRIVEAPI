package com.example.driverapi.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.driverapi.Data.ArticleData
import com.example.driverapi.databinding.ActivityMainBinding
import com.example.driverapi.databinding.LayoutItemBinding

class ArticleAdapter(val context: Context, val itemList: MutableList<ArticleData>): RecyclerView.Adapter<ArticleAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(val binding: LayoutItemBinding ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
        val binding = LayoutItemBinding.inflate(view, parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val binding = holder.binding
        val currentItem = itemList[position]

        holder.itemView.apply {
            binding.txtTenArticle.text = currentItem.Title
            Glide.with(holder.itemView)
                .load(currentItem.Image)
                .into(binding.imgArticle)
        }
    }
}