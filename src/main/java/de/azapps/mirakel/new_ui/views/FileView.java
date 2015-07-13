/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.cocosw.bottomsheet.BottomSheet;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.azapps.material_elements.utils.MenuHelper;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.views.FFTAudioView;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.fragments.TaskFragment;
import de.azapps.mirakel.new_ui.helper.AudioHelper;
import de.azapps.mirakel.new_ui.helper.ImageLoader;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class FileView extends LinearLayout implements View.OnClickListener,
    View.OnLongClickListener, DialogInterface.OnClickListener, FFTAudioView.OnRecordFinished,
    DialogInterface.OnDismissListener {
    private static final String TAG = "FileView";
    private final LayoutInflater inflater;
    private List<FileMirakel> files = new ArrayList<>(0);
    private ArrayList<FileMirakel> markedFiles = new ArrayList<>();
    private int COUNT_PER_LINE;
    private final LinearLayout.LayoutParams PREVIEW_SIZE;
    private final int MIN_PREVIEW_PADDING;
    private Task task;
    @Nullable
    private Activity activity;
    @Nullable
    private Uri photoUri;
    private boolean isDelete = false;
    private ImageView fileActionIcon;
    private View fileAction;
    private TextView fileActionText;
    private int height;
    private int width;
    private boolean is_show_bottom_sheet = false;
    private Dialog recordAudioDialog;

    public FileView(final Context context) {
        this(context, null);
    }

    public FileView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        COUNT_PER_LINE = getResources().getInteger(R.integer.file_count_per_line);
        final int size = (int) getResources().getDimension(R.dimen.file_preview_size);
        final int padding = (int) getResources().getDimension(R.dimen.padding_default_half);
        PREVIEW_SIZE = new LinearLayout.LayoutParams(size, size);
        MIN_PREVIEW_PADDING = (int) (getResources().getDimension(R.dimen.file_preview_padding) /
                                     2.0F);
        PREVIEW_SIZE.setMargins(MIN_PREVIEW_PADDING, padding, MIN_PREVIEW_PADDING, padding);

        inflater = LayoutInflater.from(context);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int count = w / PREVIEW_SIZE.width;
        while ((w - (count * PREVIEW_SIZE.width)) < (count + MIN_PREVIEW_PADDING)) {
            count--;
        }
        COUNT_PER_LINE = count;
        final int padding = (int) ((w - (count * PREVIEW_SIZE.width)) / ((count + 1.0F) * 2.0F));
        PREVIEW_SIZE.leftMargin = padding;
        PREVIEW_SIZE.rightMargin = padding;
        fixChilds(false);
    }

    public void addFile() {
        BottomSheet builder = new BottomSheet.Builder(activity)
        .title(R.string.add_files)
        .sheet(R.menu.add_file_menu)
        .grid()
        .setOnDismissListener(this)
        .listener(this).build();
        Menu menu = builder.getMenu();
        MenuHelper.colorizeMenuItems(menu, ThemeManager.getColor(R.attr.colorTextGrey));
        is_show_bottom_sheet = true;
        builder.show();
    }

    public void addPhoto() {
        if ((photoUri != null) && (task != null)) {
            //the new file is present because photouri/=null
            AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_PHOTO);
            files.add(FileMirakel.newFile(getContext(), task, photoUri).get());
            photoUri = null;
            fixChilds();
        }
    }

    public void addFile(final @NonNull Uri uri) {
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_FILE);
        final Optional<FileMirakel> fileMirakelOptional = FileMirakel.newFile(getContext(), task, uri);
        if (fileMirakelOptional.isPresent()) {
            files.add(fileMirakelOptional.get());
            fixChilds();
        }
    }


    public void setActivity(final @NonNull Activity a) {
        activity = a;
    }


    public void setFiles(final @NonNull Task t) {
        if ((task != null) && (t.getId() == task.getId())) {
            return;
        }
        task = t;
        this.files = t.getFiles();
        fixChilds();
    }

    private void fixChilds() {
        fixChilds(true);
    }

    private void fixChilds(final boolean clearMarkedFiles) {
        removeAllViews();
        if (clearMarkedFiles) {
            markedFiles.clear();
        }
        int counter = 0;
        final List<ImageView> imageViews = new ArrayList<>(files.size());
        while (counter < (files.size() + 1)) {
            final LinearLayout wrapper = new LinearLayout(getContext());
            wrapper.setOrientation(HORIZONTAL);
            wrapper.setGravity(Gravity.CENTER_VERTICAL);
            counter = getPreviewLine(counter, imageViews, wrapper);
            addView(wrapper);
        }
        invalidate();
        new ImageLoader(imageViews, getContext()).execute(files.toArray(new FileMirakel[files.size()]));
    }

    @Override
    public void invalidate() {
        measure();
        super.invalidate();
    }

    private void measure() {
        if (this.getOrientation() == LinearLayout.VERTICAL) {
            int h = 0;
            int w = 0;
            this.measureChildren(0, 0);
            for (int i = 0; i < this.getChildCount(); i++) {
                View v = this.getChildAt(i);
                h += v.getMeasuredHeight();
                w = (w < v.getMeasuredWidth()) ? v.getMeasuredWidth() : w;
            }
            height = (h < height) ? height : h;
            width = (w < width) ? width : w;
        }
        this.setMeasuredDimension(width, height);
    }

    private void deleteMarkedFiles() {
        for (final FileMirakel f : markedFiles) {
            if (files.remove(f)) {
                f.destroy();
            }
        }
        fixChilds();
    }

    private int getPreviewLine(int counter, final List<ImageView> imageViews,
                               final LinearLayout wrapper) {
        for (int i = 0; i < COUNT_PER_LINE; i++) {
            if (counter < files.size()) {
                final FileMirakel file = files.get(counter);
                final View preview = inflater.inflate(R.layout.file_item, null);
                preview.setLayoutParams(PREVIEW_SIZE);
                colorImage(preview, markedFiles.contains(file) ? ThemeManager.getColor(
                               R.attr.colorCAB) : Color.TRANSPARENT);
                imageViews.add((ImageView) preview.findViewById(R.id.file_preview_image));
                ((TextView) preview.findViewById(R.id.file_preview_text)).setText(file.getName());
                preview.setTag(file);
                preview.setOnClickListener(this);
                preview.setOnLongClickListener(this);
                wrapper.addView(preview);
            } else if (counter == files.size()) {
                fileAction = inflater.inflate(R.layout.add_file_view, null);
                fileAction.setLayoutParams(PREVIEW_SIZE);
                fileActionIcon = (ImageView) fileAction.findViewById(R.id.add_file);
                fileActionText = (TextView) fileAction.findViewById(R.id.add_file_text);

                fileAction.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (isDelete) {
                            deleteMarkedFiles();
                        } else {
                            addFile();
                        }
                    }
                });
                setAddFile();
                wrapper.addView(fileAction);
            }
            counter++;
        }
        return counter;
    }

    private void setFileActionBackground(final int backgroundColor) {
        final GradientDrawable background = (GradientDrawable) fileAction.getBackground();
        background.setColor(backgroundColor);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            fileAction.setBackgroundDrawable(background);
        } else {
            fileAction.setBackground(background);
        }
    }

    private void setAddFile() {
        final int color = ThemeManager.getColor(R.attr.colorPreviewBorder);
        fileActionIcon.setImageDrawable(ThemeManager.getColoredIcon(R.drawable.ic_plus_white_48dp, color));
        setFileActionBackground(getResources().getColor(android.R.color.transparent));
        fileActionText.setText(R.string.add_file);
        fileActionText.setTextColor(color);
        isDelete = false;
    }


    private void setDeleteFiles() {
        final int color = ThemeManager.getColor(R.attr.colorPreviewBorder);
        fileActionIcon.setImageDrawable(ThemeManager.getColoredIcon(R.drawable.ic_delete_white_48dp,
                                        color));
        fileActionText.setText(R.string.delete_files);
        fileActionText.setTextColor(color);
        isDelete = true;
    }


    @Override
    public void onClick(final View v) {
        final FileMirakel fileMirakel = (FileMirakel) v.getTag();
        if (markedFiles.isEmpty()) {
            if (FileUtils.isAudio(fileMirakel.getFileUri())) {
                new AlertDialogWrapper.Builder(getContext())
                .setItems(R.array.file_options_play, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        switch (which) {
                        case 0:
                            AudioHelper.playBack(activity, fileMirakel, true);
                            break;
                        case 1:
                            AudioHelper.playBack(activity, fileMirakel, false);
                            break;
                        case 2:
                            openFile(getContext(), fileMirakel);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown options");
                        }
                    }
                }).show();

            } else {
                openFile(getContext(), fileMirakel);
            }
        } else if (markedFiles.contains(fileMirakel)) {
            colorImage(v, Color.TRANSPARENT);
            markedFiles.remove(fileMirakel);
            if (isDelete && markedFiles.isEmpty()) {
                setAddFile();
            }
        } else {
            markedFiles.add(fileMirakel);
            colorImage(v, ThemeManager.getColor(R.attr.colorCAB));
        }
    }

    private static void colorImage(final @NonNull View imageWrapper, final int color) {
        final ImageView preview = (ImageView) imageWrapper.findViewById(R.id.file_preview_image);
        if (preview != null) {
            preview.setColorFilter(color, PorterDuff.Mode.OVERLAY);
            preview.invalidate();
        }
    }


    @Override
    public boolean onLongClick(final View v) {
        final FileMirakel file = (FileMirakel) v.getTag();
        if ((v.getTag() != null) && !markedFiles.contains(file)) {
            markedFiles.add(file);
            colorImage(v, ThemeManager.getColor(R.attr.colorCAB));
            if (markedFiles.size() == 1) {
                setDeleteFiles();
            }
            return true;
        }
        return false;
    }

    public static void openFile(final Context context, final FileMirakel file) {
        // We can't move this to helpers because we need the model dependency
        final String mimetype = FileUtils.getMimeType(file.getFileUri());
        final Intent i2 = new Intent();
        i2.setAction(android.content.Intent.ACTION_VIEW);
        i2.setDataAndType(file.getFileUri(), mimetype);
        try {
            context.startActivity(i2);
        } catch (final ActivityNotFoundException e) {
            ErrorReporter.report(ErrorType.FILE_NO_ACTIVITY);
        }
    }


    private Dialog createAudioDialog() {
        final View layout = inflate(getContext(), R.layout.view_record_audio, null);
        final FFTAudioView audioView = (FFTAudioView) layout.findViewById(R.id.audio_view);
        try {
            audioView.init(FileUtils.getOutputMediaFile(FileUtils.MEDIA_TYPE_AUDIO).getAbsolutePath(), this);
        } catch (final IOException e) {
            ErrorReporter.report(ErrorType.FILE_NOT_FOUND);
            Log.wtf(TAG, "failed to create outputfile", e);
            return null;
        } catch (final FFTAudioView.RecordingFailedException e) {
            Log.wtf(TAG, "failed to record audio");
            ErrorReporter.report(ErrorType.NO_SPEACH_RECOGNITION);
            return null;
        }
        Dialog dialog = new AlertDialogWrapper.Builder(getContext()).setTitle(R.string.audio_record_title)
        .setView(layout)
        .setPositiveButton(android.R.string.ok, null)
        .setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface dialog) {
                audioView.stopRecording();
                recordAudioDialog = null;
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                audioView.startRecording();
            }
        });
        return dialog;
    }

    void recordAudio() {
        recordAudioDialog = createAudioDialog();
        if (recordAudioDialog != null) {
            recordAudioDialog.show();
        }
    }

    @Override
    public void finishRecording(final String path) {
        final File f = new File(path);
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_RECORDING);
        files.add(FileMirakel.newFile(task, f.getName(), Uri.fromFile(new File(path))));
        fixChilds();
    }


    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        if (activity == null) {
            ErrorReporter.report(ErrorType.FILE_NO_ACTIVITY);
            return;
        }
        switch (which) {
        case R.id.file_photo:
            try {
                final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoUri = FileUtils.getOutputMediaFileUri(FileUtils.MEDIA_TYPE_IMAGE);
                if (photoUri == null) {
                    return;
                }
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                activity.startActivityForResult(cameraIntent,
                                                TaskFragment.REQUEST_IMAGE_CAPTURE);
            } catch (final ActivityNotFoundException a) {
                ErrorReporter.report (ErrorType.PHOTO_NO_CAMERA);
            } catch (final IOException e) {
                if (e.getMessage ().equals (FileUtils.ERROR_NO_MEDIA_DIR)) {
                    ErrorReporter
                    .report(ErrorType.PHOTO_NO_MEDIA_DIRECTORY);
                }
            }
            break;
        case R.id.file_audio:
            recordAudio();
            break;
        case R.id.file_file:
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                activity.startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    TaskFragment.FILE_SELECT_CODE);
            } catch (final android.content.ActivityNotFoundException ignored) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(getContext(), "Please install a File Manager.",
                               Toast.LENGTH_SHORT).show();
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown filetype");
        }
    }

    private static final String PARENT_STATE = "parent";
    private static final String MARKED_FILES = "marked_files";
    private static final String BOTTOM_SHEET_SHOWN = "bottom_sheet";
    private static final String PHOTO_URI = "photo_uri";

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle out = new Bundle();
        out.putParcelable(PARENT_STATE, super.onSaveInstanceState());
        out.putParcelableArrayList(MARKED_FILES, markedFiles);
        out.putBoolean(BOTTOM_SHEET_SHOWN, is_show_bottom_sheet);
        out.putParcelable(PHOTO_URI, photoUri);
        return out;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof Bundle)) {
            return;
        }
        final Bundle saved = (Bundle) state;
        super.onRestoreInstanceState(saved.getParcelable(PARENT_STATE));
        photoUri = saved.getParcelable(PHOTO_URI);
        markedFiles = saved.getParcelableArrayList(MARKED_FILES);
        fixChilds(false);
        if (!markedFiles.isEmpty()) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setDeleteFiles();
                }
            }, 10L);

        }
        if (saved.getBoolean(BOTTOM_SHEET_SHOWN)) {
            addFile();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        is_show_bottom_sheet = false;
    }
}
