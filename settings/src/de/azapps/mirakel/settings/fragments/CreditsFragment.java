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

package de.azapps.mirakel.settings.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;

public class CreditsFragment extends Fragment implements IDetailFragment<Settings> {

    private static final String[][] LIBRARIES = {
        {"Gson", "Apache 2.0", "https://code.google.com/p/google-gson/"},
        {"Joda-Time", "Apache 2.0", "http://joda-time.sourceforge.net"},
        {"Android Change Log", "Apache 2.0", "https://code.google.com/p/android-change-log/"},
        {"ACRA", "Apache 2.0", "http://acra.ch"},
        {"HoloColorPicker", "Apache 2.0", "https://github.com/LarsWerkman/HoloColorPicker"},
        {"DateTimePicker Compatibility Library", "Apache 2.0", "https://github.com/flavienlaurent/datetimepicker"},
        {"Guava", "Apache 2.0", "https://github.com/google/guava"},
        {"Butter Knife", "Apache 2.0", "https://github.com/JakeWharton/butterknife"},
        {"task-provider", "Apache 2.0", "https://github.com/dmfs/task-provider"},
        {"DragSortRecycler", "Apache 2.0", "https://github.com/emileb/DragSortRecycler"},

    };
    private static final String[][] TRANSLATIONS = {
        {"български", "boriana_tcholakova"},
        {"čeština", "boriana_tcholakova, Jaroslav Lichtblau, sarimak"},
        {"Deutsch", "Anatolij Zelenin, Georg Semmler, Philipp Schmutz, Wilhelm Wedernikow, Tiziano Müller, madmaxbz, Julius Härtl, bummkugel, anton-w, overlook, granner, Patrik Kernstock"},
        {"Español", "Rajaa Gutknecht, Gonzalo “Gonlos” Cortazar Vadillo, polkillas, macebal, Pablo Corbalan, afduggirala, ideas1, sml, Wenceslao Grillo"},
        {"Euskal", "Osoitz"},
        {"Français", "Rajaa Gutknecht, Olivier Le Moal, Ghost of Kendo, nbossard, choufleur, waghanza, Thomas Jost, Jordan Bouëllat, benasse, Vince4Git, senufo, Christophe Oger"},
        {"Italiano", "Rajaa Gutknecht, madmaxbz, Claudio Arseni, fazen, Marco Bonifacio"},
        {"Nederlands", "jaj.advert, mthmulders, arnoud, mthmulders, Barend, T-v-Gerwen"},
        {"Polski", "-= Poll =-, Skibbi, mruwek"},
        {"Português", "Sérgio Marques"},
        {"Русский", "xoyk, Dmitry Derjavin, Mark Prechesny, Katy, iltrof, direwolf, Хадисов Александр"},
        {"Slovenščina", "Matej"},
        {"العربية", "Rajaa Gutknecht, عبد الناصر سعيد الثبيتي, S. Wasim Tayyeb"},
        {"日本人", "bushrang3r"},
        //{"Chinese", "SICONG JIANG, xiaobo zhou"},
        //{"Swedish", "Henrik Mattsson-Mårn, AsavarTzeth"},
        //{"Slovak", "bertone"},
        //{"Hebrew", "michael1993"},
        //{"Norwegian", "Jim-Stefhan Johansen"},
        //{"Catalan", "sml"},
    };
    private LayoutInflater inflater;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getItem().getName());
        }
        this.inflater = inflater;
        final View rootView = inflater.inflate(R.layout.fragment_credits, null);

        final TextView devCredits = (TextView) rootView.findViewById(R.id.dev_credits);
        devCredits.setText(Html.fromHtml(getString(R.string.credits_dev)));
        devCredits.setMovementMethod(LinkMovementMethod.getInstance());
        final ImageView devCreditsIcon = (ImageView) rootView.findViewById(R.id.dev_credits_icon);
        devCreditsIcon.setImageDrawable(ThemeManager.getColoredIcon(R.drawable.ic_build_white_24dp,
                                        ThemeManager.getColor(R.attr.colorTextSettings)));

        final TextView designCredits = (TextView) rootView.findViewById(R.id.design_credits);
        designCredits.setText(Html.fromHtml(getString(R.string.credits_design)));
        designCredits.setMovementMethod(LinkMovementMethod.getInstance());
        final ImageView designCreditsIcon = (ImageView) rootView.findViewById(R.id.design_credits_icon);
        designCreditsIcon.setImageDrawable(ThemeManager.getColoredIcon(R.drawable.ic_brush_white_24dp,
                                           ThemeManager.getColor(R.attr.colorTextSettings)));

        initTranslations(rootView);
        initLibaries(rootView);
        initButtons(rootView);
        final TextView creditTextLicense = (TextView) rootView.findViewById(R.id.credits_license_text);
        creditTextLicense.setText(Html.fromHtml(getString(R.string.credits_license)));
        creditTextLicense.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    private TextView getListTextView() {
        return (TextView) inflater.inflate(R.layout.view_credits_list, null);
    }

    private void initLibaries(final View rootView) {
        final LinearLayout librariesWrapper = (LinearLayout) rootView.findViewById(R.id.libraries_wrapper);

        for (final String[] library : CreditsFragment.LIBRARIES) {
            final String name = library[0];
            final String license = library[1];
            final String url = library[2];

            final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(
                name + " (" + license + ")");
            final URLSpan urlSpan = new URLSpan(url);
            spannableStringBuilder.setSpan(urlSpan, 0, name.length(), 0);
            spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length(), 0);

            final TextView textView = getListTextView();
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);
            librariesWrapper.addView(textView);
        }
    }

    private void initTranslations(View rootView) {
        final LinearLayout translationsWrapper = (LinearLayout) rootView.findViewById(
                    R.id.translations_wrapper);

        for (final String[] translation : CreditsFragment.TRANSLATIONS) {
            final String language = translation[0];
            final String translators = translation[1];

            final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(
                language + ": " + translators);
            spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                                           language.length() + 2, 0);

            final TextView textView = getListTextView();
            textView.setText(spannableStringBuilder);

            translationsWrapper.addView(textView);
        }
    }

    private void initButtons(final View rootView) {
        // links
        final View buttonGitHub = rootView.findViewById(R.id.button_github);
        buttonGitHub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View fv) {
                Helpers.openURL(getActivity(), "https://github.com/MirakelX/mirakel-android/");
            }
        });

        final View buttonGPlus = rootView.findViewById(R.id.button_gplus);
        buttonGPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.openURL(getActivity(), "https://plus.google.com/u/0/communities/110640831388790835840");
            }
        });

        final View buttonFeedback = rootView.findViewById(R.id.button_feedback);
        buttonFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.contact(getActivity());
            }
        });
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.CREDITS;
    }
}
