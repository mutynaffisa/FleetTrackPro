package com.industri.fleettrack.ui.manifest

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.industri.fleettrack.R
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import java.text.NumberFormat
import java.util.Locale

class DeliveryListAdapter(
    private val onDeliverClicked: (DeliveryOrderEntity) -> Unit
) : ListAdapter<DeliveryOrderEntity, DeliveryListAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTrackingNo: TextView = itemView.findViewById(R.id.tvTrackingNo)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvCodAmount: TextView = itemView.findViewById(R.id.tvCodAmount)
        val btnActionDeliver: MaterialButton = itemView.findViewById(R.id.btnActionDeliver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = getItem(position)
        with(holder) {
            tvTrackingNo.text = item.trackingNumber
            tvRecipient.text = "${item.recipientName} (${item.recipientPhone})"
            tvAddress.text = item.destinationAddress
            tvCodAmount.text = if (item.codAmount > 0) "COD: ${rupiahFormat.format(item.codAmount)}" else "Non-COD (Lunas)"
            tvStatusBadge.text = item.deliveryStatus

            when (item.deliveryStatus) {
                "DELIVERED" -> {
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                    tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                    btnActionDeliver.isEnabled = false
                    btnActionDeliver.text = "Selesai ✓"
                    btnActionDeliver.setBackgroundColor(Color.parseColor("#E2E8F0"))
                    btnActionDeliver.setTextColor(Color.parseColor("#94A3B8"))
                }
                "IN_DELIVERY" -> {
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#E0F2FE"))
                    tvStatusBadge.setTextColor(Color.parseColor("#0369A1"))
                    btnActionDeliver.isEnabled = true
                    btnActionDeliver.text = "Tandai Terkirim"
                    btnActionDeliver.setBackgroundColor(Color.parseColor("#2563EB"))
                    btnActionDeliver.setTextColor(Color.WHITE)
                }
                else -> {
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
                    btnActionDeliver.isEnabled = true
                    btnActionDeliver.text = "Tandai Terkirim"
                    btnActionDeliver.setBackgroundColor(Color.parseColor("#2563EB"))
                    btnActionDeliver.setTextColor(Color.WHITE)
                }
            }

            btnActionDeliver.setOnClickListener {
                onDeliverClicked(item)
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<DeliveryOrderEntity>() {
        override fun areItemsTheSame(oldItem: DeliveryOrderEntity, newItem: DeliveryOrderEntity): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: DeliveryOrderEntity, newItem: DeliveryOrderEntity): Boolean {
            return oldItem == newItem
        }
    }
}