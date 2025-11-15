package com.example.outpick.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SortBottomSheetDialog extends BottomSheetDialogFragment {

    // Callback interface for passing the selected option back
    public interface OnSortAppliedListener {
        void onSortApplied(String sortOption);
    }

    private OnSortAppliedListener listener;
    private String currentSelection; // Stores last selected option

    // Set listener
    public void setOnSortAppliedListener(OnSortAppliedListener listener) {
        this.listener = listener;
    }

    // Set current selected option (from activity)
    public void setCurrentSelection(String selection) {
        this.currentSelection = selection;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Inflate layout
        View view = LayoutInflater.from(getContext()).inflate(R.layout.sort_bottom_sheet, null);
        dialog.setContentView(view);

        // Views
        RadioGroup rgSortOptions = view.findViewById(R.id.rgSortOptions);
        Button btnApply = view.findViewById(R.id.btnApplySort);
        ImageView closeButton = view.findViewById(R.id.closeButton);

        // Preselect previously chosen radio button
        if (currentSelection != null) {
            for (int i = 0; i < rgSortOptions.getChildCount(); i++) {
                View child = rgSortOptions.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton rb = (RadioButton) child;
                    if (rb.getText().toString().equalsIgnoreCase(currentSelection)) {
                        rb.setChecked(true); // visually colors the circle
                        break;
                    }
                }
            }
        }

        // Close button
        closeButton.setOnClickListener(v -> dismiss());

        // Apply button
        btnApply.setOnClickListener(v -> {
            int selectedId = rgSortOptions.getCheckedRadioButtonId();
            if (selectedId != -1 && listener != null) {
                RadioButton selectedButton = view.findViewById(selectedId);
                String selectedText = selectedButton.getText().toString();
                listener.onSortApplied(selectedText); // ✅ pass selection back
            }
            dismiss();
        });

        // Allow dismiss when tapping outside
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }
}
