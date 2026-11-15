package cool.dao.leme;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT = 100;
    private static final int REQUEST_IMPORT = 101;
    private static final String APP_AUTHOR = "Suger";
    private static final String REPOSITORY_URL = "https://github.com/SugerMus/dao-leme";
    private static final String[] ROUTES = {"checkin", "records", "analysis", "settings"};
    private static final String[] ROUTE_LABELS = {"打卡", "记录", "分析", "设置"};
    private static final String[] ROUTE_GLYPHS = {"✓", "▦", "▥", "⚙"};
    private static final int GREEN = Color.rgb(36, 107, 92);
    private static final int RED = Color.rgb(169, 74, 74);
    private static final int LIGHT_BG = Color.rgb(246, 247, 249);
    private static final int[] DAILY_WORD_COLORS = {0xFF286A5B, 0xFF8E6450, 0xFF586E91, 0xFF997340};
    private static final String[] DAILY_WORDS = {"记住今天，不为评判，只为看见。", "每一次记录，都是对自己的诚实。", "慢一点没关系，持续看见自己就很好。", "今天的选择，值得被好好记下。", "不必完美，只要如实记录。", "把今天交给今天，不提前定义明天。", "轻轻记录，然后继续生活。"};

    private LocalData data;
    private FrameLayout root;
    private LinearLayout contentColumn;
    private LinearLayout header;
    private FrameLayout pageHost;
    private ScrollView scrollContainer;
    private LinearLayout dock;
    private String route = "checkin";
    private String settingsPage = "overview";
    private YearMonth calendarMonth = YearMonth.now();
    private LocalDate analysisDate = LocalDate.now();
    private String analysisPeriod = "month";
    private boolean dark;
    private String exportJson;
    private String previousRoute = "checkin";
    private int systemTopInset;
    private int systemBottomInset;
    private LocalDate renderedDay = LocalDate.now();
    private AnimatorSet pageTransition;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = LocalData.load(this);
        dark = "dark".equals(data.theme) || ("system".equals(data.theme) && (getResources().getConfiguration().uiMode & 0x30) == 0x20);
        data.save(this);
        if (savedInstanceState != null) {
            route = savedInstanceState.getString("route", "checkin");
            settingsPage = savedInstanceState.getString("settingsPage", "overview");
            calendarMonth = YearMonth.parse(savedInstanceState.getString("calendarMonth", YearMonth.now().toString()));
            analysisDate = LocalDate.parse(savedInstanceState.getString("analysisDate", LocalDate.now().toString()));
            analysisPeriod = savedInstanceState.getString("analysisPeriod", "month");
            exportJson = savedInstanceState.getString("exportJson");
        }
        buildFrame();
        showRoute(route, false);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("route", route);
        state.putString("settingsPage", settingsPage);
        state.putString("calendarMonth", calendarMonth.toString());
        state.putString("analysisDate", analysisDate.toString());
        state.putString("analysisPeriod", analysisPeriod);
        state.putString("exportJson", exportJson);
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if (data == null) return;
        data.updateTodayPreference(this);
        if (!renderedDay.equals(today())) {
            renderedDay = today();
            showRoute(route, false);
        }
    }

    private int color(String role) {
        if (dark) {
            switch (role) {
                case "bg": return Color.rgb(18, 23, 25);
                case "surface": return Color.rgb(27, 34, 36);
                case "surface2": return Color.rgb(37, 46, 48);
                case "text": return Color.rgb(238, 244, 241);
                case "muted": return Color.rgb(155, 169, 165);
                case "line": return Color.rgb(48, 59, 61);
                case "accent": return Color.rgb(105, 195, 167);
                case "accentSoft": return Color.rgb(29, 64, 55);
                case "negativeSoft": return Color.rgb(74, 41, 45);
                case "warning": return Color.rgb(244, 191, 109);
                case "warningSoft": return Color.rgb(58, 45, 29);
                default: return Color.rgb(239, 148, 148);
            }
        }
        switch (role) {
            case "bg": return LIGHT_BG;
            case "surface": return Color.WHITE;
            case "surface2": return Color.rgb(238, 241, 244);
            case "text": return Color.rgb(23, 32, 42);
            case "muted": return Color.rgb(119, 129, 141);
            case "line": return Color.rgb(225, 229, 233);
            case "accent": return GREEN;
            case "accentSoft": return Color.rgb(220, 239, 233);
            case "negativeSoft": return Color.rgb(247, 226, 226);
            case "warning": return Color.rgb(141, 84, 17);
            case "warningSoft": return Color.rgb(255, 244, 224);
            default: return RED;
        }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable glassSurface() {
        int first = dark ? Color.argb(150, 35, 45, 46) : Color.argb(142, 255, 255, 255);
        int second = dark ? Color.argb(178, 24, 31, 33) : Color.argb(170, 245, 249, 247);
        GradientDrawable glass = new GradientDrawable(Orientation.TL_BR, new int[]{first, second});
        glass.setCornerRadius(dp(999));
        glass.setStroke(dp(1), dark ? Color.argb(90, 255, 255, 255) : Color.argb(180, 255, 255, 255));
        return glass;
    }

    private TextView text(String value, int size, String role, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(role));
        view.setFontFeatureSettings("kern");
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams lp(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height)); }

    private void buildFrame() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) window.setNavigationBarContrastEnforced(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | (dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));

        root = new FrameLayout(this);
        root.setBackgroundColor(color("bg"));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            systemTopInset = insets.getSystemWindowInsetTop();
            systemBottomInset = insets.getSystemWindowInsetBottom();
            updateInsets();
            return insets;
        });
        contentColumn = column();
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -1);
        root.addView(contentColumn, contentParams);

        header = row();
        contentColumn.addView(header, lp(-1, -2));

        scrollContainer = new ScrollView(this);
        scrollContainer.setClipToPadding(false);
        scrollContainer.setFillViewport(false);
        contentColumn.addView(scrollContainer, new LinearLayout.LayoutParams(-1, 0, 1));
        pageHost = new FrameLayout(this);
        pageHost.setPadding(dp(16), 0, dp(16), dp(112));
        scrollContainer.addView(pageHost, new ScrollView.LayoutParams(-1, -2));

        dock = new GlassDock(this, contentColumn, glassSurface());
        scrollContainer.setOnScrollChangeListener((view, x, y, oldX, oldY) -> dock.invalidate());
        dock.setGravity(Gravity.CENTER);
        ((GlassDock) dock).setOverlay(glassSurface());
        dock.setElevation(dp(13));
        dock.setPadding(dp(5), dp(5), dp(5), dp(5));
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockParams.setMargins(dp(14), 0, dp(14), dp(12));
        root.addView(dock, dockParams);
        dock.setTag(dockParams);
        setContentView(root);
        updateInsets();
    }

    private void buildHeader() {
        header.removeAllViews();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), systemTopInset + dp(10), dp(20), dp(14));
        LinearLayout titles = column();
        TextView date = text(today().format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)), 13, "muted", false);
        titles.addView(date, lp(-1, -2));
        TextView name = text("导了么", 29, "text", true);
        LinearLayout.LayoutParams nameLp = lp(-1, -2); nameLp.topMargin = dp(2); titles.addView(name, nameLp);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView theme = text(dark ? "☼" : "◐", 22, "text", false);
        theme.setGravity(Gravity.CENTER);
        theme.setContentDescription("切换主题");
        theme.setBackground(shape(color("surface"), color("line"), 50));
        theme.setElevation(dp(3));
        theme.setOnClickListener(v -> {
            if ("system".equals(data.theme)) data.theme = dark ? "light" : "dark";
            else data.theme = dark ? "light" : "dark";
            applyTheme(); data.save(this); showRoute(route, false);
        });
        LinearLayout.LayoutParams themeLp = lp(44, 44); themeLp.leftMargin = dp(12); header.addView(theme, themeLp);
    }

    private void updateInsets() {
        if (header != null) header.setPadding(dp(20), systemTopInset + dp(10), dp(20), dp(14));
        if (pageHost != null) pageHost.setPadding(dp(16), 0, dp(16), dp(112) + systemBottomInset);
        if (dock != null && dock.getTag() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dock.getTag();
            params.bottomMargin = dp(12) + systemBottomInset;
            dock.setLayoutParams(params);
        }
    }

    private void applyTheme() {
        dark = "dark".equals(data.theme) || ("system".equals(data.theme) && (getResources().getConfiguration().uiMode & 0x30) == 0x20);
        getWindow().setStatusBarColor(Color.TRANSPARENT); getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | (dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        root.setBackgroundColor(color("bg")); ((GlassDock) dock).setOverlay(glassSurface());
    }

    private void buildDock() {
        dock.removeAllViews();
        for (int i = 0; i < ROUTES.length; i++) {
            String target = ROUTES[i]; boolean selected = route.equals(target);
            LinearLayout item = column(); item.setGravity(Gravity.CENTER); item.setPadding(dp(3), dp(4), dp(3), dp(4));
            item.setBackground(shape(selected ? color("accentSoft") : Color.TRANSPARENT, 0, 999));
            TextView icon = text(ROUTE_GLYPHS[i], 21, selected ? "accent" : "muted", selected);
            icon.setGravity(Gravity.CENTER); item.addView(icon, lp(-1, 25));
            TextView label = text(ROUTE_LABELS[i], 11, selected ? "accent" : "muted", selected);
            label.setGravity(Gravity.CENTER); item.addView(label, lp(-1, 19));
            item.setContentDescription(ROUTE_LABELS[i]); item.setSelected(selected); item.setOnClickListener(v -> {
                if (target.equals(route)) {
                    if ("settings".equals(target) && !"overview".equals(settingsPage)) {
                        settingsPage = "overview";
                        showRoute("settings", true, -1);
                    }
                    return;
                }
                if ("settings".equals(target)) settingsPage = "overview";
                showRoute(target, true);
            });
            dock.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private void showRoute(String target, boolean animate) {
        int oldIndex = Arrays.asList(ROUTES).indexOf(route);
        int newIndex = Arrays.asList(ROUTES).indexOf(target);
        float direction = oldIndex < 0 || newIndex >= oldIndex ? 1 : -1;
        showRoute(target, animate, direction);
    }

    private void showRoute(String target, boolean animate, float direction) {
        previousRoute = route; route = target;
        if ("settings".equals(target) && !previousRoute.equals(target)) settingsPage = "overview";
        applyTheme(); buildHeader(); buildDock();
        View page;
        if ("records".equals(route)) page = recordsPage();
        else if ("analysis".equals(route)) page = analysisPage();
        else if ("settings".equals(route)) page = settingsPage();
        else page = checkinPage();
        displayPage(page, animate, direction);
        scrollContainer.scrollTo(0, 0);
        pageHost.requestLayout();
    }

    private void displayPage(View page, boolean animate, float direction) {
        if (pageTransition != null) {
            pageTransition.cancel();
            pageTransition = null;
        }
        View outgoing = pageHost.getChildCount() == 0 ? null : pageHost.getChildAt(pageHost.getChildCount() - 1);
        if (!animate || outgoing == null) {
            pageHost.removeAllViews();
            pageHost.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
            dock.invalidate();
            return;
        }

        outgoing.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        outgoing.setClickable(false);
        pageHost.addView(page, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        page.setRotationY(direction * 82f);
        page.setAlpha(.12f);
        page.setScaleX(.96f);
        page.setScaleY(.96f);
        AnimatorSet exit = new AnimatorSet();
        exit.playTogether(
            ObjectAnimator.ofFloat(outgoing, View.ROTATION_Y, 0f, -direction * 82f),
            ObjectAnimator.ofFloat(outgoing, View.ALPHA, 1f, .12f),
            ObjectAnimator.ofFloat(outgoing, View.SCALE_X, 1f, .96f),
            ObjectAnimator.ofFloat(outgoing, View.SCALE_Y, 1f, .96f)
        );
        exit.setDuration(135);
        exit.setInterpolator(new android.view.animation.AccelerateInterpolator());
        AnimatorSet enter = new AnimatorSet();
        enter.playTogether(
            ObjectAnimator.ofFloat(page, View.ROTATION_Y, direction * 82f, 0f),
            ObjectAnimator.ofFloat(page, View.ALPHA, .12f, 1f),
            ObjectAnimator.ofFloat(page, View.SCALE_X, .96f, 1f),
            ObjectAnimator.ofFloat(page, View.SCALE_Y, .96f, 1f)
        );
        enter.setDuration(205);
        enter.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        pageTransition = new AnimatorSet();
        pageTransition.playSequentially(exit, enter);
        pageTransition.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                outgoing.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                page.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                page.setCameraDistance(dp(8000));
                outgoing.setCameraDistance(dp(8000));
            }

            @Override public void onAnimationEnd(Animator animation) {
                pageHost.removeView(outgoing);
                page.setRotationY(0f);
                page.setAlpha(1f);
                page.setScaleX(1f);
                page.setScaleY(1f);
                page.setLayerType(View.LAYER_TYPE_NONE, null);
                page.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                page.setClickable(true);
                outgoing.setLayerType(View.LAYER_TYPE_NONE, null);
                pageTransition = null;
                dock.invalidate();
                pageHost.requestLayout();
            }
        });
        pageTransition.start();
    }

    private LocalDate today() { return LocalDate.now(); }
    private String dateCaption(LocalDate date) { return date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)); }
    private JSONObject record(LocalDate date) { return data.records.get(date.toString()); }
    private void showToast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    private LinearLayout card() {
        LinearLayout card = column(); card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(shape(color("surface"), color("line"), 18)); card.setElevation(dp(2));
        return card;
    }

    private void sectionHeading(LinearLayout parent, String title, String caption, Runnable action) {
        LinearLayout row = row();
        TextView h = text(title, 17, "text", true); row.addView(h, new LinearLayout.LayoutParams(0, -2, 1));
        if (caption != null) { TextView c = text(caption, 12, "muted", false); row.addView(c, lp(-2, -2)); }
        if (action != null) row.setOnClickListener(v -> action.run());
        parent.addView(row, lp(-1, -2));
    }

    private LinearLayout checkinPage() {
        LinearLayout page = column();
        JSONObject entry = record(today());
        LinearLayout hero = card();
        hero.addView(text(dateCaption(today()), 14, "muted", false));
        TextView label = text("今天的状态", 14, "muted", false); LinearLayout.LayoutParams labelLp = lp(-1, -2); labelLp.topMargin = dp(18); hero.addView(label, labelLp);
        String state = entry == null ? "未记录" : entry.optString("state", "未记录");
        TextView value = text(state, 38, "导了".equals(state) ? "accent" : "没导".equals(state) ? "negative" : "muted", true);
        LinearLayout.LayoutParams valueLp = lp(-1, -2); valueLp.topMargin = dp(4); valueLp.bottomMargin = dp(16); hero.addView(value, valueLp);
        if (entry != null && !entry.optString("typeName", "").isEmpty()) {
            TextView type = text(entry.optString("typeName"), 13, "accent", true); type.setGravity(Gravity.CENTER);
            type.setPadding(dp(10), dp(5), dp(10), dp(5)); type.setBackground(shape(color("accentSoft"), 0, 999)); hero.addView(type, lp(-2, 32));
        }
        LinearLayout actions = row(); actions.setGravity(Gravity.CENTER); actions.setPadding(0, dp(14), 0, 0);
        actions.addView(actionButton("导了", "positive", () -> chooseType(today())), new LinearLayout.LayoutParams(0, dp(62), 1));
        Space gap = new Space(this); actions.addView(gap, lp(12, 1));
        actions.addView(actionButton("没导", "negative", () -> saveRecord(today(), "没导", null)), new LinearLayout.LayoutParams(0, dp(62), 1));
        hero.addView(actions); page.addView(hero);

        int wordIndex = today().getDayOfMonth() % DAILY_WORDS.length;
        LinearLayout word = row(); word.setOrientation(LinearLayout.VERTICAL); word.setPadding(dp(16), dp(13), dp(16), dp(13));
        word.setBackground(shape(color("accentSoft"), 0, 14)); TextView eyebrow = text("每日一句", 12, "accent", true); word.addView(eyebrow);
        TextView quote = text(DAILY_WORDS[wordIndex], 15, "text", false); LinearLayout.LayoutParams quoteLp = lp(-1, -2); quoteLp.topMargin = dp(5); word.addView(quote, quoteLp);
        LinearLayout.LayoutParams wordLp = lp(-1, -2); wordLp.topMargin = dp(14); page.addView(word, wordLp);

        LinearLayout noteSection = column(); LinearLayout.LayoutParams sectionLp = lp(-1, -2); sectionLp.topMargin = dp(23); page.addView(noteSection, sectionLp);
        LinearLayout noteHeading = row(); TextView noteTitle = text("今天的备注", 17, "text", true); noteHeading.addView(noteTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView editNote = smallAction(entry != null && !entry.optString("note", "").isEmpty() ? "编辑" : "添加", () -> editNote(today())); noteHeading.addView(editNote, lp(-2, 42)); noteSection.addView(noteHeading);
        if (entry != null && !entry.optString("note", "").trim().isEmpty()) {
            TextView note = text(entry.optString("note"), 14, "text", false); note.setPadding(dp(12), dp(12), dp(12), dp(12)); note.setBackground(shape(color("surface2"), 0, 12)); LinearLayout.LayoutParams noteLp = lp(-1, -2); noteLp.topMargin = dp(8); noteSection.addView(note, noteLp);
        } else {
            TextView hint = text("记录一点当天的想法，最多 100 个字。", 13, "muted", false); LinearLayout.LayoutParams hintLp = lp(-1, -2); hintLp.topMargin = dp(4); noteSection.addView(hint, hintLp);
        }
        if (entry != null) { TextView edit = smallAction("修改今天的状态", () -> editState(today())); LinearLayout.LayoutParams editLp=lp(-1,48);editLp.topMargin=dp(18);page.addView(edit,editLp); }
        return page;
    }

    private View actionButton(String label, String kind, Runnable action) {
        TextView button=text(label,18,kind.equals("positive")?"surface":kind.equals("negative")?"negative":"text",true);button.setGravity(Gravity.CENTER);
        button.setBackground(shape(kind.equals("positive")?color("accent"):kind.equals("negative")?color("negativeSoft"):color("surface"),0,14));
        button.setOnClickListener(v->action.run());button.setContentDescription(label);return button;
    }

    private TextView smallAction(String label, Runnable action) {
        TextView button=text(label,14,"text",false);button.setGravity(Gravity.CENTER);button.setPadding(dp(12),0,dp(12),0);button.setBackground(shape(color("surface"),color("line"),11));button.setOnClickListener(v->action.run());return button;
    }

    private TextView topMonthButton(String label, String accessibility, Runnable action) {
        TextView button=text(label,16,"text",true);button.setGravity(Gravity.CENTER);button.setContentDescription(accessibility);button.setOnClickListener(v->action.run());return button;
    }

    private final class SwipePageLayout extends LinearLayout {
        private final Runnable onPrevious;
        private final Runnable onNext;
        private final int touchSlop = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
        private final boolean swipeEnabled;
        private final SwipeNavigation swipeNavigation = new SwipeNavigation(Math.max(touchSlop * 2, dp(40)));

        SwipePageLayout(boolean swipeEnabled, Runnable onPrevious, Runnable onNext) {
            super(MainActivity.this);
            this.swipeEnabled = swipeEnabled;
            this.onPrevious = onPrevious;
            this.onNext = onNext;
            setClickable(true);
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                swipeNavigation.begin(event.getRawX(), event.getRawY());
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE && swipeEnabled) {
                return swipeNavigation.update(event.getRawX(), event.getRawY());
            }
            if (action == MotionEvent.ACTION_CANCEL) swipeNavigation.cancel();
            return false;
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP) {
                int direction = swipeNavigation.finish(event.getRawX(), event.getRawY());
                if (direction == SwipeNavigation.NEXT && onNext != null) onNext.run();
                else if (direction == SwipeNavigation.PREVIOUS && onPrevious != null) onPrevious.run();
                else performClick();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) swipeNavigation.cancel();
            return true;
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    private LinearLayout recordsPage() {
        LinearLayout page=new SwipePageLayout(true, () -> navigateCalendar(-1), () -> navigateCalendar(1));
        page.setOrientation(LinearLayout.VERTICAL);
        YearMonth shown=calendarMonth;
        Statistics.Counts counts=Statistics.count(data.statisticsRecords(),new Statistics.Range(shown.atDay(1),shown.atEndOfMonth()),today());
        page.addView(summaryStats(counts));
        LinearLayout.LayoutParams calendarLp=lp(-1,-2);calendarLp.topMargin=dp(15);
        LinearLayout calendar=column();
        calendar.setOrientation(LinearLayout.VERTICAL);calendar.setPadding(dp(18),dp(17),dp(18),dp(17));calendar.setBackground(shape(color("surface"),color("line"),18));calendar.setElevation(dp(2));
        LinearLayout monthNav=row();monthNav.setGravity(Gravity.CENTER);TextView prev=topMonthButton("‹","上个月",()->navigateCalendar(-1));monthNav.addView(prev,lp(44,44));
        TextView month=topMonthButton(shown.getYear()+" 年 "+shown.getMonthValue()+" 月  ⌄","选择年份和月份",()->showYearMonthPicker(shown.getYear(),shown.getMonthValue()-1));monthNav.addView(month,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView next=topMonthButton("›","下个月",()->navigateCalendar(1));monthNav.addView(next,lp(44,44));calendar.addView(monthNav);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);grid.setUseDefaultMargins(false);
        for(String day:new String[]{"一","二","三","四","五","六","日"}){TextView label=text(day,11,"muted",false);label.setGravity(Gravity.CENTER);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(28);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(label,p);}
        int offset=(shown.atDay(1).getDayOfWeek().getValue()+6)%7;
        for(int i=0;i<offset;i++){TextView empty=text("",12,"muted",false);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(49);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(empty,p);}
        for(int day=1;day<=shown.lengthOfMonth();day++){
            LocalDate date=shown.atDay(day);boolean future=date.isAfter(today());JSONObject record=record(date);
            LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setPadding(0,dp(3),0,dp(3));
            String tint=record==null?"line":"导了".equals(record.optString("state"))?"accent":"negative";
            TextView number=text(String.valueOf(day),13,future?"muted":"text",false);number.setGravity(Gravity.CENTER);
            boolean todayCell=date.equals(today());
            if(todayCell){number.setBackground(shape(Color.TRANSPARENT,color("accent"),11));}
            cell.addView(number,lp(35,29));
            View dot=new View(this);dot.setBackground(shape(color(tint),0,10));LinearLayout.LayoutParams dotLp=lp(7,7);dotLp.topMargin=dp(3);cell.addView(dot,dotLp);
            if(future){cell.setAlpha(.38f);}else cell.setOnClickListener(v->showDateDetail(date));
            cell.setContentDescription(dateCaption(date)+(record==null?" 未记录":" "+record.optString("state")));
            GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(51);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(cell,p);
        }
        calendar.addView(grid);page.addView(calendar,calendarLp);return page;
    }

    private void navigateCalendar(int delta) {
        YearMonth next = CalendarNavigation.shift(calendarMonth, delta);
        if (next.equals(calendarMonth)) return;
        calendarMonth = next;
        showRoute("records", true, delta < 0 ? -1 : 1);
    }

    private LinearLayout summaryStats(Statistics.Counts counts) {
        LinearLayout stats=row();String[] names={"导了","没导","未记录"};int[] values={counts.done,counts.missed,counts.pending};String[] colors={"accent","negative","text"};
        for(int i=0;i<3;i++){LinearLayout stat=column();stat.setGravity(Gravity.CENTER);stat.setPadding(dp(6),dp(11),dp(6),dp(11));stat.setBackground(shape(color("surface"),color("line"),14));TextView number=text(String.valueOf(values[i]),24,colors[i],true);number.setGravity(Gravity.CENTER);stat.addView(number,lp(-1,31));TextView label=text(names[i],12,"muted",false);label.setGravity(Gravity.CENTER);stat.addView(label,lp(-1,21));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(76),1);if(i>0)p.leftMargin=dp(8);stats.addView(stat,p);}
        return stats;
    }

    private void showYearMonthPicker(int year,int month) {
        LinearLayout box=column();box.setPadding(dp(4),dp(8),dp(4),dp(8));
        TextView title=text("选择年份和月份",20,"text",true);box.addView(title);
        LinearLayout pickers=row();
        NumberPicker yearPicker=new NumberPicker(this);yearPicker.setMinValue(1900);yearPicker.setMaxValue(9999);yearPicker.setValue(year);yearPicker.setWrapSelectorWheel(false);
        NumberPicker monthPicker=new NumberPicker(this);monthPicker.setMinValue(1);monthPicker.setMaxValue(12);monthPicker.setValue(month+1);monthPicker.setDisplayedValues(new String[]{"1 月","2 月","3 月","4 月","5 月","6 月","7 月","8 月","9 月","10 月","11 月","12 月"});
        pickers.addView(yearPicker,new LinearLayout.LayoutParams(0,dp(158),1));pickers.addView(monthPicker,new LinearLayout.LayoutParams(0,dp(158),1));box.addView(pickers);
        new AlertDialog.Builder(this).setView(box).setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->navigateCalendarTo(YearMonth.of(yearPicker.getValue(),monthPicker.getValue()))).show();
    }

    private void navigateCalendarTo(YearMonth selected) {
        if (selected.equals(calendarMonth)) return;
        float direction = selected.isBefore(calendarMonth) ? -1 : 1;
        calendarMonth = selected;
        showRoute("records", true, direction);
    }

    private void showDateDetail(LocalDate date) {
        JSONObject current=record(date);
        String status=current==null?"未记录":current.optString("state");
        String statusRole="导了".equals(status)?"accent":"没导".equals(status)?"negative":"muted";
        LinearLayout body=column();body.setPadding(dp(20),dp(14),dp(20),dp(8));
        TextView heading=text(date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA)),19,"text",true);body.addView(heading);

        LinearLayout statusCard=row();statusCard.setPadding(dp(12),dp(9),dp(12),dp(9));statusCard.setBackground(shape(color("导了".equals(status)?"accentSoft":"没导".equals(status)?"negativeSoft":"surface2"),0,12));
        TextView mark=text("导了".equals(status)?"✓":"没导".equals(status)?"–":"＋",20,statusRole,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(color("surface"),0,99));statusCard.addView(mark,lp(36,36));
        LinearLayout statusCopy=column();statusCopy.setPadding(dp(10),0,0,0);statusCopy.addView(text(current==null?"补记这一天":"当天状态",11,"muted",false));TextView state=text(status,17,statusRole,true);LinearLayout.LayoutParams stateLp=lp(-1,26);statusCopy.addView(state,stateLp);statusCard.addView(statusCopy,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams statusCardLp=lp(-1,62);statusCardLp.topMargin=dp(14);body.addView(statusCard,statusCardLp);

        if(current!=null&&!current.optString("typeName","").isEmpty()){LinearLayout type=row();type.setPadding(dp(2),dp(12),dp(2),dp(2));TextView label=text("类型",12,"muted",false);type.addView(label,lp(54,-2));type.addView(text(current.optString("typeName"),14,"accent",true));body.addView(type);}
        if(current!=null&&!current.optString("note","").trim().isEmpty()){LinearLayout note=column();note.setPadding(dp(12),dp(10),dp(12),dp(10));note.setBackground(shape(color("surface2"),0,10));note.addView(text("备注",11,"muted",false));TextView content=text(current.optString("note"),14,"text",false);content.setMaxLines(4);LinearLayout.LayoutParams noteContentLp=lp(-1,-2);noteContentLp.topMargin=dp(3);note.addView(content,noteContentLp);LinearLayout.LayoutParams noteLp=lp(-1,-2);noteLp.topMargin=dp(10);body.addView(note,noteLp);}

        LinearLayout actions=row();actions.setGravity(Gravity.CENTER);LinearLayout.LayoutParams actionsLp=lp(-1,56);actionsLp.topMargin=dp(17);body.addView(actions,actionsLp);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(body).setNegativeButton("关闭",null).create();
        if(current==null){
            View done=actionButton("✓  导了","positive",()->{dialog.dismiss();chooseType(date);});actions.addView(done,new LinearLayout.LayoutParams(0,-1,1));Space gap=new Space(this);actions.addView(gap,lp(9,1));View missed=actionButton("–  没导","negative",()->{dialog.dismiss();saveRecord(date,"没导",null);});actions.addView(missed,new LinearLayout.LayoutParams(0,-1,1));
        }else{
            View edit=smallAction("修改状态",()->{dialog.dismiss();editState(date);});actions.addView(edit,new LinearLayout.LayoutParams(0,-1,1));Space gap=new Space(this);actions.addView(gap,lp(9,1));View note=smallAction(current.optString("note","").isEmpty()?"添加备注":"编辑备注",()->{dialog.dismiss();editNote(date);});actions.addView(note,new LinearLayout.LayoutParams(0,-1,1));
        }
        dialog.show();
    }

    private LinearLayout analysisPage() {
        boolean canSwipe = !"total".equals(analysisPeriod);
        LinearLayout page=new SwipePageLayout(canSwipe, () -> moveAnalysis(-1), () -> moveAnalysis(1));
        page.setOrientation(LinearLayout.VERTICAL);
        LinearLayout heading=row();TextView title=text("分析",22,"text",true);heading.addView(title,new LinearLayout.LayoutParams(0,dp(36),1));TextView periodLabel=text("记录概览",13,"muted",false);heading.addView(periodLabel,lp(-2,-2));page.addView(heading);
        LinearLayout tabs=row();tabs.setPadding(dp(4),dp(4),dp(4),dp(4));tabs.setBackground(shape(color("surface2"),0,14));String[][] periods={{"week","周"},{"month","月"},{"year","年"},{"total","总"}};
        for(int index=0;index<periods.length;index++){String[] pair=periods[index];boolean selected=analysisPeriod.equals(pair[0]);TextView tab=text(pair[1],14,selected?"accent":"muted",selected);tab.setGravity(Gravity.CENTER);tab.setBackground(shape(selected?color("surface"):Color.TRANSPARENT,0,11));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1);p.setMargins(dp(2),0,dp(2),0);tabs.addView(tab,p);int targetIndex=index;tab.setOnClickListener(v->{if(analysisPeriod.equals(pair[0]))return;int currentIndex=Arrays.asList("week","month","year","total").indexOf(analysisPeriod);analysisPeriod=pair[0];showRoute("analysis",true,targetIndex<currentIndex?-1:1);});}LinearLayout.LayoutParams tabsLp=lp(-1,48);tabsLp.topMargin=dp(12);page.addView(tabs,tabsLp);
        Statistics.Range range=Statistics.range(analysisPeriod,analysisDate,Statistics.firstRecordDate(data.statisticsRecords()),today());
        Statistics.Counts counts=Statistics.count(data.statisticsRecords(),range,today());
        if(!"total".equals(analysisPeriod)&&range!=null){LinearLayout nav=row();TextView previous=topMonthButton("‹","上一周期",()->moveAnalysis(-1));nav.addView(previous,lp(42,42));String label=periodLabel(range);TextView labelView=text(label,14,"text",true);labelView.setGravity(Gravity.CENTER);nav.addView(labelView,new LinearLayout.LayoutParams(0,dp(42),1));boolean canMoveNext=canMoveAnalysis(1);TextView next=topMonthButton(canMoveNext?"›":"","下一周期",()->moveAnalysis(1));next.setEnabled(canMoveNext);next.setAlpha(canMoveNext?1f:.35f);nav.addView(next,lp(42,42));LinearLayout.LayoutParams navLp=lp(-1,44);navLp.topMargin=dp(2);page.addView(nav,navLp);}
        LinearLayout.LayoutParams statsLp=lp(-1,76);statsLp.topMargin=dp(4);page.addView(summaryStats(counts),statsLp);
        TextView completion=text("已统计 "+counts.elapsedDays+" 天  ·  记录率 "+counts.completionPercent()+"%",12,"muted",false);completion.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams completionLp=lp(-1,30);page.addView(completion,completionLp);

        LinearLayout typeSection=column();LinearLayout.LayoutParams sectionLp=lp(-1,-2);sectionLp.topMargin=dp(9);page.addView(typeSection,sectionLp);sectionHeading(typeSection,"类型次数",range==null?"还没有记录":periodLabel(range),null);
        LinearLayout typeCard=card();LinearLayout.LayoutParams cardLp=lp(-1,-2);cardLp.topMargin=dp(9);typeSection.addView(typeCard,cardLp);
        if(counts.types.isEmpty())typeCard.addView(text("所选周期还没有类型记录。",14,"muted",false));else{int max=1;for(int value:counts.types.values())max=Math.max(max,value);for(Map.Entry<String,Integer> entry:counts.types.entrySet())addBar(typeCard,entry.getKey(),entry.getValue(),max,"accent",false);}
        if (!"total".equals(analysisPeriod)) addRecentComparison(page);
        return page;
    }

    private String periodLabel(Statistics.Range range) {
        if("week".equals(analysisPeriod))return range.start.format(DateTimeFormatter.ofPattern("M月d日",Locale.CHINA))+" — "+range.end.format(DateTimeFormatter.ofPattern("M月d日",Locale.CHINA));
        if("month".equals(analysisPeriod))return range.start.getYear()+" 年 "+range.start.getMonthValue()+" 月";
        if("year".equals(analysisPeriod))return range.start.getYear()+" 年";
        return range==null?"还没有记录":range.start+" 至今";
    }

    private void moveAnalysis(int direction) {
        if ("total".equals(analysisPeriod)) return;
        LocalDate next=analysisDate;
        try {
            if("week".equals(analysisPeriod))next=next.plusWeeks(direction);
            else if("month".equals(analysisPeriod))next=next.withDayOfMonth(1).plusMonths(direction);
            else if("year".equals(analysisPeriod))next=next.withDayOfYear(1).plusYears(direction);
        } catch (java.time.DateTimeException outOfRange) { return; }
        if(direction>0&&next.isAfter(today()))return;
        if(next.equals(analysisDate))return;
        analysisDate=next;showRoute("analysis",true,direction<0?-1:1);
    }

    private boolean canMoveAnalysis(int direction) {
        try {
            LocalDate next="week".equals(analysisPeriod)?analysisDate.plusWeeks(direction):"month".equals(analysisPeriod)?analysisDate.withDayOfMonth(1).plusMonths(direction):analysisDate.withDayOfYear(1).plusYears(direction);
            return direction < 0 || !next.isAfter(today());
        } catch (java.time.DateTimeException outOfRange) { return false; }
    }

    private void addRecentComparison(LinearLayout page) {
        LinearLayout section=column();LinearLayout.LayoutParams sectionLp=lp(-1,-2);sectionLp.topMargin=dp(17);page.addView(section,sectionLp);
        List<Statistics.ComparisonBucket> buckets=Statistics.recentComparison(data.statisticsRecords(),analysisPeriod,today());
        Statistics.ComparisonBucket previous=buckets.get(buckets.size()-2),current=buckets.get(buckets.size()-1);
        String unit="week".equals(analysisPeriod)?"周":"year".equals(analysisPeriod)?"年":"月";
        TextView title=text("近 6 "+unit+"对比",17,"text",true);section.addView(title);
        addComparisonChanges(section, previous.counts, current.counts, unit);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(3);grid.setRowCount(2);LinearLayout.LayoutParams gridLp=lp(-1,-2);gridLp.topMargin=dp(3);section.addView(grid,gridLp);
        for(int i=0;i<buckets.size();i++){
            Statistics.ComparisonBucket bucket=buckets.get(i);boolean currentPeriod=i==buckets.size()-1;
            LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(3),dp(5),dp(3),dp(5));tile.setBackground(shape(color("surface"),color(currentPeriod?"accent":"line"),8));tile.setContentDescription(bucket.label+"：导了 "+bucket.counts.done+" 天，没导 "+bucket.counts.missed+" 天");
            ComparisonRingView ring=new ComparisonRingView(this,bucket.counts,color("accent"),color("negative"),color("surface2"),color("text"),bucket.label);
            tile.addView(ring,lp(66,66));TextView period=text(bucket.label,11,currentPeriod?"accent":"muted",currentPeriod);period.setGravity(Gravity.CENTER);period.setMaxLines(1);tile.addView(period,lp(-1,20));
            TextView exact=text(bucket.counts.done+" / "+bucket.counts.missed,10,"text",false);exact.setGravity(Gravity.CENTER);exact.setMaxLines(1);tile.addView(exact,lp(-1,17));
            GridLayout.LayoutParams itemLp=new GridLayout.LayoutParams(GridLayout.spec(i/3),GridLayout.spec(i%3,1f));itemLp.width=0;itemLp.height=dp(112);itemLp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(tile,itemLp);
        }
        LinearLayout legend=row();LinearLayout.LayoutParams legendLp=lp(-1,26);legendLp.topMargin=dp(2);legend.addView(legendDot("accent","导了"));Space spacer=new Space(this);legend.addView(spacer,lp(14,1));legend.addView(legendDot("negative","没导"));section.addView(legend,legendLp);
    }

    private void addComparisonChanges(LinearLayout section, Statistics.Counts previous, Statistics.Counts current, String unit) {
        boolean hasRecords = previous.recorded() + current.recorded() > 0;
        boolean changed = hasRecords && (previous.done != current.done || previous.missed != current.missed);
        boolean warning = hasRecords && (current.done < previous.done || current.missed > previous.missed);
        String status = !hasRecords ? "暂无记录可对比" : warning ? "△ 需要关注" : changed ? "↑ 趋势改善" : "与上" + unit + "持平";
        TextView heading = text(status + " · 较上" + unit, 13, warning ? "negative" : changed ? "accent" : "muted", true);
        LinearLayout.LayoutParams headingLp = lp(-1, -2);
        headingLp.topMargin = dp(10);
        section.addView(heading, headingLp);

        boolean stacked = getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.2f;
        LinearLayout changes = stacked ? column() : row();
        if (!stacked) changes.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams changesLp = lp(-1, -2);
        changesLp.topMargin = dp(8);
        section.addView(changes, changesLp);
        String[] labels = {"导了次数", "没导天数"};
        int[] prior = {previous.done, previous.missed};
        int[] now = {current.done, current.missed};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout.LayoutParams itemLp = stacked ? lp(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) {
                if (stacked) itemLp.topMargin = dp(8);
                else itemLp.leftMargin = dp(10);
            }
            changes.addView(comparisonDeltaCard(labels[i], prior[i], now[i], unit, hasRecords, i == 1), itemLp);
        }
        TextView note = text("本" + unit + "截至 " + today().format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)) + "，与上" + unit + "完整周期对比", 11, "muted", false);
        LinearLayout.LayoutParams noteLp = lp(-1, -2);
        noteLp.topMargin = dp(8);
        noteLp.bottomMargin = dp(8);
        section.addView(note, noteLp);
    }

    private View comparisonDeltaCard(String label, int previous, int current, String unit, boolean hasRecords, boolean reverseTrend) {
        int difference = current - previous;
        boolean changed = hasRecords && difference != 0;
        boolean warning = changed && (reverseTrend ? difference > 0 : difference < 0);
        String role = !changed ? "muted" : warning ? "negative" : "accent";
        String change = difference > 0 ? "增加" : difference < 0 ? "减少" : "持平";
        String arrow = difference > 0 ? "↑" : difference < 0 ? "↓" : "—";
        LinearLayout panel = column();
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setMinimumHeight(dp(124));
        panel.setBackground(shape(color(!changed ? "surface" : warning ? "negativeSoft" : "accentSoft"), color(!changed ? "line" : role), 14));
        panel.addView(text(label, 13, "text", true), lp(-1, -2));
        TextView value = text(hasRecords ? arrow + " " + Math.abs(difference) : "—", 32, role, true);
        value.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        value.setSingleLine(true);
        value.setAutoSizeTextTypeUniformWithConfiguration(22, 32, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams valueLp = lp(-1, 48);
        valueLp.topMargin = dp(2);
        panel.addView(value, valueLp);
        panel.addView(text(hasRecords ? change + " · " + previous + " → " + current : "本期与上期均无记录", 12, role, false), lp(-1, -2));
        panel.setContentDescription(label + "，" + (!hasRecords ? "本期和上期均无记录" : "较上" + unit + change + " " + Math.abs(difference) + "，上期 " + previous + "，本期 " + current));
        if (Build.VERSION.SDK_INT >= 28) panel.setScreenReaderFocusable(true);
        return panel;
    }
    private View legendDot(String role,String label){LinearLayout item=row();View dot=new View(this);dot.setBackground(shape(color(role),0,99));item.addView(dot,lp(8,8));TextView text=text(label,11,"muted",false);LinearLayout.LayoutParams textLp=lp(-2,-2);textLp.leftMargin=dp(5);item.addView(text,textLp);return item;}

    private void addBar(LinearLayout parent,String label,int value,int max,String role,boolean compact){LinearLayout row=row();TextView name=text(label,13,"text",false);name.setMaxLines(1);row.addView(name,lp(compact?38:74,25));LinearLayout track=row();track.setBackground(shape(color("surface2"),0,999));track.setClipToOutline(true);View fill=new View(this);fill.setBackgroundColor(color(role));track.addView(fill,new LinearLayout.LayoutParams(0,dp(9),Math.max(.04f,value/(float)max)));row.addView(track,new LinearLayout.LayoutParams(0,dp(10),1));TextView count=text(String.valueOf(value),13,"text",true);count.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(count,lp(30,25));LinearLayout.LayoutParams rowLp=lp(-1,30);parent.addView(row,rowLp);}

    private LinearLayout settingsPage() {
        if(!"overview".equals(settingsPage))return settingsChildPage();
        LinearLayout page=column();page.addView(text("本地偏好",13,"muted",false));TextView title=text("设置",22,"text",true);LinearLayout.LayoutParams titleLp=lp(-1,38);page.addView(title,titleLp);
        LinearLayout group=card();LinearLayout.LayoutParams groupLp=lp(-1,-2);groupLp.topMargin=dp(10);page.addView(group,groupLp);
        addSettingEntry(group,"类型管理",activeTypeCount()+" 个可用类型","types");addSettingEntry(group,"提醒设置",data.reminderEnabled?"每天 "+data.reminderTime:"当前已关闭","reminder");addSettingEntry(group,"外观设置",themeName(),"appearance");
        LinearLayout backup=card();LinearLayout.LayoutParams backupLp=lp(-1,-2);backupLp.topMargin=dp(12);page.addView(backup,backupLp);addSettingEntry(backup,"本地备份","导入或导出 JSON 备份","backup");
        LinearLayout about=card();LinearLayout.LayoutParams aboutLp=lp(-1,-2);aboutLp.topMargin=dp(12);page.addView(about,aboutLp);addSettingEntry(about,"关于导了么","版本 "+BuildConfig.VERSION_NAME+" · "+APP_AUTHOR,"about");return page;
    }

    private String themeName(){return "system".equals(data.theme)?"跟随系统":"light".equals(data.theme)?"明亮模式":"暗色模式";}
    private int activeTypeCount(){int n=0;for(LocalData.TypeOption type:data.types)if(type.active)n++;return n;}
    private void addSettingEntry(LinearLayout parent,String title,String detail,String target){LinearLayout row=row();row.setPadding(dp(2),dp(14),dp(2),dp(14));LinearLayout copy=column();copy.addView(text(title,16,"text",true));TextView caption=text(detail,12,"muted",false);LinearLayout.LayoutParams cp=lp(-1,-2);cp.topMargin=dp(4);copy.addView(caption,cp);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView chevron=text("›",26,"muted",false);chevron.setGravity(Gravity.CENTER);row.addView(chevron,lp(28,34));row.setOnClickListener(v->{settingsPage=target;showSettingsChild();});parent.addView(row,lp(-1,70));if(parent.getChildCount()>1){View divider=new View(this);divider.setBackgroundColor(color("line"));parent.addView(divider,lp(-1,1));}}

    private void showSettingsChild(){route="settings";previousRoute=route;applyTheme();buildHeader();buildDock();View page=settingsChildPage();displayPage(page,true,1f);scrollContainer.scrollTo(0,0);}

    private LinearLayout settingsChildPage(){LinearLayout page=column();LinearLayout heading=row();TextView back=text("‹",31,"text",false);back.setGravity(Gravity.CENTER);back.setBackground(shape(color("surface"),color("line"),50));back.setOnClickListener(v->leaveSettingsChild());heading.addView(back,lp(42,42));LinearLayout titleColumn=column();titleColumn.setPadding(dp(12),0,0,0);titleColumn.addView(text("设置",12,"muted",false));String title="types".equals(settingsPage)?"类型管理":"reminder".equals(settingsPage)?"提醒设置":"appearance".equals(settingsPage)?"外观设置":"about".equals(settingsPage)?"关于":"本地备份";titleColumn.addView(text(title,21,"text",true));heading.addView(titleColumn,new LinearLayout.LayoutParams(0,-2,1));page.addView(heading);LinearLayout.LayoutParams groupLp=lp(-1,-2);groupLp.topMargin=dp(17);
        if("types".equals(settingsPage))buildTypesPage(page);
        else if("reminder".equals(settingsPage))buildReminderPage(page);
        else if("appearance".equals(settingsPage))buildAppearancePage(page);
        else if("about".equals(settingsPage))buildAboutPage(page);
        else buildBackupPage(page);
        return page;}

    private void leaveSettingsChild(){settingsPage="overview";showRoute("settings",true);}

    private void buildAboutPage(LinearLayout page) {
        LinearLayout identity = row();
        identity.setPadding(dp(4), dp(24), dp(4), dp(20));
        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.mipmap.ic_launcher);
        appIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        identity.addView(appIcon, lp(52, 52));
        LinearLayout identityCopy = column();
        identityCopy.setPadding(dp(14), 0, 0, 0);
        identityCopy.addView(text("导了么", 23, "text", true));
        identityCopy.addView(text("如实记录，看见自己的变化", 12, "muted", false));
        identity.addView(identityCopy, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(identity, lp(-1, -2));

        LinearLayout details = card();
        details.setPadding(dp(14), dp(4), dp(14), dp(4));
        page.addView(details, lp(-1, -2));
        addAboutInfo(details, android.R.drawable.ic_menu_info_details, "关于", "原生 Android · 本地记录", null);
        addAboutInfo(details, android.R.drawable.ic_menu_myplaces, "作者", APP_AUTHOR, null);
        addAboutInfo(details, android.R.drawable.ic_menu_rotate, "版本", BuildConfig.VERSION_NAME + "（" + BuildConfig.VERSION_CODE + "）", null);
        addAboutInfo(details, android.R.drawable.ic_menu_recent_history, "编译时间", BuildConfig.BUILD_TIME + "\nUTC+8 · 北京时间", null);
        addAboutInfo(details, android.R.drawable.ic_menu_share, "GitHub", "github.com/SugerMus/dao-leme", () -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)));
            } catch (android.content.ActivityNotFoundException exception) {
                showToast("没有可打开链接的应用");
            }
        });
    }

    private void addAboutInfo(LinearLayout group, int iconResource, String label, String value, Runnable action) {
        if (group.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(color("line"));
            LinearLayout.LayoutParams dividerLp = lp(-1, 1);
            dividerLp.leftMargin = dp(48);
            group.addView(divider, dividerLp);
        }
        LinearLayout info = row();
        info.setMinimumHeight(dp(78));
        info.setPadding(dp(6), dp(15), dp(6), dp(15));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResource);
        icon.setColorFilter(color("accent"));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        info.addView(icon, lp(24, 24));
        LinearLayout copy = column();
        copy.setPadding(dp(18), 0, dp(4), 0);
        copy.addView(text(label, 15, "text", true));
        TextView content = text(value, 13, action == null ? "muted" : "accent", false);
        content.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        LinearLayout.LayoutParams contentLp = lp(-1, -2);
        contentLp.topMargin = dp(3);
        copy.addView(content, contentLp);
        info.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        info.setContentDescription(label + "：" + value);
        if (action != null) {
            TextView arrow = text("↗", 21, "accent", false);
            arrow.setGravity(Gravity.CENTER);
            arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            info.addView(arrow, lp(22, 36));
            info.setBackground(new RippleDrawable(ColorStateList.valueOf(color("accentSoft")), null, shape(Color.WHITE, 0, 10)));
            info.setFocusable(true);
            info.setOnClickListener(v -> action.run());
        } else if (Build.VERSION.SDK_INT >= 28) info.setScreenReaderFocusable(true);
        group.addView(info, lp(-1, -2));
    }

    private void buildTypesPage(LinearLayout page){LinearLayout group=card();page.addView(group);group.addView(text("可用类型",12,"muted",false));for(LocalData.TypeOption type:data.types){if(!type.active)continue;LinearLayout row=row();TextView name=text(type.name,14,"text",false);row.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));TextView edit=smallAction("编辑",()->renameType(type));row.addView(edit,lp(54,38));TextView delete=smallAction("删除",()->deleteType(type));LinearLayout.LayoutParams deleteLp=lp(54,38);deleteLp.leftMargin=dp(5);row.addView(delete,deleteLp);group.addView(row,lp(-1,49));}LinearLayout add=row();EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setHint("添加类型");input.setHintTextColor(color("muted"));input.setTextColor(color("text"));input.setBackground(shape(color("surface2"),0,10));input.setPadding(dp(10),0,dp(10),0);add.addView(input,new LinearLayout.LayoutParams(0,dp(44),1));TextView button=smallAction("添加",()->addType(input));LinearLayout.LayoutParams buttonLp=lp(60,42);buttonLp.leftMargin=dp(8);add.addView(button,buttonLp);LinearLayout.LayoutParams addLp=lp(-1,48);addLp.topMargin=dp(13);page.addView(add,addLp);}

    private void renameType(LocalData.TypeOption type){EditText input=new EditText(this);input.setSingleLine(true);input.setText(type.name);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("编辑类型").setView(input).setNegativeButton("取消",null).setPositiveButton("保存",null).create();dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=input.getText().toString().trim();if(!validType(name,type.id))return;type.name=name;for(JSONObject entry:data.records.values())if(type.id.equals(entry.optString("typeId")))try{entry.put("typeName",name);}catch(JSONException ignoredException){}data.save(this);dialog.dismiss();showSettingsChild();}));dialog.show();}
    private boolean validType(String name,String exceptId){if(name.isEmpty()||name.length()>20){showToast("类型需为 1 至 20 个字");return false;}for(LocalData.TypeOption type:data.types)if(!type.id.equals(exceptId)&&type.name.equals(name)){showToast("类型名称不能重复");return false;}return true;}
    private void addType(EditText input){String name=input.getText().toString().trim();if(!validType(name,""))return;data.types.add(new LocalData.TypeOption(java.util.UUID.randomUUID().toString(),name,true));data.save(this);showSettingsChild();showToast("类型已添加");}
    private void deleteType(LocalData.TypeOption type){if(activeTypeCount()<=1){showToast("至少保留一个类型");return;}new AlertDialog.Builder(this).setMessage("删除“"+type.name+"”？历史记录会保留该名称。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{type.active=false;data.save(this);showSettingsChild();}).show();}

    private void buildReminderPage(LinearLayout page){LinearLayout group=card();page.addView(group);LinearLayout row=row();LinearLayout copy=column();copy.addView(text("开启提醒",15,"text",true));copy.addView(text("当天未记录时提醒",12,"muted",false));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));CheckBox toggle=new CheckBox(this);toggle.setButtonTintList(ColorStateList.valueOf(color("accent")));toggle.setChecked(data.reminderEnabled);toggle.setOnCheckedChangeListener((v,checked)->{data.reminderEnabled=checked;data.save(this);if(checked&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);});row.addView(toggle,lp(48,48));group.addView(row);LinearLayout time=row();LinearLayout timeCopy=column();timeCopy.addView(text("提醒时间",15,"text",true));timeCopy.addView(text("默认 23:45",12,"muted",false));time.addView(timeCopy,new LinearLayout.LayoutParams(0,-2,1));TextView button=smallAction(data.reminderTime,()->showTimePicker());time.addView(button,lp(82,40));LinearLayout.LayoutParams tp=lp(-1,55);tp.topMargin=dp(4);group.addView(time,tp);}
    private void showTimePicker(){String[] pieces=data.reminderTime.split(":");TimePickerDialog dialog=new TimePickerDialog(this,(TimePicker v,int hour,int minute)->{data.reminderTime=String.format(Locale.US,"%02d:%02d",hour,minute);data.save(this);showSettingsChild();},Integer.parseInt(pieces[0]),Integer.parseInt(pieces[1]),true);dialog.show();}
    private void buildAppearancePage(LinearLayout page){LinearLayout group=card();page.addView(group);group.addView(text("显示模式",15,"text",true));String[] labels={"跟随系统","明亮","暗色"};String[] values={"system","light","dark"};for(int i=0;i<values.length;i++){String value=values[i];android.widget.RadioButton option=new android.widget.RadioButton(this);option.setText(labels[i]);option.setTextColor(color("text"));option.setButtonTintList(ColorStateList.valueOf(color("accent")));option.setChecked(data.theme.equals(value));option.setOnClickListener(v->{data.theme=value;data.save(this);applyTheme();showSettingsChild();});group.addView(option,lp(-1,46));}}
    private void buildBackupPage(LinearLayout page){LinearLayout group=card();page.addView(group);group.addView(text("备份数据",12,"muted",false));LinearLayout export=row();LinearLayout copy=column();copy.addView(text("导出备份",15,"text",true));copy.addView(text("JSON，包含记录、类型和提醒设置",12,"muted",false));export.addView(copy,new LinearLayout.LayoutParams(0,-2,1));export.addView(smallAction("导出",()->exportBackup()),lp(66,40));group.addView(export,lp(-1,64));View divider=new View(this);divider.setBackgroundColor(color("line"));group.addView(divider,lp(-1,1));LinearLayout imp=row();LinearLayout impCopy=column();impCopy.addView(text("导入备份",15,"text",true));impCopy.addView(text("合并日期，同日期记录由备份覆盖",12,"muted",false));imp.addView(impCopy,new LinearLayout.LayoutParams(0,-2,1));imp.addView(smallAction("导入",()->importBackup()),lp(66,40));group.addView(imp,lp(-1,64));}

    private void chooseType(LocalDate date){ArrayList<LocalData.TypeOption> active=new ArrayList<>();for(LocalData.TypeOption item:data.types)if(item.active)active.add(item);LinearLayout list=column();list.setPadding(dp(18),dp(10),dp(18),dp(4));list.addView(text("选择类型",19,"text",true));TextView subtitle=text(dateCaption(date)+" · 导了",12,"muted",false);LinearLayout.LayoutParams subtitleLp=lp(-1,-2);subtitleLp.topMargin=dp(2);list.addView(subtitle,subtitleLp);ScrollView scroll=new ScrollView(this);LinearLayout.LayoutParams scrollLp=lp(-1,Math.min(380,active.size()*58));scrollLp.topMargin=dp(12);list.addView(scroll,scrollLp);LinearLayout options=column();options.setPadding(dp(1),dp(2),dp(1),dp(2));scroll.addView(options,new ScrollView.LayoutParams(-1,-2));AlertDialog dialog=new AlertDialog.Builder(this).setView(list).setNegativeButton("取消",null).create();for(int i=0;i<active.size();i++){LocalData.TypeOption item=active.get(i);LinearLayout option=row();option.setPadding(dp(11),0,dp(11),0);option.setBackground(shape(color("surface2"),color("line"),10));TextView index=text(String.format(Locale.CHINA,"%02d",i+1),12,"accent",true);index.setGravity(Gravity.CENTER);index.setBackground(shape(color("accentSoft"),0,99));option.addView(index,lp(34,34));TextView name=text(item.name,15,"text",true);LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(0,dp(54),1);nameLp.leftMargin=dp(10);option.addView(name,nameLp);TextView arrow=text("›",22,"muted",false);option.addView(arrow,lp(22,-1));option.setContentDescription("选择类型："+item.name);option.setOnClickListener(v->{dialog.dismiss();saveRecord(date,"导了",item);});LinearLayout.LayoutParams itemLp=lp(-1,54);itemLp.bottomMargin=dp(7);options.addView(option,itemLp);}dialog.show();}
    private void saveRecord(LocalDate date,String state,LocalData.TypeOption type){if(date.isAfter(today())){showToast("未来日期不能补记");return;}JSONObject previous=record(date);JSONObject next=new JSONObject();try{next.put("state",state);if("导了".equals(state)&&type!=null){next.put("typeId",type.id);next.put("typeName",type.name);}if(previous!=null)next.put("note",previous.optString("note",""));next.put("updatedAt",java.time.Instant.now().toString());}catch(JSONException ignored){}data.records.put(date.toString(),next);data.save(this);showRoute(route,false);showToast("已记录");}
    private void editState(LocalDate date){LinearLayout box=column();box.setPadding(dp(20),dp(12),dp(20),dp(4));box.addView(text("选择这一天的状态",18,"text",true));TextView caption=text(date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA)),13,"muted",false);LinearLayout.LayoutParams captionLp=lp(-1,-2);captionLp.topMargin=dp(3);box.addView(caption,captionLp);LinearLayout choices=row();LinearLayout.LayoutParams choicesLp=lp(-1,60);choicesLp.topMargin=dp(17);box.addView(choices,choicesLp);AlertDialog dialog=new AlertDialog.Builder(this).setView(box).setNegativeButton("取消",null).create();View done=actionButton("✓  导了","positive",()->{dialog.dismiss();chooseType(date);});choices.addView(done,new LinearLayout.LayoutParams(0,-1,1));Space gap=new Space(this);choices.addView(gap,lp(9,1));View missed=actionButton("–  没导","negative",()->{dialog.dismiss();saveRecord(date,"没导",null);});choices.addView(missed,new LinearLayout.LayoutParams(0,-1,1));dialog.show();}
    private void editNote(LocalDate date){JSONObject current=record(date);if(current==null){showToast("请先完成当天状态记录");return;}EditText note=new EditText(this);note.setText(current.optString("note",""));note.setMinLines(3);note.setMaxLines(5);note.setTextSize(15);note.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(100)});new AlertDialog.Builder(this).setTitle("编辑备注 · "+dateCaption(date)).setView(note).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{try{current.put("note",note.getText().toString().trim());}catch(JSONException ignored){}data.save(this);showRoute(route,false);}).show();}

    private void exportBackup(){try{exportJson=data.toJson().toString(2);Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"DAO_Leme_backup.json");startActivityForResult(intent,REQUEST_EXPORT);}catch(Exception ex){showToast("无法创建备份");}}
    private void importBackup(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");startActivityForResult(intent,REQUEST_IMPORT);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);if(resultCode!=RESULT_OK||intent==null||intent.getData()==null)return;try{Uri uri=intent.getData();if(requestCode==REQUEST_EXPORT){try(java.io.OutputStream stream=getContentResolver().openOutputStream(uri)){if(stream==null||exportJson==null)throw new IllegalStateException();stream.write(exportJson.getBytes(StandardCharsets.UTF_8));}showToast("备份已导出");}else if(requestCode==REQUEST_IMPORT){String raw;try(InputStream stream=getContentResolver().openInputStream(uri)){if(stream==null)throw new IllegalStateException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int read;while((read=stream.read(buffer))!=-1){if(bytes.size()+read>8*1024*1024)throw new IllegalArgumentException("Backup too large");bytes.write(buffer,0,read);}raw=new String(bytes.toByteArray(),StandardCharsets.UTF_8);}JSONObject root=new JSONObject(raw);JSONObject records=root.optJSONObject("records");int affected=records==null?0:records.length();new AlertDialog.Builder(this).setMessage("将合并 "+affected+" 个日期，同日期已有记录将由备份覆盖。继续吗？").setNegativeButton("取消",null).setPositiveButton("导入",(d,w)->{try{data.mergeJson(raw);data.save(this);showRoute("checkin",false);showToast("备份已导入");}catch(Exception exception){showToast("备份文件无效");}}).show();}}catch(Exception exception){showToast("备份文件无效或无法读取");}}

    private void handleBack(){if("settings".equals(route)&&!"overview".equals(settingsPage)){settingsPage="overview";showRoute("settings",true);return;}if(!"checkin".equals(route)){showRoute("checkin",true);return;}finish();}
    @Override @SuppressWarnings("deprecation") public void onBackPressed(){handleBack();}

    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);if("system".equals(data.theme)){dark=(configuration.uiMode&0x30)==0x20;applyTheme();showRoute(route,false);}}
}
