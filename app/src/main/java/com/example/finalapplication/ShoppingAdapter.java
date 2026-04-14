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

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    private List<ShoppingItem> items;
    private boolean isParent;
    private ShoppingListener listener;

    public interface ShoppingListener {
        void onCheckedChanged(ShoppingItem item, boolean checked);
        void onDeleteRequested(ShoppingItem item);
    }

    public ShoppingAdapter(List<ShoppingItem> items, boolean isParent, ShoppingListener listener) {
        this.items = items;
        this.isParent = isParent;
        this.listener = listener;
    }

    public void setIsParent(boolean isParent) {
        this.isParent = isParent;
        notifyDataSetChanged();
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

        // Strikethrough and grey out when item is bought
        if (item.isChecked()) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setTextColor(0xFFAAAAAA);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setTextColor(0xFF333333);
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

        holder.checkBox.setOnClickListener(v -> {
            boolean newChecked = holder.checkBox.isChecked();
            item.setChecked(newChecked);
            if (listener != null) listener.onCheckedChanged(item, newChecked);
        });

        // Long press to delete — parents only
        holder.itemView.setOnLongClickListener(v -> {
            if (isParent && listener != null) {
                listener.onDeleteRequested(item);
            }
            return true;
        });
    }

    public void updateItems(List<ShoppingItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String oldId = items.get(oldPos).getId();
                String newId = newItems.get(newPos).getId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                ShoppingItem o = items.get(oldPos);
                ShoppingItem n = newItems.get(newPos);
                return o.isChecked() == n.isChecked()
                        && strEq(o.getName(), n.getName())
                        && strEq(o.getCreatedBy(), n.getCreatedBy())
                        && strEq(o.getAssignedToName(), n.getAssignedToName());
            }

            private boolean strEq(String a, String b) {
                return a == null ? b == null : a.equals(b);
            }
        });
        items.clear();
        items.addAll(newItems);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCreatedBy, tvAssignedTo;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvCreatedBy = itemView.findViewById(R.id.tvCreatedBy);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            checkBox = itemView.findViewById(R.id.checkItem);
        }
    }
}
