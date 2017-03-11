package jp.hazuki.yuzubrowser.speeddial.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import jp.hazuki.yuzubrowser.R;
import jp.hazuki.yuzubrowser.bookmark.view.BookmarkActivity;
import jp.hazuki.yuzubrowser.history.BrowserHistoryActivity;
import jp.hazuki.yuzubrowser.speeddial.SpeedDial;
import jp.hazuki.yuzubrowser.speeddial.WebIcon;
import jp.hazuki.yuzubrowser.utils.ImageUtils;
import jp.hazuki.yuzubrowser.utils.Logger;
import jp.hazuki.yuzubrowser.utils.appinfo.AppInfo;
import jp.hazuki.yuzubrowser.utils.appinfo.ApplicationListFragment;
import jp.hazuki.yuzubrowser.utils.appinfo.ShortCutListFragment;

public class SpeedDialSettingActivity extends AppCompatActivity
        implements SpeedDialEditCallBack, FragmentManager.OnBackStackChangedListener,
        SpeedDialSettingActivityFragment.OnSpeedDialAddListener, SpeedDialSettingActivityEditFragment.GoBackController,
        ApplicationListFragment.OnAppSelectListener, ShortCutListFragment.OnShortCutSelectListener {

    private static final int RESULT_REQUEST_BOOKMARK = 100;
    private static final int RESULT_REQUEST_HISTORY = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_base);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SpeedDialSettingActivityFragment(), "main")
                    .commit();
        }

        shouldDisplayHomeUp();
    }

    @Override
    public boolean goBack() {
        return getSupportFragmentManager().popBackStackImmediate();
    }

    @Override
    public void goEdit(SpeedDial speedDial) {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack("")
                .replace(R.id.container, SpeedDialSettingActivityEditFragment.newInstance(speedDial))
                .commit();
    }

    @Override
    public void addFromBookmark() {
        Intent intent = new Intent(this, BookmarkActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, RESULT_REQUEST_BOOKMARK);
    }

    @Override
    public void addFromHistory() {
        Intent intent = new Intent(this, BrowserHistoryActivity.class);
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(intent, RESULT_REQUEST_HISTORY);
    }

    @Override
    public void addFromAppList() {
        Intent target = new Intent(Intent.ACTION_MAIN);
        target.addCategory(Intent.CATEGORY_LAUNCHER);

        getSupportFragmentManager().beginTransaction()
                .addToBackStack("")
                .replace(R.id.container, ApplicationListFragment.newInstance(target))
                .commit();
    }

    @Override
    public void addFromShortCutList() {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack("")
                .replace(R.id.container, new ShortCutListFragment())
                .commit();
    }

    @Override
    public void onShortCutSelected(Intent data) {
        goBack();
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (icon == null) {
            Intent.ShortcutIconResource iconRes = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (iconRes != null) {
                try {
                    Resources foreignResources = getPackageManager().getResourcesForApplication(iconRes.packageName);
                    int id = foreignResources.getIdentifier(iconRes.resourceName, null, null);
                    icon = BitmapFactory.decodeResource(foreignResources, id);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (icon == null) {
                try {
                    ComponentName component = intent.getComponent();
                    if (component != null) {
                        Drawable drawable = getPackageManager().getApplicationIcon(component.getPackageName());
                        icon = ImageUtils.getBitmap(drawable);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        WebIcon webIcon = null;
        if (icon != null)
            webIcon = WebIcon.createIcon(icon);

        SpeedDial speedDial = new SpeedDial(intent.toUri(Intent.URI_INTENT_SCHEME), name, webIcon, false);
        goEdit(speedDial);
    }

    @Override
    public void onAppSelected(int type, AppInfo appInfo) {
        goBack();
        Intent intent = new Intent();
        intent.setClassName(appInfo.getPackageName(), appInfo.getClassName());
        WebIcon webIcon = WebIcon.createIcon(ImageUtils.getBitmap(appInfo.getIcon()));
        SpeedDial speedDial = new SpeedDial(intent.toUri(Intent.URI_INTENT_SCHEME), appInfo.getAppName(), webIcon, false);
        goEdit(speedDial);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_REQUEST_BOOKMARK: {
                if (resultCode != Activity.RESULT_OK || data == null) break;
                String title = data.getStringExtra(Intent.EXTRA_TITLE);
                String url = data.getStringExtra(Intent.EXTRA_TEXT);
                goEdit(new SpeedDial(url, title));
                break;
            }
            case RESULT_REQUEST_HISTORY: {
                if (resultCode != Activity.RESULT_OK || data == null) break;
                String title = data.getStringExtra(Intent.EXTRA_TITLE);
                String url = data.getStringExtra(Intent.EXTRA_TEXT);
                byte[] icon = data.getByteArrayExtra(Intent.EXTRA_STREAM);
                SpeedDial speedDial;
                if (icon == null) {
                    speedDial = new SpeedDial(url, title);
                } else {
                    speedDial = new SpeedDial(url, title, new WebIcon(icon), true);
                }
                goEdit(speedDial);
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onEdited(SpeedDial speedDial) {
        goBack();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("main");
        if (fragment instanceof SpeedDialEditCallBack) {
            ((SpeedDialEditCallBack) fragment).onEdited(speedDial);
        } else {
            Logger.d("fragment", "null");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void shouldDisplayHomeUp() {
        boolean canback = getFragmentManager().getBackStackEntryCount() == 0;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(canback);
        }
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }
}
