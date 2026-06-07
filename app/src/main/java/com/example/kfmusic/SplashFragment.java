package com.example.kfmusic;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kfmusic.utils.MediaScanner;

public class SplashFragment extends Fragment {

    private View permissionCard;
    private Button btnGrantPermission;
    private Button btnPermissionSettings;
    private Button btnSkipDemo;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    navigateToLibrary();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.splash_permission_denied_demo), Toast.LENGTH_SHORT).show();
                    permissionCard.setVisibility(View.VISIBLE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splash, container, false);

        permissionCard = view.findViewById(R.id.permissionCard);
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission);
        btnPermissionSettings = view.findViewById(R.id.btnPermissionSettings);
        btnSkipDemo = view.findViewById(R.id.btnSkipDemo);

        // Hide permission card initially while we check
        permissionCard.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnGrantPermission.setOnClickListener(v -> requestStoragePermission());

        btnPermissionSettings.setOnClickListener(v -> {
            try {
                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                android.net.Uri uri = android.net.Uri.fromParts("package", requireContext().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), getString(R.string.splash_settings_error), Toast.LENGTH_SHORT).show();
            }
        });
        
        btnSkipDemo.setOnClickListener(v -> navigateToLibrary());

        // Delay slightly for branding, then check permissions
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                checkPermissionsAndProceed();
            }
        }, 1500);
    }

    private void checkPermissionsAndProceed() {
        if (MediaScanner.hasStoragePermission(requireContext())) {
                navigateToLibrary();
        } else {
            permissionCard.setVisibility(View.VISIBLE);
        }
    }

    private void requestStoragePermission() {
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissionLauncher.launch(permission);
    }

    private void navigateToLibrary() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.navigateTo(new LibraryFragment(), false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null && getActivity().getWindow() != null) {
            getActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }
}
