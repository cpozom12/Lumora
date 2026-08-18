package com.cpozom.mediahub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class HubActivity extends Activity {
    private GridLayout grid;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(createUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (grid != null) {
            populateGrid();
        }
    }

    private ScrollView createUi() {
        float density = getResources().getDisplayMetrics().density;
        int padding = Math.round(20f * density);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText(R.string.hub_title);
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.hub_subtitle);
        subtitle.setTextSize(16f);
        subtitle.setPadding(0, Math.round(8f * density), 0, Math.round(4f * density));
        content.addView(subtitle);

        TextView note = new TextView(this);
        note.setText(R.string.hub_note);
        note.setTextSize(13f);
        note.setPadding(0, 0, 0, Math.round(14f * density));
        content.addView(note);

        grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(true);
        content.addView(grid, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView legal = new TextView(this);
        legal.setText(R.string.third_party_notice);
        legal.setTextSize(11f);
        legal.setPadding(0, Math.round(16f * density), 0, 0);
        content.addView(legal);

        populateGrid();

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        return scroll;
    }

    private void populateGrid() {
        grid.removeAllViews();
        ProviderRegistry.Provider[] providers = ProviderRegistry.PROVIDERS;

        for (int i = 0; i < providers.length; i++) {
            ProviderRegistry.Provider provider = providers[i];
            Intent launchIntent = getLaunchIntent(provider);
            boolean installed = launchIntent != null;

            Button button = new Button(this);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER);
            button.setText(getString(provider.labelRes) + "\n" +
                getString(installed ? R.string.status_installed : R.string.status_not_installed));
            button.setEnabled(installed);
            button.setOnClickListener(view -> launch(provider));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(i / 2, 1f),
                GridLayout.spec(i % 2, 1f)
            );
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            grid.addView(button, params);
        }
    }

    private Intent getLaunchIntent(ProviderRegistry.Provider provider) {
        return getPackageManager().getLaunchIntentForPackage(provider.packageName);
    }

    private void launch(ProviderRegistry.Provider provider) {
        String label = getString(provider.labelRes);
        Intent intent = getLaunchIntent(provider);
        if (intent == null) {
            Toast.makeText(this, getString(R.string.launch_missing, label), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            startActivity(intent);
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.launch_blocked, label), Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.launch_missing, label), Toast.LENGTH_LONG).show();
        }
    }
}
