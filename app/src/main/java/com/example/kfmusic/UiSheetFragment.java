package com.example.kfmusic;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class UiSheetFragment extends Fragment {
    public static final String ARG_MODE = "mode";
    public static final String ARG_TITLE = "title";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_ITEMS = "items";
    public static final String ARG_INPUT_HINT = "input_hint";
    public static final String ARG_PRIMARY = "primary";
    public static final String ARG_SECONDARY = "secondary";
    public static final String ARG_RESULT_KEY = "result_key";

    public static final int MODE_LIST = 0;
    public static final int MODE_INPUT = 1;

    public static final String RESULT_ACTION = "action";
    public static final String RESULT_INDEX = "index";
    public static final String RESULT_LABEL = "label";
    public static final String RESULT_TEXT = "text";

    public static UiSheetFragment newList(String title, String message, ArrayList<String> items, String primary, String secondary, String resultKey) {
        UiSheetFragment fragment = new UiSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_LIST);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putStringArrayList(ARG_ITEMS, items);
        args.putString(ARG_PRIMARY, primary);
        args.putString(ARG_SECONDARY, secondary);
        args.putString(ARG_RESULT_KEY, resultKey);
        fragment.setArguments(args);
        return fragment;
    }

    public static UiSheetFragment newInput(String title, String message, String hint, String primary, String secondary, String resultKey) {
        UiSheetFragment fragment = new UiSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_INPUT);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_INPUT_HINT, hint);
        args.putString(ARG_PRIMARY, primary);
        args.putString(ARG_SECONDARY, secondary);
        args.putString(ARG_RESULT_KEY, resultKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ui_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        TextView tvMessage = view.findViewById(R.id.tvSheetMessage);
        EditText etInput = view.findViewById(R.id.etSheetInput);
        LinearLayout itemsContainer = view.findViewById(R.id.itemsContainer);
        Button btnPrimary = view.findViewById(R.id.btnSheetPrimary);
        Button btnSecondary = view.findViewById(R.id.btnSheetSecondary);

        Bundle args = getArguments();
        int mode = args != null ? args.getInt(ARG_MODE, MODE_LIST) : MODE_LIST;
        String resultKey = args != null ? args.getString(ARG_RESULT_KEY, "ui_sheet_result") : "ui_sheet_result";

        tvTitle.setText(args != null ? args.getString(ARG_TITLE, "") : "");
        tvMessage.setText(args != null ? args.getString(ARG_MESSAGE, "") : "");

        btnPrimary.setText(args != null ? args.getString(ARG_PRIMARY, getString(R.string.close)) : getString(R.string.close));
        String secondaryText = args != null ? args.getString(ARG_SECONDARY, null) : null;
        if (!TextUtils.isEmpty(secondaryText)) {
            btnSecondary.setText(secondaryText);
            btnSecondary.setVisibility(View.VISIBLE);
        }

        if (mode == MODE_INPUT) {
            etInput.setVisibility(View.VISIBLE);
            etInput.setHint(args != null ? args.getString(ARG_INPUT_HINT, "") : "");
            itemsContainer.setVisibility(View.GONE);
        } else {
            etInput.setVisibility(View.GONE);
            itemsContainer.setVisibility(View.VISIBLE);
            ArrayList<String> items = args != null ? args.getStringArrayList(ARG_ITEMS) : null;
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    final int index = i;
                    final String label = items.get(i);
                    Button itemButton = new Button(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
                    itemButton.setLayoutParams(lp);
                    itemButton.setText(label);
                    itemButton.setAllCaps(false);
                    itemButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.card_soft)));
                    itemButton.setTextColor(getResources().getColor(R.color.text_main));
                    itemButton.setOnClickListener(v -> {
                        Bundle result = new Bundle();
                        result.putString(RESULT_ACTION, "item");
                        result.putInt(RESULT_INDEX, index);
                        result.putString(RESULT_LABEL, label);
                        getParentFragmentManager().setFragmentResult(resultKey, result);
                        dismiss();
                    });
                    itemsContainer.addView(itemButton);
                }
            }
        }

        btnPrimary.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_ACTION, "primary");
            if (mode == MODE_INPUT) {
                result.putString(RESULT_TEXT, etInput.getText().toString().trim());
            }
            getParentFragmentManager().setFragmentResult(resultKey, result);
            dismiss();
        });

        btnSecondary.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_ACTION, "secondary");
            if (mode == MODE_INPUT) {
                result.putString(RESULT_TEXT, etInput.getText().toString().trim());
            }
            getParentFragmentManager().setFragmentResult(resultKey, result);
            dismiss();
        });

        view.findViewById(R.id.sheetCard).setOnClickListener(v -> {});
        view.setOnClickListener(v -> dismiss());
    }

    private void dismiss() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }
}
