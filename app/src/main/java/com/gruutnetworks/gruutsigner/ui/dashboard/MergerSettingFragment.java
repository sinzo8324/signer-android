package com.gruutnetworks.gruutsigner.ui.dashboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;


public class MergerSettingFragment extends DialogFragment {
    private String[] mergersID;
    DashboardViewModel viewModel;
    DashboardViewModel.MergerNum merger;

    public static MergerSettingFragment newInstance(String[] mergersID, DashboardViewModel viewModel, DashboardViewModel.MergerNum merger) {
        MergerSettingFragment fragment = new MergerSettingFragment();
        fragment.viewModel = viewModel;
        fragment.merger = merger;

        Bundle args = new Bundle();
        args.putStringArray("MERGER_IDs", mergersID);
        fragment.setArguments(args);

        return fragment;
    }

    public MergerSettingFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String[] arg = getArguments().getStringArray("MERGER_IDs");
            this.mergersID = arg;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<String> ListItems = new ArrayList<>();
        for(int i = 0; i < mergersID.length; i++){
            ListItems.add(mergersID[i]);
        }
        final CharSequence[] items =  ListItems.toArray(new String[ ListItems.size()]);

        final List SelectedItems  = new ArrayList();
        int defaultItem = 0;
        SelectedItems.add(defaultItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Select Merger");
        builder.setSingleChoiceItems(items, defaultItem,
                (dialog, which) -> {
                    SelectedItems.clear();
                    SelectedItems.add(which);
                });
        builder.setPositiveButton("OK",
                (dialog, which) -> {
                    String msg = "";
                    if(!SelectedItems.isEmpty()) {
                        int idx = (int) SelectedItems.get(0);
                        msg = ListItems.get(idx);
                        switch (merger) {
                            case MERGER_1:
                                viewModel.setTargetMerger1(mergersID[idx]);
                                viewModel.refreshMerger1();
                                break;
                            case MERGER_2:
                                viewModel.setTargetMerger2(mergersID[idx]);
                                viewModel.refreshMerger2();
                                break;
                        }
                    }

                    Toast.makeText(getActivity(),
                            "Items Selected.\n" + msg, Toast.LENGTH_LONG).show();
                });
        return builder.create();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
