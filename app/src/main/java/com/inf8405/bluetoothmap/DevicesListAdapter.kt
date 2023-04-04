package com.inf8405.bluetoothmap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources


class DevicesListAdapter(private val context: Context, private val items: MutableList<DeviceData>) : BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): DeviceData {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].device.address.hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.device_list, parent, false)

        val item = getItem(position)

        val textView = itemView.findViewById<TextView>(R.id.textView_deviceTitle)

        textView.text = item.toString()

        textView.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            if (item.starred) AppCompatResources.getDrawable(context, R.drawable.baseline_star_24) else null,
            null
        )

        return itemView
    }

    override fun notifyDataSetChanged() {
        items.sortBy { !it.starred }

        super.notifyDataSetChanged()
    }
}
