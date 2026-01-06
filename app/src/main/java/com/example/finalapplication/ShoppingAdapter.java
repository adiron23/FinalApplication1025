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

    // הגדרת ממשק (Interface) כדי שה-Activity יוכל להגיב ללחיצה
    public interface OnItemCheckedListener {
        void onItemChecked(ShoppingItem item);
    }

    // בנאי (Constructor)
    public ShoppingAdapter(List<ShoppingItem> items, OnItemCheckedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // קישור לקובץ ה-XML של השורה הבודדת
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        // הצגת שם המוצר
        holder.tvName.setText(item.getName());

        // איפוס הצ'קבוקס (שלא יישאר מסומן מפריטים קודמים בזיכרון)
        holder.checkBox.setChecked(false);

        // הגדרת פעולה בעת לחיצה על הצ'קבוקס
        holder.checkBox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemChecked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // מחלקה פנימית שמחזיקה את הרכיבים של כל שורה
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName); // חייב להיות קיים ב-item_shopping.xml
            checkBox = itemView.findViewById(R.id.checkItem); // חייב להיות קיים ב-item_shopping.xml
        }
    }
}