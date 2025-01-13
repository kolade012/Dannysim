package com.example.dannysim.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dannysim.R;
import com.example.dannysim.models.Entry;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.EntryViewHolder> {
    private List<Entry> entries;
    private OnEntryClickListener listener;

    public interface OnEntryClickListener {
        void onEntryClick(Entry entry);
    }

    public EntriesAdapter(List<Entry> entries, OnEntryClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entries.get(position);

        // Set the date
        holder.tvDate.setText(entry.getDate());

        // Set the control number
        holder.tvControlNumber.setText(String.format("Control #: %s", entry.getControlNumber()));

        // Set the entry type with appropriate background color
        holder.tvEntryType.setText(entry.getEntryType());
        int backgroundColorRes;

        switch (entry.getEntryType().toLowerCase()) {
            case "stock received":
                backgroundColorRes = R.color.green_color; // Assuming you have green_color defined in your colors.xml
                break;
            case "sales":
                backgroundColorRes = R.color.red_color; // Assuming you have red_color defined in your colors.xml
                break;
            case "stock out":
                backgroundColorRes = R.color.red_color; // Assuming you have red_color defined in your colors.xml
                break;
            default:
                backgroundColorRes = R.color.gray; // Or any other default color
        }

        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.context, backgroundColorRes));

        // Set the driver name
        holder.tvDriver.setText(entry.getDriver());

        // Set the first product info
        String productInfo = String.format("%s - %d units",
                entry.getFirstProductName(),
                entry.getFirstProductSoldQuantity());
        holder.tvProductInfo.setText(productInfo);

        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvDate;
        TextView tvControlNumber;
        TextView tvEntryType;
        TextView tvDriver;
        TextView tvProductInfo;
        private Context context; // Add context field

        EntryViewHolder(View itemView, Context context) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvDate = itemView.findViewById(R.id.tvDate);
            tvControlNumber = itemView.findViewById(R.id.tvControlNumber);
            tvEntryType = itemView.findViewById(R.id.tvEntryType);
            tvDriver = itemView.findViewById(R.id.tvDriver);
            tvProductInfo = itemView.findViewById(R.id.tvProductInfo);
            this.context = this.context; // Assign the passed context
        }
    }
}