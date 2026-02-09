package com.example.finalapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private List<ShoppingItem> items;
    private OnItemCheckedListener listener;

    public interface OnItemCheckedListener {
        void onItemChecked(ShoppingItem item);
    }

    public ShoppingAdapter(List<ShoppingItem> items, OnItemCheckedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        holder.tvName.setText(item.getName());

        // ביטול המאזין לפני שינוי המצב כדי למנוע קריאות מיותרות בזמן מיחזור השורה
        holder.checkBox.setOnCheckedChangeListener(null);

        // כאן הנחתי שיש לך שדה isChecked ב-ShoppingItem.
        // אם אין לך, מומלץ להוסיף כדי שהרשימה תדע מה מצב הפריט.
        holder.checkBox.setChecked(false);

        // שימוש ב-OnClickListener עדיף כאן כדי לזהות לחיצה אמיתית של משתמש
        holder.checkBox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemChecked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            checkBox = itemView.findViewById(R.id.checkItem);
        }
    }
}