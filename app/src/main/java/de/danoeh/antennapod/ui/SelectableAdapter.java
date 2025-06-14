package de.danoeh.antennapod.ui;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;

import java.util.HashSet;

/**
 * Used by Recyclerviews that need to provide ability to select items.
 */
public abstract class SelectableAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    public static final int COUNT_AUTOMATICALLY = -1;
    private ActionMode actionMode;
    private final HashSet<Long> selectedIds = new HashSet<>();
    private final FragmentActivity activity;
    private OnSelectModeListener onSelectModeListener;
    protected boolean shouldSelectLazyLoadedItems = false;
    private int totalNumberOfItems = COUNT_AUTOMATICALLY;

    public SelectableAdapter(FragmentActivity activity) {
        this.activity = activity;
    }

    public void startSelectMode(int pos) {
        if (inActionMode()) {
            endSelectMode();
        }

        shouldSelectLazyLoadedItems = false;
        selectedIds.clear();
        selectedIds.add(getItemId(pos));

        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (actionMode != null) {
                    actionMode.finish();
                } else {
                    this.remove();
                }
            }
        };

        actionMode = activity.startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multi_select_options, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                onSelectedItemsUpdated();
                toggleSelectAllIcon(menu.findItem(R.id.select_toggle), false);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.select_toggle) {
                    boolean selectAll = selectedIds.size() != getItemCount();
                    shouldSelectLazyLoadedItems = selectAll;
                    setSelected(0, getItemCount(), selectAll);
                    toggleSelectAllIcon(item, selectAll);
                    onSelectedItemsUpdated();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                shouldSelectLazyLoadedItems = false;
                selectedIds.clear();
                callOnEndSelectMode();
                notifyDataSetChanged();
                backPressedCallback.remove();
            }
        });
        activity.getOnBackPressedDispatcher().addCallback(activity, backPressedCallback);
        onSelectedItemsUpdated();

        if (onSelectModeListener != null) {
            onSelectModeListener.onStartSelectMode();
        }
        notifyDataSetChanged();
    }

    /**
     * End action mode if currently in select mode, otherwise do nothing
     */
    public void endSelectMode() {
        if (inActionMode()) {
            actionMode.finish();
            actionMode = null;
            callOnEndSelectMode();
        }
    }

    public boolean isSelected(int pos) {
        return selectedIds.contains(getItemId(pos));
    }

    /**
     * Set the selected state of item at given position
     *
     * @param pos      the position to select
     * @param selected true for selected state and false for unselected
     */
    public void setSelected(int pos, boolean selected) {
        if (selected) {
            selectedIds.add(getItemId(pos));
        } else {
            selectedIds.remove(getItemId(pos));
        }
        onSelectedItemsUpdated();
    }

    /**
     * Set the selected state of item for a given range
     *
     * @param startPos start position of range, inclusive
     * @param endPos   end position of range, inclusive
     * @param selected indicates the selection state
     * @throws IllegalArgumentException if start and end positions are not valid
     */
    public void setSelected(int startPos, int endPos, boolean selected) throws IllegalArgumentException {
        for (int i = startPos; i < endPos && i < getItemCount(); i++) {
            setSelected(i, selected);
        }
        notifyItemRangeChanged(startPos, (endPos - startPos));
    }

    protected void toggleSelection(int pos) {
        setSelected(pos, !isSelected(pos));
        notifyItemChanged(pos);

        if (selectedIds.isEmpty()) {
            endSelectMode();
        }
    }

    public boolean inActionMode() {
        return actionMode != null;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    private void toggleSelectAllIcon(MenuItem selectAllItem, boolean allSelected) {
        if (allSelected) {
            selectAllItem.setIcon(R.drawable.ic_select_none);
            selectAllItem.setTitle(R.string.deselect_all_label);
        } else {
            selectAllItem.setIcon(R.drawable.ic_select_all);
            selectAllItem.setTitle(R.string.select_all_label);
        }
    }

    protected void onSelectedItemsUpdated() {
        if (actionMode == null) {
            return;
        }
        int totalCount = getItemCount();
        int selectedCount = selectedIds.size();
        if (totalNumberOfItems != COUNT_AUTOMATICALLY) {
            totalCount = totalNumberOfItems;
            if (shouldSelectLazyLoadedItems) {
                selectedCount += (totalNumberOfItems - getItemCount());
            }
        }
        actionMode.setTitle(activity.getResources()
                .getQuantityString(R.plurals.num_selected_label, selectedIds.size(),
                selectedCount, totalCount));
    }

    public void setOnSelectModeListener(OnSelectModeListener onSelectModeListener) {
        this.onSelectModeListener = onSelectModeListener;
    }

    private void callOnEndSelectMode() {
        if (onSelectModeListener != null) {
            onSelectModeListener.onEndSelectMode();
        }
    }

    public boolean shouldSelectLazyLoadedItems() {
        return shouldSelectLazyLoadedItems;
    }

    /**
     * Sets the total number of items that could be lazy-loaded.
     * Can also be set to {@link #COUNT_AUTOMATICALLY} to simply use {@link #getItemCount}
     */
    public void setTotalNumberOfItems(int totalNumberOfItems) {
        this.totalNumberOfItems = totalNumberOfItems;
    }

    public interface OnSelectModeListener {
        void onStartSelectMode();

        void onEndSelectMode();
    }
}
