package com.my.servicetesting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.my.servicetesting.db.LocationData

class LocationListAdapter (private val context: Context, private val locations: List<LocationData>?) : RecyclerView.Adapter<LocationListAdapter.ViewHolder>(){

    override fun onCreateViewHolder(viewGroup: ViewGroup, index: Int): ViewHolder {
        val rootView = LayoutInflater.from(viewGroup.context).inflate(R.layout.recyclerview_item, viewGroup, false)
        return ViewHolder(rootView)
    }

    override fun getItemCount(): Int {
            return locations?.size!!
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, index: Int) {
        viewHolder.messageTV.text = locations?.get(index)?.location
        viewHolder.details.text=locations?.get(index)?.dateTime+" Battery: "+locations?.get(index)?.battery+" Id :"+locations?.get(index)?.id
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var messageTV: TextView = itemView.findViewById(R.id.textView) as TextView
        var details:TextView=itemView.findViewById(R.id.details)as TextView
    }

}