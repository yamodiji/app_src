package com.appdrawer.fast.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appdrawer.fast.R
import com.appdrawer.fast.models.AppInfo

class FavoriteAppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, FavoriteAppAdapter.FavoriteAppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_app, parent, false)
        return FavoriteAppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FavoriteAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FavoriteAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.appName)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAppClick(getItem(position))
                }
            }
            
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAppLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }
        
        fun bind(app: AppInfo) {
            nameTextView.text = app.appName
            
            // Set app icon
            if (app.icon != null) {
                iconImageView.setImageDrawable(app.icon)
            } else {
                iconImageView.setImageResource(R.drawable.ic_default_app)
            }
            
            // Always show favorite icon for this adapter
            favoriteIcon.visibility = View.VISIBLE
            
            // Apply alias if available
            if (!app.alias.isNullOrBlank()) {
                nameTextView.text = app.alias
            }
        }
    }
    
    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
} 