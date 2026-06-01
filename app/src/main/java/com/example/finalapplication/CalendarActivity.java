package com.example.finalapplication;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class CalendarActivity extends BaseActivity {

    private static final String[] MONTH_NAMES = {
            "ינואר","פברואר","מרץ","אפריל","מאי","יוני",
            "יולי","אוגוסט","ספטמבר","אוקטובר","נובמבר","דצמבר"
    };
    private static final String[] DAY_HEADERS = {"א׳","ב׳","ג׳","ד׳","ה׳","ו׳","ש׳"};

    private TextView tvMonthYear, tvEventsHeader;
    private ImageButton btnPrev, btnNext;
    private LinearLayout rowDayHeaders, layoutEmptyEvents;
    private RecyclerView rvDays, rvEvents;
    private FloatingActionButton fabAddEvent;

    private FirebaseFirestore db;
    private String currentUid, currentName, userFamilyCode, userRole;

    private final Calendar displayedMonth = Calendar.getInstance();
    private String selectedDate = null;

    private final List<DayItem> dayItems = new ArrayList<>();
    private final List<CalendarEvent> dayEvents = new ArrayList<>();
    private final Set<String> dotDates = new HashSet<>();

    private DayAdapter dayAdapter;
    private EventAdapter eventAdapter;

    private final List<CalendarEvent> monthEvents = new ArrayList<>();

    private static final int REQUEST_CALENDAR_PERMISSION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        bindViews();
        buildDayHeaderRow();
        setupRecyclerViews();
        setupMonthNavigation();

        fabAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarActivity.this.showAddEventDialog();
            }
        });

        ImageButton btnSync = findViewById(R.id.btnSyncCalendar);
        if (btnSync != null) btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarActivity.this.onSyncClicked();
            }
        });

        loadUserThenMonth();
    }

    private void bindViews() {
        tvMonthYear       = findViewById(R.id.tvMonthYear);
        btnPrev           = findViewById(R.id.btnPrevMonth);
        btnNext           = findViewById(R.id.btnNextMonth);
        rowDayHeaders     = findViewById(R.id.rowDayHeaders);
        rvDays            = findViewById(R.id.rvDays);
        rvEvents          = findViewById(R.id.rvEvents);
        tvEventsHeader    = findViewById(R.id.tvEventsHeader);
        layoutEmptyEvents = findViewById(R.id.layoutEmptyEvents);
        fabAddEvent       = findViewById(R.id.fabAddEvent);
    }

    private void buildDayHeaderRow() {
        rowDayHeaders.removeAllViews();
        for (String label : DAY_HEADERS) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            tv.setLayoutParams(lp);
            tv.setText(label);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(13f);
            tv.setTextColor(0xFFBCAAA4);
            tv.setTypeface(null, Typeface.BOLD);
            rowDayHeaders.addView(tv);
        }
    }

    private void setupRecyclerViews() {
        dayAdapter = new DayAdapter();
        rvDays.setLayoutManager(new GridLayoutManager(this, 7));
        rvDays.setAdapter(dayAdapter);

        eventAdapter = new EventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
    }

    private void setupMonthNavigation() {
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarActivity.this.shiftMonth(-1);
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalendarActivity.this.shiftMonth(+1);
            }
        });
    }

    private void shiftMonth(int delta) {
        displayedMonth.add(Calendar.MONTH, delta);
        selectedDate = null;
        dayEvents.clear();
        eventAdapter.notifyDataSetChanged();
        dotDates.clear();
        tvEventsHeader.setText("בחר יום לצפייה באירועים");
        layoutEmptyEvents.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        if (userFamilyCode != null) loadMonthDots();
    }

    private void loadUserThenMonth() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (!doc.exists()) return;
                        currentName = doc.getString("name");
                        userFamilyCode = doc.getString("familyCode");
                        userRole = doc.getString("role");
                        CalendarActivity.this.loadMonthDots();
                    }
                });
    }

    private void loadMonthDots() {
        int year    = displayedMonth.get(Calendar.YEAR);
        int month   = displayedMonth.get(Calendar.MONTH) + 1;
        int lastDay = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        String firstDay = dateStr(year, month, 1);
        String lastDayS = dateStr(year, month, lastDay);

        db.collection("events")
                .whereEqualTo("familyCode", userFamilyCode)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        dotDates.clear();
                        monthEvents.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            String d = doc.getString("date");
                            if (d != null && d.compareTo(firstDay) >= 0 && d.compareTo(lastDayS) <= 0) {
                                dotDates.add(d);
                                CalendarEvent e = doc.toObject(CalendarEvent.class);
                                e.setEventId(doc.getId());
                                monthEvents.add(e);
                            }
                        }
                        CalendarActivity.this.buildGrid();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CalendarActivity.this, "שגיאה בטעינת יומן: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadDayEvents(String date) {
        db.collection("events")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        dayEvents.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            CalendarEvent e = doc.toObject(CalendarEvent.class);
                            e.setEventId(doc.getId());
                            dayEvents.add(e);
                        }
                        dayEvents.sort(new Comparator<CalendarEvent>() {
                            @Override
                            public int compare(CalendarEvent a, CalendarEvent b) {
                                return Long.compare(a.getTimestamp(), b.getTimestamp());
                            }
                        });
                        eventAdapter.notifyDataSetChanged();
                        boolean empty = dayEvents.isEmpty();
                        rvEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
                        layoutEmptyEvents.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CalendarActivity.this, "שגיאה בטעינת אירועים: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void buildGrid() {
        updateHeader();
        dayItems.clear();

        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);

        for (int i = 1; i < firstDow; i++) {
            dayItems.add(new DayItem(0, false, false, false));
        }

        Calendar today    = Calendar.getInstance();
        int todayYear     = today.get(Calendar.YEAR);
        int todayMonth    = today.get(Calendar.MONTH);
        int todayDay      = today.get(Calendar.DAY_OF_MONTH);
        int dispYear      = displayedMonth.get(Calendar.YEAR);
        int dispMonth     = displayedMonth.get(Calendar.MONTH);
        int daysInMonth   = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday   = dispYear == todayYear && dispMonth == todayMonth && day == todayDay;
            String ds         = dateStr(dispYear, dispMonth + 1, day);
            boolean hasEvents = dotDates.contains(ds);
            boolean isSelected = ds.equals(selectedDate);
            dayItems.add(new DayItem(day, isToday, isSelected, hasEvents));
        }

        dayAdapter.notifyDataSetChanged();
    }

    private void updateHeader() {
        String text = MONTH_NAMES[displayedMonth.get(Calendar.MONTH)]
                + "  " + displayedMonth.get(Calendar.YEAR);
        tvMonthYear.setText(text);
    }

    private void showAddEventDialog() {
        String target = selectedDate != null ? selectedDate : todayStr();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        TextInputEditText etTitle      = dialogView.findViewById(R.id.etEventTitle);
        TextInputEditText etNotes      = dialogView.findViewById(R.id.etEventNotes);
        android.widget.Button btnTime  = dialogView.findViewById(R.id.btnPickTime);
        android.widget.TextView tvTime = dialogView.findViewById(R.id.tvSelectedTime);

        final String[] chosenTime = {null};

        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                new TimePickerDialog(CalendarActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker tp, int hour, int minute) {
                        chosenTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        tvTime.setText(chosenTime[0]);
                        tvTime.setTextColor(0xFF40E0D0);
                    }
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("אירוע חדש — " + humanDate(target))
                .setView(dialogView)
                .setPositiveButton("שמור", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String title = etTitle.getText() != null
                                ? etTitle.getText().toString().trim() : "";
                        if (title.isEmpty()) {
                            Toast.makeText(CalendarActivity.this, "נא להזין שם אירוע", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String notes = etNotes.getText() != null
                                ? etNotes.getText().toString().trim() : "";
                        CalendarActivity.this.saveEvent(title, notes, target, chosenTime[0]);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void saveEvent(String title, String notes, String date, String time) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",      title);
        data.put("notes",      notes);
        data.put("date",       date);
        data.put("time",       time != null ? time : "");
        data.put("familyCode", userFamilyCode);
        data.put("authorUid",  currentUid);
        data.put("authorName", currentName != null ? currentName : "");
        data.put("timestamp",  System.currentTimeMillis());

        db.collection("events").add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference ref) {
                        dotDates.add(date);
                        CalendarActivity.this.buildGrid();
                        if (date.equals(selectedDate)) CalendarActivity.this.loadDayEvents(date);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CalendarActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteEvent(String eventId, String date) {
        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        CalendarActivity.this.loadMonthDots();
                        if (date.equals(selectedDate)) CalendarActivity.this.loadDayEvents(date);
                    }
                });
    }

    private String dateStr(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    private String todayStr() {
        Calendar c = Calendar.getInstance();
        return dateStr(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private String humanDate(String iso) {
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            return out.format(in.parse(iso));
        } catch (Exception e) { return iso; }
    }

    private static class DayItem {
        final int day;
        final boolean isToday;
        boolean isSelected;
        final boolean hasEvents;
        DayItem(int d, boolean today, boolean selected, boolean events) {
            day = d; isToday = today; isSelected = selected; hasEvents = events;
        }
    }

    private class DayAdapter extends RecyclerView.Adapter<DayAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.calendar_day_cell, parent, false);
            int side = parent.getMeasuredWidth() / 7;
            if (side > 0) {
                v.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT, side));
            }
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DayItem item = dayItems.get(position);

            if (item.day == 0) {
                h.tvDay.setText("");
                h.circle.setBackground(null);
                h.dot.setVisibility(View.INVISIBLE);
                h.itemView.setClickable(false);
                return;
            }

            h.tvDay.setText(String.valueOf(item.day));
            h.dot.setVisibility(item.hasEvents ? View.VISIBLE : View.INVISIBLE);
            h.itemView.setClickable(true);

            if (item.isToday) {
                h.circle.setBackgroundResource(R.drawable.bg_circle_turquoise);
                h.tvDay.setTextColor(0xFF004D40);
                h.tvDay.setTypeface(null, Typeface.BOLD);
            } else if (item.isSelected) {
                h.circle.setBackgroundResource(R.drawable.bg_circle_selected);
                h.tvDay.setTextColor(0xFF3E2723);
                h.tvDay.setTypeface(null, Typeface.BOLD);
            } else {
                h.circle.setBackground(null);
                h.tvDay.setTextColor(0xFF3E2723);
                h.tvDay.setTypeface(null, Typeface.NORMAL);
            }

            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (DayItem d : dayItems) d.isSelected = false;
                    item.isSelected = true;
                    DayAdapter.this.notifyDataSetChanged();

                    selectedDate = CalendarActivity.this.dateStr(
                            displayedMonth.get(Calendar.YEAR),
                            displayedMonth.get(Calendar.MONTH) + 1,
                            item.day);
                    tvEventsHeader.setText("אירועים — " + CalendarActivity.this.humanDate(selectedDate));
                    CalendarActivity.this.loadDayEvents(selectedDate);
                }
            });
        }

        @Override public int getItemCount() { return dayItems.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvDay;
            final LinearLayout circle;
            final View dot;
            VH(@NonNull View v) {
                super(v);
                tvDay  = v.findViewById(R.id.tvDay);
                circle = v.findViewById(R.id.dayCircle);
                dot    = v.findViewById(R.id.vEventDot);
            }
        }
    }

    private void onSyncClicked() {
        if (monthEvents.isEmpty()) {
            Toast.makeText(this, "אין אירועים לסנכרון בחודש זה", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.READ_CALENDAR,
                            android.Manifest.permission.WRITE_CALENDAR},
                    REQUEST_CALENDAR_PERMISSION);
        } else {
            confirmAndSync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                confirmAndSync();
            } else {
                Toast.makeText(this, "נדרשת הרשאה לגישה ללוח השנה", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmAndSync() {
        String month = MONTH_NAMES[displayedMonth.get(Calendar.MONTH)]
                + " " + displayedMonth.get(Calendar.YEAR);
        new AlertDialog.Builder(this)
                .setTitle("סנכרון עם Google Calendar")
                .setMessage("לייצא " + monthEvents.size() + " אירועים של " + month
                        + " לאפליקציית לוח השנה של המכשיר?")
                .setPositiveButton("סנכרן", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        CalendarActivity.this.exportEventsToDeviceCalendar();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void exportEventsToDeviceCalendar() {
        long calId = getPrimaryCalendarId();
        if (calId == -1) {
            Toast.makeText(this, "לא נמצא לוח שנה של Google במכשיר", Toast.LENGTH_LONG).show();
            return;
        }

        int count = 0;
        for (CalendarEvent event : monthEvents) {
            try {
                long startMillis = parseEventMillis(event.getDate(), event.getTime());
                long endMillis   = startMillis + 3600_000L;

                ContentValues cv = new ContentValues();
                cv.put(CalendarContract.Events.CALENDAR_ID,    calId);
                cv.put(CalendarContract.Events.TITLE,          event.getTitle() != null ? event.getTitle() : "");
                cv.put(CalendarContract.Events.DESCRIPTION,    event.getNotes() != null ? event.getNotes() : "");
                cv.put(CalendarContract.Events.DTSTART,        startMillis);
                cv.put(CalendarContract.Events.DTEND,          endMillis);
                cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

                Uri result = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, cv);
                if (result != null) count++;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "סונכרנו " + count + " אירועים בהצלחה", Toast.LENGTH_SHORT).show();
    }

    private long getPrimaryCalendarId() {
        try (android.database.Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID},
                CalendarContract.Calendars.IS_PRIMARY + " = 1",
                null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception ignored) {}
        try (android.database.Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars._ID},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private long parseEventMillis(String date, String time) {
        try {
            String t = (time != null && !time.isEmpty()) ? time : "09:00";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date d = sdf.parse(date + " " + t);
            return d != null ? d.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.calendar_event_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CalendarEvent e = dayEvents.get(position);
            h.tvTitle.setText(e.getTitle() != null ? e.getTitle() : "");
            String timeStr = (e.getTime() != null && !e.getTime().isEmpty()) ? "🕐 " + e.getTime() + "  " : "";
            h.tvAuthor.setText(timeStr + "נוסף ע\"י: " + (e.getAuthorName() != null ? e.getAuthorName() : ""));

            String notes = e.getNotes();
            if (notes != null && !notes.isEmpty()) {
                h.tvNotes.setVisibility(View.VISIBLE);
                h.tvNotes.setText(notes);
            } else {
                h.tvNotes.setVisibility(View.GONE);
            }

            boolean canDelete = currentUid.equals(e.getAuthorUid()) || "הורה".equals(userRole);
            h.itemView.setOnLongClickListener(canDelete ? new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(CalendarActivity.this)
                            .setTitle("מחיקת אירוע")
                            .setMessage("למחוק את \"" + e.getTitle() + "\"?")
                            .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    CalendarActivity.this.deleteEvent(e.getEventId(), e.getDate());
                                }
                            })
                            .setNegativeButton("ביטול", null)
                            .show();
                    return true;
                }
            } : null);
        }

        @Override public int getItemCount() { return dayEvents.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvTitle, tvAuthor, tvNotes;
            VH(@NonNull View v) {
                super(v);
                tvTitle  = v.findViewById(R.id.tvEventTitle);
                tvAuthor = v.findViewById(R.id.tvEventAuthor);
                tvNotes  = v.findViewById(R.id.tvEventNotes);
            }
        }
    }
}