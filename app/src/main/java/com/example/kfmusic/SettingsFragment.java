package com.example.kfmusic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.utils.AppSettings;

public class SettingsFragment extends Fragment {

    private ImageButton btnSettingsBack;
    private TextView tvSongCount;
    private TextView tvLibrarySize;
    private RadioGroup rgLanguage;
    private RadioGroup rgTheme;
    private boolean bindingState = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        btnSettingsBack = root.findViewById(R.id.btnSettingsBack);
        tvSongCount = root.findViewById(R.id.tvSongCount);
        tvLibrarySize = root.findViewById(R.id.tvLibrarySize);
        rgLanguage = root.findViewById(R.id.rgLanguage);
        rgTheme = root.findViewById(R.id.rgTheme);

        btnSettingsBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        setupListeners();
        bindCurrentSettings();
        updateSongCount();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindCurrentSettings();
        updateSongCount();
    }

    private void setupListeners() {
        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingState || checkedId == View.NO_ID) return;
            String language = AppSettings.LANGUAGE_SYSTEM;
            if (checkedId == R.id.rbLanguageEnglish) {
                language = AppSettings.LANGUAGE_EN;
            } else if (checkedId == R.id.rbLanguageTurkish) {
                language = AppSettings.LANGUAGE_TR;
            }
            AppSettings.setLanguage(requireContext(), language);
            requireActivity().recreate();
        });

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingState || checkedId == View.NO_ID) return;
            String theme = AppSettings.THEME_SYSTEM;
            if (checkedId == R.id.rbThemeDark) {
                theme = AppSettings.THEME_DARK;
            } else if (checkedId == R.id.rbThemeLight) {
                theme = AppSettings.THEME_LIGHT;
            }
            AppSettings.setTheme(requireContext(), theme);
        });
    }

    private void bindCurrentSettings() {
        bindingState = true;

        String language = AppSettings.getLanguage(requireContext());
        if (AppSettings.LANGUAGE_EN.equals(language)) {
            rgLanguage.check(R.id.rbLanguageEnglish);
        } else if (AppSettings.LANGUAGE_TR.equals(language)) {
            rgLanguage.check(R.id.rbLanguageTurkish);
        } else {
            rgLanguage.check(R.id.rbLanguageSystem);
        }

        String theme = AppSettings.getTheme(requireContext());
        if (AppSettings.THEME_LIGHT.equals(theme)) {
            rgTheme.check(R.id.rbThemeLight);
        } else if (AppSettings.THEME_DARK.equals(theme)) {
            rgTheme.check(R.id.rbThemeDark);
        } else {
            rgTheme.check(R.id.rbThemeSystem);
        }

        bindingState = false;
    }

    private void updateSongCount() {
        MusicRepository repo = new MusicRepository(requireContext());
        int count = repo.getCachedSongs().size();
        tvSongCount.setText(getString(count == 1 ? R.string.song_count_one : R.string.song_count_format, count));
        tvLibrarySize.setText(repo.getTotalLibrarySizeDisplay());
    }
}
