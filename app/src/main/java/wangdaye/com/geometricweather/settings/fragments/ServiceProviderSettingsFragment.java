package wangdaye.com.geometricweather.settings.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import wangdaye.com.geometricweather.BuildConfig;
import wangdaye.com.geometricweather.GeometricWeather;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.common.basic.models.Location;
import wangdaye.com.geometricweather.common.basic.models.options.provider.LocationProvider;
import wangdaye.com.geometricweather.common.basic.models.options.provider.WeatherSource;
import wangdaye.com.geometricweather.db.DatabaseHelper;
import wangdaye.com.geometricweather.common.utils.helpers.SnackbarHelper;
import wangdaye.com.geometricweather.settings.SettingsManager;

/**
 * Service provider settings fragment.
 * */

public class ServiceProviderSettingsFragment extends AbstractSettingsFragment {

    private @Nullable OnWeatherSourceChangedListener mListener;

    public interface OnWeatherSourceChangedListener {
        void onWeatherSourceChanged(Location location);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.perference_service_provider);
        initPreferences();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // do nothing.
    }

    private void initPreferences() {
        // weather source.
        Preference weatherSource = findPreference(getString(R.string.key_weather_source));
        weatherSource.setSummary(getSettingsOptionManager().getWeatherSource().getSourceName(requireContext()));
        weatherSource.setOnPreferenceChangeListener((preference, newValue) -> {
            WeatherSource source = WeatherSource.getInstance((String) newValue);

            preference.setSummary(source.getSourceName(requireContext()));

            List<Location> locationList = DatabaseHelper.getInstance(requireActivity()).readLocationList();
            for (int i = 0; i < locationList.size(); i ++) {
                Location src = locationList.get(i);
                if (src.isCurrentPosition()) {
                    if (src.getWeatherSource() != null) {
                        DatabaseHelper.getInstance(requireActivity()).deleteWeather(src);
                    }
                    locationList.set(i, new Location(src, source));
                    if (mListener != null) {
                        mListener.onWeatherSourceChanged(locationList.get(i));
                    }
                    break;
                }
            }
            DatabaseHelper.getInstance(requireActivity()).writeLocationList(locationList);
            return true;
        });

        // location source.
        ListPreference locationService = findPreference(getString(R.string.key_location_service));
        if (getBuildFlavor().contains("fdroid")) {
            // Remove closed source providers if building the F-Droid flavor

            // set provider as native if necessary.
            LocationProvider provider = SettingsManager.getInstance(requireContext()).getLocationProvider();
            if (provider == LocationProvider.AMAP || provider == LocationProvider.BAIDU) {
                SettingsManager.getInstance(requireContext()).setLocationProvider(LocationProvider.NATIVE);
                locationService.setValue("native");
            }

            // lock the entries and values.
            List<CharSequence> locationEntries = new ArrayList<>();
            List<CharSequence> locationValues = new ArrayList<>();
            for (int i = 0; i < locationService.getEntries().length; ++i) {
                if (LocationProvider.getInstance((String) locationService.getEntryValues()[i]) != LocationProvider.AMAP
                        && LocationProvider.getInstance((String) locationService.getEntryValues()[i]) != LocationProvider.BAIDU) {
                    locationEntries.add(locationService.getEntries()[i]);
                    locationValues.add(locationService.getEntryValues()[i]);
                }
            }
            setListPreferenceValues(locationService, locationEntries, locationValues);
        } else if (getBuildFlavor().contains("gplay")) {
            // Remove closed source providers if building the Google Play flavor

            // set provider as native if necessary.
            LocationProvider provider = SettingsManager.getInstance(requireContext()).getLocationProvider();
            if (provider == LocationProvider.AMAP) {
                SettingsManager.getInstance(requireContext()).setLocationProvider(LocationProvider.NATIVE);
                locationService.setValue("native");
            }

            // lock the entries and values.
            List<CharSequence> locationEntries = new ArrayList<>();
            List<CharSequence> locationValues = new ArrayList<>();
            for (int i = 0; i < locationService.getEntries().length; ++i) {
                if (LocationProvider.getInstance((String) locationService.getEntryValues()[i]) != LocationProvider.AMAP) {
                    locationEntries.add(locationService.getEntries()[i]);
                    locationValues.add(locationService.getEntryValues()[i]);
                }
            }
            setListPreferenceValues(locationService, locationEntries, locationValues);
        } else if (getBuildFlavor().contains("fix")) {
            // 1. 强制当前配置为原生定位
            SettingsManager.getInstance(requireContext()).setLocationProvider(LocationProvider.NATIVE);
            locationService.setValue("native");

            // 2. 获取“原生定位”的显示名称（为了支持多语言，从原始 entries 中提取）
            CharSequence nativeEntry = "";
            CharSequence[] entryValues = locationService.getEntryValues();
            for (int i = 0; i < entryValues.length; i++) {
                if ("native".equals(entryValues[i].toString())) {
                    nativeEntry = locationService.getEntries()[i];
                    break;
                }
            }

            // 3. 直接覆盖选项列表，只留一个
            locationService.setEntries(new CharSequence[]{ nativeEntry });
            locationService.setEntryValues(new CharSequence[]{ "native" });

            // 可选：既然只有一个选项，可以把这个设置项设为不可点击，防止用户误点弹出空列表
            // locationService.setEnabled(false);
        }

        locationService.setSummary(getSettingsOptionManager().getLocationProvider().getProviderName(requireContext()));
        locationService.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(getSettingsOptionManager().getLocationProvider().getProviderName(requireContext()));
            SnackbarHelper.showSnackbar(
                    getString(R.string.feedback_restart),
                    getString(R.string.restart),
                    v -> GeometricWeather.getInstance().recreateAllActivities()
            );
            return true;
        });
    }

    private static void setListPreferenceValues(ListPreference pref, List<CharSequence> entries, List<CharSequence> values) {
        CharSequence[] contents = new CharSequence[entries.size()];
        entries.toArray(contents);
        pref.setEntries(contents);
        contents = new CharSequence[values.size()];
        values.toArray(contents);
        pref.setEntryValues(contents);
    }

    private String getBuildFlavor() {
        return BuildConfig.FLAVOR;
    }

    public void setOnWeatherSourceChangedListener(@Nullable OnWeatherSourceChangedListener l) {
        mListener = l;
    }
}