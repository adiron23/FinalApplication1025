package com.example.finalapplication;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// אדפטר לרשימת הקניות – מציג פריטים עם צ'קבוקס, שם מוסיף, מיועד ל, ומחיקה בלחיצה ארוכה להורים
public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private List<ShoppingItem> items;
    private boolean isParent;
    private ShoppingListener listener;

    // ממשק להאזנה לאירועים: סימון/ביטול פריט ובקשת מחיקה
    public interface ShoppingListener {
        void onCheckedChanged(ShoppingItem item, boolean checked);
        void onDeleteRequested(ShoppingItem item);
    }

    // קונסטרקטור – מקבל את רשימת הפריטים, תפקיד המשתמש ומאזין לאירועים
    public ShoppingAdapter(List<ShoppingItem> items, boolean isParent, ShoppingListener listener) {
        this.items    = items;
        this.isParent = isParent;
        this.listener = listener;
    }

    // מעדכן האם המשתמש הנוכחי הוא הורה ומרענן את הרשימה
    public void setIsParent(boolean isParent) {
        this.isParent = isParent;
        notifyDataSetChanged();
    }

    // יוצר תצוגת פריט חדשה מה-layout המתאים
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    // ממלא את פרטי הפריט בתצוגה: שם (עם קו חוצה אם נקנה), מוסיף, מיועד ל, צ'קבוקס ומחיקה
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        holder.tvName.setText(item.getName());

        if (item.isChecked()) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setTextColor(0xFFAAAAAA);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setTextColor(0xFF3E2723);
        }

        String creator = (item.getCreatedBy() != null && !item.getCreatedBy().isEmpty())
                ? item.getCreatedBy() : "בן משפחה";
        holder.tvCreatedBy.setText("נוסף ע\"י: " + creator);

        String assignee = item.getAssignedToName();
        if (assignee != null && !assignee.isEmpty()) {
            holder.tvAssignedTo.setVisibility(View.VISIBLE);
            holder.tvAssignedTo.setText("מיועד ל: " + assignee);
        } else {
            holder.tvAssignedTo.setVisibility(View.GONE);
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked());

        // מעדכן את מצב הסימון של הפריט ומודיע למאזין
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newChecked = holder.checkBox.isChecked();
                item.setChecked(newChecked);
                if (listener != null) listener.onCheckedChanged(item, newChecked);
            }
        });

        // לחיצה ארוכה למחיקת פריט – זמין להורים בלבד
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isParent && listener != null) {
                    listener.onDeleteRequested(item);
                }
                return true;
            }
        });
    }

    // מעדכן את הרשימה בצורה חכמה עם DiffUtil כדי לרנדר רק את השינויים
    public void updateItems(List<ShoppingItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            // מחזיר את גודל הרשימה הישנה
            @Override public int getOldListSize() { return items.size(); }
            // מחזיר את גודל הרשימה החדשה
            @Override public int getNewListSize() { return newItems.size(); }

            // בודק האם שני פריטים מייצגים את אותו מסמך לפי ID
            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String oldId = items.get(oldPos).getId();
                String newId = newItems.get(newPos).getId();
                return oldId != null && oldId.equals(newId);
            }

            // בודק האם תוכן הפריטים זהה (סימון, שם, מוסיף, מיועד)
            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                ShoppingItem o = items.get(oldPos);
                ShoppingItem n = newItems.get(newPos);
                return o.isChecked() == n.isChecked()
                        && areStringsEqual(o.getName(),           n.getName())
                        && areStringsEqual(o.getCreatedBy(),      n.getCreatedBy())
                        && areStringsEqual(o.getAssignedToName(), n.getAssignedToName());
            }

            // פונקציית עזר להשוואת מחרוזות תוך טיפול ב-null
            private boolean areStringsEqual(String s1, String s2) {
                return s1 == null ? s2 == null : s1.equals(s2);
            }
        });
        items.clear();
        items.addAll(newItems);
        result.dispatchUpdatesTo(this);
    }

    // מחזיר את מספר הפריטים ברשימה
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // מחזיק את ה-views של פריט בודד ברשימה
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCreatedBy, tvAssignedTo;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tvItemName);
            tvCreatedBy  = itemView.findViewById(R.id.tvCreatedBy);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            checkBox     = itemView.findViewById(R.id.checkItem);
        }
    }
}